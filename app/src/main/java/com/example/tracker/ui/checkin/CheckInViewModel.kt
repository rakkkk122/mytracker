package com.example.tracker.ui.checkin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.TrackerApplication
import com.example.tracker.data.DailyCheckIn
import com.example.tracker.data.DailyCheckInRepository
import com.example.tracker.data.Expense
import com.example.tracker.data.ExpenseRepository
import com.example.tracker.data.ImageStorage
import com.example.tracker.data.Plan
import com.example.tracker.data.PlanRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class CheckInViewModel(
    private val checkInRepository: DailyCheckInRepository,
    private val planRepository: PlanRepository,
    private val expenseRepository: ExpenseRepository,
    private val imageStorage: ImageStorage
) : ViewModel() {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val today: LocalDate get() = LocalDate.now()
    private val todayStr: String get() = today.toString()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // Status check-in hari ini
    val todayCheckIn: StateFlow<DailyCheckIn?> =
        checkInRepository.getCheckInForDateStream(todayStr)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    // Habit/Plan aktif untuk hari ini
    val todayPlans: StateFlow<List<Plan>> =
        planRepository.getPlansForDayStream(
            startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli(),
            endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Map status check-in dan no-spend day per LocalDate untuk indikator kalender
    val allCheckInsMap: StateFlow<Map<LocalDate, DailyCheckIn>> =
        checkInRepository.getAllCheckInsStream()
            .map { list ->
                list.mapNotNull { checkIn ->
                    runCatching { LocalDate.parse(checkIn.dateStr) to checkIn }.getOrNull()
                }.toMap()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

    fun togglePlanCompletion(plan: Plan) {
        viewModelScope.launch {
            val updated = plan.copy(isCompleted = !plan.isCompleted)
            planRepository.updatePlan(updated)
        }
    }

    /**
     * Submit check-in lengkap
     */
    fun submitCheckIn(
        isNoSpendDay: Boolean,
        mood: String?,
        note: String?,
        newExpense: Expense? = null,
        pickedImage: Uri? = null
    ) {
        viewModelScope.launch {
            // Jika ada pencatatan pengeluaran baru bersamaan
            if (!isNoSpendDay && newExpense != null && newExpense.amount > 0) {
                val savedImagePath = pickedImage?.let { imageStorage.save(it) }
                expenseRepository.insertExpense(newExpense.copy(imagePath = savedImagePath))
            }

            val checkIn = DailyCheckIn(
                dateStr = todayStr,
                isCheckedIn = true,
                isNoSpendDay = isNoSpendDay,
                mood = mood,
                note = note?.takeIf { it.isNotBlank() },
                checkedInAt = System.currentTimeMillis()
            )
            checkInRepository.saveCheckIn(checkIn)
            _messages.tryEmit("Daily Check-in hari ini berhasil disimpan! ✨")
        }
    }

    /**
     * Tandai langsung No-Spend Day dari aksi cepat
     */
    fun markTodayNoSpendDay() {
        viewModelScope.launch {
            checkInRepository.markNoSpendDay(todayStr, isNoSpend = true)
            _messages.tryEmit("Hari ini ditandai sebagai No-Spend Day! 🛡️")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrackerApplication)
                val checkInRepository = application.container.dailyCheckInRepository
                val planRepository = application.container.planRepository
                val expenseRepository = application.container.expenseRepository
                val imageStorage = ImageStorage(application.applicationContext)
                CheckInViewModel(checkInRepository, planRepository, expenseRepository, imageStorage)
            }
        }
    }
}
