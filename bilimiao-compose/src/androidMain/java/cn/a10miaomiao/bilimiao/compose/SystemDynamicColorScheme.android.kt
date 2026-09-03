package cn.a10miaomiao.bilimiao.compose

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.a10miaomiao.bilimiao.comm.platform.getMaterialYouColor
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

@Composable
internal actual fun rememberSystemDynamicColorScheme(isDark: Boolean): ColorScheme {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // 使用系统动态配色：壁纸与系统配色风格（tonal spot / vibrant / expressive 等）变化时自动跟随
        if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        // Android 11 及以下无系统动态色，按系统主题色生成常规配色
        rememberDynamicColorScheme(
            seedColor = Color(getMaterialYouColor()),
            isDark = isDark,
            style = PaletteStyle.TonalSpot,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        )
    }
}
