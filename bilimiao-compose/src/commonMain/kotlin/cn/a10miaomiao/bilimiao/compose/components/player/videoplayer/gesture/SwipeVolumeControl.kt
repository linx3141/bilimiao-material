@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cn.a10miaomiao.bilimiao.compose.components.player.videoplayer.gesture

import androidx.annotation.MainThread
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs

/**
 * 等级控制器, 用于控制音量/亮度等具有上下限的连续值.
 *
 * 原始 animeko 实现中的 `AudioManager`/`BrightnessManager` 相关扩展已移除,
 * 调用方需自行实现该接口以对接具体平台能力.
 */
interface LevelController {
    val level: Float

    val range: ClosedRange<Float>

    /** 该控制器能表示的最小等级变化. */
    val levelStep: Float get() = 0.01f

    @MainThread
    fun setLevel(level: Float)
}

object NoOpLevelController : LevelController {
    override val level: Float
        get() = 0f

    override val range: ClosedRange<Float> = 0f..1f

    override fun setLevel(level: Float) {

    }
}

@MainThread
fun LevelController.increaseLevel(step: Float = 0.05f) {
    setLevel((level + step).coerceAtMost(range.endInclusive))
}

@MainThread
fun LevelController.decreaseLevel(step: Float = 0.05f) {
    setLevel((level - step).coerceAtLeast(range.start))
}

/**
 * 满程拖动距离 = stepSize * LEVEL_FULL_RANGE_STEPS（与原步进实现的 40 步全行程一致）。
 */
private const val LEVEL_FULL_RANGE_STEPS = 40f

/**
 * 音量/亮度手势控制：把纵向拖动距离**连续线性映射**到等级值
 * （取代原 animeko 的“每步进一格”逻辑），滑动全程跟手：
 *
 * - 起点记录当前等级，向上/向下滑动按 位移/满程 连续换算等级；
 * - 每次变化都会更新 [indicatorState]（浮窗数值实时跟随）；
 * - 横向明显的手势让给 seek，不做任何消费。
 */
fun Modifier.swipeLevelControlWithIndicator(
    controller: LevelController,
    stepSize: Dp,
    orientation: Orientation,
    indicatorState: GestureIndicatorState,
    enabled: Boolean = true,
    step: Float = 0.05f,
    setup: () -> Unit = {},
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "swipeLevelControl"
        properties["controller"] = controller
    },
) {
    val currentEnabled by rememberUpdatedState(enabled)
    Modifier.pointerInput(controller) {
        awaitPointerEventScope {
            val touchSlopPx = viewConfiguration.touchSlop
            // 满程拖动距离（px）：40 步全行程（指针作用域即 Density）
            val fullRangePx = (stepSize * LEVEL_FULL_RANGE_STEPS).toPx()
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false)
                val id = down.id
                val downX = down.position.x
                val downY = down.position.y
                if (!currentEnabled) {
                    // 等待该手指抬起（不消费），期间不做任何手势
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == id }
                        if (change == null) {
                            if (event.changes.none { it.id == id }) break
                            continue
                        }
                        if (!change.pressed) break
                    }
                    continue
                }
                var started = false
                var startLevel = 0f
                var lastLevel = 0f
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == id }
                    if (change == null) {
                        if (event.changes.none { it.id == id }) break
                        continue
                    }
                    if (!change.pressed) break
                    if (change.isConsumed) continue
                    if (!started) {
                        val dx = change.position.x - downX
                        val dy = change.position.y - downY
                        if (abs(dy) > touchSlopPx && abs(dy) * 1.2f >= abs(dx)) {
                            // 纵向为主：启动连续调节并接管本次触摸
                            started = true
                            startLevel = controller.level
                            lastLevel = startLevel
                            indicatorState.visible = true
                            change.consume()
                        } else if (abs(dx) > touchSlopPx &&
                            abs(dx) > abs(dy) * 1.5f
                        ) {
                            // 横向为主：让给 seek，整次触摸不再参与
                            while (true) {
                                val waitEvent = awaitPointerEvent()
                                val waitChange =
                                    waitEvent.changes.firstOrNull { it.id == id }
                                if (waitChange == null) {
                                    if (waitEvent.changes.none { it.id == id }) break
                                    continue
                                }
                                if (!waitChange.pressed) break
                            }
                            break
                        }
                    } else {
                        // 连续映射：level = 起点等级 + 纵向位移 / 满程
                        val target = startLevel +
                            (downY - change.position.y) / fullRangePx
                        val newLevel = target.coerceIn(
                            controller.range.start,
                            controller.range.endInclusive,
                        )
                        if (newLevel != lastLevel) {
                            lastLevel = newLevel
                            controller.setLevel(newLevel)
                            setup()
                            indicatorState.progressValue = newLevel
                        }
                        change.consume()
                    }
                }
                if (started) {
                    indicatorState.visible = false
                }
            }
        }
    }
}

/**
 * 创建音量等级控制器（平台实现）。
 * 安卓端接入系统媒体音量；桌面端为 no-op。
 */
expect fun createVolumeLevelController(): LevelController

/**
 * 创建亮度等级控制器（平台实现）。
 * 安卓端调节当前窗口亮度；桌面端为 no-op。
 */
expect fun createBrightnessLevelController(): LevelController
