@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.dynamic.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bilibili.app.dynamic.v2.UpListItem
import cn.a10miaomiao.bilimiao.compose.common.localPageNavigation
import cn.a10miaomiao.bilimiao.compose.components.video.gridSegmentedShape
import cn.a10miaomiao.bilimiao.compose.components.video.rememberGridColumnCount
import cn.a10miaomiao.bilimiao.compose.pages.mine.MyFollowPage
import coil3.compose.AsyncImage

/**
 * 动态页"最常访问"Tab 内容：多列分段网格展示经常访问的 UP 主。
 * 列数由屏幕宽度决定（手机两列，平板更多），卡片为 M3E 分段样式
 * （相邻项小圆角、网格四角大圆角、surfaceBright 背景）。
 * 点击进入该 UP 主的动态页。
 */
@Composable
fun DynamicMostVisitedContent(
    modifier: Modifier = Modifier,
    upperList: List<UpListItem>,
    selectedUpper: UpListItem? = null,
    onSelected: (UpListItem) -> Unit,
) {
    val pageNavigation = localPageNavigation()
    if (upperList.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "暂无最常访问的UP主",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val colCount = rememberGridColumnCount(minColumnWidth = 150.dp)
    LazyVerticalGrid(
        columns = GridCells.Fixed(colCount),
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        itemsIndexed(
            items = upperList,
            key = { _, item -> item.uid },
        ) { index, item ->
            val isSelected = selectedUpper?.uid == item.uid
            // 多列分段网格卡片：四角大圆角、相邻小圆角（与视频网格一致）
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = gridSegmentedShape(index, upperList.size, colCount),
                color = MaterialTheme.colorScheme.surfaceBright,
                onClick = { onSelected(item) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = item.face,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(
                    onClick = {
                        pageNavigation.navigate(MyFollowPage())
                    },
                ) {
                    Text(
                        text = "更多关注",
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = "more",
                    )
                }
            }
        }
    }
}
