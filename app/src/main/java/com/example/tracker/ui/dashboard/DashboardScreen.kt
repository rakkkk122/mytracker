package com.example.tracker.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.data.Expense
import com.example.tracker.data.Plan
import com.example.tracker.ui.money.MoneyViewModel
import com.example.tracker.ui.money.formatRupiah
import com.example.tracker.ui.plan.CopyPlanDatePicker
import com.example.tracker.ui.plan.PlanImage
import com.example.tracker.ui.plan.PlanViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IndonesianLocale = Locale("id", "ID")
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", IndonesianLocale)
private val WeekdayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

enum class DetailViewType {
    WEEKLY, MONTHLY
}

private fun Plan.localDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(startTime).atZone(zone).toLocalDate()

private fun greetingFor(hour: Int): String = when {
    hour < 11 -> "Selamat pagi"
    hour < 15 -> "Selamat siang"
    hour < 18 -> "Selamat sore"
    else -> "Selamat malam"
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h jam $m mnt"
        h > 0 -> "$h jam"
        else -> "$m menit"
    }
}

private fun formatWeekRange(start: LocalDate, end: LocalDate): String {
    return if (start.month == end.month && start.year == end.year) {
        "${start.dayOfMonth} - ${end.format(DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale))}"
    } else if (start.year == end.year) {
        val s = start.format(DateTimeFormatter.ofPattern("d MMM", IndonesianLocale))
        val e = end.format(DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale))
        "$s - $e"
    } else {
        val s = start.format(DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale))
        val e = end.format(DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale))
        "$s - $e"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.Factory),
    moneyViewModel: MoneyViewModel = viewModel(factory = MoneyViewModel.Factory)
) {
    val allPlans by viewModel.allPlansUiState.collectAsState()
    val allExpenses by moneyViewModel.allExpensesUiState.collectAsState()

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val nowMillis = System.currentTimeMillis()

    val todayPlans = allPlans.filter { it.localDate(zone) == today }
    val completed = todayPlans.count { it.isCompleted }
    val total = todayPlans.size

    val nextPlan = todayPlans.firstOrNull { !it.isCompleted && it.startTime >= nowMillis }

    val currentWeekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val plansByDay = allPlans.groupBy { it.localDate(zone) }

    // Minggu yang dipilih untuk statistik plan mingguan
    var selectedPlanWeekStart by remember { mutableStateOf(currentWeekStart) }
    val planWeekEnd = selectedPlanWeekStart.plusDays(6)
    val planWeekDays = (0..6).map { selectedPlanWeekStart.plusDays(it.toLong()) }
    val planWeekTotal = allPlans.count {
        val d = it.localDate(zone)
        !d.isBefore(selectedPlanWeekStart) && !d.isAfter(planWeekEnd)
    }
    val planWeekCompleted = allPlans.count {
        val d = it.localDate(zone)
        it.isCompleted && !d.isBefore(selectedPlanWeekStart) && !d.isAfter(planWeekEnd)
    }

    // Bulan yang dipilih untuk statistik plan bulanan
    var selectedStatsMonth by remember { mutableStateOf(YearMonth.now()) }

    val monthStart = selectedStatsMonth.atDay(1)
    val daysInMonth = (0 until selectedStatsMonth.lengthOfMonth()).map { monthStart.plusDays(it.toLong()) }
    val monthTotal = allPlans.count {
        val d = it.localDate(zone)
        d.year == selectedStatsMonth.year && d.month == selectedStatsMonth.month
    }
    val monthCompleted = allPlans.count {
        val d = it.localDate(zone)
        it.isCompleted && d.year == selectedStatsMonth.year && d.month == selectedStatsMonth.month
    }

    // Minggu yang dipilih untuk statistik pengeluaran mingguan
    var selectedExpenseWeekStart by remember { mutableStateOf(currentWeekStart) }
    val expenseWeekEnd = selectedExpenseWeekStart.plusDays(6)
    val expenseWeekDays = (0..6).map { selectedExpenseWeekStart.plusDays(it.toLong()) }
    val weekExpenses = allExpenses.filter {
        val d = Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate()
        !d.isBefore(selectedExpenseWeekStart) && !d.isAfter(expenseWeekEnd)
    }
    val weekExpenseTotal = weekExpenses.sumOf { it.amount }
    val weekExpenseCount = weekExpenses.size

    // Bulan yang dipilih untuk statistik pengeluaran (navigasi per bulan)
    var selectedExpenseMonth by remember { mutableStateOf(YearMonth.now()) }
    var activeExpenseDetailView by remember { mutableStateOf<DetailViewType?>(null) }

    // Perhitungan bulanan untuk pengeluaran (berdasarkan selectedExpenseMonth)
    val expenseMonthStart = selectedExpenseMonth.atDay(1)
    val daysInExpenseMonth = (0 until selectedExpenseMonth.lengthOfMonth()).map { expenseMonthStart.plusDays(it.toLong()) }
    val monthExpenses = allExpenses.filter {
        val d = Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate()
        d.year == selectedExpenseMonth.year && d.month == selectedExpenseMonth.month
    }
    val monthExpenseTotal = monthExpenses.sumOf { it.amount }
    val monthExpenseCount = monthExpenses.size

    // Perhitungan bulan sebelum selectedExpenseMonth
    val prevExpenseMonth = selectedExpenseMonth.minusMonths(1)
    val prevMonthExpenses = allExpenses.filter {
        val d = Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate()
        d.year == prevExpenseMonth.year && d.month == prevExpenseMonth.month
    }
    val prevMonthTotal = prevMonthExpenses.sumOf { it.amount }

    val expensePercentageChange = if (prevMonthTotal > 0) {
        ((monthExpenseTotal - prevMonthTotal) / prevMonthTotal) * 100.0
    } else if (monthExpenseTotal > 0) {
        100.0
    } else {
        0.0
    }

    var planToCopy by remember { mutableStateOf<Plan?>(null) }
    var planToDelete by remember { mutableStateOf<Plan?>(null) }
    var planDetail by remember { mutableStateOf<Plan?>(null) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    var activeDetailView by remember { mutableStateOf<DetailViewType?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                delay(600)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = greetingFor(LocalDateTime.now().hour),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = today.format(DateFormatter),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isRefreshing = true
                                delay(600)
                                isRefreshing = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Dashboard",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

        item {
            ProgressCard(completed = completed, total = total)
        }

        if (nextPlan != null) {
            item {
                NextPlanCard(
                    plan = nextPlan,
                    zone = zone,
                    nowMillis = nowMillis,
                    onClick = { planDetail = nextPlan }
                )
            }
        }

        item {
            WeekOverview(
                days = planWeekDays,
                today = today,
                weekStart = selectedPlanWeekStart,
                currentWeekStart = currentWeekStart,
                plansByDay = plansByDay,
                onPreviousWeek = { selectedPlanWeekStart = selectedPlanWeekStart.minusWeeks(1) },
                onNextWeek = { selectedPlanWeekStart = selectedPlanWeekStart.plusWeeks(1) },
                onResetToCurrentWeek = { selectedPlanWeekStart = currentWeekStart },
                onWeekClick = { activeDetailView = DetailViewType.WEEKLY }
            )
        }

        item {
            SummaryReportCard(
                weekTotal = planWeekTotal,
                weekCompleted = planWeekCompleted,
                monthTotal = monthTotal,
                monthCompleted = monthCompleted,
                selectedMonth = selectedStatsMonth,
                selectedWeekStart = selectedPlanWeekStart,
                currentWeekStart = currentWeekStart,
                onPreviousMonth = { selectedStatsMonth = selectedStatsMonth.minusMonths(1) },
                onNextMonth = { selectedStatsMonth = selectedStatsMonth.plusMonths(1) },
                onPreviousWeek = { selectedPlanWeekStart = selectedPlanWeekStart.minusWeeks(1) },
                onNextWeek = { selectedPlanWeekStart = selectedPlanWeekStart.plusWeeks(1) },
                onWeekClick = { activeDetailView = DetailViewType.WEEKLY },
                onMonthClick = { activeDetailView = DetailViewType.MONTHLY }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jadwal hari ini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (todayPlans.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = "${todayPlans.size} plan",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (todayPlans.isEmpty()) {
            item {
                Text(
                    text = "Belum ada plan hari ini. Buka tab Plan untuk menambahkan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(todayPlans, key = { it.id }) { plan ->
                PlanItem(
                    plan = plan,
                    onCheckedChange = { viewModel.togglePlanCompletion(plan) },
                    onCopy = { planToCopy = plan },
                    onClick = { planDetail = plan },
                    onImageClick = { previewImagePath = it }
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            FinanceDashboardCard(
                selectedMonth = selectedExpenseMonth,
                onPreviousMonth = { selectedExpenseMonth = selectedExpenseMonth.minusMonths(1) },
                onNextMonth = { selectedExpenseMonth = selectedExpenseMonth.plusMonths(1) },
                onResetToCurrentMonth = { selectedExpenseMonth = YearMonth.now() },
                selectedWeekStart = selectedExpenseWeekStart,
                currentWeekStart = currentWeekStart,
                onPreviousWeek = { selectedExpenseWeekStart = selectedExpenseWeekStart.minusWeeks(1) },
                onNextWeek = { selectedExpenseWeekStart = selectedExpenseWeekStart.plusWeeks(1) },
                onResetToCurrentWeek = { selectedExpenseWeekStart = currentWeekStart },
                weekTotal = weekExpenseTotal,
                weekCount = weekExpenseCount,
                monthTotal = monthExpenseTotal,
                monthCount = monthExpenseCount,
                percentageChange = expensePercentageChange,
                recentExpenses = monthExpenses.take(5),
                onWeekClick = { activeExpenseDetailView = DetailViewType.WEEKLY },
                onMonthClick = { activeExpenseDetailView = DetailViewType.MONTHLY }
            )
        }
    }
}

    if (activeExpenseDetailView != null) {
        val isWeekly = activeExpenseDetailView == DetailViewType.WEEKLY
        val isCurrentExpenseWeek = selectedExpenseWeekStart == currentWeekStart
        val title = if (isWeekly) {
            if (isCurrentExpenseWeek) "Rincian Pengeluaran Minggu Ini" else "Rincian Pengeluaran Mingguan"
        } else {
            "Rincian Pengeluaran Bulanan"
        }
        val subtitle = if (isWeekly) {
            formatWeekRange(selectedExpenseWeekStart, expenseWeekEnd)
        } else {
            selectedExpenseMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale))
        }
        val totalAmount = if (isWeekly) weekExpenseTotal else monthExpenseTotal
        val totalCount = if (isWeekly) weekExpenseCount else monthExpenseCount

        val expensesByDay = allExpenses.groupBy {
            Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate()
        }

        val groupedDays: List<Pair<LocalDate, List<Expense>>> = if (isWeekly) {
            expenseWeekDays.map { day -> day to (expensesByDay[day].orEmpty()) }
        } else {
            daysInExpenseMonth
                .filter { expensesByDay[it]?.isNotEmpty() == true }
                .map { day -> day to (expensesByDay[day].orEmpty()) }
        }

        ExpenseDetailBottomSheet(
            title = title,
            subtitle = subtitle,
            isWeekly = isWeekly,
            selectedMonth = selectedExpenseMonth,
            onPreviousMonth = { selectedExpenseMonth = selectedExpenseMonth.minusMonths(1) },
            onNextMonth = { selectedExpenseMonth = selectedExpenseMonth.plusMonths(1) },
            onResetToCurrentMonth = { selectedExpenseMonth = YearMonth.now() },
            selectedWeekStart = selectedExpenseWeekStart,
            currentWeekStart = currentWeekStart,
            onPreviousWeek = { selectedExpenseWeekStart = selectedExpenseWeekStart.minusWeeks(1) },
            onNextWeek = { selectedExpenseWeekStart = selectedExpenseWeekStart.plusWeeks(1) },
            onResetToCurrentWeek = { selectedExpenseWeekStart = currentWeekStart },
            groupedDays = groupedDays,
            totalAmount = totalAmount,
            totalCount = totalCount,
            onDismiss = { activeExpenseDetailView = null }
        )
    }

    if (activeDetailView != null) {
        val isWeekly = activeDetailView == DetailViewType.WEEKLY
        val isCurrentPlanWeek = selectedPlanWeekStart == currentWeekStart
        val title = if (isWeekly) {
            if (isCurrentPlanWeek) "Rencana Minggu Ini" else "Rencana Mingguan"
        } else {
            "Rencana Bulanan"
        }
        val subtitle = if (isWeekly) {
            formatWeekRange(selectedPlanWeekStart, planWeekEnd)
        } else {
            selectedStatsMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale))
        }
        val totalCount = if (isWeekly) planWeekTotal else monthTotal
        val completedCount = if (isWeekly) planWeekCompleted else monthCompleted

        val groupedDays: List<Pair<LocalDate, List<Plan>>> = if (isWeekly) {
            planWeekDays.map { day -> day to (plansByDay[day].orEmpty()) }
        } else {
            // Untuk bulanan, tampilkan tanggal di bulan terpilih yang memiliki jadwal plan
            daysInMonth
                .filter { plansByDay[it]?.isNotEmpty() == true }
                .map { day -> day to (plansByDay[day].orEmpty()) }
        }

        PlanDetailBottomSheet(
            title = title,
            subtitle = subtitle,
            isWeekly = isWeekly,
            selectedMonth = selectedStatsMonth,
            onPreviousMonth = { selectedStatsMonth = selectedStatsMonth.minusMonths(1) },
            onNextMonth = { selectedStatsMonth = selectedStatsMonth.plusMonths(1) },
            onResetToCurrentMonth = { selectedStatsMonth = YearMonth.now() },
            selectedWeekStart = selectedPlanWeekStart,
            currentWeekStart = currentWeekStart,
            onPreviousWeek = { selectedPlanWeekStart = selectedPlanWeekStart.minusWeeks(1) },
            onNextWeek = { selectedPlanWeekStart = selectedPlanWeekStart.plusWeeks(1) },
            onResetToCurrentWeek = { selectedPlanWeekStart = currentWeekStart },
            groupedDays = groupedDays,
            totalCount = totalCount,
            completedCount = completedCount,
            today = today,
            zone = zone,
            onDismiss = { activeDetailView = null },
            onToggleCompletion = { viewModel.togglePlanCompletion(it) },
            onCopy = { planToCopy = it },
            onPlanClick = { planDetail = it },
            onImageClick = { previewImagePath = it }
        )
    }

    if (planToCopy != null) {
        CopyPlanDatePicker(
            onDismiss = { planToCopy = null },
            onConfirm = { date ->
                viewModel.copyPlanToDate(planToCopy!!, date)
                planToCopy = null
            }
        )
    }

    planDetail?.let { plan ->
        PlanDetailDialog(
            plan = plan,
            zone = zone,
            onDismiss = { planDetail = null },
            onDelete = {
                planToDelete = plan
                planDetail = null
            },
            onToggleCompletion = {
                viewModel.togglePlanCompletion(plan)
                planDetail = plan.copy(isCompleted = !plan.isCompleted)
            },
            onCopy = {
                planToCopy = plan
                planDetail = null
            },
            onImageClick = { path -> previewImagePath = path }
        )
    }

    if (planToDelete != null) {
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text("Hapus Plan") },
            text = { Text("Apakah Anda yakin ingin menghapus plan '${planToDelete?.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        planToDelete?.let { viewModel.deletePlan(it) }
                        planToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
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

@Composable
private fun ProgressCard(completed: Int, total: Int) {
    val target = if (total > 0) completed.toFloat() / total else 0f
    val progress by animateFloatAsState(targetValue = target, label = "todayProgress")

    val colors = MaterialTheme.colorScheme
    val content = colors.onPrimaryContainer
    val headline = when {
        total == 0 -> "Belum ada plan"
        completed == total -> "Semua plan selesai"
        else -> "${total - completed} plan lagi"
    }
    val supporting = when {
        total == 0 -> "Hari ini masih kosong."
        completed == total -> "Kerja bagus, hari ini tuntas."
        else -> "$completed dari $total plan sudah selesai."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer))
                )
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(88.dp),
                        color = colors.primary,
                        trackColor = content.copy(alpha = 0.12f),
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = content.copy(alpha = 0.8f)
                    )
                    if (total > 0) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = content.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "$completed selesai",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = content,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = content.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${total - completed} tersisa",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = content,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextPlanCard(
    plan: Plan,
    zone: ZoneId,
    nowMillis: Long,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val start = Instant.ofEpochMilli(plan.startTime).atZone(zone).toLocalTime()
    val minutesLeft = ((plan.startTime - nowMillis) / 60_000).coerceAtLeast(0)
    val countdown = when {
        minutesLeft < 1 -> "Sekarang"
        minutesLeft < 60 -> "$minutesLeft menit lagi"
        else -> "${minutesLeft / 60} jam ${minutesLeft % 60} menit lagi"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Berikutnya",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onTertiaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onTertiaryContainer
                )
                if (plan.location.isNotBlank()) {
                    Text(
                        text = plan.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = start.format(TimeFormatter),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onTertiaryContainer
                )
                Text(
                    text = "Durasi: ${formatDuration(plan.durationMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onTertiaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = countdown,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun WeekOverview(
    days: List<LocalDate>,
    today: LocalDate,
    weekStart: LocalDate,
    currentWeekStart: LocalDate,
    plansByDay: Map<LocalDate, List<Plan>>,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onResetToCurrentWeek: () -> Unit,
    onWeekClick: () -> Unit
) {
    val weekEnd = weekStart.plusDays(6)
    val isCurrentWeek = weekStart == currentWeekStart

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onWeekClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onPreviousWeek,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Minggu sebelumnya",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isCurrentWeek) "Minggu ini" else "Plan Mingguan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatWeekRange(weekStart, weekEnd),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onNextWeek,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Minggu berikutnya",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Rincian",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (!isCurrentWeek) {
                Text(
                    text = "Kembali ke minggu ini",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 40.dp, bottom = 8.dp)
                        .clickable(onClick = onResetToCurrentWeek)
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                days.forEachIndexed { index, date ->
                    val dayPlans = plansByDay[date].orEmpty()
                    DayBar(
                        label = WeekdayLabels[index],
                        total = dayPlans.size,
                        done = dayPlans.count { it.isCompleted },
                        isToday = date == today,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayBar(
    label: String,
    total: Int,
    done: Int,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val fraction = if (total > 0) done.toFloat() / total else 0f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (total > 0) colors.surfaceContainerHighest
                    else colors.surfaceContainer
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .background(colors.primary)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) colors.primary else colors.onSurfaceVariant
        )
        Text(
            text = if (total > 0) "$done/$total" else "-",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryReportCard(
    weekTotal: Int,
    weekCompleted: Int,
    monthTotal: Int,
    monthCompleted: Int,
    selectedMonth: YearMonth,
    selectedWeekStart: LocalDate,
    currentWeekStart: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onWeekClick: () -> Unit,
    onMonthClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Statistik plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSecondaryContainer
                )
                Text(
                    text = "Ketuk untuk rincian",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val weekEnd = selectedWeekStart.plusDays(6)
                val isCurrentWeek = selectedWeekStart == currentWeekStart
                val weekLabel = if (isCurrentWeek) "Minggu ini" else "Mingguan"
                val weekSubtitle = formatWeekRange(selectedWeekStart, weekEnd)

                ReportItemCard(
                    label = weekLabel,
                    subtitle = weekSubtitle,
                    total = weekTotal,
                    completed = weekCompleted,
                    onClick = onWeekClick,
                    modifier = Modifier.weight(1f),
                    navigationControls = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onPreviousWeek,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Minggu lalu",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Rincian",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = onNextWeek,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Minggu kedepan",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )

                val monthLabel = if (selectedMonth == YearMonth.now()) "Bulan ini" else "Bulanan"
                val monthSubtitle = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale))

                ReportItemCard(
                    label = monthLabel,
                    subtitle = monthSubtitle,
                    total = monthTotal,
                    completed = monthCompleted,
                    onClick = onMonthClick,
                    modifier = Modifier.weight(1f),
                    navigationControls = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onPreviousMonth,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Bulan lalu",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Rincian",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = onNextMonth,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Bulan kedepan",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Kartu item statistik plan.
 * Menggunakan linear progress bar (horizontal)
 */
@Composable
private fun ReportItemCard(
    label: String,
    subtitle: String,
    total: Int,
    completed: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigationControls: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val target = if (total > 0) completed.toFloat() / total else 0f
    val progress by animateFloatAsState(targetValue = target, label = "reportProgress")
    val percent = if (total > 0) (completed * 100 / total) else 0

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar (menggantikan lingkaran di persen)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colors.primary,
                trackColor = colors.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "$completed dari $total selesai",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))

            if (navigationControls != null) {
                navigationControls()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rincian",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanDetailBottomSheet(
    title: String,
    subtitle: String,
    isWeekly: Boolean,
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToCurrentMonth: () -> Unit,
    selectedWeekStart: LocalDate = LocalDate.now(),
    currentWeekStart: LocalDate = LocalDate.now(),
    onPreviousWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    onResetToCurrentWeek: () -> Unit = {},
    groupedDays: List<Pair<LocalDate, List<Plan>>>,
    totalCount: Int,
    completedCount: Int,
    today: LocalDate,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onToggleCompletion: (Plan) -> Unit,
    onCopy: (Plan) -> Unit,
    onPlanClick: (Plan) -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filterOnlyCompleted by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            // Kontrol pemilihan minggu lalu dan kedepan jika tampilan mingguan
            if (isWeekly) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousWeek) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Minggu sebelumnya"
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val weekEnd = selectedWeekStart.plusDays(6)
                        Text(
                            text = formatWeekRange(selectedWeekStart, weekEnd),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedWeekStart == currentWeekStart) {
                            Text(
                                text = "Minggu Ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Ketuk untuk kembali ke minggu ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onResetToCurrentWeek)
                            )
                        }
                    }
                    IconButton(onClick = onNextWeek) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Minggu berikutnya"
                        )
                    }
                }
            } else {
                // Kontrol pemilihan bulan lalu dan kedepan jika tampilan bulanan
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Bulan sebelumnya"
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedMonth == YearMonth.now()) {
                            Text(
                                text = "Bulan Ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Ketuk untuk kembali ke bulan ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onResetToCurrentMonth)
                            )
                        }
                    }
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Bulan berikutnya"
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress Bar & Filter Chips
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pencapaian: $completedCount dari $totalCount selesai",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val percent = if (totalCount > 0) (completedCount * 100 / totalCount) else 0
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !filterOnlyCompleted,
                    onClick = { filterOnlyCompleted = false },
                    label = { Text("Semua ($totalCount)") }
                )
                FilterChip(
                    selected = filterOnlyCompleted,
                    onClick = { filterOnlyCompleted = true },
                    label = { Text("Selesai ($completedCount)") }
                )
            }

            Spacer(Modifier.height(12.dp))

            // List per Hari
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (groupedDays.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isWeekly) "Belum ada plan untuk minggu ini." else "Belum ada plan untuk bulan ${selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale))}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    groupedDays.forEach { (date, plans) ->
                        val displayedPlans = if (filterOnlyCompleted) {
                            plans.filter { it.isCompleted }
                        } else {
                            plans
                        }

                        // Untuk mingguan selalu tampilkan hari Senin-Minggu.
                        // Untuk bulanan, jika difilter "Hanya Selesai", tampilkan jika ada yang selesai.
                        if (isWeekly || displayedPlans.isNotEmpty()) {
                            item(key = date.toString()) {
                                DaySection(
                                    date = date,
                                    plans = displayedPlans,
                                    totalForDay = plans.size,
                                    completedForDay = plans.count { it.isCompleted },
                                    isToday = date == today,
                                    zone = zone,
                                    onToggleCompletion = onToggleCompletion,
                                    onCopy = onCopy,
                                    onPlanClick = onPlanClick,
                                    onImageClick = onImageClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySection(
    date: LocalDate,
    plans: List<Plan>,
    totalForDay: Int,
    completedForDay: Int,
    isToday: Boolean,
    zone: ZoneId,
    onToggleCompletion: (Plan) -> Unit,
    onCopy: (Plan) -> Unit,
    onPlanClick: (Plan) -> Unit,
    onImageClick: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val dayName = date.format(DateTimeFormatter.ofPattern("EEEE", IndonesianLocale))
    val dateText = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", IndonesianLocale))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (isToday) colors.primaryContainer.copy(alpha = 0.3f)
                else colors.surfaceContainerLow
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isToday) colors.primary else colors.onSurface
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            if (isToday) {
                Surface(
                    shape = CircleShape,
                    color = colors.primary,
                    contentColor = colors.onPrimary
                ) {
                    Text(
                        text = "Hari ini",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else if (totalForDay > 0) {
                Text(
                    text = "$completedForDay/$totalForDay selesai",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        if (totalForDay > 0) {
            LinearProgressIndicator(
                progress = { completedForDay.toFloat() / totalForDay },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = colors.primary,
                trackColor = colors.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )
        }

        if (plans.isEmpty()) {
            Text(
                text = "Tidak ada jadwal plan",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
            )
        } else {
            plans.forEach { plan ->
                DetailPlanCard(
                    plan = plan,
                    zone = zone,
                    onCheckedChange = { onToggleCompletion(plan) },
                    onCopy = { onCopy(plan) },
                    onClick = { onPlanClick(plan) },
                    onImageClick = onImageClick
                )
            }
        }
    }
}

@Composable
private fun DetailPlanCard(
    plan: Plan,
    zone: ZoneId,
    onCheckedChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onClick: () -> Unit = {},
    onImageClick: ((String) -> Unit)? = null
) {
    PlanRowCard(
        plan = plan,
        zone = zone,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        onCheckedChange = onCheckedChange,
        onCopy = onCopy,
        onClick = onClick,
        onImageClick = onImageClick
    )
}

@Composable
fun PlanItem(
    plan: Plan,
    onCheckedChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onClick: () -> Unit = {},
    onImageClick: ((String) -> Unit)? = null
) {
    PlanRowCard(
        plan = plan,
        zone = ZoneId.systemDefault(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        onCheckedChange = onCheckedChange,
        onCopy = onCopy,
        onClick = onClick,
        onImageClick = onImageClick
    )
}

/**
 * Kartu plan yang dipakai di daftar hari ini dan di bottom sheet rincian.
 * Hanya menampilkan waktu mulai dan durasi (tanpa waktu selesai).
 */
@Composable
private fun PlanRowCard(
    plan: Plan,
    zone: ZoneId,
    containerColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onClick: () -> Unit = {},
    onImageClick: ((String) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val start = Instant.ofEpochMilli(plan.startTime).atZone(zone).toLocalTime()
    val done = plan.isCompleted
    val hasImage = plan.imagePath != null
    val muted = colors.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (done) colors.surfaceContainerLow else containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasImage) Modifier.height(120.dp) else Modifier.heightIn(min = 96.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gambar memenuhi tinggi kartu; diredupkan kalau plan sudah selesai
            if (plan.imagePath != null) {
                Box(
                    modifier = Modifier
                        .width(104.dp)
                        .fillMaxHeight()
                        .then(
                            if (onImageClick != null) {
                                Modifier.clickable { onImageClick(plan.imagePath) }
                            } else Modifier
                        )
                ) {
                    PlanImage(
                        model = plan.imagePath,
                        contentDescription = "Gambar untuk ${plan.title}",
                        modifier = Modifier.fillMaxSize()
                    )
                    if (done) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.surface.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp)
            ) {
                // Waktu mulai + durasi
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (done) colors.surfaceContainerHighest else colors.primaryContainer,
                        contentColor = if (done) muted else colors.onPrimaryContainer
                    ) {
                        Text(
                            text = start.format(TimeFormatter),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = muted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = formatDuration(plan.durationMinutes),
                        style = MaterialTheme.typography.labelMedium,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    color = if (done) muted else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (plan.location.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = muted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = plan.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Aksi disusun vertikal agar teks punya ruang lebih
            Column(
                modifier = Modifier.padding(end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Checkbox(checked = done, onCheckedChange = onCheckedChange)
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Salin plan",
                        tint = muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** Rincian satu plan (dibuka dengan mengetuk kartu plan), lengkap dengan gambar, rincian, dan aksi. */
@Composable
private fun PlanDetailDialog(
    plan: Plan,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onToggleCompletion: () -> Unit,
    onCopy: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val dateTime = Instant.ofEpochMilli(plan.startTime).atZone(zone)
    val dateText = dateTime.format(DateFormatter)
    val startTimeText = dateTime.format(TimeFormatter)
    val endTimeText = dateTime.plusMinutes(plan.durationMinutes.toLong()).format(TimeFormatter)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (plan.imagePath != null) {
                    PlanImage(
                        model = plan.imagePath,
                        contentDescription = "Gambar untuk ${plan.title}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable { onImageClick(plan.imagePath) }
                    )
                }
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (plan.isCompleted) colors.primaryContainer else colors.surfaceContainerHighest,
                            contentColor = if (plan.isCompleted) colors.onPrimaryContainer else colors.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onToggleCompletion)
                        ) {
                            Text(
                                text = if (plan.isCompleted) "Selesai" else "Belum selesai",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "$dateText\nPukul $startTimeText – $endTimeText (${formatDuration(plan.durationMinutes)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )

                    if (plan.location.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = plan.location,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface
                            )
                        }
                    }

                    if (plan.alarmEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = colors.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Alarm pengingat aktif",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    if (plan.description.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceContainerLow
                        ) {
                            Text(
                                text = "Catatan: ${plan.description}",
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDelete) {
                            Text("Hapus", color = colors.error)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onCopy) {
                                Text("Salin")
                            }
                            Button(onClick = onDismiss) {
                                Text("Tutup")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceDashboardCard(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToCurrentMonth: () -> Unit,
    selectedWeekStart: LocalDate,
    currentWeekStart: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onResetToCurrentWeek: () -> Unit,
    weekTotal: Double,
    weekCount: Int,
    monthTotal: Double,
    monthCount: Int,
    percentageChange: Double,
    recentExpenses: List<Expense>,
    onWeekClick: () -> Unit,
    onMonthClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = colors.primary
                    )
                    Text(
                        text = "Ringkasan Keuangan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Control Pemilihan Bulan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Bulan sebelumnya"
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedMonth == YearMonth.now()) {
                        Text(
                            text = "Bulan Ini",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary
                        )
                    } else {
                        Text(
                            text = "Kembali ke bulan ini",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                            modifier = Modifier.clickable(onClick = onResetToCurrentMonth)
                        )
                    }
                }
                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Bulan berikutnya"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2 Stat Cards Side-by-Side: Mingguan & Bulanan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card Pengeluaran Mingguan
                val isCurrentExpenseWeek = selectedWeekStart == currentWeekStart
                val weekEnd = selectedWeekStart.plusDays(6)
                val weekTitle = if (isCurrentExpenseWeek) "Minggu Ini" else "Mingguan"
                val weekSubtitle = formatWeekRange(selectedWeekStart, weekEnd)

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onWeekClick),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = weekTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primary
                                )
                                Text(
                                    text = weekSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!isCurrentExpenseWeek) {
                                    Text(
                                        text = "Kembali ke minggu ini",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary,
                                        modifier = Modifier.clickable(onClick = onResetToCurrentWeek)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = formatRupiah(weekTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "$weekCount pengeluaran",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onPreviousWeek,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Minggu lalu",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Rincian",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = onNextWeek,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Minggu kedepan",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Card Pengeluaran Bulanan
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onMonthClick),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedMonth == YearMonth.now()) "Bulan Ini" else "Bulanan",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primary
                                )
                                Text(
                                    text = selectedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", IndonesianLocale)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = formatRupiah(monthTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "$monthCount pengeluaran",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onPreviousMonth,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Bulan lalu",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Rincian",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = onNextMonth,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Bulan kedepan",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Percentage change badge
            val (badgeBg, badgeFg, icon, pctText) = when {
                percentageChange > 0 -> PctBadgeData(
                    colors.errorContainer,
                    colors.onErrorContainer,
                    Icons.AutoMirrored.Filled.TrendingUp,
                    "+${String.format(Locale.US, "%.1f", percentageChange)}% dibanding bulan lalu"
                )
                percentageChange < 0 -> PctBadgeData(
                    colors.primaryContainer,
                    colors.onPrimaryContainer,
                    Icons.AutoMirrored.Filled.TrendingDown,
                    "${String.format(Locale.US, "%.1f", percentageChange)}% dibanding bulan lalu"
                )
                else -> PctBadgeData(
                    colors.surfaceContainerHigh,
                    colors.onSurfaceVariant,
                    Icons.AutoMirrored.Filled.TrendingFlat,
                    "0% dibanding bulan lalu"
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = pctText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg
                    )
                }
            }

            if (recentExpenses.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pengeluaran (${selectedMonth.format(DateTimeFormatter.ofPattern("MMM", IndonesianLocale))})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )
                    TextButton(onClick = onMonthClick) {
                        Text("Lihat Rincian")
                    }
                }
                Spacer(Modifier.height(4.dp))
                recentExpenses.forEach { expense ->
                    val zone = ZoneId.systemDefault()
                    val dateStr = Instant.ofEpochMilli(expense.dateMillis).atZone(zone).toLocalDate()
                        .format(DateTimeFormatter.ofPattern("d MMM", IndonesianLocale))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = expense.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$dateStr ${if (expense.reason.isNotBlank()) "• ${expense.reason}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = formatRupiah(expense.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDetailBottomSheet(
    title: String,
    subtitle: String,
    isWeekly: Boolean,
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToCurrentMonth: () -> Unit,
    selectedWeekStart: LocalDate = LocalDate.now(),
    currentWeekStart: LocalDate = LocalDate.now(),
    onPreviousWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    onResetToCurrentWeek: () -> Unit = {},
    groupedDays: List<Pair<LocalDate, List<Expense>>>,
    totalAmount: Double,
    totalCount: Int,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            // Week selector for weekly view
            if (isWeekly) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousWeek) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Minggu sebelumnya"
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val weekEnd = selectedWeekStart.plusDays(6)
                        Text(
                            text = formatWeekRange(selectedWeekStart, weekEnd),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedWeekStart == currentWeekStart) {
                            Text(
                                text = "Minggu Ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Ketuk untuk kembali ke minggu ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onResetToCurrentWeek)
                            )
                        }
                    }
                    IconButton(onClick = onNextWeek) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Minggu berikutnya"
                        )
                    }
                }
            } else {
                // Month selector for monthly view
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Bulan sebelumnya"
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedMonth == YearMonth.now()) {
                            Text(
                                text = "Bulan Ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Ketuk untuk kembali ke bulan ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onResetToCurrentMonth)
                            )
                        }
                    }
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Bulan berikutnya"
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Total Card Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Pengeluaran",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatRupiah(totalAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "$totalCount transaksi",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val hasData = groupedDays.any { it.second.isNotEmpty() }
            if (!hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada pengeluaran pada periode ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(groupedDays, key = { it.first.toString() }) { (day, dayExpenses) ->
                        if (dayExpenses.isNotEmpty()) {
                            val dayTotal = dayExpenses.sumOf { it.amount }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", IndonesianLocale)),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = formatRupiah(dayTotal),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                dayExpenses.forEach { expense ->
                                    val timeStr = Instant.ofEpochMilli(expense.dateMillis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalTime()
                                        .format(TimeFormatter)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = expense.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "$timeStr ${if (expense.reason.isNotBlank()) "• ${expense.reason}" else ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = formatRupiah(expense.amount),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PctBadgeData(
    val bg: Color,
    val fg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val text: String
)
