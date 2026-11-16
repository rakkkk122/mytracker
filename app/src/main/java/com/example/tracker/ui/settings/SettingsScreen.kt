package com.example.tracker.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tracker.data.BackupPayload
import com.example.tracker.data.DataBackupManager
import com.example.tracker.data.Plan
import com.example.tracker.data.UserPreferences
import com.example.tracker.reminder.AlarmPermissionUtils
import com.example.tracker.reminder.AlarmTones
import com.example.tracker.reminder.AndroidAlarmScheduler
import com.example.tracker.ui.common.rememberTonePicker
import com.example.tracker.ui.theme.EmeraldPrimary
import com.example.tracker.ui.theme.IndigoPrimary
import com.example.tracker.ui.theme.RosePrimary
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(onThemeChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    var notificationsOn by remember { mutableStateOf(prefs.notificationsEnabled) }
    var defaultTone by remember { mutableStateOf(prefs.defaultAlarmTone) }
    var darkMode by remember { mutableStateOf(prefs.darkMode) }
    var themeColor by remember { mutableStateOf(prefs.themeColor) }

    var hasNotificationPerm by remember {
        mutableStateOf(AlarmPermissionUtils.hasNotificationPermission(context))
    }
    var canScheduleExact by remember {
        mutableStateOf(AlarmPermissionUtils.canScheduleExactAlarms(context))
    }

    LaunchedEffect(Unit) {
        hasNotificationPerm = AlarmPermissionUtils.hasNotificationPermission(context)
        canScheduleExact = AlarmPermissionUtils.canScheduleExactAlarms(context)
    }

    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showThemeColorDialog by remember { mutableStateOf(false) }

    var isProcessingBackup by remember { mutableStateOf(false) }
    var pendingBackupPayload by remember { mutableStateOf<BackupPayload?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    // Launcher untuk mengekspor data ke file JSON
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    isProcessingBackup = true
                    val jsonString = DataBackupManager.exportToJson(context)
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Data berhasil diekspor ke file JSON!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal mengekspor data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isProcessingBackup = false
                }
            }
        }
    }

    // Launcher untuk memilih file JSON yang akan diimpor
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    isProcessingBackup = true
                    val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                    if (jsonString.isNullOrBlank()) {
                        Toast.makeText(context, "File kosong atau tidak dapat dibaca.", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    val payload = DataBackupManager.parseJson(jsonString)
                    if (payload.plans.isEmpty() && payload.expenses.isEmpty() && payload.preferences == null) {
                        Toast.makeText(context, "File cadangan tidak memuat data yang valid.", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    pendingBackupPayload = payload
                    showImportConfirmDialog = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Format file JSON tidak valid: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isProcessingBackup = false
                }
            }
        }
    }

    val toneTitle = remember(defaultTone) { AlarmTones.title(context, defaultTone) }
    val pickTone = rememberTonePicker(currentTone = defaultTone) { picked ->
        defaultTone = picked
        prefs.defaultAlarmTone = picked
    }

    val darkModeLabel = when (darkMode) {
        1 -> "Terang"
        2 -> "Gelap"
        else -> "Ikuti sistem"
    }

    val themeColorLabel = when (themeColor) {
        1 -> "Emerald"
        2 -> "Rose"
        else -> "Indigo"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Pengaturan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        Spacer(Modifier.height(24.dp))

        SettingsGroup(title = "Pengingat & Alarm") {
            SettingRow(
                icon = Icons.Default.Notifications,
                title = "Notifikasi plan",
                subtitle = "Tampilkan pengingat saat waktu plan tiba",
                trailing = {
                    Switch(checked = notificationsOn, onCheckedChange = {
                        notificationsOn = it
                        prefs.notificationsEnabled = it
                    })
                },
                onClick = {
                    notificationsOn = !notificationsOn
                    prefs.notificationsEnabled = notificationsOn
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingRow(
                icon = Icons.Default.MusicNote,
                title = "Nada alarm default",
                subtitle = toneTitle,
                enabled = notificationsOn,
                onClick = pickTone
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingRow(
                icon = Icons.Default.NotificationsActive,
                title = "Izin notifikasi sistem",
                subtitle = if (hasNotificationPerm) "Aktif" else "Belum diizinkan (Ketuk untuk membuka setelan)",
                onClick = {
                    AlarmPermissionUtils.openNotificationSettings(context)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingRow(
                icon = Icons.Default.Alarm,
                title = "Izin alarm tepat waktu",
                subtitle = if (canScheduleExact) "Aktif (Alarm berbunyi tepat waktu)" else "Belum diizinkan (Ketuk untuk mengaktifkan)",
                onClick = {
                    AlarmPermissionUtils.openExactAlarmSettings(context)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingRow(
                icon = Icons.Default.PlayArrow,
                title = "Uji coba alarm (5 detik)",
                subtitle = "Tes alarm berdering dan notifikasi di layar belakang",
                onClick = {
                    val scheduler = AndroidAlarmScheduler(context)
                    val testPlan = Plan(
                        id = 999999,
                        title = "Uji Coba Alarm Tracker",
                        description = "Alarm dan notifikasi di layar belakang berfungsi!",
                        location = "Pengaturan",
                        startTime = System.currentTimeMillis() + 5000,
                        durationMinutes = 15,
                        alarmEnabled = true,
                        alarmTone = defaultTone
                    )
                    scheduler.schedule(testPlan)
                    Toast.makeText(
                        context,
                        "Alarm uji coba dijadwalkan dalam 5 detik. Silakan kunci HP atau beralih ke aplikasi lain untuk mengetes!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsGroup(title = "Cadangan Data (Backup & Restore)") {
            SettingRow(
                icon = Icons.Default.FileUpload,
                title = "Ekspor data (Backup JSON)",
                subtitle = "Cadangkan semua plan, pengeluaran, dan pengaturan ke file .json",
                enabled = !isProcessingBackup,
                onClick = {
                    val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").format(LocalDateTime.now())
                    exportLauncher.launch("tracker_backup_$timestamp.json")
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingRow(
                icon = Icons.Default.FileDownload,
                title = "Impor data (Restore JSON)",
                subtitle = "Pulihkan plan, pengeluaran, dan pengaturan dari file cadangan .json",
                enabled = !isProcessingBackup,
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        SettingsGroup(title = "Tampilan") {
            SettingRow(
                icon = Icons.Default.Palette,
                title = "Your Theme",
                subtitle = darkModeLabel,
                onClick = { showDarkModeDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingRow(
                icon = Icons.Default.Palette,
                title = "Warna Aksen",
                subtitle = themeColorLabel,
                onClick = { showThemeColorDialog = true }
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Versi 1.2.0",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(16.dp))
    }

    if (showDarkModeDialog) {
        DarkModeDialog(
            currentMode = darkMode,
            onDismiss = { showDarkModeDialog = false },
            onSelect = { mode ->
                darkMode = mode
                prefs.darkMode = mode
                showDarkModeDialog = false
                onThemeChanged()
            }
        )
    }

    if (showThemeColorDialog) {
        ThemeColorDialog(
            currentTheme = themeColor,
            onDismiss = { showThemeColorDialog = false },
            onSelect = { color ->
                themeColor = color
                prefs.themeColor = color
                showThemeColorDialog = false
                onThemeChanged()
            }
        )
    }

    // Dialog konfirmasi pemulihan backup
    val payloadToImport = pendingBackupPayload
    if (showImportConfirmDialog && payloadToImport != null) {
        ImportConfirmDialog(
            payload = payloadToImport,
            onDismiss = {
                showImportConfirmDialog = false
                pendingBackupPayload = null
            },
            onConfirm = { replaceExisting, restorePreferences ->
                showImportConfirmDialog = false
                coroutineScope.launch {
                    try {
                        isProcessingBackup = true
                        val count = DataBackupManager.importBackup(
                            context = context,
                            payload = payloadToImport,
                            replaceExisting = replaceExisting,
                            restorePreferences = restorePreferences
                        )
                        if (restorePreferences && payloadToImport.preferences != null) {
                            darkMode = prefs.darkMode
                            themeColor = prefs.themeColor
                            notificationsOn = prefs.notificationsEnabled
                            defaultTone = prefs.defaultAlarmTone
                            onThemeChanged()
                        }
                        Toast.makeText(context, "Berhasil mengimpor $count plan!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal mengimpor data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    } finally {
                        isProcessingBackup = false
                        pendingBackupPayload = null
                    }
                }
            }
        )
    }

    // Indikator loading saat memproses ekspor/impor
    if (isProcessingBackup) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Memproses Data...") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Text("Mohon tunggu sebentar...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
    }
}

@Composable
private fun ImportConfirmDialog(
    payload: BackupPayload,
    onDismiss: () -> Unit,
    onConfirm: (replaceExisting: Boolean, restorePreferences: Boolean) -> Unit
) {
    var replaceExisting by remember { mutableStateOf(false) }
    var restorePreferences by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Konfirmasi Impor Data")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ditemukan ${payload.plans.size} plan dan ${payload.expenses.size} pengeluaran dalam file cadangan.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (payload.exportedDate.isNotBlank()) {
                    Text(
                        text = "Waktu backup: ${payload.exportedDate.replace('T', ' ')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Metode Impor:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                // Opsi 1: Gabungkan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { replaceExisting = false }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !replaceExisting,
                        onClick = { replaceExisting = false }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Gabungkan (Merge)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tambahkan plan & pengeluaran tanpa menghapus data saat ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Opsi 2: Gantikan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { replaceExisting = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = replaceExisting,
                        onClick = { replaceExisting = true }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Gantikan Semua (Timpa)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Hapus seluruh plan & pengeluaran lama dan ganti dengan data dari backup",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (payload.preferences != null) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restorePreferences = !restorePreferences }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = restorePreferences,
                            onCheckedChange = { restorePreferences = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Pulihkan juga tema & nada alarm dari backup",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(replaceExisting, restorePreferences) },
                colors = if (replaceExisting) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(if (replaceExisting) "Timpa & Impor" else "Impor Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun DarkModeDialog(currentMode: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Tema") },
        text = {
            Column {
                listOf("Ikuti sistem" to 0, "Terang" to 1, "Gelap" to 2).forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentMode == value, onClick = null)
                        Text(label, Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun ThemeColorDialog(currentTheme: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val themes = listOf(
        Triple("Indigo", 0, IndigoPrimary),
        Triple("Emerald", 1, EmeraldPrimary),
        Triple("Rose", 2, RosePrimary)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Warna Aksen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEach { (label, value, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(value) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        )
                        Text(
                            text = label,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        )
                        if (currentTheme == value) {
                            RadioButton(selected = true, onClick = null)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}
