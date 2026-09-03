package cn.a10miaomiao.bilimiao.compose.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import cn.a10miaomiao.bilimiao.compose.common.localPlayerState
import com.a10miaomiao.bilimiao.comm.store.PlayerStore
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import org.kodein.di.compose.rememberInstance

/**
 * 播放器锚点容器组件。
 *
 * 当播放器正在播放 [aid] 对应的视频时，播放器窗口会覆盖到此组件的位置上，
 * 同时 [content]（通常是封面）会被隐藏。
 *
 * 当播放器未播放此视频时，[content] 正常显示。
 *
 * **锚点冻结**：页面切换/返回等过渡动画是逐帧布局位移，会带动本组件位置逐帧变化。
 * 若每帧都把新位置上报给播放器，播放器画面会跟着页面一起滑动（旧版播放器悬浮在
 * 导航层之上，不会随页面移动）。这里在连续高频位移（动画帧）期间冻结上报，
 * 画面停留在动画开始前的位置；动画结束（返回完成移除本组件，或手势取消页面归位）
 * 后恢复同步。
 *
 * @param aid 关联的视频 aid，用于判断播放器是否应覆盖到此位置
 * @param modifier 修饰符
 * @param content 封面内容，当播放器覆盖时隐藏
 */
@Composable
fun PlayerAnchorBox(
    aid: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val playerState = localPlayerState()
    val playerStore by rememberInstance<PlayerStore>()
    val playerStoreState by playerStore.stateFlow.collectAsState()

    val isPlayingThisVideo = playerStoreState.aid == aid && playerState.showPlayer

    // 最近一次实际写入锚点的时间点与位置，用于识别"动画帧"（两次布局间隔极短）
    val lastWriteMark = remember { mutableStateOf<TimeMark?>(null) }
    val lastWriteBounds = remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            if (isPlayingThisVideo) {
                val position = coordinates.positionInRoot()
                val size = coordinates.size
                val newBounds = Rect(
                    left = position.x,
                    top = position.y,
                    right = position.x + size.width,
                    bottom = position.y + size.height,
                )
                val mark = TimeSource.Monotonic.markNow()
                val sinceLastWrite = lastWriteMark.value?.elapsedNow()
                val isAnimationFrame =
                    sinceLastWrite != null &&
                        lastWriteBounds.value != null &&
                        sinceLastWrite < ANCHOR_UPDATE_MIN_INTERVAL
                if (!isAnimationFrame) {
                    // 静止/低频布局：正常上报
                    lastWriteMark.value = mark
                    lastWriteBounds.value = newBounds
                    playerState.setAnchorBounds(newBounds)
                }
                // 动画帧：冻结，不更新锚点（画面停留在动画开始前的位置）
            }
        }
    ) {
        // 当播放器覆盖到此位置时，隐藏封面内容
        AnimatedVisibility(
            visible = !isPlayingThisVideo,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            content()
        }
    }

    // 当组件离开组合或不再播放此视频时，清除 anchorBounds
    DisposableEffect(aid) {
        onDispose {
            if (playerState.anchorBounds != null) {
                playerState.setAnchorBounds(null)
            }
        }
    }

    // 当 isPlayingThisVideo 变为 false 时清除 anchorBounds
    LaunchedEffect(isPlayingThisVideo) {
        if (!isPlayingThisVideo && playerState.anchorBounds != null) {
            playerState.setAnchorBounds(null)
        }
    }
}

/**
 * 锚点连续上报间隔小于该时长视为动画帧（导航过渡为 60/120fps 逐帧布局）。
 * 动画中会保持冻结，动画结束后的下一次布局间隔必然大于该值。
 */
private val ANCHOR_UPDATE_MIN_INTERVAL = 100.milliseconds
