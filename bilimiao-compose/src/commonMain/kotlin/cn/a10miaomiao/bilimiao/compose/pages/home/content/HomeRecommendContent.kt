@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.home.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bilibili.app.card.v1.Card
import bilibili.app.card.v1.SmallCoverV5
import bilibili.app.show.v1.EntranceShow
import bilibili.app.show.v1.PopularGRPC
import bilibili.app.show.v1.PopularResultReq
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.entity.FlowPaginationInfo
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.navigation.BilibiliNavigation
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.list.SwipeToRefresh
import cn.a10miaomiao.bilimiao.compose.components.video.MiniVideoItemBox
import cn.a10miaomiao.bilimiao.compose.components.video.gridSegmentedShape
import cn.a10miaomiao.bilimiao.compose.components.video.rememberGridColumnCount
import cn.a10miaomiao.bilimiao.compose.components.video.VideoItemBox
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.BangumiDetailPage

import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.datastore.appDataStore
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.home.HomeRecommendInfo
import com.a10miaomiao.bilimiao.comm.entity.home.RecommendCardInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.FilterStore
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import coil3.compose.AsyncImage
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance


@Stable
private class HomeRecommendContentViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val pageNavigation: PageNavigation by instance()
    private val filterStore: FilterStore by instance()

    private val lastIdx
        get() = list.data.value.lastOrNull()?.idx ?: 0
    val list = FlowPaginationInfo<RecommendCardInfo>()
    val isRefreshing = MutableStateFlow(false)
    val listStyle = MutableStateFlow(0)



    init {
        viewModelScope.launch {
            appDataStore.data.map {
                it[SettingPreferences.HomeRecommendListStyle] ?: 0
            }.collect {
                listStyle.value = it
            }
        }
        loadData(0)
    }

    private fun loadData(
        idx: Long = lastIdx
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            list.loading.value = true
            val res = BiliApiService.homeApi.recommendList(
                idx = idx,
            ).awaitCall().json<ResponseData<HomeRecommendInfo>>()
            if (res.isSuccess) {
                val itemsList = res.requireData().items
                val filterList = itemsList.filter {
                    (it.goto?.isNotEmpty() ?: false)
                            && filterStore.filterWord(it.title)
                            && it.args != null
                            && it.args!!.up_id != null
                            && filterStore.filterUpper(it.args!!.up_id!!)
                }
                // 诊断：输出首页推荐解码出的充电相关字段，便于确认标识判定
                filterList.firstOrNull()?.let {
                    miaoLogger() debug "充电标识-首页: ${it.title} " +
                        "ugc_pay=${it.ugc_pay} charging_pay=${it.charging_pay?.level} " +
                        "args.ugc_pay=${it.args?.ugc_pay}"
                }
                val newList = if (idx == 0L) mutableListOf()
                else list.data.value.toMutableList()
                if (filterStore.filterTagListIsEmpty()) {
                    newList.addAll(filterList)
                    list.data.value = newList
                } else {
                    filterList.forEach {
                        if (filterStore.filterTag(it.param)) {
                            newList.add(it)
                            list.data.value = newList.toList()
                        }
                    }
                }
                list.finished.value = itemsList.isEmpty()
            } else {
                GlobalToaster.show(res.message)
                throw Exception(res.message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            list.fail.value = e.message ?: e.toString()
        } finally {
            list.loading.value = false
            isRefreshing.value = false
        }
    }

    fun tryAgainLoadData() {
        loadData()
    }

    fun loadMore() {
        if (!list.finished.value && !list.loading.value) {
            loadData(lastIdx)
        }
    }

    fun refresh() {
        isRefreshing.value = true
        list.finished.value = false
        list.fail.value = ""
        loadData(0)
    }

    fun toVideoDetail(item: RecommendCardInfo) {
        if (item.goto == "av" || item.goto == "vertical_av") {
            pageNavigation.navigateToVideoInfo(item.param)
        } else if (item.goto == "bangumi") {
            pageNavigation.navigate(BangumiDetailPage(epId = item.param))
        } else if (!BilibiliNavigation.navigationTo(pageNavigation, item.uri)){
            BilibiliNavigation.navigationToWeb(pageNavigation, item.uri)
        }
    }


}

@Composable
internal fun HomeRecommendContent() {
    val viewModel: HomeRecommendContentViewModel = diViewModel { HomeRecommendContentViewModel(it) }
    val windowInsets = localContentInsets()

    val list by viewModel.list.data.collectAsState()
    val listLoading by viewModel.list.loading.collectAsState()
    val listFinished by viewModel.list.finished.collectAsState()
    val listFail by viewModel.list.fail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val listStyle by viewModel.listStyle.collectAsState()

    val listState = rememberLazyGridState()
    val emitter = localEmitter()
    LaunchedEffect(Unit) {
        emitter.collectAction<EmitterAction.DoubleClickTab> {
            if (it.tab == PageTabIds.HomeRecommend) {
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
        val colCount = rememberGridColumnCount(
            if (listStyle == 0) 300.dp else 180.dp,
        )
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            columns = GridCells.Fixed(colCount),
            contentPadding = windowInsets.toPaddingValues(
                left = 12.dp,
                right = 12.dp,
                top = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(
                list,
                key = { _, item -> item.idx },
            ) { index, item ->
                if (listStyle == 0) {
                    // 保留 m3e 分段卡片样式，多列分段排列（行列决定四角圆角）
                    VideoItemBox(
                        modifier = Modifier,
                        title = item.title,
                        pic = item.cover,
                        upperName = item.args?.up_name,
                        playNum = item.cover_left_text_1,
                        damukuNum = item.cover_left_text_2,
                        duration = item.cover_right_text,
                        isChargeVideo = item.ugc_pay == 1
                            || item.charging_pay?.level != null
                            || item.args?.ugc_pay == 1,
                        segmentedShape = gridSegmentedShape(index, list.size, colCount),
                        onClick = {
                            viewModel.toVideoDetail(item)
                        }
                    )
                } else {
                    MiniVideoItemBox(
                        modifier = Modifier.padding(5.dp),
                        title = item.title,
                        pic = item.cover,
                        upperName = item.args?.up_name,
                        playNum = item.cover_left_text_1,
                        damukuNum = item.cover_left_text_2,
                        duration = item.cover_right_text,
                        isChargeVideo = item.ugc_pay == 1
                            || item.charging_pay?.level != null
                            || item.args?.ugc_pay == 1,
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
}
