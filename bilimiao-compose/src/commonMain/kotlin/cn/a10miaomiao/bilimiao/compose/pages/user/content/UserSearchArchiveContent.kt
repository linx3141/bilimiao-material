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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bilibili.app.interfaces.v1.SearchArchiveReq
import bilibili.app.interfaces.v1.SpaceGRPC
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.entity.FlowPaginationInfo
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.video.VideoItemBox
import cn.a10miaomiao.bilimiao.compose.components.video.isChargeVideo
import com.a10miaomiao.bilimiao.comm.entity.comm.PaginationInfo
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

private typealias ArchiveItem = bilibili.app.archive.v1.Arc

private class UserSearchArchiveContentViewModel(
    override val di: DI,
    private val mid: Long,
    private val keyword: String,
) : ViewModel(), DIAware {

    private val pageNavigation: PageNavigation by instance()

    val isRefreshing = MutableStateFlow(false)
    val list = FlowPaginationInfo<ArchiveItem>()

    init {
        loadData(1)
    }

    private fun loadData(
        pageNum: Int = list.pageNum
    ) = viewModelScope.launch(Dispatchers.IO){
        try {
            list.loading.value = true
            val req = SearchArchiveReq(
                keyword = keyword,
                mid = mid,
                pn = pageNum.toLong(),
                ps = list.pageSize.toLong(),
            )
            val res = BiliGRPCHttp.request {
                SpaceGRPC.searchArchive(req)
            }.awaitCall()
            val archivesList = res.archives.map {
                it.archive
            }.filterNotNull()
            if (pageNum == 1) {
                list.data.value = archivesList
            } else {
                list.data.value = list.data.value
                    .toMutableList()
                    .apply { addAll(archivesList) }
            }
            list.finished.value = archivesList.size < list.pageSize
        } catch (e: Exception) {
            e.printStackTrace()
            list.fail.value = e.message ?: e.toString()
        } finally {
            list.loading.value = false
            isRefreshing.value = false
        }
    }

    fun tryAgainLoadData() = loadData()

    fun refresh() {
        isRefreshing.value = true
        list.reset()
        loadData(1)
    }

    fun loadMore() {
        if (!list.finished.value && !list.loading.value) {
            loadData(list.pageNum + 1)
        }
    }

    fun toDetailPage(
        item: ArchiveItem
    ) {
        pageNavigation.navigateToVideoInfo(item.aid.toString())
    }

}

@Composable
fun UserSearchArchiveContent(
    mid: Long,
    keyword: String,
) {
    val windowInsets = localContentInsets()

    val viewModel = diViewModel(
        key = "${PageTabIds.UserSearchArchive}:$mid:$keyword"
    ) {
        UserSearchArchiveContentViewModel(it, mid, keyword)
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
            if (it.tab == PageTabIds.UserSearchArchive) {
                if (listState.firstVisibleItemIndex == 0) {
                    viewModel.refresh()
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
                    pic = item.pic,
                    playNum = item.stat?.view.toString(),
                    damukuNum = item.stat?.danmaku.toString(),
                    remark = NumberUtil.converCTime(item.ctime),
                    duration = NumberUtil.converDuration(item.duration),
                    isHtml = true,
                    isChargeVideo = item.rights.isChargeVideo(),
                    onClick = {
                        viewModel.toDetailPage(item)
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
