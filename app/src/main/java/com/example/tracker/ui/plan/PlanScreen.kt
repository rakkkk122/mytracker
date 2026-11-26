package com.example.tracker.ui.plan

import android.Manifest
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.data.DailyCheckIn
import com.example.tracker.data.Plan
import com.example.tracker.data.UserPreferences
import com.example.tracker.reminder.AlarmPermissionUtils
import com.example.tracker.reminder.AlarmTones
import com.example.tracker.ui.checkin.CheckInViewModel
import com.example.tracker.ui.common.rememberTonePicker
import java.time.Duration
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

/** Data yang diisi pengguna di dialog plan baru. */
data class NewPlanInput(
    val title: String,
    val description: String,
    val location: String,
    val start: LocalTime,
    val end: LocalTime,
    val alarmEnabled: Boolean,
    val alarmTone: String?,
    val imageUri: Uri?
)

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.Factory),
    checkInViewModel: CheckInViewModel = viewModel(factory = CheckInViewModel.Factory)
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val visibleMonth by viewModel.visibleMonth.collectAsState()
    val plans by viewModel.plansForSelectedDate.collectAsState()
    val planCounts by viewModel.planCountByDate.collectAsState()
    val allCheckIns by checkInViewModel.allCheckInsMap.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var planToCopy by remember { mutableStateOf<Plan?>(null) }

    var hasNotificationPermission by remember {
        mutableStateOf(AlarmPermissionUtils.hasNotificationPermission(context))
    }
    var canScheduleExact by remember {
        mutableStateOf(AlarmPermissionUtils.canScheduleExactAlarms(context))
    }

    val requestNotifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(Unit) {
        hasNotificationPermission = AlarmPermissionUtils.hasNotificationPermission(context)
        canScheduleExact = AlarmPermissionUtils.canScheduleExactAlarms(context)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tambah plan") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Text(
                text = "Plan saya",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp)
            )

            if (!hasNotificationPermission || !canScheduleExact) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ Izin Alarm / Notifikasi Diperlukan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Agar alarm dan pengingat berdering saat aplikasi berjalan di layar belakang atau layar HP terkunci, aktifkan izin berikut:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!hasNotificationPermission) {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            AlarmPermissionUtils.openNotificationSettings(context)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Izinkan Notifikasi", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (!canScheduleExact) {
                                Button(
                                    onClick = { AlarmPermissionUtils.openExactAlarmSettings(context) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Izinkan Alarm Tepat Waktu", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            MonthCalendar(
                month = visibleMonth,
                selectedDate = selectedDate,
                planCounts = planCounts,
                checkIns = allCheckIns,
                onPreviousMonth = viewModel::showPreviousMonth,
                onNextMonth = viewModel::showNextMonth,
                onToday = viewModel::goToToday,
                onSelectDate = viewModel::selectDate,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate.format(DayFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (plans.isNotEmpty()) {
                    Text(
                        text = "${plans.size} plan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (plans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada plan di hari ini.\nKetuk Tambah plan untuk membuat yang pertama.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(plans, key = { it.id }) { plan ->
                        PlanCard(
                            plan = plan,
                            onDelete = { viewModel.deletePlan(plan) },
                            onToggle = { viewModel.togglePlanCompletion(plan) },
                            onCopy = { planToCopy = plan }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val defaultTone = remember { UserPreferences(context).defaultAlarmTone }
        AddPlanDialog(
            selectedDate = selectedDate,
            defaultTone = defaultTone,
            onDismiss = { showAddDialog = false },
            onConfirm = { input ->
                val zone = ZoneId.systemDefault()
                val startMillis = LocalDateTime.of(selectedDate, input.start)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
                val durationMinutes = Duration.between(input.start, input.end).toMinutes().toInt()

                viewModel.addPlan(
                    plan = Plan(
                        title = input.title,
                        description = input.description,
                        location = input.location,
                        startTime = startMillis,
                        durationMinutes = durationMinutes,
                        alarmEnabled = input.alarmEnabled,
                        alarmTone = input.alarmTone
                    ),
                    pickedImage = input.imageUri
                )
                showAddDialog = false
            }
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
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    planCounts: Map<LocalDate, Int>,
    checkIns: Map<LocalDate, DailyCheckIn>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
                }
                Text(
                    text = month.format(MonthFormatter).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                if (selectedDate != today || month != YearMonth.from(today)) {
                    TextButton(onClick = onToday) { Text("Hari ini") }
                }
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                WeekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            val firstOfMonth = month.atDay(1)
            val leadingBlanks = (firstOfMonth.dayOfWeek.value % 7) - 1
            val adjustedBlanks = if (leadingBlanks < 0) 6 else leadingBlanks
            val daysInMonth = month.lengthOfMonth()
            val rows = (adjustedBlanks + daysInMonth + 6) / 7

            Column {
                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val day = row * 7 + col - adjustedBlanks + 1
                            if (day in 1..daysInMonth) {
                                val date = month.atDay(day)
                                val checkIn = checkIns[date]
                                DayCell(
                                    day = day,
                                    isSelected = date == selectedDate,
                                    isToday = date == today,
                                    hasPlans = (planCounts[date] ?: 0) > 0,
                                    isCheckedIn = checkIn?.isCheckedIn == true,
                                    isNoSpend = checkIn?.isNoSpendDay == true,
                                    onClick = { onSelectDate(date) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Keterangan Indikator Kalender
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                    Text("Plan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Text("Check-in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                    Text("No-Spend", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    hasPlans: Boolean,
    isCheckedIn: Boolean = false,
    isNoSpend: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val textColor = when {
        isSelected -> colors.onPrimary
        isToday -> colors.primary
        else -> colors.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (isSelected) colors.primary else Color.Transparent)
            .then(
                if (isToday && !isSelected) Modifier.border(1.5.dp, colors.primary, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )

        // Indikator Titik: Plan, Check-in (Hijau), No-Spend (Biru)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasPlans) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.onPrimary else colors.tertiary)
                )
            }
            if (isCheckedIn) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.onPrimary else Color(0xFF10B981))
                )
            }
            if (isNoSpend) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.onPrimary else Color(0xFF3B82F6))
                )
            }
        }
    }
}

@Composable
fun PlanCard(plan: Plan, onDelete: () -> Unit, onToggle: () -> Unit, onCopy: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(plan.startTime).atZone(zone).toLocalTime()
    val end = start.plusMinutes(plan.durationMinutes.toLong())
    val done = plan.isCompleted
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (done) colors.surfaceContainerLow else colors.surfaceContainerHigh
        )
    ) {
        Column {
            if (plan.imagePath != null) {
                PlanImage(
                    model = plan.imagePath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentDescription = "Gambar untuk ${plan.title}"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = start.format(TimeFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (done) colors.onSurfaceVariant else colors.primary
                    )
                    Text(
                        text = end.format(TimeFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "(${formatDuration(Duration.ofMinutes(plan.durationMinutes.toLong()))})",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .width(3.dp)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(if (done) colors.outlineVariant else colors.primary)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        color = if (done) colors.onSurfaceVariant else colors.onSurface
                    )
                    if (plan.location.isNotBlank()) {
                        Text(
                            text = plan.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    if (plan.description.isNotBlank()) {
                        Text(
                            text = plan.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

                Checkbox(checked = done, onCheckedChange = { onToggle() })
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin plan")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus plan")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyPlanDatePicker(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onConfirm(date)
                    }
                }
            ) { Text("Salin") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun defaultEnd(start: LocalTime): LocalTime {
    val end = start.plusMinutes(60)
    return if (end.isAfter(start)) end else LocalTime.of(23, 59)
}

private fun formatDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours j $minutes m"
        hours > 0 -> "$hours j"
        else -> "$minutes m"
    }
}

@Composable
fun AddPlanDialog(
    selectedDate: LocalDate,
    defaultTone: String?,
    onDismiss: () -> Unit,
    onConfirm: (NewPlanInput) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var start by remember {
        mutableStateOf(
            if (selectedDate == LocalDate.now()) {
                LocalTime.now().plusMinutes(5).withSecond(0).withNano(0)
            } else {
                LocalTime.of(9, 0)
            }
        )
    }
    var end by remember { mutableStateOf(defaultEnd(start)) }
    var alarmEnabled by remember { mutableStateOf(true) }
    var alarmTone by remember { mutableStateOf(defaultTone) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    var titleError by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val now = LocalDateTime.now()
    val planStartDateTime = LocalDateTime.of(selectedDate, start)
    val isPastTime = selectedDate == LocalDate.now() && planStartDateTime.isBefore(now)
    val timeOrderValid = end.isAfter(start)
    val timeValid = timeOrderValid && (!alarmEnabled || !isPastTime)
    val toneTitle = remember(alarmTone) { AlarmTones.title(context, alarmTone) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) imageUri = uri }

    val pickTone = rememberTonePicker(currentTone = alarmTone) { picked -> alarmTone = picked }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Plan baru")
                Text(
                    text = selectedDate.format(DayFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ImagePickerArea(
                    imageUri = imageUri,
                    onPick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemove = { imageUri = null }
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    label = { Text("Judul (mis. Jogging)") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Judul wajib diisi") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lokasi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Rentang waktu", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Mulai ${start.format(TimeFormatter)}") }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Selesai ${end.format(TimeFormatter)}") }
                }
                if (timeOrderValid && !isPastTime) {
                    Text(
                        text = "Durasi ${formatDuration(Duration.between(start, end))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!timeOrderValid) {
                    Text(
                        text = "Waktu selesai harus setelah waktu mulai",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (isPastTime && alarmEnabled) {
                    Text(
                        text = "Waktu mulai sudah lewat untuk hari ini. Ubah waktu ke masa mendatang agar alarm dapat dijadwalkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ingatkan saat waktunya", modifier = Modifier.weight(1f))
                    Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it })
                }
                if (alarmEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = pickTone)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nada alarm", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = toneTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = pickTone) { Text("Ubah") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    title.isBlank() -> titleError = true
                    !timeValid -> Unit
                    else -> onConfirm(
                        NewPlanInput(
                            title = title.trim(),
                            description = description.trim(),
                            location = location.trim(),
                            start = start,
                            end = end,
                            alarmEnabled = alarmEnabled,
                            alarmTone = alarmTone,
                            imageUri = imageUri
                        )
                    )
                }
            }) { Text("Simpan plan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )

    if (showStartPicker) {
        TimeDialog(
            title = "Waktu mulai",
            initial = start,
            onConfirm = { picked ->
                start = picked
                if (!end.isAfter(picked)) end = defaultEnd(picked)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker) {
        TimeDialog(
            title = "Waktu selesai",
            initial = end,
            onConfirm = { picked ->
                end = picked
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
private fun ImagePickerArea(
    imageUri: Uri?,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    if (imageUri == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceContainerHigh)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tambah gambar",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PlanImage(
                model = imageUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentDescription = "Gambar plan yang dipilih"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPick) { Text("Ganti gambar") }
                TextButton(onClick = onRemove) { Text("Hapus gambar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    title: String,
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )
    var typing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (typing) TimeInput(state = state) else TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("OK")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { typing = !typing }) {
                    Text(if (typing) "Pakai dial" else "Ketik jam")
                }
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        }
    )
}
