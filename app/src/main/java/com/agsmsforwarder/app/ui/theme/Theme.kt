package com.agsmsforwarder.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VaultPulseColorScheme = darkColorScheme(
    primary = VaultPrimary,
    onPrimary = VaultOnPrimary,
    primaryContainer = VaultPrimaryContainer,
    onPrimaryContainer = VaultOnPrimaryContainer,
    secondary = VaultSecondary,
    onSecondary = VaultOnSecondary,
    secondaryContainer = VaultSecondaryContainer,
    onSecondaryContainer = VaultOnSecondaryContainer,
    tertiary = VaultTertiary,
    onTertiary = VaultOnTertiary,
    tertiaryContainer = VaultTertiaryContainer,
    onTertiaryContainer = VaultOnTertiaryContainer,
    background = VaultBackground,
    onBackground = VaultOnBackground,
    surface = VaultSurface,
    onSurface = VaultOnSurface,
    surfaceVariant = VaultSurfaceContainerHighest,
    onSurfaceVariant = VaultOnSurfaceVariant,
    outline = VaultOutline,
    outlineVariant = VaultOutlineVariant,
    error = VaultError,
    onError = VaultOnError,
    errorContainer = VaultErrorContainer,
    onErrorContainer = VaultOnErrorContainer
)

@Composable
fun AGSMSForwarderTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = VaultPulseColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = VaultBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
