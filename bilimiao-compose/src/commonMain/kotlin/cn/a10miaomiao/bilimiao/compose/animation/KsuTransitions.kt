// 本文件照抄自 KernelSU Manager 使用的 miuix-navigation3-ui 默认导航过渡动画。
//
// 原实现出处：
//   compose-miuix-ui / miuix-navigation3-ui
//   NavDisplay.kt（defaultTransitionSpec / defaultPopTransitionSpec /
//   defaultPredictivePopTransitionSpec）与 NavTransitionEasing.kt
//   版权：Copyright 2025, compose-miuix-ui contributors（含 Android Open Source Project）
//   许可证：Apache-2.0（https://www.apache.org/licenses/LICENSE-2.0）
// KernelSU Manager 参考：https://github.com/tiann/KernelSU（GPL-3.0）
// 本文件仅借鉴其页面过渡动画的表现与数值，使用方法与参数保持一致。
package cn.a10miaomiao.bilimiao.compose.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 导航缓动：带轻微回弹的阻尼曲线。
 * 与 miuix-navigation3-ui 的 [NavTransitionEasing] 一致（response = 0.8f, damping = 0.95f）。
 */
@Immutable
internal class NavTransitionEasing(
    response: Float,
    damping: Float,
) : Easing {
    private val r: Float
    private val w: Float
    private val c2: Float

    init {
        val omega = 2.0 * PI / response
        val k = omega * omega
        val c = damping * 4.0 * PI / response

        w = (sqrt(4.0 * k - c * c) / 2.0).toFloat()
        r = (-c / 2.0).toFloat()
        c2 = r / w
    }

    override fun transform(fraction: Float): Float {
        val t = fraction.toDouble()
        val decay = exp(r * t)
        return (decay * (-cos(w * t) + c2 * sin(w * t)) + 1.0).toFloat()
    }

    fun inverseTransform(fraction: Float, tolerance: Float = 1e-6f): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f

        var low = 0f
        var high = 1f
        var mid = 0f

        repeat(16) {
            mid = (low + high) / 2f
            val value = transform(mid)
            if (abs(value - fraction) < tolerance) return mid

            if (value < fraction) {
                low = mid
            } else {
                high = mid
            }
        }
        return mid
    }
}

private val NavAnimationEasing = NavTransitionEasing(0.8f, 0.95f)

/**
 * 打开页面动画：新页面从右侧全宽滑入，当前页向左以 1/4 视差滑出。
 * 500ms，回弹曲线。与 KernelSU Manager 的默认 transitionSpec 一致。
 */
fun AnimatedContentTransitionScope<*>.ksuOpenTransition(): ContentTransform =
    ContentTransform(
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
        ),
        slideOutHorizontally(
            targetOffsetX = { -it / 4 },
            animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
        ),
    )

/**
 * 退出页面动画：上一页从左侧以 1/4 视差滑入，当前页向右全宽滑出。
 * 500ms，回弹曲线。与 KernelSU Manager 的默认 popTransitionSpec 一致。
 */
fun AnimatedContentTransitionScope<*>.ksuCloseTransition(): ContentTransform =
    ContentTransform(
        slideInHorizontally(
            initialOffsetX = { -it / 4 },
            animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
        ),
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
        ),
    )

/**
 * 预测性返回动画：方向固定为屏幕左侧返回的方向（不随左右手势边缘变化），
 * 上一页从左侧以 1/4 视差滑入，当前页向右全宽滑出。
 * 500ms 回弹曲线（KernelSU Manager 在预测性返回收尾时实际使用的缓动），
 * 跟手阶段由手势进度驱动、收尾自然减速，不生硬。
 */
fun AnimatedContentTransitionScope<*>.ksuPredictiveBackTransition(): ContentTransform =
    ContentTransform(
        slideInHorizontally(
            initialOffsetX = { -it / 4 },
            animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
        ),
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
        ),
    )

/**
 * 平板（竖排导航）打开页面动画：新页面从下方全高滑入，当前页向下以 1/4 视差滑出。
 * 与手机横向的 [ksuOpenTransition] 同一套曲线与视差，仅方向改为垂直。
 */
fun AnimatedContentTransitionScope<*>.ksuOpenTransitionVertical(): ContentTransform =
    ContentTransform(
        slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
        ),
        slideOutVertically(
            targetOffsetY = { -it / 4 },
            animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
        ),
    )

/**
 * 平板退出页面动画：上一页从上方以 1/4 视差滑入，当前页向下全高滑出。
 */
fun AnimatedContentTransitionScope<*>.ksuCloseTransitionVertical(): ContentTransform =
    ContentTransform(
        slideInVertically(
            initialOffsetY = { -it / 4 },
            animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
        ),
        slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
        ),
    )
