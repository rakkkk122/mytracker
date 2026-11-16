package com.example.tracker.ui.plan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.TrackerApplication
import com.example.tracker.data.ImageStorage
import com.example.tracker.data.Plan
import com.example.tracker.data.PlanRepository
import com.example.tracker.reminder.AlarmScheduler
import com.example.tracker.reminder.AndroidAlarmScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModel(
    private val planRepository: PlanRepository,
    private val alarmScheduler: AlarmScheduler,
    private val imageStorage: ImageStorage
) : ViewModel() {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Pesan sekali tampil untuk UI (mis. gambar gagal disimpan)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    /** Seluruh plan, dipakai oleh Dashboard. */
    val allPlansUiState: StateFlow<List<Plan>> =
        planRepository.getAllPlansStream()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** Plan pada tanggal yang sedang dipilih. Otomatis berganti saat tanggal berubah. */
    val plansForSelectedDate: StateFlow<List<Plan>> =
        _selectedDate
            .flatMapLatest { date ->
                planRepository.getPlansForDayStream(
                    startOfDay = date.startMillis(),
                    endOfDay = date.plusDays(1).startMillis()
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** Jumlah plan per tanggal, dipakai untuk titik penanda di kalender. */
    val planCountByDate: StateFlow<Map<LocalDate, Int>> =
        planRepository.getAllPlansStream()
            .map { plans ->
                plans.groupingBy {
                    Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate()
                }.eachCount()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _visibleMonth.value = YearMonth.from(date)
    }

    fun showPreviousMonth() {
        _visibleMonth.value = _visibleMonth.value.minusMonths(1)
    }

    fun showNextMonth() {
        _visibleMonth.value = _visibleMonth.value.plusMonths(1)
    }

    fun goToToday() = selectDate(LocalDate.now())

    /**
     * [pickedImage] adalah URI sementara dari galeri; disalin ke penyimpanan
     * internal dulu, lalu path-nya disimpan di plan.
     */
    fun addPlan(plan: Plan, pickedImage: Uri? = null) {
        viewModelScope.launch {
            val savedPath = pickedImage?.let { imageStorage.save(it) }
            if (pickedImage != null && savedPath == null) {
                _messages.tryEmit("Gambar gagal disimpan, plan disimpan tanpa gambar.")
            }
            val toInsert = plan.copy(imagePath = savedPath)
            val id = planRepository.insertPlan(toInsert)
            val newPlan = toInsert.copy(id = id.toInt())
            if (newPlan.alarmEnabled) {
                alarmScheduler.schedule(newPlan)
            }
        }
    }

    fun copyPlanToDate(plan: Plan, targetDate: LocalDate) {
        viewModelScope.launch {
            val oldStartTime = Instant.ofEpochMilli(plan.startTime).atZone(zone).toLocalTime()
            val newStartTime = LocalDateTime.of(targetDate, oldStartTime)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()

            val newImagePath = plan.imagePath?.let { imageStorage.copyImageFile(it) }

            val copiedPlan = plan.copy(
                id = 0,
                startTime = newStartTime,
                imagePath = newImagePath,
                isCompleted = false
            )
            val id = planRepository.insertPlan(copiedPlan)
            val savedPlan = copiedPlan.copy(id = id.toInt())

            if (savedPlan.alarmEnabled && savedPlan.startTime > System.currentTimeMillis()) {
                alarmScheduler.schedule(savedPlan)
            }
            _messages.tryEmit("Plan berhasil disalin ke ${targetDate.dayOfMonth}/${targetDate.monthValue}")
        }
    }

    fun deletePlan(plan: Plan) {
        viewModelScope.launch {
            planRepository.deletePlan(plan)
            alarmScheduler.cancel(plan)
            imageStorage.delete(plan.imagePath)
        }
    }

    fun togglePlanCompletion(plan: Plan) {
        viewModelScope.launch {
            val updatedPlan = plan.copy(isCompleted = !plan.isCompleted)
            planRepository.updatePlan(updatedPlan)
            if (updatedPlan.isCompleted) {
                alarmScheduler.cancel(updatedPlan)
            } else if (updatedPlan.alarmEnabled && updatedPlan.startTime > System.currentTimeMillis()) {
                // Plan dibuka kembali dan belum lewat waktunya: jadwalkan ulang alarm
                alarmScheduler.schedule(updatedPlan)
            }
        }
    }

    private fun LocalDate.startMillis(): Long =
        atStartOfDay(zone).toInstant().toEpochMilli()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrackerApplication)
                val planRepository = application.container.planRepository
                val alarmScheduler = AndroidAlarmScheduler(application.applicationContext)
                val imageStorage = ImageStorage(application.applicationContext)
                PlanViewModel(planRepository, alarmScheduler, imageStorage)
            }
        }
    }
}