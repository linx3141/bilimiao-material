package cn.a10miaomiao.bilimiao.compose.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a10miaomiao.bilimiao.comm.delegate.player.entity.SubtitleLineInfo

/**
 * CC 字幕叠加层：按当前播放位置显示对应字幕行，置于视频画面底部。
 *
 * 字幕行来自 [PlayerSourceState.subtitleLines]（由播放器代理拉取解析 CC 字幕 JSON），
 * 时间轴与播放位置同单位（毫秒）。当前无匹配行时不做绘制。
 * 全屏播放时字号更大，未全屏（内嵌）时字号适当调小。
 */
@Composable
fun SubtitleOverlay(
    positionMillis: Long,
    lines: List<SubtitleLineInfo>?,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier,
) {
    if (lines == null || lines.isEmpty()) return
    val currentLine = remember(lines, positionMillis) {
        lines.firstOrNull { positionMillis in it.fromMs until it.toMs }
    } ?: return
    val fontSize = if (isFullscreen) 20.sp else 16.sp
    val lineHeight = if (isFullscreen) 26.sp else 21.sp
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = currentLine.text,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize,
            lineHeight = lineHeight,
            softWrap = true,
            modifier = Modifier
                .padding(start = 32.dp, end = 32.dp, bottom = 24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
