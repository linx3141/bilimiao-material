@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package cn.a10miaomiao.bilimiao.compose.components.player.videoplayer.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * 播放器波浪进度条：视觉采用 Material 3 Expressive 官方
 * [LinearWavyProgressIndicator]（active 轨道带波浪），并叠加
 * 播放器进度交互（点按定位 / 横向拖动预览与跳转）。
 *
 * 进度数据与拖动回调均复用 [PlayerProgressSliderState]。
 */
@Composable
fun WavyPlayerProgressSlider(
    state: PlayerProgressSliderState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // 触摸区（高于视觉轨道，便于点按/拖动）
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        val displayRatio = state.displayPositionRatio
        val thumbColor = MaterialTheme.colorScheme.primary
        LinearWavyProgressIndicator(
            progress = { state.displayPositionRatio },
            modifier = Modifier.fillMaxWidth(),
        )
        // 进度末端指示器（可拖动的小圆点），避免进度条“到头就断了”
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width * displayRatio.coerceIn(0f, 1f)
            val centerY = size.height / 2f
            val center = Offset(centerX, centerY)
            val thumbRadius = 7.dp.toPx()
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = center,
            )
        }
        if (enabled) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(state) {
                        detectTapGestures { offset ->
                            if (size.width > 0) {
                                state.previewPositionRatio(
                                    (offset.x / size.width).coerceIn(0f, 1f)
                                )
                                state.finishPreview()
                            }
                        }
                    },
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(state) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                if (size.width > 0) {
                                    state.previewPositionRatio(
                                        (offset.x / size.width).coerceIn(0f, 1f)
                                    )
                                }
                            },
                            onDragEnd = {
                                state.finishPreview()
                            },
                            onDragCancel = {
                                state.cancelPreview()
                            },
                        ) { change, _ ->
                            if (size.width > 0) {
                                state.previewPositionRatio(
                                    (change.position.x / size.width).coerceIn(0f, 1f)
                                )
                            }
                        }
                    },
            )
        }
    }
}
