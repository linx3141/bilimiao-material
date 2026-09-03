package cn.a10miaomiao.bilimiao.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.a10miaomiao.bilimiao.comm.platform.getMaterialYouColor
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

@Composable
internal actual fun rememberSystemDynamicColorScheme(isDark: Boolean): ColorScheme {
    // 桌面端无系统壁纸动态色，使用默认主题色生成常规配色
    return rememberDynamicColorScheme(
        seedColor = Color(getMaterialYouColor()),
        isDark = isDark,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )
}
