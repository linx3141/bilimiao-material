@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.user.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.video.VideoItemBox
import cn.a10miaomiao.bilimiao.compose.pages.user.UserArchiveViewModel
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil

@Composable
fun UserArchiveListContent(
    viewModel: UserArchiveViewModel,
) {
    val windowInsets = localContentInsets()

    LaunchedEffect(true) {
        viewModel.initData()
    }

    val listFlow = viewModel.list
    val list by listFlow.data.collectAsState()
    val listLoading by listFlow.loading.collectAsState()
    val listFinished by listFlow.finished.collectAsState()
    val listFail by listFlow.fail.collectAsState()

    val emitter = localEmitter()
    val listState = rememberLazyGridState()
    LaunchedEffect(Unit) {
        emitter.collectAction<EmitterAction.DoubleClickTab> {
            if (it.tab == PageTabIds.UserArchive) {
                if (listState.firstVisibleItemIndex == 0) {
                    viewModel.refreshList()
                } else {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        columns = GridCells.Adaptive(300.dp),
        contentPadding = windowInsets.toPaddingValues(
            top = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        itemsIndexed(
            list,
            key = { _, item -> item.param },
            span = { _, _ -> GridItemSpan(maxLineSpan) },
        ) { index, item ->
            CompositionLocalProvider(
                LocalListItemShapes provides segmentedItemShapes(
                    index,
                    list.size,
                ),
            ) {
                VideoItemBox(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    title = item.title,
                    pic = item.cover,
                    playNum = item.play,
                    damukuNum = item.danmaku,
                    remark = NumberUtil.converCTime(item.ctime),
                    duration = NumberUtil.converDuration(item.duration),
                    isChargeVideo = item.isChargeVideo,
                    onClick = {
                        viewModel.toVideoDetail(item)
                    }
                )
            }
        }
        item(
            span = { GridItemSpan(maxLineSpan) }
        ) {
            ListStateBox(
                loading = listLoading,
                finished = listFinished,
                fail = listFail,
                listData = list,
            ) {
                viewModel.loadMore()
            }
        }
    }
}
