package cn.a10miaomiao.bilimiao.compose.common.foundation

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Tab 切换动画：与主界面四个 Tab（首页/动态/搜索/我的）的 Pager 切换完全一致。
 *
 * 按像素距离 [animateScrollBy] 滚动，会平滑经过中间所有页；
 * 不能用 [androidx.compose.foundation.pager.PagerState.animateScrollToPage]——
 * 它距离太远时会预跳到邻近页再动画，造成"先闪现到中间页"，且默认弹簧动画偏快。
 * 相邻页 300ms、EaseInOut 缓动，距离越远时长线性增加。
 */
suspend fun PagerState.animateTabSwitchTo(target: Int) {
    val layoutInfo = layoutInfo
    val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
    if (pageSize <= 0) {
        // 布局尚未完成，退回默认跳转
        if (target != currentPage) {
            animateScrollToPage(target)
        }
        return
    }
    // 精确剩余距离（含小数偏移）。不能用 currentPage 判断：动画中途
    // currentPage 是"首个可见页"的取整值，可能已等于目标页但画面仍在
    // 两页中间（offsetFraction != 0），若跳过动画就会卡死在中间。
    val currentDistanceInPages =
        target - currentPage - currentPageOffsetFraction
    if (abs(currentDistanceInPages) <= 1e-4f) return
    val distance = abs(target - currentPage).coerceAtLeast(2)
    val duration = 100 * distance + 100
    animateScrollBy(
        value = currentDistanceInPages * pageSize,
        animationSpec = tween(easing = EaseInOut, durationMillis = duration),
    )
}

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
