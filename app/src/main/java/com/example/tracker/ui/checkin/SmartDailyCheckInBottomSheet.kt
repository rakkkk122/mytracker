package com.example.tracker.ui.checkin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tracker.data.DailyCheckIn
import com.example.tracker.data.Expense
import com.example.tracker.data.Plan
import com.example.tracker.ui.money.formatAmountInput
import com.example.tracker.ui.money.parseAmountInput
import com.example.tracker.ui.plan.PlanImage
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MOOD_OPTIONS = listOf(
    "😊 Senang",
    "⚡ Bersemangat",
    "🧘 Tenang",
    "🎯 Produktif",
    "😴 Lelah",
    "🌧️ Stres"
)

private val QUICK_NOTES = listOf(
    "Hari yang produktif!",
    "Terkendali & hemat",
    "Fokus istirahat",
    "Banyak pengeluaran tak terduga",
    "Besok harus lebih konsisten"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartDailyCheckInBottomSheet(
    checkIn: DailyCheckIn?,
    allCheckIns: Map<LocalDate, DailyCheckIn> = emptyMap(),
    todayPlans: List<Plan>,
    onTogglePlan: (Plan) -> Unit,
    onSubmit: (
        isNoSpendDay: Boolean,
        mood: String?,
        note: String?,
        newExpense: Expense?,
        pickedImage: Uri?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val streakCount = remember(allCheckIns, checkIn) { calculateStreak(allCheckIns) }
    val alreadyCheckedIn = remember { checkIn?.isCheckedIn == true }
    val streakAtOpen = remember { streakCount }

    // State form
    var isNoSpendDay by remember(checkIn) { mutableStateOf(checkIn?.isNoSpendDay ?: true) }
    var selectedMood by remember(checkIn) { mutableStateOf(checkIn?.mood) }
    var note by remember(checkIn) { mutableStateOf(checkIn?.note ?: "") }

    var expenseTitle by remember { mutableStateOf("") }
    var expenseAmountText by remember { mutableStateOf("") }
    var expenseReason by remember { mutableStateOf("") }
    var expenseImageUri by remember { mutableStateOf<Uri?>(null) }

    var submitted by remember { mutableStateOf(false) }
    var submittedNoSpend by remember { mutableStateOf(true) }
    var submittedMood by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) expenseImageUri = uri }

    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID")))
    }
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 4..10 -> "Selamat pagi"
            in 11..14 -> "Selamat siang"
            in 15..17 -> "Selamat sore"
            else -> "Selamat malam"
        }
    }

    val completedHabits = todayPlans.count { it.isCompleted }

    // Progres 3 langkah
    val habitDone = todayPlans.isEmpty() || completedHabits == todayPlans.size
    val financeDone = isNoSpendDay || (expenseTitle.isNotBlank() && parseAmountInput(expenseAmountText) > 0)
    val moodDone = selectedMood != null
    val stepsDone = listOf(habitDone, financeDone, moodDone).count { it }
    val progress by animateFloatAsState(
        targetValue = stepsDone / 3f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "stepProgress"
    )

    // Tutup otomatis setelah animasi sukses
    LaunchedEffect(submitted) {
        if (submitted) {
            delay(1900)
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        AnimatedContent(
            targetState = submitted,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
            label = "sheetContent"
        ) { isDone ->
            if (isDone) {
                SuccessPanel(
                    streak = if (alreadyCheckedIn) streakCount else maxOf(streakCount, streakAtOpen + 1),
                    isNoSpendDay = submittedNoSpend,
                    mood = submittedMood
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ───────── HEADER ─────────
                    StaggeredReveal(index = 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.primaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = colors.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "$greeting! 👋",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = todayFormatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (streakCount > 0) {
                                        StreakPill(count = streakCount)
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = colors.onSurfaceVariant)
                                    }
                                }
                            }

                            // Progress bar langkah
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = colors.primary,
                                    trackColor = colors.surfaceContainerHighest
                                )
                                AnimatedContent(
                                    targetState = stepsDone,
                                    transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith fadeOut() },
                                    label = "stepsLabel"
                                ) { n ->
                                    Text(
                                        text = if (n == 3) "Siap simpan ✨" else "$n/3 langkah",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (n == 3) colors.primary else colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // ───────── 1. HABIT ─────────
                    StaggeredReveal(index = 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader(number = 1, title = "Habit & Plan Hari Ini", done = habitDone) {
                                if (todayPlans.isNotEmpty()) {
                                    val allDone = completedHabits == todayPlans.size
                                    val chipBg by animateColorAsState(
                                        if (allDone) colors.primaryContainer else colors.surfaceContainerHigh,
                                        label = "habitChipBg"
                                    )
                                    Surface(shape = RoundedCornerShape(10.dp), color = chipBg) {
                                        AnimatedContent(
                                            targetState = completedHabits,
                                            transitionSpec = { (scaleIn(initialScale = 0.7f) + fadeIn()) togetherWith fadeOut() },
                                            label = "habitCount"
                                        ) { n ->
                                            Text(
                                                text = "$n/${todayPlans.size} Selesai",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (allDone) colors.onPrimaryContainer else colors.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (todayPlans.isEmpty()) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.TaskAlt, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                                        Text(
                                            text = "Tidak ada habit terjadwal hari ini. Kamu bisa langsung isi status keuangan & mood!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                val habitProgress by animateFloatAsState(
                                    targetValue = completedHabits.toFloat() / todayPlans.size,
                                    animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
                                    label = "habitProgress"
                                )
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier
                                        .padding(10.dp)
                                        .animateContentSize()) {
                                        LinearProgressIndicator(
                                            progress = { habitProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = colors.primary,
                                            trackColor = colors.surfaceContainerHighest
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        todayPlans.forEach { plan ->
                                            HabitRow(plan = plan, onToggle = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onTogglePlan(plan)
                                            })
                                        }

                                        AnimatedVisibility(
                                            visible = completedHabits < todayPlans.size,
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    todayPlans.filter { !it.isCompleted }.forEach { onTogglePlan(it) }
                                                }
                                            ) {
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Tandai semua selesai", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        AnimatedVisibility(
                                            visible = completedHabits == todayPlans.size,
                                            enter = fadeIn() + expandVertically(),
                                            exit = fadeOut() + shrinkVertically()
                                        ) {
                                            Text(
                                                text = "🎉 Semua habit hari ini selesai. Keren!",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.primary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                    // ───────── 2. FINANSIAL ─────────
                    StaggeredReveal(index = 2) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader(number = 2, title = "Status Finansial Hari Ini", done = financeDone)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FinanceOptionCard(
                                    selected = isNoSpendDay,
                                    title = "No-Spend Day",
                                    subtitle = "Rp 0 keluar 🛡️",
                                    icon = Icons.Default.Shield,
                                    accent = colors.primary,
                                    onAccent = colors.onPrimary,
                                    container = colors.primaryContainer,
                                    onContainer = colors.onPrimaryContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isNoSpendDay = true
                                    }
                                )
                                FinanceOptionCard(
                                    selected = !isNoSpendDay,
                                    title = "Ada Pengeluaran",
                                    subtitle = "Catat transaksi 📝",
                                    icon = Icons.Default.ReceiptLong,
                                    accent = colors.tertiary,
                                    onAccent = colors.onTertiary,
                                    container = colors.tertiaryContainer.copy(alpha = 0.8f),
                                    onContainer = colors.onTertiaryContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isNoSpendDay = false
                                    }
                                )
                            }

                            AnimatedVisibility(
                                visible = !isNoSpendDay,
                                enter = fadeIn() + expandVertically(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
                                    border = BorderStroke(1.dp, colors.tertiary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AttachMoney, null, tint = colors.tertiary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Catat Pengeluaran Cepat",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.onSurface
                                            )
                                        }

                                        OutlinedTextField(
                                            value = expenseTitle,
                                            onValueChange = { expenseTitle = it },
                                            label = { Text("Judul (e.g. Makan siang, Bensin)") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Edit, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = expenseAmountText,
                                            onValueChange = { input -> expenseAmountText = formatAmountInput(input) },
                                            label = { Text("Nominal Biaya (Rp)") },
                                            leadingIcon = {
                                                Text(
                                                    "Rp ",
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.primary,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            },
                                            placeholder = { Text("0") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = expenseReason,
                                            onValueChange = { expenseReason = it },
                                            label = { Text("Keterangan / Alasan (Opsional)") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        AnimatedContent(
                                            targetState = expenseImageUri,
                                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                                            label = "receipt"
                                        ) { uri ->
                                            if (uri != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(120.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    PlanImage(
                                                        model = uri,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentDescription = "Bukti pengeluaran"
                                                    )
                                                    IconButton(
                                                        onClick = { expenseImageUri = null },
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(6.dp)
                                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                            .size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "Hapus Gambar",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = {
                                                        photoPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Tambah Foto Struk (Opsional)", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                    // ───────── 3. MOOD ─────────
                    StaggeredReveal(index = 3) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(number = 3, title = "Refleksi Mood & Catatan", done = moodDone)

                            Text(
                                text = "Bagaimana perasaanmu hari ini?",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )

                            MOOD_OPTIONS.chunked(3).forEach { rowMoods ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowMoods.forEach { m ->
                                        MoodTile(
                                            mood = m,
                                            selected = selectedMood == m,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedMood = if (selectedMood == m) null else m
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = "Catatan cepat / refleksi:",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QUICK_NOTES.forEach { qn ->
                                    val chipShape = RoundedCornerShape(10.dp)
                                    Surface(
                                        shape = chipShape,
                                        color = colors.surfaceContainerHigh,
                                        modifier = Modifier.bouncyClickable(chipShape) {
                                            note = if (note.isBlank()) qn else "$note. $qn"
                                        }
                                    ) {
                                        Text(
                                            text = "+ $qn",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                placeholder = { Text("Tulis refleksi singkat harimu di sini...") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize(),
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                    }

                    // ───────── SIMPAN ─────────
                    StaggeredReveal(index = 4) {
                        val buttonShape = RoundedCornerShape(16.dp)
                        Button(
                            onClick = {
                                val newExpense = if (!isNoSpendDay && expenseTitle.isNotBlank()) {
                                    val amount = parseAmountInput(expenseAmountText)
                                    if (amount > 0) {
                                        Expense(
                                            id = 0,
                                            title = expenseTitle.trim(),
                                            amount = amount,
                                            reason = expenseReason.trim(),
                                            dateMillis = System.currentTimeMillis()
                                        )
                                    } else null
                                } else null

                                onSubmit(isNoSpendDay, selectedMood, note, newExpense, expenseImageUri)
                                submittedNoSpend = isNoSpendDay
                                submittedMood = selectedMood
                                submitted = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(buttonShape)
                                .shine(),
                            shape = buttonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Simpan & Selesaikan Check-in",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── KOMPONEN ─────────────────────────

@Composable
private fun SectionHeader(
    number: Int,
    title: String,
    done: Boolean,
    trailing: @Composable () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (done) colors.primary else colors.surfaceContainerHighest,
        animationSpec = tween(350),
        label = "sectionBadgeBg"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = CircleShape, color = bg, modifier = Modifier.size(26.dp)) {
                AnimatedContent(
                    targetState = done,
                    transitionSpec = { (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith fadeOut(tween(120)) },
                    contentAlignment = Alignment.Center,
                    label = "sectionBadge"
                ) { d ->
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (d) {
                            Icon(Icons.Default.Check, null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                text = "$number",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }
        trailing()
    }
}

@Composable
private fun HabitRow(plan: Plan, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    val bg by animateColorAsState(
        targetValue = if (plan.isCompleted) colors.primaryContainer.copy(alpha = 0.45f) else Color.Transparent,
        animationSpec = tween(300),
        label = "habitBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (plan.isCompleted) colors.onSurfaceVariant else colors.onSurface,
        animationSpec = tween(300),
        label = "habitText"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(shape, onToggle)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = plan.isCompleted, onCheckedChange = null, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plan.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (plan.isCompleted) TextDecoration.LineThrough else null,
                color = textColor
            )
            if (plan.location.isNotBlank()) {
                Text(
                    text = plan.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FinanceOptionCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onAccent: Color,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val bounce = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)

    val selScale by animateFloatAsState(if (selected) 1f else 0.97f, bounce, label = "selScale")
    val iconScale by animateFloatAsState(if (selected) 1.15f else 1f, bounce, label = "iconScale")
    val bg by animateColorAsState(if (selected) container else colors.surfaceContainerLow, tween(300), label = "finBg")
    val borderColor by animateColorAsState(
        if (selected) accent else colors.outlineVariant.copy(alpha = 0.6f), tween(300), label = "finBorder"
    )
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, label = "finBorderW")
    val iconBg by animateColorAsState(if (selected) accent else colors.surfaceContainerHigh, tween(300), label = "iconBg")
    val iconTint by animateColorAsState(if (selected) onAccent else colors.onSurfaceVariant, tween(300), label = "iconTint")
    val titleColor by animateColorAsState(if (selected) onContainer else colors.onSurface, tween(300), label = "finTitle")

    Surface(
        modifier = modifier
            .graphicsLayer { scaleX = selScale; scaleY = selScale }
            .bouncyClickable(shape, onClick),
        shape = shape,
        color = bg,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconBg,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = accent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MoodTile(
    mood: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val emoji = mood.substringBefore(' ')
    val label = mood.substringAfter(' ', mood)

    val emojiScale by animateFloatAsState(
        targetValue = if (selected) 1.3f else 1f,
        animationSpec = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium),
        label = "emojiScale"
    )
    val bg by animateColorAsState(
        if (selected) colors.primaryContainer else colors.surfaceContainerLow, tween(250), label = "moodBg"
    )
    val borderColor by animateColorAsState(
        if (selected) colors.primary else colors.outlineVariant.copy(alpha = 0.5f), tween(250), label = "moodBorder"
    )

    Surface(
        modifier = modifier.bouncyClickable(shape, onClick),
        shape = shape,
        color = bg,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 26.sp,
                modifier = Modifier.graphicsLayer { scaleX = emojiScale; scaleY = emojiScale }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SuccessPanel(streak: Int, isNoSpendDay: Boolean, mood: String?) {
    val colors = MaterialTheme.colorScheme
    val pop = remember { Animatable(0f) }
    val checkPop = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        delay(180)
        checkPop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    LaunchedEffect(Unit) {
        delay(350)
        textAlpha.animateTo(1f, tween(450))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
        contentAlignment = Alignment.Center
    ) {
        ConfettiBurst(
            modifier = Modifier.fillMaxSize(),
            palette = listOf(colors.primary, colors.tertiary, colors.secondary, Color(0xFFFF9800), Color(0xFFFFC107))
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = CircleShape,
                color = colors.primary,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer { scaleX = checkPop.value; scaleY = checkPop.value }
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = (1f - textAlpha.value) * 16.dp.toPx()
                }
            ) {
                Text(
                    text = "Check-in tersimpan! ✨",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildString {
                        append(if (isNoSpendDay) "🛡️ No-Spend Day" else "💸 Pengeluaran tercatat")
                        if (!mood.isNullOrBlank()) append("  ·  $mood")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                if (streak > 0) {
                    Spacer(Modifier.height(4.dp))
                    StreakPill(count = streak, suffix = "Hari Streak")
                }
            }
        }
    }
}