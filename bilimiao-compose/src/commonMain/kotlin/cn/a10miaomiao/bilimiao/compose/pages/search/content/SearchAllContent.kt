@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.search.content

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bilibili.app.dynamic.v2.DynamicGRPC
import bilibili.app.dynamic.v2.DynamicItem
import bilibili.polymer.app.search.v1.Item.CardItem
import bilibili.polymer.app.search.v1.SearchGRPC
import cn.a10miaomiao.bilimiao.compose.common.addPaddingValues
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.entity.FlowPaginationInfo
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.mypage.LocalPageConfigState
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageListener
import cn.a10miaomiao.bilimiao.compose.common.mypage.rememberMyMenu
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.dyanmic.DynamicItemCard
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.list.SwipeToRefresh
import cn.a10miaomiao.bilimiao.compose.pages.search.components.SearchFilterMenu
import cn.a10miaomiao.bilimiao.compose.pages.search.components.SearchFilterState
import cn.a10miaomiao.bilimiao.compose.pages.search.components.SearchItemCard
import com.a10miaomiao.bilimiao.comm.mypage.MenuActions
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.mypage.SearchConfigInfo
import com.a10miaomiao.bilimiao.comm.mypage.myMenu
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import cn.a10miaomiao.bilimiao.compose.store.RegionStore
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

typealias SearchItem = bilibili.polymer.app.search.v1.Item

