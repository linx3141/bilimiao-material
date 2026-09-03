package cn.a10miaomiao.bilimiao.compose.common.foundation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 输入法避让：让页面内容区正好停在键盘顶部，不在键盘上方留下遮罩。
 *
 * 竖屏手机布局中，页面区域位于底栏上方（底栏占位），而系统键盘高度包含
 * 底栏区域；若直接使用 [androidx.compose.foundation.layout.imePadding]，
 * 内容区会比键盘顶部多收缩"底栏 + 导航栏"的高度，这段区域便露在键盘上方
 * 形成背景遮罩。这里把键盘高度扣除底栏与导航栏高度后再作为底部 padding。
 * 横屏（平板竖排导航不占底部）时使用完整键盘高度。
 */
@Composable
fun Modifier.imePaddingAboveBottomBar(
    bottomBarHeight: Dp = 80.dp,
): Modifier {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val navigationBarsInsets = WindowInsets.navigationBars
    val imeBottomPx = with(density) { imeInsets.getBottom(density) }
    if (imeBottomPx <= 0) {
        // 无键盘：不需要避让，页面区域本身已在底栏上方
        return padding(bottom = 0.dp)
    }
    val isLandscape = LocalWindowInfo.current.containerSize.width >
        LocalWindowInfo.current.containerSize.height
    val bottomBarPx: Int = if (isLandscape) {
        0
    } else {
        with(density) { bottomBarHeight.toPx() }.toInt() +
            with(density) { navigationBarsInsets.getBottom(density) }
    }
    val padPx = (imeBottomPx - bottomBarPx).coerceAtLeast(0)
    return padding(bottom = with(density) { padPx.toDp() })
}
