package cn.a10miaomiao.bilimiao.compose.common.foundation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import cn.a10miaomiao.bilimiao.compose.common.isImeVisible
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 输入法避让的"动画期冻结"版本。
 *
 * 与实时 [androidx.compose.foundation.layout.imePadding] 不同，这里不逐帧跟随键盘
 * 动画的 insets 中间值：键盘可见状态翻转时立即以缓存的高度为目标，用一次短暂的
 * tween 平滑调整到最终位置。键盘动画大部分时间里布局保持静止（只在开头短暂调整），
 * 避免 IME 动画期间主窗口每帧重排/重绘造成的输入卡顿，
 * 同时键盘一出现内容区就完成让位，键盘上方不留空白。
 */
@Composable
fun Modifier.settledImePadding(
    settleDelayMillis: Long = 120,
    adjustDurationMillis: Int = 150,
): Modifier {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    var keyboardHeightPx by remember { mutableIntStateOf(0) }

    @OptIn(FlowPreview::class)
    LaunchedEffect(Unit) {
        snapshotFlow { imeInsets.getBottom(density) }
            .distinctUntilChanged()
            .debounce(settleDelayMillis)
            .collect { keyboardHeightPx = it }
    }

    // 键盘可见状态翻转（点击输入框/按返回）时立即更新目标高度；
    // 首次展开尚无缓存高度时由 debounce 兜底
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    val imeVisible = isImeVisible()
    val imeBottomDp: Dp by animateDpAsState(
        targetValue = with(density) {
            (if (imeVisible) keyboardHeightPx else 0).toDp()
        },
        animationSpec = tween(durationMillis = adjustDurationMillis),
        label = "settledImePadding",
    )
    return padding(bottom = imeBottomDp)
}
