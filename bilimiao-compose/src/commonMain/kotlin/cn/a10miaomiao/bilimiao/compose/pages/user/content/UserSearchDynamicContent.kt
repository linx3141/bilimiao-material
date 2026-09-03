package cn.a10miaomiao.bilimiao.compose.pages.user.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import bilibili.app.dynamic.v2.DynamicItem
import bilibili.app.interfaces.v1.SearchArchiveReq
import bilibili.app.interfaces.v1.SearchDynamicReq
import bilibili.app.interfaces.v1.SpaceGRPC
import cn.a10miaomiao.bilimiao.compose.common.addPaddingValues
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.entity.FlowPaginationInfo
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.components.dyanmic.DynamicItemCard
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance


private class UserSearchDynamicContentViewModel(
    override val di: DI,
    private val mid: Long,
    private val keyword: String,
) : ViewModel(), DIAware {

    private val pageNavigation: PageNavigation by instance()

    val isRefreshing = MutableStateFlow(false)
    val list = FlowPaginationInfo<DynamicItem>()

    init {
        loadData(1)
    }

    private fun loadData(
        pageNum: Int = list.pageNum
    ) = viewModelScope.launch(Dispatchers.IO){
        try {
            list.loading.value = true
            val req = SearchDynamicReq(
                keyword = keyword,
                mid = mid,
                pn = pageNum.toLong(),
                ps = list.pageSize.toLong(),
            )
            val res = BiliGRPCHttp.request {
                SpaceGRPC.searchDynamic(req)
            }.awaitCall()

            val archivesList = res.dynamics.map {
                it.dynamic
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
        item: DynamicItem
    ) {
        val extend = item.extend ?: return
        val toUrl = extend.cardUrl
        try {
            pageNavigation.navigateByUri(toUrl)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserSearchDynamicContent(
    mid: Long,
    keyword: String,
) {
    val viewModel = diViewModel(
        key = "${PageTabIds.UserSearchDynamic}:$mid:$keyword"
    ) {
        UserSearchDynamicContentViewModel(it, mid, keyword)
    }
    val windowInsets = localContentInsets()

    val listFlow = viewModel.list
    val list by listFlow.data.collectAsState()
    val listLoading by listFlow.loading.collectAsState()
    val listFinished by listFlow.finished.collectAsState()
    val listFail by listFlow.fail.collectAsState()

    val listState = rememberLazyListState()
    val emitter = localEmitter()
    LaunchedEffect(Unit) {
        emitter.collectAction<EmitterAction.DoubleClickTab> {
            if (it.tab == PageTabIds.UserSearchDynamic) {
                if (listState.firstVisibleItemIndex == 0) {
                    viewModel.refresh()
                } else {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = windowInsets.addPaddingValues(
            addTop = -windowInsets.topDp.dp + 12.dp,
            addBottom = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        itemsIndexed(list) { index, item ->
            CompositionLocalProvider(
                LocalListItemShapes provides segmentedItemShapes(
                    index,
                    list.size,
                ),
            ) {
                DynamicItemCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    item = item,
                    isJumpToUser = false,
                    onClick = {
                        viewModel.toDetailPage(item)
                    },
                )
            }
        }
        item {
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
