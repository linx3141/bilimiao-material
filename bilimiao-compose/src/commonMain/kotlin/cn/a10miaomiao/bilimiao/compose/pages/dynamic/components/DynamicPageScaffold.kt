package cn.a10miaomiao.bilimiao.compose.pages.dynamic.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bilibili.app.dynamic.v2.UpListItem
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.foundation.animateTabSwitchTo
import cn.a10miaomiao.bilimiao.compose.common.foundation.combinedTabDoubleClick
import cn.a10miaomiao.bilimiao.compose.common.foundation.pagerTabIndicatorOffset
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import kotlinx.coroutines.launch

@Composable
fun DynamicPageScaffold(
    allContent: @Composable () -> Unit,
    videoContent: @Composable () -> Unit,
    mostVisitedContent: @Composable () -> Unit = {},
    upperList: @Composable (maxWidth: Dp) -> Unit,
) {
    val windowInsets = localContentInsets()

    BoxWithConstraints {
        val isMiniUpList = maxWidth < 600.dp
        val upListMaxWidth = if (isMiniUpList) {
            72.dp
        } else if (maxWidth < 1000.dp) {
            180.dp
        } else {
            200.dp
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 宽屏侧栏（UP 列表）在"全部动态"页常驻
            if (!isMiniUpList) {
                upperList(upListMaxWidth)
            }
            // UP 主动态页已改为独立导航页面（DynamicUpperPage），
            // 这里只承载"全部动态"内容（动态/视频/最常访问）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                DynamicAllAndVideoWrap(
                    allContent = allContent,
                    videoContent = videoContent,
                    mostVisitedContent = mostVisitedContent,
                    isMiniUpList = isMiniUpList,
                )
            }
        }
    }
}

@Composable
private fun DynamicAllAndVideoWrap(
    allContent: @Composable () -> Unit,
    videoContent: @Composable () -> Unit,
    mostVisitedContent: @Composable () -> Unit,
    isMiniUpList: Boolean = false,
) {
    val windowInsets = localContentInsets()

    val scope = rememberCoroutineScope()
    val tabs = if (isMiniUpList) {
        listOf(
            PageTabIds.DynamicAll to "动态",
            PageTabIds.DynamicVideo to "视频",
            PageTabIds.DynamicMostVisited to "最常访问",
        )
    } else {
        listOf(
            PageTabIds.DynamicAll to "动态",
            PageTabIds.DynamicVideo to "视频",
        )
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val emitter = localEmitter()
    val combinedTabClick = combinedTabDoubleClick(
        pagerState = pagerState,
        onDoubleClick = {
            scope.launch {
                emitter.emit(
                    EmitterAction.DoubleClickTab(
                        tab = tabs[it].first
                    )
                )
            }
        }
    )


    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isMiniUpList) {
            TabRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(windowInsets.toPaddingValues(bottom = 0.dp)),
                selectedTabIndex = pagerState.currentPage,
                indicator = { positions ->
                    TabRowDefaults.PrimaryIndicator(
                        Modifier.pagerTabIndicatorOffset(pagerState, positions),
                    )
                },
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        text = {
                            Text(
                                text = tab.second,
                                color = if (index == pagerState.currentPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                }
                            )
                        },
                        selected = pagerState.currentPage == index,
                        onClick = { combinedTabClick(index) },
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth()
                    .padding(
                        top = windowInsets.topDp.dp,
                        end = windowInsets.rightDp.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    horizontal = 8.dp
                )
            ) {
                items(tabs.size, { it }) { index ->
                    val tab = tabs[index]
                    FilterChip(
                        selected = index == pagerState.currentPage,
                        onClick = {
                            scope.launch {
                                pagerState.animateTabSwitchTo(index)
                            }
                        },
                        label = {
                            Text(text = tab.second)
                        }
                    )
                }
            }
        }
        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = pagerState,
        ) { index ->
            // 每页独立图层缓存：整树重绘时非当前页走 GPU 合成
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { },
            ) {
                when (index) {
                    0 -> allContent()
                    1 -> videoContent()
                    else -> mostVisitedContent()
                }
            }
        }
    }
}
