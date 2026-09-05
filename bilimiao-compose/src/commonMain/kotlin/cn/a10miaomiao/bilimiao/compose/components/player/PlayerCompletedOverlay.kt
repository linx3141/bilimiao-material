package cn.a10miaomiao.bilimiao.compose.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 播放完成遮罩：铺满整个视频画面。
 *
 * - 半透明阴影盖住画面
 * - 中央显示"播放完成"与 退出播放 / 重新播放 两个按钮
 * - 遮罩消费全部触摸事件：播放完成态下无法点出上/下控制条，
 *   也无法触发手势（点按/滑动均被拦截）
 */
@Composable
fun PlayerCompletedOverlay(
    onReplay: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f))
            .pointerInput(Unit) {
                // 消费所有触摸，屏蔽下层控制器/手势
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "播放完成",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Button(onClick = onExit) {
                    Text("退出播放")
                }
                Button(onClick = onReplay) {
                    Text(
                        "重新播放",
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}
