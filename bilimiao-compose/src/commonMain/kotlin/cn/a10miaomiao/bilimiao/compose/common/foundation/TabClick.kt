package cn.a10miaomiao.bilimiao.compose.common.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Tab 切换弹簧参数（与 KernelSU Manager #3492 完全一致）。
 *
 * 由原先的阻尼弹簧模型映射而来：
 * ```
 * x'' = -c * x' - k * (x - target)
 * stiffness = k = 322.2
 * dampingRatio = c / (2 * sqrt(k)) ≈ 0.9（c = 32.31）
 * ```
 */
val PagerTabSpringSpec: SpringSpec<Float> = spring(
    stiffness = 322.2f,
    dampingRatio = 32.31f / (2f * kotlin.math.sqrt(322.2f)),
    visibilityThreshold = 0.5f,
)

/**
 * 用弹簧动画滚动 Pager 到 [target] 页（与 KernelSU Manager 的
 * `springAnimateToPage` 一致）。
 *
 * 按像素距离逐帧 [PagerState.scrollBy] 滚动，会平滑经过中间所有页；
 * 动画帧内检查滚动消费情况，被用户手势打断/超界时收尾对齐到目标页。
 */
suspend fun PagerState.springAnimateToPage(target: Int) {
    if (target !in 0 until pageCount) return
    val layoutInfo = layoutInfo
    val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
    if (pageSize <= 0) {
        // 布局尚未完成，退回默认跳转
        if (target != currentPage) {
            animateScrollToPage(target)
        }
        return
    }
    var shouldSnapToTarget = false
    scroll(MutatePriority.UserInput) {
        // 精确剩余距离（含小数偏移）。不能用 currentPage 判断：动画中途
        // currentPage 是"首个可见页"的取整值，可能已等于目标页但画面仍在
        // 两页中间（offsetFraction != 0），若跳过动画就会卡死在中间。
        val currentDistanceInPages = target - currentPage - currentPageOffsetFraction
        val scrollPixels = currentDistanceInPages * pageSize
        if (abs(scrollPixels) <= 0.5f) return@scroll

        var consumedScroll = 0f
        var skipScroll = false
        Animatable(0f).animateTo(
            targetValue = scrollPixels,
            animationSpec = PagerTabSpringSpec,
        ) {
            if (skipScroll) return@animateTo

            val delta = value - consumedScroll
            if (abs(delta) > 0.5f) {
                val consumed = scrollBy(delta)
                consumedScroll += consumed
                if (abs(delta - consumed) > 0.1f) {
                    // 滚动被边界/手势消费中断：放弃剩余动画，直接收尾对齐
                    shouldSnapToTarget = true
                    skipScroll = true
                }
            } else {
                consumedScroll = value
            }

            if (abs(velocity) < 0.1f && abs(scrollPixels - consumedScroll) < 1.0f) {
                skipScroll = true
            }
        }

        val remaining = scrollPixels - consumedScroll
        if (abs(remaining) > 0.5f) {
            scrollBy(remaining)
        }
    }

    if (shouldSnapToTarget || currentPage != target) {
        scrollToPage(target)
    }
}

/** [springAnimateToPage] 的别名，语义：点击 Tab 后的切换动画。 */
suspend fun PagerState.animateTabSwitchTo(target: Int) = springAnimateToPage(target)

@Composable
fun combinedTabDoubleClick(
    pagerState: PagerState,
    onDoubleClick: (Int) -> Unit,
): (Int) -> Unit {
    val scope = rememberCoroutineScope()
    val lastClickTime = remember { arrayOf(0L) }
    fun tabClick(index: Int) {
        if (pagerState.currentPage == index) {
            val nowTime = System.currentTimeMillis()
            if (nowTime - lastClickTime[0] < 2000L) {
                lastClickTime[0] = 0L
                onDoubleClick(index)
            } else {
                lastClickTime[0] = System.currentTimeMillis()
            }
        } else {
            scope.launch {
                pagerState.animateTabSwitchTo(index)
            }
        }
    }
    return ::tabClick
}
