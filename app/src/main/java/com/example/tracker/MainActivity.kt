package com.example.tracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tracker.data.UserPreferences
import com.example.tracker.reminder.AlarmPermissionUtils
import com.example.tracker.ui.dashboard.DashboardScreen
import com.example.tracker.ui.money.MoneyScreen
import com.example.tracker.ui.navigation.TrackerDestination
import com.example.tracker.ui.navigation.bottomNavItems
import com.example.tracker.ui.plan.PlanScreen
import com.example.tracker.ui.settings.SettingsScreen
import com.example.tracker.ui.theme.TrackerTheme

class MainActivity : ComponentActivity() {

    private var openCheckInDirectly by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.getBooleanExtra("OPEN_CHECK_IN", false) == true) {
            openCheckInDirectly = true
        }

        // Izinkan layar menyala dan tampil di atas layar kunci saat alarm berbunyi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { UserPreferences(context) }
            
            var darkModeSetting by remember { mutableIntStateOf(prefs.darkMode) }
            var themeColorSetting by remember { mutableIntStateOf(prefs.themeColor) }
            
            val darkTheme = when (darkModeSetting) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            TrackerTheme(
                darkTheme = darkTheme,
                themeColor = themeColorSetting
            ) {
                RequestNotificationPermission()
                TrackerApp(
                    initialOpenCheckIn = openCheckInDirectly,
                    onThemeChanged = {
                        darkModeSetting = prefs.darkMode
                        themeColorSetting = prefs.themeColor
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_CHECK_IN", false)) {
            openCheckInDirectly = true
        }
    }

    override fun onResume() {
        super.onResume()
        // Jika izin exact alarm baru saja diberikan, jadwalkan ulang semua alarm aktif
        if (AlarmPermissionUtils.canScheduleExactAlarms(this)) {
            AlarmPermissionUtils.rescheduleAllActiveAlarms(this)
        }
    }
}

@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { _ -> }
        )
        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun TrackerApp(
    initialOpenCheckIn: Boolean = false,
    onThemeChanged: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TrackerDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TrackerDestination.Dashboard.route) {
                DashboardScreen(initialOpenCheckIn = initialOpenCheckIn)
            }
            composable(TrackerDestination.Plan.route) {
                PlanScreen()
            }
            composable(TrackerDestination.Money.route) {
                MoneyScreen()
            }
            composable(TrackerDestination.Settings.route) {
                SettingsScreen(onThemeChanged = onThemeChanged)
            }
        }
    }
}
