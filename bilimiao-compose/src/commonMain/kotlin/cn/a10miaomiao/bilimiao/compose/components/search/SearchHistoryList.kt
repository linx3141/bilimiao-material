package cn.a10miaomiao.bilimiao.compose.components.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.common.preference.ExpressivePreferenceItem
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes

/**
 * 搜索历史列表（单列分段 m3e 样式）：标题 + 历史关键词（可点击/删除）+ 清空。
 * 供全局搜索、历史记录搜索、UP 主搜索等输入页共用。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchHistoryList(
    history: List<String>,
    onKeywordClick: (String) -> Unit,
    onKeywordDelete: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    if (history.isEmpty()) return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 0.dp,
            end = 12.dp,
            bottom = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "header") {
            Text(
                text = "搜索历史",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
            )
        }
        itemsIndexed(
            history,
            key = { _, text -> text },
        ) { index, text ->
            CompositionLocalProvider(
                LocalListItemShapes provides segmentedItemShapes(
                    index,
                    history.size,
                ),
            ) {
                ExpressivePreferenceItem(
                    title = {
                        Text(text = text)
                    },
                    onClick = { onKeywordClick(text) },
                    trailing = {
                        IconButton(onClick = { onKeywordDelete(text) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }
        }
        item(key = "clear") {
            Text(
                text = "清空搜索历史",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClearAll)
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
