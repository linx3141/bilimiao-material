@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.components.dyanmic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.components.miao.MiaoCard
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes

@Composable
fun DynamicItemCard(
    modifier: Modifier = Modifier,
    item: bilibili.app.dynamic.v2.DynamicItem,
    isJumpToUser: Boolean = true,
    onClick: () -> Unit,
) {
    val segmentedShapes = LocalListItemShapes.current
    MiaoCard(
        modifier = modifier.then(
            if (segmentedShapes == null) {
                Modifier.padding(horizontal = 10.dp)
            } else {
                Modifier
            }
        ),
        onClick = onClick,
    ) {
        for(module in item.modules) {
            DynamicModuleBox(
                module = module,
                isJumpToUser = isJumpToUser,
                item = item,
            )
        }
    }
}
