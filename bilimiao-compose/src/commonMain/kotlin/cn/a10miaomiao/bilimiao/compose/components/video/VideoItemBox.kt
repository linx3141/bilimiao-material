package cn.a10miaomiao.bilimiao.compose.components.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bilimiao.bilimiao_compose.generated.resources.Res
import bilimiao.bilimiao_compose.generated.resources.bili_default_placeholder_img_tv
import bilimiao.bilimiao_compose.generated.resources.bili_fail_placeholder_img_tv
import cn.a10miaomiao.bilimiao.compose.assets.BilimiaoIcons
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.Common
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.common.Danmukunum
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.common.Playnum
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.common.Upper
import cn.a10miaomiao.bilimiao.compose.common.foundation.htmlText
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoItemBox(
    modifier: Modifier = Modifier,
    title: String? = null,
    pic: String? = null,
    upperName: String? = null,
    remark: String? = null,
    playNum: String? = null,
    damukuNum: String? = null,
    duration: String? = null,
    progress: Float = -1f,
    isHtml: Boolean = false,
    isChargeVideo: Boolean = false,
    segmentedShape: Shape? = null,
    onClick: (() -> Unit)? = null,
) {

    // 内容行：封面 + 文字信息
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
        if (pic != null) {
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 85.dp)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                AsyncImage(
                    model = UrlUtil.autoHttps(pic) + "@672w_378h_1c_",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = painterResource(Res.drawable.bili_default_placeholder_img_tv),
                    error = painterResource(Res.drawable.bili_fail_placeholder_img_tv),
                )
                if (duration != null) {
                    Box(
                        modifier = Modifier
                            .wrapContentHeight()
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .background(
                                color = Color(0x99000000),
                                shape = RoundedCornerShape(5.dp)
                            )
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                    ) {
                        Text(
                            text = duration,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.semantics {
                                contentDescription = "视频时长：$duration"
                            }
                        )
                    }
                }
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                            .align(Alignment.BottomStart),
                        drawStopIndicator = { }
                    )
                }
            }

        }
        Column(
            modifier = Modifier
                .weight(1f)
                .height(85.dp)
                .padding(start = 10.dp),
        ) {
            if (title != null) {
                TitleWithChargeBadge(
                    title = if (isHtml) htmlText(title) else AnnotatedString(title),
                    isChargeVideo = isChargeVideo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (upperName != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        imageVector = BilimiaoIcons.Common.Upper,
                        contentDescription = null,
                    )
                    Text(
                        modifier = Modifier.padding(start = 2.dp)
                            .semantics {
                                contentDescription = "up主：$upperName"
                            },
                        text = upperName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (remark != null) {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = NumberUtil.converString(remark),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (playNum != null && damukuNum != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        imageVector = BilimiaoIcons.Common.Playnum,
                        contentDescription = null,
                    )
                    Text(
                        modifier = Modifier.padding(start = 2.dp)
                            .semantics {
                                contentDescription = "播放量：$playNum"
                            },
                        text = NumberUtil.converString(playNum),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        imageVector = BilimiaoIcons.Common.Danmukunum,
                        contentDescription = null,
                    )
                    Text(
                        modifier = Modifier.padding(start = 2.dp)
                            .semantics {
                                contentDescription = "弹幕数：$damukuNum"
                            },
                        text = NumberUtil.converString(damukuNum),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        }
    }

    val segmentedShapes = LocalListItemShapes.current
    if (segmentedShapes != null || segmentedShape != null) {
        // 分段（Segmented）列表：与设置页一致，相邻项小圆角、首尾大圆角、连体背景
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = segmentedShape ?: segmentedShapes?.shape ?: RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceBright,
            onClick = onClick ?: {},
            content = content,
        )
    } else if (onClick != null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            onClick = onClick,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            content = content,
        )
    }
}
