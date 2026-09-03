package cn.a10miaomiao.bilimiao.compose.components.miao

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiaoCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val segmentedShapes = LocalListItemShapes.current
    if (segmentedShapes != null) {
        // 分段（Segmented）列表：与设置页一致，相邻项小圆角、首尾大圆角、连体背景
        Card(
            modifier = modifier,
            shape = segmentedShapes.shape,
            enabled = enabled,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            elevation = CardDefaults.outlinedCardElevation(
                defaultElevation = 0.dp,
                disabledElevation = 0.dp
            ),
            onClick = onClick,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(5.dp),
            enabled = enabled,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            elevation = CardDefaults.outlinedCardElevation(
                defaultElevation = 1.dp,
                disabledElevation = 0.dp
            ),
            onClick = onClick,
            content = content,
        )
    }
}

@Composable
fun MiaoOutlinedCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(5.dp),
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.outlinedCardElevation(
            defaultElevation = 1.dp,
            disabledElevation = 0.dp
        ),
        border = CardDefaults.outlinedCardBorder(),
        onClick = onClick,
        content = content,
    )
}
