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

/** 应用皮肤。顺序即菜单中的显示顺序。 */
enum class AppSkin(val label: String) {
  SYSTEM("\u8DDF\u968F\u7CFB\u7EDF"),
  LIGHT("\u6D45\u8272"),
  LIGHT_YELLOW("\u6D45\u9EC4"),
  DARK("\u6DF1\u8272");

  companion object {
    fun fromName(name: String?): AppSkin = entries.firstOrNull { it.name == name } ?: SYSTEM
  }
}

@Composable
fun AppSkin.isDark(): Boolean =
  when (this) {
    AppSkin.SYSTEM -> isSystemInDarkTheme()
    AppSkin.DARK -> true
    AppSkin.LIGHT, AppSkin.LIGHT_YELLOW -> false
  }

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

private val LightYellowColorScheme =
  lightColorScheme(
    primary = LightYellowPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E7B0),
    onPrimaryContainer = Color(0xFF261A00),
    secondary = Color(0xFF6B5E3A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1E6C0),
    onSecondaryContainer = Color(0xFF241A04),
    tertiary = Pink40,
    background = LightYellow,
    onBackground = LightYellowText,
    surface = LightYellowCard,
    onSurface = LightYellowText,
    surfaceVariant = LightYellowVariant,
    onSurfaceVariant = LightYellowSubText,
    surfaceTint = LightYellowPrimary,
    surfaceContainerLowest = Color(0xFFFFFFFA),
    surfaceContainerLow = LightYellowCard,
    surfaceContainer = Color(0xFFFAFAD8),
    surfaceContainerHigh = Color(0xFFF5F4D2),
    surfaceContainerHighest = Color(0xFFEFEECC),
    outline = Color(0xFF7D7A5F),
    outlineVariant = Color(0xFFD6D3AE),
  )

@Composable
fun MyApplicationTheme(
  skin: AppSkin = AppSkin.SYSTEM,
  // Dynamic color is available on Android 12+，仅用于浅色和深色皮肤
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val darkTheme = skin.isDark()
  val colorScheme =
    when {
      skin == AppSkin.LIGHT_YELLOW -> LightYellowColorScheme
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
