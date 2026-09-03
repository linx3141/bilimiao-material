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
import bilibili.app.dynamic.v2.DynamicGRPC
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
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

private class UserDynamicListContentViewModel(
    override val di: DI,
    private val vmid: String,
) : ViewModel(), DIAware {

    private val pageNavigation: PageNavigation by instance()

    val isRefreshing = MutableStateFlow(false)
    val list = FlowPaginationInfo<bilibili.app.dynamic.v2.DynamicItem>()
    var listHistoryOffset = ""

    init {
        loadData(1)
    }

    private fun loadData(
        pageNum: Int = list.pageNum
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            list.loading.value = true
            val req = bilibili.app.dynamic.v2.DynSpaceReq(
                hostUid = vmid.toLong(),
                historyOffset = listHistoryOffset,
                page = pageNum.toLong(),
            )
            val res = BiliGRPCHttp.request {
                DynamicGRPC.dynSpace(req)
            }.awaitCall()
            listHistoryOffset = res.historyOffset
            list.finished.value = !res.hasMore
            list.pageNum = pageNum
            if (pageNum == 1) {
                list.data.value = res.list.toMutableList()
            } else {
                list.data.value = mutableListOf<bilibili.app.dynamic.v2.DynamicItem>()
                    .apply {
                        addAll(list.data.value)
                        addAll(res.list)
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            list.fail.value = "无法连接到御坂网络"
        } finally {
            list.loading.value = false
            isRefreshing.value = false
        }
    }

    fun tryAgainLoadData() = loadData()

    fun refresh() {
        listHistoryOffset = ""
        isRefreshing.value = true
        list.finished.value = false
        list.fail.value = ""
        loadData(1)
    }

    fun loadMore() {
        if (!list.finished.value && !list.loading.value) {
            loadData(list.pageNum + 1)
        }
    }

    fun toDetailPage(
        item: bilibili.app.dynamic.v2.DynamicItem
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
fun UserDynamicListContent(
    vmid: String,
) {
    val viewModel = diViewModel(key = "dynamic-$vmid") {
        UserDynamicListContentViewModel(it, vmid)
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
            if (it.tab == PageTabIds.UserDynamic) {
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
