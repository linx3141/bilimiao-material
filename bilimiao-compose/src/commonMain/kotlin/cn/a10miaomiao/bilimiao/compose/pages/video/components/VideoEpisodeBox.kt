package cn.a10miaomiao.bilimiao.compose.pages.video.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.components.video.TitleWithChargeBadge
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import coil3.compose.AsyncImage

@Composable
fun VideoEpisodeBox(
    modifier: Modifier = Modifier,
    title: String,
    cover: String,
    desc: String,
    isPlaying: Boolean,
    isCurrent: Boolean,
    isChargeVideo: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = UrlUtil.autoHttps(cover) + "@672w_378h_1c_",
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .size(width = 60.dp, height = 40.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Column(
            modifier = Modifier.weight(1f)
                .padding(start = 5.dp),
        ) {
            TitleWithChargeBadge(
                title = AnnotatedString(title),
                isChargeVideo = isChargeVideo,
                modifier = Modifier.weight(1f),
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (isPlaying) {
                Text(
                    text = "正在播放",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier,
                )
            } else {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier,
                )
            }
        }
    }
}
