package com.example.tracker.ui.money

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tracker.TrackerApplication
import com.example.tracker.data.Expense
import com.example.tracker.data.ExpenseRepository
import com.example.tracker.data.ImageStorage
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
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyViewModel(
    private val expenseRepository: ExpenseRepository,
    private val imageStorage: ImageStorage
) : ViewModel() {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Semua data pengeluaran */
    val allExpensesUiState: StateFlow<List<Expense>> =
        expenseRepository.getAllExpensesStream()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** Pengeluaran pada tanggal terpilih */
    val expensesForSelectedDate: StateFlow<List<Expense>> =
        _selectedDate
            .flatMapLatest { date ->
                expenseRepository.getExpensesForDayStream(
                    startOfDay = date.startMillis(),
                    endOfDay = date.plusDays(1).startMillis()
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** Total biaya pengeluaran pada tanggal terpilih */
    val dailyTotal: StateFlow<Double> =
        expensesForSelectedDate
            .map { list -> list.sumOf { it.amount } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0.0
            )

    /** Jumlah pengeluaran per tanggal (untuk titik/penanda di kalender) */
    val expenseCountByDate: StateFlow<Map<LocalDate, Int>> =
        expenseRepository.getAllExpensesStream()
            .map { list ->
                list.groupingBy {
                    Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate()
                }.eachCount()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

    /** Total biaya pengeluaran per tanggal */
    val expenseSumByDate: StateFlow<Map<LocalDate, Double>> =
        expenseRepository.getAllExpensesStream()
            .map { list ->
                list.groupBy {
                    Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate()
                }.mapValues { entry ->
                    entry.value.sumOf { it.amount }
                }
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

    fun addExpense(expense: Expense, pickedImage: Uri? = null) {
        viewModelScope.launch {
            val savedPath = pickedImage?.let { imageStorage.save(it) }
            if (pickedImage != null && savedPath == null) {
                _messages.tryEmit("Gambar gagal disimpan, pengeluaran disimpan tanpa gambar.")
            }
            val toInsert = expense.copy(imagePath = savedPath)
            expenseRepository.insertExpense(toInsert)
            _messages.tryEmit("Pengeluaran berhasil ditambahkan.")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
            imageStorage.delete(expense.imagePath)
            _messages.tryEmit("Pengeluaran berhasil dihapus.")
        }
    }

    private fun LocalDate.startMillis(): Long =
        atStartOfDay(zone).toInstant().toEpochMilli()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrackerApplication)
                val expenseRepository = application.container.expenseRepository
                val imageStorage = ImageStorage(application.applicationContext)
                MoneyViewModel(expenseRepository, imageStorage)
            }
        }
    }
}
