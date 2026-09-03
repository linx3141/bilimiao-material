package cn.a10miaomiao.bilimiao.compose.components.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import cn.a10miaomiao.bilimiao.compose.components.miao.MiaoCard
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun UserInfoCard(
    name: String,
    face: String,
    sign: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionContent: @Composable RowScope.() -> Unit,
) {
    val segmentedShapes = LocalListItemShapes.current
    if (segmentedShapes != null) {
        // 分段（Segmented）列表：与设置页一致，相邻项小圆角、首尾大圆角、连体背景
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = segmentedShapes.shape,
            color = MaterialTheme.colorScheme.surfaceBright,
            onClick = onClick,
        ) {
            UserInfoRowContent(
                name = name,
                face = face,
                sign = sign,
                actionContent = actionContent,
            )
        }
    } else {
        MiaoCard(
            modifier = modifier.fillMaxWidth(),
            onClick = onClick,
        ) {
            UserInfoRowContent(
                name = name,
                face = face,
                sign = sign,
                actionContent = actionContent,
            )
        }
    }
}

@Composable
private fun UserInfoRowContent(
    name: String,
    face: String,
    sign: String,
    actionContent: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = UrlUtil.autoHttps(face) + "@200w_200h",
            placeholder = painterResource(Res.drawable.bili_akari_img),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = name,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 2.dp),
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleSmall,
            )
            if (sign.isNotBlank()) {
                Text(
                    text = sign,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.outline,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        actionContent()
    }
}
