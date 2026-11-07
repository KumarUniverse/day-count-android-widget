package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = SkyBlueAccent,
  onPrimary = Slate950,
  primaryContainer = SkyBlueContainer,
  onPrimaryContainer = Color(0xFFE0F2FE),
  secondary = VioletAccent,
  onSecondary = Slate950,
  secondaryContainer = VioletContainer,
  onSecondaryContainer = Color(0xFFF3E8FF),
  tertiary = CoralAccent,
  onTertiary = Slate950,
  tertiaryContainer = CoralContainer,
  onTertiaryContainer = Color(0xFFFFEDD5),
  background = Slate950,
  onBackground = Slate100,
  surface = Slate900,
  onSurface = Slate100,
  surfaceVariant = Slate800,
  onSurfaceVariant = Slate200,
  outline = Slate700,
  outlineVariant = Slate800
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF0284C7),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0F2FE),
  onPrimaryContainer = Color(0xFF0369A1),
  secondary = Color(0xFF7C3AED),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFEDE9FE),
  onSecondaryContainer = Color(0xFF5B21B6),
  tertiary = Color(0xFFEA580C),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFFEDD5),
  onTertiaryContainer = Color(0xFFC2410C),
  background = Color(0xFFF8FAFC),
  onBackground = Slate900,
  surface = Color.White,
  onSurface = Slate900,
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = Slate700,
  outline = Color(0xFFCBD5E1),
  outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
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