private class SearchAllContentViewModel(
    override val di: DI,
    val keyword: String,
) : ViewModel(), DIAware {

    private val pageNavigation: PageNavigation by instance()
    private val regionStore: RegionStore by instance()

    private var _next = ""
    val list = FlowPaginationInfo<SearchItem>()
    val isRefreshing = MutableStateFlow(false)

    val rankOrderList = listOf(
        0 to "默认排序",
        2 to "新发布",
        1 to "播放多",
        3 to "弹幕多",
    )
    val rankOrder = mutableStateOf(rankOrderList[0])

    val searchFilterState = SearchFilterState(
        regionStore,
        onChange = ::applyConditions
    )
    val hasFilter = mutableStateOf(false)

    init {
        loadData("")
    }

    private fun loadData(
        next: String = _next
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val moreConditions = searchFilterState.data
            val order = rankOrder.value.first
            val timeType = moreConditions.timeType
                .let { if (it == 0) "" else "${it}d" }
            val tidList = moreConditions.regionList
                .joinToString(",")
            val durationList = moreConditions.durationList
                .joinToString(",")
            list.loading.value = true
            val req = bilibili.polymer.app.search.v1.SearchAllRequest(
                keyword = keyword,
                order = order,
                timeType = timeType,
                tidList = tidList,
                durationList = durationList,
                pagination = bilibili.pagination.Pagination(
                    pageSize = list.pageSize,
                    next = next
                )
            )
            val result = BiliGRPCHttp.request {
                SearchGRPC.searchAll(req)
            }.awaitCall()
            val itemList = if (next.isNotBlank()
                || hasFilter.value
                || order != 0
            ) {
                result.item.filter { it.cardItem is CardItem.Av }
            } else {
                result.item
            }
            _next = result.pagination?.next ?: ""
            list.finished.value = itemList.isEmpty() || _next.isBlank()
            if (next.isBlank()) {
                list.data.value = itemList
            } else {
                list.data.value = list.data.value
                    .toMutableList()
                    .apply { addAll(itemList) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            list.fail.value = e.message ?: e.toString()
            list.loading.value = false
        } finally {
            list.loading.value = false
            isRefreshing.value = false
        }
    }


    fun tryAgainLoadData() {
        if (!list.loading.value && !list.finished.value) {
            loadData()
        }
    }

    fun loadMore() {
        if (!list.loading.value && !list.finished.value) {
            loadData(_next)
        }
    }

    fun refresh() {
        list.reset()
        isRefreshing.value = true
        loadData("")
    }

    fun applyConditions() {
        val moreConditions = searchFilterState.data
        hasFilter.value = moreConditions.timeType != 0
                || moreConditions.regionList[0] != 0
                || moreConditions.durationList[0] != 0
        refresh()
    }

    fun menuItemClick(item: MenuItemPropInfo) {
        val key = item.key ?: return
        when (key) {
            in 10..19 -> {
                val order = key - 10
                rankOrder.value = rankOrderList.find {
                    it.first == order
                } ?: rankOrderList[0]
                refresh()
            }
        }
    }

    fun toDetailPage(item: SearchItem) {
        pageNavigation.navigateByUri(item.uri)
    }

}

@Composable
private fun SearchAllContentConfig(
    keyword: String,
    viewModel: SearchAllContentViewModel,
) {
    // 注册“筛选”的自定义分组下拉菜单内容，供底栏按 MenuKeys.filter 取用。
    // 不能把 @Composable 函数类型放进 comm 层（无 Compose 编译器插件会导致 ABI 不一致）。
    val pageConfigState = LocalPageConfigState.current
    DisposableEffect(pageConfigState, viewModel) {
        pageConfigState?.setCustomMenuContent(MenuKeys.filter) { dismiss ->
            SearchFilterMenu(
                state = viewModel.searchFilterState,
                onDismiss = dismiss,
            )
        }
        onDispose {
            pageConfigState?.setCustomMenuContent(MenuKeys.filter, null)
        }
    }
    val rankOrder by viewModel.rankOrder
    val hasFilter by viewModel.hasFilter
    val pageConfigId = PageConfig(
        title = "搜索\n-\n$keyword",
        menu = rememberMyMenu(rankOrder, hasFilter) {
            myItem {
                key = MenuKeys.search
                action = MenuActions.search
                title = "继续搜索"
                iconVector = androidx.compose.material.icons.Icons.Default.Search
            }
            myItem {
                key = MenuKeys.sort
                title = rankOrder.second
                iconVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort
                childMenu = myMenu {
                    checkable = true
                    checkedKey = 10 + rankOrder.first
                    viewModel.rankOrderList.forEach {
                        myItem {
                            title = it.second
                            key = 10 + it.first
                        }
                    }
                }
            }
            myItem {
                key = MenuKeys.filter
                title = if (hasFilter) "已筛选" else "筛选"
                iconVector = androidx.compose.material.icons.Icons.Default.FilterAlt
            }
        },
        search = SearchConfigInfo(
            keyword = keyword
        )
    )
    PageListener(
        pageConfigId,
        onMenuItemClick = viewModel::menuItemClick
    )
}

@Composable
internal fun SearchAllContent(
    keyword: String,
    isActive: Boolean,
) {
    val viewModel = diViewModel(
        key = PageTabIds.SearchAll + keyword
    ) {
        SearchAllContentViewModel(it, keyword)
    }
    if (isActive) {
        SearchAllContentConfig(keyword, viewModel)
    }
    val windowInsets = localContentInsets()

    val list by viewModel.list.data.collectAsState()
    val listLoading by viewModel.list.loading.collectAsState()
    val listFinished by viewModel.list.finished.collectAsState()
    val listFail by viewModel.list.fail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val listState = rememberLazyGridState()
    val emitter = localEmitter()
    LaunchedEffect(Unit) {
        emitter.collectAction<EmitterAction.DoubleClickTab> {
            if (it.tab == PageTabIds.SearchAll) {
                if (listState.firstVisibleItemIndex == 0) {
                    viewModel.refresh()
                } else {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    SwipeToRefresh(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
    ) {
        LazyVerticalGrid(
            state = listState,
            modifier = Modifier.fillMaxSize(),
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
                val cardItem = item.cardItem
                if (cardItem != null) {
                    CompositionLocalProvider(
                        LocalListItemShapes provides segmentedItemShapes(
                            index,
                            list.size,
                        ),
                    ) {
                        SearchItemCard(
                            cardItem,
                            onClick = {
                                viewModel.toDetailPage(item)
                            }
                        )
                    }
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

}
