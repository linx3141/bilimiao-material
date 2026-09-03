@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.home.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
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
import cn.a10miaomiao.bilimiao.compose.components.video.VideoItemBox
import cn.a10miaomiao.bilimiao.compose.components.video.MiniVideoItemBox
import cn.a10miaomiao.bilimiao.compose.components.video.gridSegmentedShape
import cn.a10miaomiao.bilimiao.compose.components.video.rememberGridColumnCount
import cn.a10miaomiao.bilimiao.compose.pages.message.components.MessageItemBox
import cn.a10miaomiao.bilimiao.compose.pages.web.WebPage
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.datastore.mapPreferences
import com.a10miaomiao.bilimiao.comm.entity.archive.ArchiveInfo
import com.a10miaomiao.bilimiao.comm.entity.comm.PaginationInfo
import com.a10miaomiao.bilimiao.comm.entity.message.AtMessageInfo
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import com.a10miaomiao.bilimiao.comm.store.FilterStore
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance


@Stable
private class HomePopularContentViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val pageNavigation: PageNavigation by instance()
    private val filterStore: FilterStore by instance()

    private val lastIdx
        get() = list.data.value.lastOrNull()?.base?.idx ?: 0
    val list = FlowPaginationInfo<SmallCoverV5>()
    val topEntranceList = MutableStateFlow(listOf<EntranceShow>())
    val isRefreshing = MutableStateFlow(false)

    init {
        loadData(0)
    }

    private fun loadData(
        idx: Long = lastIdx
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            list.loading.value = true
            val carryToken = SettingPreferences.mapPreferences {
                it[SettingPreferences.HomePopularCarryToken] ?: true
            }
            val req = PopularResultReq(
                idx = idx,
            )
            val result = BiliGRPCHttp.request {
                PopularGRPC.index(req)
            }.also {
                it.needToken = carryToken
            }.awaitCall()
            val itemsList = result.items
            val filterList = itemsList.mapNotNull {
                (it.item as? Card.Item.SmallCoverV5)?.value
            }.filter {
                val base = it.base
//                val upper = it?.up.id
                (base != null // && upper != null
                        && base.cardGoto == "av"
                        && filterStore.filterWord(base.title)
                        && filterStore.filterUpperName(it.rightDesc1))
            }
            val topItems = result.config?.topItems
            if (topItems != null) {
                topEntranceList.value = topItems
            }
            val newList = if (idx == 0L) mutableListOf()
                else list.data.value.toMutableList()
            if (filterStore.filterTagListIsEmpty()) {
                newList.addAll(filterList)
                list.data.value = newList
            } else {
                filterList.forEach {
                    if (filterStore.filterTag(it.base!!.param)) {
                        newList.add(it)
                        list.data.value = newList.toList()
                    }
                }
            }
            list.finished.value = itemsList.isEmpty()
            list.loading.value = false
            isRefreshing.value = false
        } catch (e: Exception) {
            e.printStackTrace()
            list.fail.value = e.message ?: e.toString()
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

    fun toVideoDetail(item: SmallCoverV5) {
        val base = item.base ?: return
        pageNavigation.navigateToVideoInfo(base.param)
    }

    fun toPageByUrl(url: String) {
        if (!BilibiliNavigation.navigationTo(pageNavigation, url)) {
            BilibiliNavigation.navigationToWeb(pageNavigation, url)
        }
    }

}

// 热门入口图标：排行榜/每周必看/入站必刷（统一 Material 图标）
private val entranceIconMap: Map<String, ImageVector> = mapOf(
    "排行榜" to Icons.Filled.Leaderboard,
    "每周必看" to Icons.Filled.Star,
    "入站必刷" to Icons.Filled.EmojiEvents,
)

@Composable
private fun EntranceListBox(
    viewModel: HomePopularContentViewModel,
    colCount: Int,
) {
    val topEntranceList by viewModel.topEntranceList.collectAsState()
    // 排行榜/每周必看/入站必刷：与视频列表同款分段卡片。
    // 单列时保持单列分段；两列及以上时三个卡片固定一行 3 列（多列分段圆角）。
    // 左右边距由网格 contentPadding 提供，这里不再额外加水平 padding。
    if (colCount <= 1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            // 单列分段列表：相邻卡片之间保留 m3e 分段的小间距
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            topEntranceList.forEachIndexed { index, item ->
                CompositionLocalProvider(
                    LocalListItemShapes provides segmentedItemShapes(
                        index,
                        topEntranceList.size,
                    ),
                ) {
                    val shapes = LocalListItemShapes.current
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes?.shape ?: RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceBright,
                        onClick = { viewModel.toPageByUrl(item.uri) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = entranceIconMap[item.title] ?: Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }
    } else {
        // 多列：三个卡片一行 3 列（即使视频列表是两列，这里也固定 3 列）
        // 测量最宽文字（如"每周必看"四个字），统一所有卡片宽度（等宽）
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val textStyle = MaterialTheme.typography.labelLarge
        val maxTextWidthPx = remember(topEntranceList, textMeasurer, textStyle) {
            topEntranceList.maxOfOrNull { item ->
                textMeasurer.measure(
                    text = AnnotatedString(item.title),
                    style = textStyle,
                ).size.width
            } ?: 0
        }
        val cardWidth = with(density) { maxTextWidthPx.toDp() } + 16.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            // 居左：到左侧边缘的边距由网格 contentPadding 提供（与视频列表一致）
            horizontalArrangement = Arrangement.spacedBy(
                ListItemDefaults.SegmentedGap,
            ),
        ) {
            topEntranceList.forEachIndexed { index, item ->
                Surface(
                    // 三个卡片等宽（取最宽内容，如"每周必看"四个字），不撑满整行
                    modifier = Modifier.width(cardWidth),
                    shape = gridSegmentedShape(index, topEntranceList.size, 3),
                    color = MaterialTheme.colorScheme.surfaceBright,
                    onClick = { viewModel.toPageByUrl(item.uri) },
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = entranceIconMap[item.title] ?: Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomePopularContent() {
    val viewModel: HomePopularContentViewModel = diViewModel { HomePopularContentViewModel(it) }
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
            if (it.tab == PageTabIds.HomePopular) {
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
        val colCount = rememberGridColumnCount(300.dp)
        LazyVerticalGrid(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // 多列分段：列数由屏幕宽度/dpi 决定，列间距与上下卡片间距一致
            columns = GridCells.Fixed(colCount),
            contentPadding = windowInsets.toPaddingValues(
                left = 12.dp,
                right = 12.dp,
                top = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EntranceListBox(viewModel, colCount)
            }
            itemsIndexed(
                list,
                key = { _, item -> item.base!!.idx },
            ) { index, item ->
                // 保留 m3e 分段卡片样式，多列分段排列（行列决定四角圆角）
                VideoItemBox(
                    modifier = Modifier,
                    title = item.base?.title,
                    pic = item.base?.cover,
                    upperName = item.rightDesc1,
                    remark = item.rightDesc2,
                    duration = item.coverRightText1,
                    isChargeVideo = item.cornerMarkStyle?.text?.contains("充电") == true
                        || item.leftCornerMarkStyle?.text?.contains("充电") == true,
                    segmentedShape = gridSegmentedShape(index, list.size, colCount),
                    onClick = {
                        viewModel.toVideoDetail(item)
                    }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
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
