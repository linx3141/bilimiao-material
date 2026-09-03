package cn.a10miaomiao.bilimiao.compose.components.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import bilimiao.bilimiao_compose.generated.resources.Res
import bilimiao.bilimiao_compose.generated.resources.bili_default_placeholder_img_tv
import bilimiao.bilimiao_compose.generated.resources.bili_fail_placeholder_img_tv
import cn.a10miaomiao.bilimiao.compose.assets.BilimiaoIcons
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.Common
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.common.Danmukunum
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.common.Playnum
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.common.Upper
import cn.a10miaomiao.bilimiao.compose.components.miao.MiaoCard
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource

@Composable
fun MiniVideoItemBox(
    modifier: Modifier = Modifier,
    title: String? = null,
    pic: String? = null,
    upperName: String? = null,
    remark: String? = null,
    playNum: String? = null,
    damukuNum: String? = null,
    duration: String? = null,
    isHtml: Boolean = false,
    isChargeVideo: Boolean = false,
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
                    .aspectRatio(672f / 378f)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                if (pic != null) {
                    AsyncImage(
                        model = UrlUtil.autoHttps(pic) + "@672w_378h_1c_",
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = painterResource(Res.drawable.bili_default_placeholder_img_tv),
                        error = painterResource(Res.drawable.bili_fail_placeholder_img_tv),
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                            )
                        )
                        .padding(5.dp)
                ) {
                    if (playNum != null && damukuNum != null) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            tint = Color.White,
                            imageVector = BilimiaoIcons.Common.Playnum,
                            contentDescription = "播放量"
                        )
                        Text(
                            modifier = Modifier.padding(start = 2.dp),
                            text = NumberUtil.converString(playNum),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            modifier = Modifier.size(16.dp),
                            tint = Color.White,
                            imageVector = BilimiaoIcons.Common.Danmukunum,
                            contentDescription = "弹幕数"
                        )
                        Text(
                            modifier = Modifier.padding(start = 2.dp),
                            text = damukuNum,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (duration != null) {
                        Text(
                            modifier = Modifier.padding(start = 2.dp)
                                .semantics {
                                    contentDescription = "视频时长：$duration"
                                },
                            text = duration,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (title != null) {
                TitleWithChargeBadge(
                    title = AnnotatedString(title),
                    isChargeVideo = isChargeVideo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (upperName != null) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        imageVector = BilimiaoIcons.Common.Upper,
                        contentDescription = "UP主",
                    )
                    Text(
                        modifier = Modifier.padding(start = 2.dp),
                        text = upperName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (remark != null) {
            Text(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                text = NumberUtil.converString(remark),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

}
