package com.shohan.pro.downloader.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = BrandNavyDark,
    primaryContainer = BrandNavySurface,
    onPrimaryContainer = NeonCyan,
    secondary = NeonCoral,
    onSecondary = BrandNavyDark,
    background = BrandNavy,
    onBackground = TextWhite,
    surface = BrandNavySurface,
    onSurface = TextWhite,
    surfaceVariant = BrandNavyLight,
    onSurfaceVariant = TextMuted,
    outline = NeonRed,
    error = ErrorRed,
    onError = TextWhite
)

@Composable
fun LinkDownloaderTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BrandNavyDark.toArgb()
                window.navigationBarColor = BrandNavyDark.toArgb()
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
