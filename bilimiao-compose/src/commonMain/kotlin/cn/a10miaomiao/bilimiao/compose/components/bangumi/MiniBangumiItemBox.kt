package cn.a10miaomiao.bilimiao.compose.components.bangumi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.components.miao.MiaoCard
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import coil3.compose.AsyncImage

@Composable
fun MiniBangumiItemBox(
    modifier: Modifier = Modifier,
    title: String,
    cover: String,
    desc: String? = null,
    onClick: () -> Unit,
) {
    MiaoCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            // 封面：圆角缩略图（与外面视频列表一致），不填满卡片边缘
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(560f / 746f)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                AsyncImage(
                    model = UrlUtil.autoHttps(cover) + "@560w_746h",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
            if (desc != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
