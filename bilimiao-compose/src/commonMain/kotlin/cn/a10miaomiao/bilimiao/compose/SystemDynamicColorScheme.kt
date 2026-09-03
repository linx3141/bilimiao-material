package cn.a10miaomiao.bilimiao.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * 获取跟随系统的 Material You 动态配色。
 *
 * Android 12+ 返回系统壁纸动态色（跟随系统当前配色风格，如 tonal spot / vibrant /
 * expressive）；低版本与桌面端无系统动态色，回退为种子色生成的常规配色。
 */
@Composable
internal expect fun rememberSystemDynamicColorScheme(isDark: Boolean): ColorScheme
