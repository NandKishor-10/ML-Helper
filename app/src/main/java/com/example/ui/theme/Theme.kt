package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = BrandCyan,
    onPrimary = Color(0xFF003643),
    primaryContainer = Color(0xFF004E60),
    onPrimaryContainer = Color(0xFFB8EAFF),
    secondary = BrandAmber,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF623F00),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = Color(0xFF90CAF9),
    onTertiary = Color(0xFF0D47A1),
    tertiaryContainer = Color(0xFF1565C0),
    onTertiaryContainer = Color(0xFFD1E4FF),
    background = DarkBackground,
    onBackground = Color(0xFFE6EDF3),
    surface = DarkSurface,
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4C7C5),
    outline = DarkBorder,
    surfaceContainerHigh = Color(0xFF22252E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00677F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EAFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF825500),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB3),
    onSecondaryContainer = Color(0xFF291800),
    tertiary = Color(0xFF1E608F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCEE5FF),
    onTertiaryContainer = Color(0xFF001D33),
    background = Color(0xFFF9F9FC),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFECEFF4),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFD0D5DD),
    surfaceContainerHigh = Color(0xFFF1F3F8)
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
