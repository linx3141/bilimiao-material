package cn.a10miaomiao.bilimiao.compose.pages.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bilimiao.bilimiao_compose.generated.resources.Res
import bilimiao.bilimiao_compose.generated.resources.bili_akari_img
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.components.user.UserLevelIcon
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthorItemBox(
    modifier: Modifier = Modifier,
    name: String,
    face: String,
    sign: String,
    fans: Int,
    archives: Int,
    level: Int,
    onClick: () -> Unit,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 固定行高 = 头像高度 + 上下 10dp 内边距，
                // 头像填满内容区后到卡片上/下/侧边距均为 10dp
                .height(80.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = UrlUtil.autoHttps(face) + "@200w_200h",
                placeholder = painterResource(Res.drawable.bili_akari_img),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    UserLevelIcon(
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .size(20.dp, 15.dp),
                        level = level,
                    )
                }
                Row {
                    Text(
                        text = NumberUtil.converString(fans) + "粉丝",
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(modifier = Modifier.width(15.dp))
                    Text(
                        text = NumberUtil.converString(archives) + "个视频",
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    text = sign,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.outline,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
    val segmentedShapes = LocalListItemShapes.current
    if (segmentedShapes != null) {
        // 分段（Segmented）列表：与设置页一致，连体圆角 + surfaceBright 背景
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = segmentedShapes.shape,
            color = MaterialTheme.colorScheme.surfaceBright,
            onClick = onClick,
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier
                .clickable { onClick() }
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}
