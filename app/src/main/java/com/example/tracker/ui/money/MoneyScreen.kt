package com.example.tracker.ui.money

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import java.time.temporal.WeekFields
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import com.example.tracker.data.Budget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.data.Expense
import com.example.tracker.ui.plan.PlanImage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IndonesianLocale = Locale("id", "ID")
private val WeekdayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val MonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale)
private val DayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", IndonesianLocale)

private val WeekdayInitials = listOf("S", "S", "R", "K", "J", "S", "M")
private val ShortDateFormatter = DateTimeFormatter.ofPattern("d MMM", IndonesianLocale)
private val AgendaGutter = 56.dp
private val WeekNumberGutter = 32.dp

/** Helper format rupiah dalam IDR */
fun formatRupiah(amount: Double): String {
    val formatter = DecimalFormat("#,###", DecimalFormatSymbols(IndonesianLocale))
    return "Rp ${formatter.format(amount.toLong())}"
}

/** Utility untuk memformat string input nominal secara otomatis dengan pemisah ribuan (mis. 1.000.000) */
fun formatAmountInput(rawInput: String): String {
    val cleanDigits = rawInput.filter { it.isDigit() }
    if (cleanDigits.isEmpty()) return ""
    val number = cleanDigits.toLongOrNull() ?: return cleanDigits
    val formatter = DecimalFormat("#,###", DecimalFormatSymbols(IndonesianLocale))
    return formatter.format(number)
}

/** Utility untuk mengambil nilai Double dari string input nominal yang terformat */
fun parseAmountInput(formattedInput: String): Double {
    val cleanDigits = formattedInput.filter { it.isDigit() }
    return cleanDigits.toDoubleOrNull() ?: 0.0
}

data class NewExpenseInput(
    val title: String,
    val amount: Double,
    val reason: String,
    val time: LocalTime,
    val imageUri: Uri?
)

