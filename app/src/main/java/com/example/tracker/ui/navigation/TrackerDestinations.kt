package com.example.tracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TrackerDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : TrackerDestination("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Plan : TrackerDestination("plan", "Plan", Icons.Default.CalendarToday)
    object Money : TrackerDestination("money", "Money", Icons.Default.AccountBalanceWallet)
    object Settings : TrackerDestination("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    TrackerDestination.Dashboard,
    TrackerDestination.Plan,
    TrackerDestination.Money,
    TrackerDestination.Settings
)
