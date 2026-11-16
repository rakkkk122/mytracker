package com.example.tracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun getDarkColorScheme(themeColor: Int): ColorScheme {
    val primary = when (themeColor) {
        1 -> EmeraldDarkPrimary
        2 -> RoseDarkPrimary
        else -> IndigoDarkPrimary
    }
    val primaryContainer = when (themeColor) {
        1 -> EmeraldDarkPrimaryContainer
        2 -> RoseDarkPrimaryContainer
        else -> IndigoDarkPrimaryContainer
    }
    val onPrimaryContainer = when (themeColor) {
        1 -> EmeraldDarkOnPrimaryContainer
        2 -> RoseDarkOnPrimaryContainer
        else -> IndigoDarkOnPrimaryContainer
    }

    return darkColorScheme(
        primary = primary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = DarkSecondary,
        onSecondary = DarkOnSecondary,
        secondaryContainer = DarkSecondaryContainer,
        onSecondaryContainer = DarkOnSecondaryContainer,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline
    )
}

private fun getLightColorScheme(themeColor: Int): ColorScheme {
    val primary = when (themeColor) {
        1 -> EmeraldPrimary
        2 -> RosePrimary
        else -> IndigoPrimary
    }
    val primaryContainer = when (themeColor) {
        1 -> EmeraldPrimaryContainer
        2 -> RosePrimaryContainer
        else -> IndigoPrimaryContainer
    }
    val onPrimaryContainer = when (themeColor) {
        1 -> EmeraldOnPrimaryContainer
        2 -> RoseOnPrimaryContainer
        else -> IndigoOnPrimaryContainer
    }

    return lightColorScheme(
        primary = primary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = Secondary,
        onSecondary = OnSecondary,
        secondaryContainer = SecondaryContainer,
        onSecondaryContainer = OnSecondaryContainer,
        background = Background,
        onBackground = OnBackground,
        surface = Surface,
        onSurface = OnSurface,
        surfaceVariant = SurfaceVariant,
        onSurfaceVariant = OnSurfaceVariant,
        outline = Outline
    )
}

@Composable
fun TrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: Int = 0,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) getDarkColorScheme(themeColor) else getLightColorScheme(themeColor)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
