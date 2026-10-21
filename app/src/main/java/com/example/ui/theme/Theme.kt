package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val QuranifyDarkColorScheme = darkColorScheme(
  primary = AccentBlue,
  secondary = AccentCyan,
  tertiary = EmeraldGlow,
  background = LiquidBlack,
  surface = DeepSapphire,
  onPrimary = TextMainWhite,
  onSecondary = TextMainWhite,
  onTertiary = LiquidBlack,
  onBackground = TextMainWhite,
  onSurface = TextMainWhite,
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = QuranifyDarkColorScheme,
    typography = Typography,
    content = content,
  )
}
