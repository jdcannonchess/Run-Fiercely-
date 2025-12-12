package com.example.runlogger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = DarkBackground,
    primaryContainer = TealDark,
    onPrimaryContainer = Teal80,
    secondary = Coral80,
    onSecondary = DarkBackground,
    secondaryContainer = Coral40,
    onSecondaryContainer = Coral80,
    tertiary = Green80,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = LightSurface,
    primaryContainer = Teal80,
    onPrimaryContainer = TealDark,
    secondary = Coral40,
    onSecondary = LightSurface,
    secondaryContainer = Coral80,
    onSecondaryContainer = Coral40,
    tertiary = Green40,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = OnLightBackground,
    surface = LightSurface,
    onSurface = OnLightBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightBackground
)

@Composable
fun RunLoggerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled to use our custom theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