@Composable
fun MoneyScreen(
    viewModel: MoneyViewModel = viewModel(factory = MoneyViewModel.Factory)
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val visibleMonth by viewModel.visibleMonth.collectAsState()
    val allExpenses by viewModel.allExpensesUiState.collectAsState()
    val dailyTotal by viewModel.dailyTotal.collectAsState()
    val expenseCounts by viewModel.expenseCountByDate.collectAsState()

    val budgetCounter by viewModel.budgetUpdateCounter.collectAsState()
    val visibleMonthBudget = remember(visibleMonth, budgetCounter) { viewModel.getBudgetForMonth(visibleMonth) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }

    val monthTotalExpenses = remember(allExpenses, visibleMonth) {
        val z = ZoneId.systemDefault()
        allExpenses.filter {
            val date = Instant.ofEpochMilli(it.dateMillis).atZone(z).toLocalDate()
            YearMonth.from(date) == visibleMonth
        }.sumOf { it.amount }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var expenseDetail by remember { mutableStateOf<Expense?>(null) }
    var calendarExpanded by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val zone = ZoneId.systemDefault()
    val agendaItems = remember(allExpenses, visibleMonth, selectedDate) {
        buildAgenda(allExpenses, visibleMonth, selectedDate, zone)
    }
    val listState = rememberLazyListState()

    // Daftar agenda ikut bergulir ke tanggal yang dipilih di kalender
    LaunchedEffect(selectedDate, visibleMonth, agendaItems.isEmpty()) {
        if (agendaItems.isEmpty()) return@LaunchedEffect
        val dayIndex = agendaItems.indexOfFirst { it is AgendaItem.Day && it.date == selectedDate }
        val target = when {
            dayIndex < 0 -> 0
            // belum ada hari lain di atasnya: tampilkan dari ringkasan bulan
            agendaItems.take(dayIndex).none { it is AgendaItem.Day } -> 0
            // sertakan judul minggu bila ada
            agendaItems[dayIndex - 1] is AgendaItem.WeekHeader -> dayIndex - 1
            else -> dayIndex
        }
        listState.animateScrollToItem(target)
    }

    // Pindah bulan: pilih hari ini (kalau bulan berjalan) atau tanggal 1
    val selectForMonth: (YearMonth) -> Unit = { m ->
        val today = LocalDate.now()
        viewModel.selectDate(if (m == YearMonth.from(today)) today else m.atDay(1))
    }
    val onPrevious: () -> Unit = {
        if (calendarExpanded) selectForMonth(visibleMonth.minusMonths(1))
        else viewModel.selectDate(selectedDate.minusWeeks(1))
    }
    val onNext: () -> Unit = {
        if (calendarExpanded) selectForMonth(visibleMonth.plusMonths(1))
        else viewModel.selectDate(selectedDate.plusWeeks(1))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah pengeluaran")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Header + kalender satu blok dengan bayangan di bawahnya
            Surface(
                modifier = Modifier.zIndex(1f),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 16.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Keuangan",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Total Hari Ini: ${formatRupiah(dailyTotal)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    MonthCalendar(
                        month = visibleMonth,
                        selectedDate = selectedDate,
                        expenseCounts = expenseCounts,
                        expanded = calendarExpanded,
                        onToggleExpanded = { calendarExpanded = !calendarExpanded },
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onToday = viewModel::goToToday,
                        onSelectDate = viewModel::selectDate
                    )

                    MonthlyBudgetCard(
                        month = visibleMonth,
                        budget = visibleMonthBudget,
                        monthTotal = monthTotalExpenses,
                        onSetBudgetClick = { showSetBudgetDialog = true }
                    )
                }
            }

            if (agendaItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada pengeluaran di bulan ini.\nKetuk tombol + untuk mencatat transaksi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
                ) {
                    items(agendaItems, key = { it.key }) { item ->
                        when (item) {
                            is AgendaItem.MonthSummary -> MonthSummaryRow(item)
                            is AgendaItem.WeekHeader -> WeekHeaderRow(item.weekStart)
                            is AgendaItem.Day -> DayAgenda(
                                date = item.date,
                                expenses = item.expenses,
                                isSelected = item.date == selectedDate,
                                onDayClick = { viewModel.selectDate(item.date) },
                                onExpenseClick = { expenseDetail = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSetBudgetDialog) {
        SetBudgetDialog(
            selectedMonth = visibleMonth,
            currentBudget = visibleMonthBudget,
            onDismiss = { showSetBudgetDialog = false },
            onConfirm = { newAmount, startDate, endDate ->
                viewModel.setBudgetForMonth(visibleMonth, newAmount, startDate, endDate)
                showSetBudgetDialog = false
            }
        )
    }

    if (showAddDialog) {
        AddExpenseDialog(
            selectedDate = selectedDate,
            onDismiss = { showAddDialog = false },
            onConfirm = { input ->
                val dateMillis = LocalDateTime.of(selectedDate, input.time)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()

                viewModel.addExpense(
                    expense = Expense(
                        title = input.title,
                        amount = input.amount,
                        reason = input.reason,
                        dateMillis = dateMillis
                    ),
                    pickedImage = input.imageUri
                )
                showAddDialog = false
            }
        )
    }

    expenseDetail?.let { expense ->
        ExpenseDetailDialog(
            expense = expense,
            onDismiss = { expenseDetail = null },
            onDelete = {
                expenseToDelete = expense
                expenseDetail = null
            },
            onImageClick = { path -> previewImagePath = path }
        )
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Hapus Pengeluaran") },
            text = { Text("Apakah Anda yakin ingin menghapus pengeluaran '${expenseToDelete?.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it) }
                        expenseToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (previewImagePath != null) {
        Dialog(onDismissRequest = { previewImagePath = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                PlanImage(
                    model = previewImagePath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentDescription = "Preview Gambar"
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Agenda (data)
// ---------------------------------------------------------------------------

private sealed interface AgendaItem {
    val key: String

    data class MonthSummary(val month: YearMonth, val count: Int, val total: Double) : AgendaItem {
        override val key: String get() = "summary_$month"
    }

    data class WeekHeader(val weekStart: LocalDate) : AgendaItem {
        override val key: String get() = "week_$weekStart"
    }

    data class Day(val date: LocalDate, val expenses: List<Expense>) : AgendaItem {
        override val key: String get() = "day_$date"
    }
}

/**
 * Susun daftar agenda untuk satu bulan: ringkasan bulan, judul minggu, lalu hari-hari
 * yang punya pengeluaran. Tanggal terpilih ikut tampil (kosong) supaya kalender dan daftar selaras.
 */
private fun buildAgenda(
    allExpenses: List<Expense>,
    month: YearMonth,
    selectedDate: LocalDate,
    zone: ZoneId
): List<AgendaItem> {
    val grouped: Map<LocalDate, List<Expense>> = allExpenses
        .map { it to Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate() }
        .filter { (_, date) -> YearMonth.from(date) == month }
        .groupBy({ it.second }, { it.first })

    if (grouped.isEmpty()) return emptyList()

    val dates = grouped.keys.toMutableSet()
    if (YearMonth.from(selectedDate) == month) dates.add(selectedDate)

    val all = grouped.values.flatten()
    val items = mutableListOf<AgendaItem>(
        AgendaItem.MonthSummary(month, all.size, all.sumOf { it.amount })
    )

    var lastWeekStart: LocalDate? = null
    dates.sorted().forEach { date ->
        val weekStart = date.minusDays((date.dayOfWeek.value - 1).toLong())
        if (weekStart != lastWeekStart) {
            items += AgendaItem.WeekHeader(weekStart)
            lastWeekStart = weekStart
        }
        items += AgendaItem.Day(date, grouped[date].orEmpty().sortedBy { it.dateMillis })
    }
    return items
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    expenseCounts: Map<LocalDate, Int>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "calendarArrow"
    )
    val currentPrevious by rememberUpdatedState(onPrevious)
    val currentNext by rememberUpdatedState(onNext)

    val monthName = month.format(DateTimeFormatter.ofPattern("MMMM", IndonesianLocale))
        .replaceFirstChar { it.uppercase() }
    val title = if (month.year != today.year) "$monthName ${month.year}" else monthName

    // Minggu yang ditampilkan: seluruh bulan (diperluas) atau hanya minggu terpilih (diciutkan)
    val first = month.atDay(1)
    val leadingBlanks = first.dayOfWeek.value - 1
    val gridStart = first.minusDays(leadingBlanks.toLong())
    val rowCount = (leadingBlanks + month.lengthOfMonth() + 6) / 7
    val selectedWeekStart = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
    val weekStarts = if (expanded) {
        (0 until rowCount).map { gridStart.plusWeeks(it.toLong()) }
    } else {
        listOf(selectedWeekStart)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // Judul bulan (ketuk untuk ciutkan/perluas) + navigasi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Ciutkan kalender" else "Perluas kalender",
                    modifier = Modifier.rotate(arrowRotation)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = if (expanded) "Bulan sebelumnya" else "Minggu sebelumnya"
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (expanded) "Bulan berikutnya" else "Minggu berikutnya"
                )
            }
            // Tombol "Hari ini": ikon kalender berisi tanggal hari ini
            IconButton(onClick = onToday) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.5.dp, colors.onSurfaceVariant, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = today.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        // Label hari, dengan lajur kosong untuk nomor minggu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Spacer(Modifier.width(WeekNumberGutter))
            WeekdayInitials.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Grid tanggal: geser kiri/kanan untuk pindah bulan (atau minggu)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .animateContentSize()
                .pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (total <= -80f) currentNext()
                            else if (total >= 80f) currentPrevious()
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            total += amount
                        }
                    )
                }
        ) {
            weekStarts.forEach { weekStart ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = weekStart.get(WeekFields.ISO.weekOfWeekBasedYear()).toString(),
                        modifier = Modifier.width(WeekNumberGutter),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    for (col in 0 until 7) {
                        val date = weekStart.plusDays(col.toLong())
                        val inMonth = YearMonth.from(date) == month
                        if (expanded && !inMonth) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            )
                        } else {
                            DayCell(
                                day = date.dayOfMonth,
                                isSelected = date == selectedDate,
                                isToday = date == today,
                                inMonth = inMonth,
                                hasExpenses = (expenseCounts[date] ?: 0) > 0,
                                onClick = { onSelectDate(date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    inMonth: Boolean,
    hasExpenses: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val textColor = when {
        isSelected -> colors.onPrimary
        isToday -> colors.primary
        !inMonth -> colors.onSurfaceVariant.copy(alpha = 0.4f)
        else -> colors.onSurface
    }

    Column(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSelected) colors.primary else Color.Transparent)
                .then(
                    if (isToday && !isSelected) Modifier.border(1.5.dp, colors.primary, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }
        Spacer(Modifier.height(2.dp))
        // Titik penanda di bawah angka (ruangnya tetap disediakan agar baris sejajar)
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (hasExpenses) colors.error else Color.Transparent)
        )
    }
}

// ---------------------------------------------------------------------------
// Agenda (tampilan)
// ---------------------------------------------------------------------------

@Composable
private fun MonthSummaryRow(item: AgendaItem.MonthSummary) {
    val colors = MaterialTheme.colorScheme
    val monthName = item.month.format(DateTimeFormatter.ofPattern("MMMM", IndonesianLocale))
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$monthName  •  ${item.count} pengeluaran",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatRupiah(item.total),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )
    }
}

@Composable
private fun WeekHeaderRow(weekStart: LocalDate) {
    val weekEnd = weekStart.plusDays(6)
    val weekNumber = weekStart.get(WeekFields.ISO.weekOfWeekBasedYear())
    val range = if (YearMonth.from(weekStart) == YearMonth.from(weekEnd)) {
        "${weekStart.dayOfMonth} – ${weekEnd.format(ShortDateFormatter)}"
    } else {
        "${weekStart.format(ShortDateFormatter)} – ${weekEnd.format(ShortDateFormatter)}"
    }

    Text(
        text = "Minggu ke-$weekNumber  •  $range",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(
            start = 16.dp + AgendaGutter,
            end = 16.dp,
            top = 14.dp,
            bottom = 2.dp
        )
    )
}

/** Satu hari di agenda: angka tanggal di kiri, blok pengeluaran di kanan. */
@Composable
private fun DayAgenda(
    date: LocalDate,
    expenses: List<Expense>,
    isSelected: Boolean,
    onDayClick: () -> Unit,
    onExpenseClick: (Expense) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isToday = date == LocalDate.now()
    val dayName = date.format(DateTimeFormatter.ofPattern("EEE", IndonesianLocale))
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(AgendaGutter)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onDayClick)
                .padding(vertical = 2.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected || isToday) colors.primary else colors.onSurface
            )
            Text(
                text = dayName,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (expenses.isEmpty()) {
                Text(
                    text = "Tidak ada pengeluaran",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else {
                expenses.forEach { expense ->
                    ExpenseBlock(
                        expense = expense,
                        emphasized = isSelected,
                        onClick = { onExpenseClick(expense) }
                    )
                }
                if (expenses.size >= 2) {
                    Text(
                        text = "Total ${formatRupiah(expenses.sumOf { it.amount })}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Blok berwarna seperti event di kalender: judul, jam, nominal, dan avatar gambar bila ada. */
@Composable
private fun ExpenseBlock(
    expense: Expense,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container = if (emphasized) colors.primary else colors.secondaryContainer
    val content = if (emphasized) colors.onPrimary else colors.onSecondaryContainer
    val time = Instant.ofEpochMilli(expense.dateMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(TimeFormatter)
    val subtitle = if (expense.reason.isNotBlank()) "$time  •  ${expense.reason}" else time

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatRupiah(expense.amount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = content
        )
        if (expense.imagePath != null) {
            Spacer(Modifier.width(8.dp))
            PlanImage(
                model = expense.imagePath,
                contentDescription = "Gambar pengeluaran",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
        }
    }
}

/** Rincian satu pengeluaran (dibuka dengan mengetuk blok), lengkap dengan tombol hapus. */
@Composable
private fun ExpenseDetailDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val dateTime = Instant.ofEpochMilli(expense.dateMillis).atZone(ZoneId.systemDefault())
    val dateText = dateTime.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", IndonesianLocale))
    val timeText = dateTime.format(TimeFormatter)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                if (expense.imagePath != null) {
                    PlanImage(
                        model = expense.imagePath,
                        contentDescription = "Gambar pengeluaran",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable { onImageClick(expense.imagePath) }
                    )
                }
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$dateText, pukul $timeText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = formatRupiah(expense.amount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.error
                    )
                    if (expense.reason.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceContainerLow
                        ) {
                            Text(
                                text = "Alasan: ${expense.reason}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDelete) {
                            Text("Hapus", color = colors.error)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onDismiss) { Text("Tutup") }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(
    expense: Expense,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val timeStr = Instant.ofEpochMilli(expense.dateMillis).atZone(zone).toLocalTime().format(TimeFormatter)
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh)
    ) {
        Column {
            if (expense.imagePath != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable { onImageClick(expense.imagePath) }
                ) {
                    PlanImage(
                        model = expense.imagePath,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "Gambar pengeluaran"
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = expense.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = colors.onSurfaceVariant
                            )
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatRupiah(expense.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.error
                        )
                    }
                }

                if (expense.reason.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceContainerLow
                    ) {
                        Text(
                            text = "Alasan: ${expense.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus pengeluaran",
                            tint = colors.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (NewExpenseInput) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    val initialTime = LocalTime.now()
    var selectedTime by remember { mutableStateOf(initialTime) }
    var showTimePicker by remember { mutableStateOf(false) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tambah Pengeluaran",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Pengeluaran (mis. Makan, Bensin)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = formatAmountInput(input) },
                    label = { Text("Biaya (Rupiah)") },
                    leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Contoh: 100.000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Input Waktu (Tanpa Alarm)
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Waktu: ${selectedTime.format(TimeFormatter)}")
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan / Catatan (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(Modifier.height(16.dp))

                // Pilihan Lampiran Gambar
                if (imageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        PlanImage(
                            model = imageUri,
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = "Gambar terpilih"
                        )
                        IconButton(
                            onClick = { imageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Gambar", tint = Color.White)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tambah Gambar (Opsional)")
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = parseAmountInput(amountText)
                            if (title.isNotBlank() && amount > 0) {
                                onConfirm(
                                    NewExpenseInput(
                                        title = title,
                                        amount = amount,
                                        reason = reason,
                                        time = selectedTime,
                                        imageUri = imageUri
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank() && parseAmountInput(amountText) > 0
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Batal")
                }
            },
            text = {
                TimePicker(state = timeState)
            }
        )
    }
}

@Composable
private fun MonthlyBudgetCard(
    month: YearMonth,
    budget: Budget,
    monthTotal: Double,
    onSetBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val monthName = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale))
    val dateRangeStr = "${budget.startDate.format(ShortDateFormatter)} - ${budget.endDate.format(ShortDateFormatter)}"

    var expanded by rememberSaveable { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "budgetCardArrow"
    )

    val hasBudget = budget.amount > 0
    val ratio = if (hasBudget) (monthTotal / budget.amount).coerceIn(0.0, 1.0).toFloat() else 0f
    val pct = if (hasBudget) ((monthTotal / budget.amount) * 100).toInt() else 0
    val savedAmount = if (hasBudget && monthTotal < budget.amount) budget.amount - monthTotal else 0.0
    val isNearBudget = hasBudget && monthTotal >= 0.8 * budget.amount && monthTotal < budget.amount
    val isOverBudget = hasBudget && monthTotal >= budget.amount

    val progressColor = when {
        isOverBudget -> colors.error
        isNearBudget -> Color(0xFFE65100)
        else -> colors.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverBudget -> colors.errorContainer.copy(alpha = 0.5f)
                isNearBudget -> Color(0xFFFFF3E0)
                else -> colors.surfaceContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = progressColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Budget $monthName",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Periode: $dateRangeStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Minimize Budget" else "Perluas Budget",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
                    )
                }

                OutlinedButton(
                    onClick = onSetBudgetClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (hasBudget) "Edit Budget" else "Atur Budget",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (!expanded) {
                // Tampilan Minimized (Ringkas)
                if (hasBudget) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatRupiah(monthTotal)} / ${formatRupiah(budget.amount)} ($pct%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant
                        )
                        if (isOverBudget) {
                            Text(
                                text = "🚨 Over ${formatRupiah(monthTotal - budget.amount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.error,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (isNearBudget) {
                            Text(
                                text = "⚠️ Hampir Habis",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Sisa: ${formatRupiah(savedAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Tampilan Expanded (Lengkap)
                Spacer(Modifier.height(8.dp))

                if (!hasBudget) {
                    Text(
                        text = "Belum ada budget periode ini. Ketuk 'Atur Budget' untuk memasukkan batas pengeluaran.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Terpakai: ${formatRupiah(monthTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                            Text(
                                text = "Budget: ${formatRupiah(budget.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$pct% terpakai",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = progressColor
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = progressColor,
                        trackColor = colors.surfaceContainerHighest
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOverBudget) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.error
                            ) {
                                Text(
                                    text = "🚨 Melebihi budget! Total: ${formatRupiah(monthTotal)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onError,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else if (isNearBudget) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE65100)
                            ) {
                                Text(
                                    text = "⚠️ Hampir mencapai batas budget!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.primaryContainer
                            ) {
                                Text(
                                    text = "Disimpan dari Budget: ${formatRupiah(savedAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetDialog(
    selectedMonth: YearMonth,
    currentBudget: Budget,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, startDate: LocalDate, endDate: LocalDate) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (currentBudget.amount > 0) formatAmountInput(currentBudget.amount.toLong().toString()) else "")
    }
    var startDate by remember { mutableStateOf(currentBudget.startDate) }
    var endDate by remember { mutableStateOf(currentBudget.endDate) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }

    val monthName = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Atur Budget ($monthName)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tentukan batas pengeluaran dan periode tanggal budget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = formatAmountInput(input)
                        errorText = null
                    },
                    label = { Text("Budget (Rp)") },
                    leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Contoh: 3.000.000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorText != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Periode Tanggal Budget:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickingStartDate = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Awal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(startDate.format(ShortDateFormatter), fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { pickingEndDate = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Akhir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(endDate.format(ShortDateFormatter), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = parseAmountInput(amountText)
                    if (parsed <= 0 && amountText.isNotBlank()) {
                        errorText = "Masukkan nilai budget yang valid."
                    } else if (startDate.isAfter(endDate)) {
                        errorText = "Tanggal awal tidak boleh setelah tanggal akhir."
                    } else {
                        onConfirm(parsed, startDate, endDate)
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (pickingStartDate) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingStartDate = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        errorText = null
                    }
                    pickingStartDate = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartDate = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (pickingEndDate) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingEndDate = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        endDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        errorText = null
                    }
                    pickingEndDate = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingEndDate = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}