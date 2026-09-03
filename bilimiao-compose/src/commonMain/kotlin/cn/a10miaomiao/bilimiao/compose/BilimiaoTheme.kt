package cn.a10miaomiao.bilimiao.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.store.AppStore
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicColorScheme

/**
 * 应用主题：基于 Material 3 Expressive 设计系统。
 *
 * - 配色：
 *   - Material You：使用系统动态配色，跟随系统壁纸与配色风格（tonal spot / vibrant / expressive 等）；
 *   - 手选主题色：使用 2025 色彩规范（SPEC_2025）+ TonalSpot 调色风格（不再强制 Expressive），
 *     深色模式采用标准表面色阶（surface tone 4 等），不使用 AMOLED 纯黑。
 * - 动效：使用 Expressive 运动方案（弹簧动效），主题颜色切换时平滑过渡。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BilimiaoTheme(
    appState: AppStore.State,
    content: @Composable () -> Unit
) {
    val themeState = appState.theme ?: return
    val isDarkTheme = isAppDarkTheme(themeState)
    val isAmoled = themeState.darkMode == 6
    val paletteStyle = try {
        PaletteStyle.valueOf(themeState.paletteStyle)
    } catch (_: Exception) {
        PaletteStyle.TonalSpot
    }
    val colorSpec = try {
        ColorSpec.SpecVersion.valueOf(themeState.colorSpec)
    } catch (_: Exception) {
        ColorSpec.SpecVersion.SPEC_2025
    }
    val baseScheme = if (themeState.type == SettingConstants.THEME_TYPE_DYNAMIC_COLOR) {
        // 动态色：以系统动态色板的 primary 为种子，仍走莫奈取色逻辑，
        // 配色风格与色彩规范同样生效（与 KernelSU 一致）
        val systemScheme = rememberSystemDynamicColorScheme(isDarkTheme)
        rememberBilimiaoColorScheme(
            seedColor = systemScheme.primary,
            isDark = isDarkTheme,
            isAmoled = isAmoled,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
        )
    } else {
        // 莫奈取色：配色风格与色彩规范跟随 KernelSU 的取色逻辑
        rememberBilimiaoColorScheme(
            seedColor = Color(themeState.color),
            isDark = isDarkTheme,
            isAmoled = isAmoled,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
        )
    }
    // 与 KernelSU Material 主题一致：
    // - 页面背景使用 surfaceContainer（KSU ExpressiveScaffold 默认背景色）
    // - 列表/卡片背景使用 surfaceBright（KSU surfaceColorAtElevation(3.dp) 对应的表面色）
    val colorScheme = baseScheme.copy(
        surface = baseScheme.surfaceContainer,
        background = baseScheme.surfaceContainer,
    )
    // 页面缩放：根据主题设置的整体缩放比例调整 Density（与 KernelSU 一致）
    val systemDensity = LocalDensity.current
    val pageScale = themeState.pageScale
    val scaledDensity = remember(systemDensity, pageScale) {
        Density(
            density = systemDensity.density * pageScale,
            fontScale = systemDensity.fontScale,
        )
    }
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialExpressiveTheme(
            colorScheme = animateColorScheme(colorScheme),
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

@Composable
fun isAppDarkTheme(themeState: AppStore.ThemeSettingState): Boolean {
    return when (themeState.darkMode) {
        0 -> isSystemInDarkTheme()
        1 -> false
        2, 6 -> true
        else -> isSystemInDarkTheme()
    }
}
