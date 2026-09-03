@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.mine

import cn.a10miaomiao.bilimiao.compose.common.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bilibili.app.interfaces.v1.Cursor
import bilibili.app.interfaces.v1.CursorItem
import bilibili.app.interfaces.v1.CursorV2Req
import bilibili.app.interfaces.v1.HistoryGRPC
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.addPaddingValues
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.entity.FlowPaginationInfo
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageListener
import cn.a10miaomiao.bilimiao.compose.common.mypage.rememberMyMenu
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialogState
import cn.a10miaomiao.bilimiao.compose.components.layout.sticky.StickyHeaders
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.list.SwipeToRefresh
import cn.a10miaomiao.bilimiao.compose.components.video.VideoItemBox
import cn.a10miaomiao.bilimiao.compose.components.video.gridSegmentedShape
import cn.a10miaomiao.bilimiao.compose.components.video.rememberGridColumnCount
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.BangumiDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserFavouriteDetailPage
import com.a10miaomiao.bilimiao.comm.entity.comm.PaginationInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuActions
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.mypage.SearchConfigInfo
import com.a10miaomiao.bilimiao.comm.mypage.myMenu
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.compose.rememberInstance
import cn.a10miaomiao.bilimiao.compose.components.dialogs.FullScreenDialogProperties

@Serializable
class HistoryPage(
    val keyword: String = "",
) : ComposePage {

    @Composable
    override fun Content() {
        val viewModel: HistoryPageViewModel = diViewModel {
            HistoryPageViewModel(it, keyword)
        }
        BoxWithConstraints {
            HistoryPageContent(viewModel, maxWidth)
        }
    }
}

private class HistoryPageViewModel(
    override val di: DI,
    private val initKeyword: String = "",
) : ViewModel(), DIAware {

    @Stable
    class HistoryItem(
        val localDate: LocalDate,
        val item: CursorItem?, // 数据为空时表示为日期分割线
    )

    private val pageNavigation by instance<PageNavigation>()
    private val messageDialog by instance<MessageDialogState>()

    var keyword = initKeyword

    val isRefreshing = MutableStateFlow(false)
    val list = FlowPaginationInfo<HistoryItem>()
    private val _selectedItemMap = mutableStateMapOf<Long, Int>()
    val selectedItemMap: Map<Long, Int> get() = _selectedItemMap

    private var _mapTp = 3
    private var _maxId = 0L
    private var _viewAt = 0L

    init {
        loadData(0L)
    }

    fun clearSelectedItemMap() {
        _selectedItemMap.clear()
    }

    fun addSelectedItem(key: Long, i: Int) {
        _selectedItemMap[key] = i
    }

    fun removeSelectedItem(key: Long) {
        _selectedItemMap.remove(key)
    }

    fun getDateByCursorItem(item: CursorItem): LocalDate {
        return Instant.fromEpochMilliseconds(item.viewAt * 1000)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }

    private suspend fun loadPage(
        maxId: Long = _maxId,
    ) {
        try {
            list.loading.value = true
            val keywordText = keyword
            val itemList = if (keywordText.isBlank()) {
                loadList(maxId)
            } else {
                searchList(keywordText, maxId + 1)
            }
            appendItems(itemList)
        } catch (e: Exception) {
            e.printStackTrace()
            list.fail.value = e.message ?: e.toString()
        } finally {
            list.loading.value = false
            isRefreshing.value = false
        }
    }

    private fun loadData(
        maxId: Long = _maxId,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            loadPage(maxId)
        }
    }

    /**
     * 把 [itemList] 追加到历史列表末尾：按观看时间降序排序、
     * 按 kid 去重、为日期变化添加分割线（避免日期分割反复出现）。
     */
    private fun appendItems(itemList: List<CursorItem>) {
        val newListData = list.data.value.toMutableList()
        val seenKids = newListData.mapNotNull { it.item?.kid }.toMutableSet()
        val sortedItems = itemList.sortedByDescending { it.viewAt }
        var prevItem = newListData.lastOrNull()
        sortedItems.forEach { item ->
            if (!seenKids.add(item.kid)) {
                return@forEach
            }
            val localData = getDateByCursorItem(item)
            if (prevItem?.localDate != localData) {
                newListData.add(
                    HistoryItem(
                        item = null,
                        localDate = localData,
                    )
                )
            }
            prevItem = HistoryItem(
                item = item,
                localDate = localData,
            ).also(newListData::add)
        }
        list.data.value = newListData
    }

    /**
     * 持续加载历史记录直到 [targetDate] 出现在列表中（用于日历点击跳转），
     * 返回该日期分割线在列表中的索引；加载完成仍没有则返回 -1。
     */
    suspend fun loadUntilDate(targetDate: LocalDate): Int {
        var index = list.data.value.indexOfFirst { it.localDate == targetDate }
        if (index != -1) return index
        // 时间戳粗跳：历史记录游标按时间排序，用目标日期次日 0 点的时间戳
        // 作为游标一次请求目标日期附近的数据，避免逐页加载中间的记录
        val targetEndSeconds = targetDate.plus(1, DateTimeUnit.DAY)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .epochSeconds
        try {
            val req = CursorV2Req(
                business = "archive",
                cursor = Cursor(
                    max = targetEndSeconds,
                    maxTp = _mapTp,
                ),
            )
            val res = BiliGRPCHttp.request {
                HistoryGRPC.cursorV2(req)
            }.awaitCall()
            val items = res.items
            if (items.isNotEmpty()) {
                appendItems(items)
                res.cursor?.let {
                    _maxId = it.max
                    _mapTp = it.maxTp
                }
                list.finished.value = !res.hasMore
                index = list.data.value.indexOfFirst { it.localDate == targetDate }
                if (index != -1) return index
            }
        } catch (_: Exception) {
            // 粗跳失败（游标类型不支持时间戳等），回退逐页加载
        }
        // 回退：逐页加载直到目标日期
        while (
            index == -1 &&
            !list.finished.value &&
            list.fail.value.isEmpty() &&
            list.data.value.isNotEmpty()
        ) {
            loadPage(_maxId)
            index = list.data.value.indexOfFirst { it.localDate == targetDate }
        }
        return index
    }

    private suspend fun loadList(
        maxId: Long,
    ): List<CursorItem>{
        val req = CursorV2Req(
            business = "archive",
            cursor = if (maxId != 0L) {
                Cursor(
                    max = maxId,
                    maxTp = _mapTp, // 本页最大值游标类型
                )
            } else {
                Cursor()
            }
        )
        val res = BiliGRPCHttp.request {
            HistoryGRPC.cursorV2(req)
        }.awaitCall()
        res.cursor?.let {
            _maxId = it.max
            _mapTp = it.maxTp
        }
        list.finished.value = !res.hasMore
        return res.items
    }

    private suspend fun searchList(
        keywordText: String,
        pageNum: Long,
    ): List<CursorItem> {
        val req = bilibili.app.interfaces.v1.SearchReq(
            business = "archive",
            keyword = keywordText,
            pn = pageNum,
        )
        val res = BiliGRPCHttp.request {
            HistoryGRPC.search(req)
        }.awaitCall()
        _maxId = res.page?.pn ?: 0
        list.finished.value = !res.hasMore
        return res.items
    }

    fun deleteHistory(kids: Set<Long>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            messageDialog.loading("操作请求中")
            val deleteItems = mutableListOf<CursorItem>()
            val newItems = mutableListOf<HistoryItem>()
            list.data.value.forEach {
                val item = it.item
                if (item != null && kids.indexOf(item.kid) != -1) {
                    deleteItems.add(item)
                } else {
                    newItems.add(it)
                }
            }
            val req = bilibili.app.interfaces.v1.DeleteReq(
                hisInfo = deleteItems.map {
                    bilibili.app.interfaces.v1.HisInfo(
                        business = it.business,
                        kid = it.kid,
                    )
                }
            )
            BiliGRPCHttp.request {
                HistoryGRPC.delete(req)
            }.awaitCall()
            list.data.value = newItems
            GlobalToaster.show("已删除选中的${deleteItems.size}个记录")
            clearSelectedItemMap()
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalToaster.show("删除失败:$e")
        } finally {
            messageDialog.close()
        }
    }

    fun clearHistoryList() = viewModelScope.launch(Dispatchers.IO) {
        try {
            messageDialog.loading("操作请求中")
            val req = bilibili.app.interfaces.v1.ClearReq(
                business = "archive"
            )
            BiliGRPCHttp.request {
                HistoryGRPC.clear(req)
            }.awaitCall()
            list.data.value = listOf()
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalToaster.show("操作失败:$e")
        } finally {
            messageDialog.close()
        }
    }

    private fun tryAgainLoadData() {
        loadData()
    }

    fun loadMore() {
        if (!list.finished.value && !list.loading.value) {
            loadData(_maxId)
        }
    }

    fun refreshList() {
        isRefreshing.value = true
        list.reset()
        loadData(0L)
    }

    fun toVideoDetail(item: CursorItem) {
        when(item.business) {
            "archive" -> {
                pageNavigation.navigateToVideoInfo(item.oid.toString())
            }
            "pgc" -> {
                pageNavigation.navigate(BangumiDetailPage(
                    id = item.kid.toString()
                ))
            }
            else -> {
                GlobalToaster.show("未知类型:${item.business}")
            }
        }

    }

    fun searchSelfPage(text: String) {
        keyword = text
        _selectedItemMap.clear()
        refreshList()
    }
}


@Composable
private fun HistoryPageContent(
    viewModel: HistoryPageViewModel,
    pageWidth: Dp,
) {

    val windowInsets = localContentInsets()

    val showClearTipsDialog = remember {
        mutableStateOf(false)
    }
    val enableEditMode = remember {
        mutableStateOf(false)
    }
    val pageNavigation by rememberInstance<PageNavigation>()

    fun clearHistoryList() {
        showClearTipsDialog.value = false
        viewModel.clearHistoryList()
    }

    fun menuItemClick (menuItem: MenuItemPropInfo) {
        when(menuItem.key) {
            MenuKeys.clear -> {
                showClearTipsDialog.value = true
            }
            MenuKeys.edit -> {
                viewModel.clearSelectedItemMap()
                enableEditMode.value = true
            }
            MenuKeys.delete -> {
                val selectedKeys = viewModel.selectedItemMap.keys
                if (selectedKeys.isEmpty()) {
                    GlobalToaster.show("未选中任何视频")
                } else {
                    viewModel.deleteHistory(selectedKeys)
                }
            }
            MenuKeys.complete -> {
                enableEditMode.value = false
            }
        }
    }
    val pageConfigId = PageConfig(
        title = if (viewModel.keyword.isBlank()) "历史记录"
            else "搜索历史\n-\n${viewModel.keyword}",
        menu = rememberMyMenu(enableEditMode.value) {
            if (enableEditMode.value) {
                myItem {
                    key = MenuKeys.complete
                    title = "完成编辑"
                    iconVector = androidx.compose.material.icons.Icons.Default.Check
                }
                myItem {
                    key = MenuKeys.delete
                    title = "删除选中"
                    iconVector = androidx.compose.material.icons.Icons.Outlined.Delete
                }
            } else {
                myItem {
                    key = MenuKeys.more
                    title = "更多"
                    iconVector = androidx.compose.material.icons.Icons.Default.MoreVert
                    childMenu = myMenu {
                        myItem {
                            key = MenuKeys.edit
                            title = "批量管理"
                            iconVector = androidx.compose.material.icons.Icons.Default.EditNote
                        }
                        myItem {
                            key = MenuKeys.clear
                            title = "清空历史记录"
                        }
                    }
                }
                myItem {
                    key = MenuKeys.search
                    action = MenuActions.search
                    title = "搜索"
                    iconVector = androidx.compose.material.icons.Icons.Default.Search
                }
            }
        },
        search = SearchConfigInfo(
            name = "搜索历史记录",
            keyword = viewModel.keyword,
        )
    )
    PageListener(
        configId = pageConfigId,
        onMenuItemClick = ::menuItemClick,
        onSearchSelfPage = { text ->
            // 进入"搜索历史记录"输入页（与 UP 主详情搜索逻辑一致）
            pageNavigation.navigate(HistorySearchInputPage(initKeyword = text))
        }
    )
    BackHandler(
        enabled = enableEditMode.value,
        onBack = {
            enableEditMode.value = false
        }
    )

    val listFlow = viewModel.list
    val list by listFlow.data.collectAsState()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()
    val calendarListState = rememberLazyListState()
    val sideTimeline = pageWidth >= 650.dp

    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val currentDate = remember {
        mutableStateOf(today)
    }

    LaunchedEffect(listState, calendarListState) {
        launch {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collectLatest {
                    if (list.size > it) {
                        val itemDate = list[it].localDate
                        calendarListState.animateScrollToItem(
                            itemDate.daysUntil(today)
                        )
                        currentDate.value = itemDate
                    }
                }
        }
    }

    fun scrollToDate(date: LocalDate) {
        scope.launch {
            val index = withContext(Dispatchers.IO) {
                // 若该日期尚未加载，持续加载直到出现（或没有更多数据）
                viewModel.loadUntilDate(date)
            }
            if (index != -1) {
                listState.scrollToItem(index)
            } else {
                GlobalToaster.show("没有找到该日期的记录")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(windowInsets.toPaddingValues(
                bottom = 0.dp
            ))
    ) {
        CalendarRowView(
            modifier = Modifier
                .fillMaxWidth(),
            listState = calendarListState,
            startDate = today,
            endDate = list.lastOrNull()?.localDate,
            currentDate = currentDate.value,
            onChangeDate = ::scrollToDate,
        )
        HistoryListView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            viewModel = viewModel,
            bottomEdgePadding = windowInsets.bottom,
            sideTimeline = sideTimeline,
            listState = listState,
            enableEdit = enableEditMode.value,
        )
    }


    if (showClearTipsDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showClearTipsDialog.value = false
            },
            title = {
                Text(text = "提示")
            },
            text = {
                Text(text = "确认清空历史记录(⊙ˍ⊙)？")
            },
            confirmButton = {
                TextButton(onClick = ::clearHistoryList) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showClearTipsDialog.value = false
                }) {
                    Text(text = "取消")
                }
            },
            properties = FullScreenDialogProperties,
        )
    }
}

private object LocalDayOfWeekNames {
    val CHINESE_ABBREVIATED: DayOfWeekNames = DayOfWeekNames(
        listOf(
            "一", "二", "三", "四", "五", "六", "日"
        )
    )

    val CHINESE_FULL: DayOfWeekNames = DayOfWeekNames(
        listOf(
            "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"
        )
    )
}

@Composable
private fun CalendarRowView(
    modifier: Modifier,
    listState: LazyListState,
    startDate: LocalDate,
    endDate: LocalDate?,
    currentDate: LocalDate,
    onChangeDate: (LocalDate) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        StickyHeaders(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            key = { item ->
                val date = startDate.minus(item.index, DateTimeUnit.DAY)
                LocalDate(date.year, date.month, 1)
            },
        ) {
            val formatter = LocalDate.Format {
                year()
                chars("年")
                monthNumber()
                chars("月")
            }
            Text(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                text = it.key.format(formatter),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        LazyRow(
            state = listState,
        ) {
            items(
                count = Int.MAX_VALUE,
                key = { it },
            ) {
                val date = startDate.minus(it, DateTimeUnit.DAY)

                val formatter = LocalDate.Format {
                    dayOfWeek(LocalDayOfWeekNames.CHINESE_ABBREVIATED)
                }

                val dateHeader = formatter.format(date).let { day ->
                    day.firstOrNull()?.toString() ?: day
                }

                // 所有日期统一使用正常文字色（不再灰显），选中日期用主题色高亮
                val color = MaterialTheme.colorScheme.onBackground
                Column(
                    modifier = Modifier
                        .size(40.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = dateHeader,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        textAlign = TextAlign.Center,
                    )
                    if (date == currentDate) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                )
                                .size(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${date.dayOfMonth}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onChangeDate(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${date.dayOfMonth}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = color,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                }
            }
        }
    }
}



@Composable
private fun HistoryListView(
    modifier: Modifier,
    viewModel: HistoryPageViewModel,
    bottomEdgePadding: Dp,
    sideTimeline: Boolean,
    listState: LazyGridState,
    enableEdit: Boolean,
) {
    val listFlow = viewModel.list
    val list by listFlow.data.collectAsState()
    val listLoading by listFlow.loading.collectAsState()
    val listFinished by listFlow.finished.collectAsState()
    val listFail by listFlow.fail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    // 计算每个条目在所属日期分段中的位置（组内序号、组内数量），用于连体圆角
    val groupInfo = remember(list) {
        val position = IntArray(list.size)
        val size = IntArray(list.size)
        var groupItems = mutableListOf<Int>()
        list.forEachIndexed { index, historyItem ->
            if (historyItem.item == null) {
                groupItems.forEach { size[it] = groupItems.size }
                groupItems = mutableListOf()
            } else {
                position[index] = groupItems.size
                groupItems.add(index)
            }
        }
        groupItems.forEach { size[it] = groupItems.size }
        position to size
    }
    SwipeToRefresh(
        modifier = modifier,
        refreshing = isRefreshing,
        onRefresh = viewModel::refreshList,
    ) {
        val colCount = rememberGridColumnCount(300.dp)
        // 按日期分组独立计算每张卡片的圆角：
        // 每组（一个日期的卡片）内部按自己的行列取大圆角，
        // 组首行首列/首行末列/末行首列/末行末列分别大圆角，其余相邻边小圆角
        val cardShapeMap = remember(list, colCount) {
            val map = HashMap<Int, Shape>()
            val groupCards = mutableListOf<Int>()
            fun finishGroup() {
                if (groupCards.isNotEmpty()) {
                    groupCards.forEachIndexed { seqInGroup, index ->
                        map[index] = gridSegmentedShape(
                            seqInGroup,
                            groupCards.size,
                            colCount,
                        )
                    }
                }
                groupCards.clear()
            }
            list.forEachIndexed { index, historyItem ->
                if (historyItem.item == null) {
                    finishGroup()
                } else {
                    groupCards.add(index)
                }
            }
            finishGroup()
            map
        }
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            columns = GridCells.Fixed(colCount),
            contentPadding = PaddingValues(
                start = if (sideTimeline) {
                    50.dp
                } else {
                    12.dp
                },
                end = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            items(
                list.size,
                key = { index ->
                    // 稳定 key：日期分割线用日期，卡片用 kid（提升列表滚动/复用性能）
                    val historyItem = list[index]
                    historyItem.item?.kid?.toString()
                        ?: "date-${historyItem.localDate}"
                },
                span = { index ->
                    // 日期分割线占满整行，卡片占一列
                    if (list[index].item == null) {
                        GridItemSpan(maxLineSpan)
                    } else {
                        GridItemSpan(1)
                    }
                },
                contentType = { if (list[it].item == null) 0 else 1 }
            ) { index ->
                val item = list[index].item
                if (item == null) {
                    Spacer(modifier = Modifier.height(if (sideTimeline) 10.dp else 30.dp))
                } else {
                    val isChecked = enableEdit && viewModel.selectedItemMap.containsKey(item.kid)
                    val duration = item.cardOgv?.duration ?: item.cardUgc?.duration ?: 0
                    val progress = item.cardUgc?.progress ?: item.cardUgc?.progress ?: 0
                    val progressRatio = if (duration > 0L) progress.toFloat() / duration.toFloat() else 0f
                    Box(
                        contentAlignment = Alignment.CenterStart
                    ) {
                        VideoItemBox(
                            modifier = Modifier
                                .run {
                                    if (enableEdit) alpha(0.6f)
                                    else this
                                },
                            title = item.title,
                            pic = item.cardOgv?.cover
                                ?: item.cardUgc?.cover,
                            upperName = item.cardUgc?.name,
                            remark = NumberUtil.converCTime(item.viewAt),
                            duration = if (progressRatio >= 0.95f) {
                                "已看完"
                            } else if (progressRatio > 0f) {
                                "${NumberUtil.converDuration(progress)}/${NumberUtil.converDuration(duration)}"
                            } else {
                                NumberUtil.converDuration(duration)
                            },
                            progress = progressRatio,
                            isHtml = true,
                            isChargeVideo = item.cardUgc?.let {
                                it.badge.contains("充电")
                                    || it.badgeV2?.text?.contains("充电") == true
                            } == true,
                            segmentedShape = cardShapeMap[index],
                            onClick = {
                                if (!enableEdit) {
                                    viewModel.toVideoDetail(item)
                                } else if (isChecked) {
                                    viewModel.removeSelectedItem(item.kid)
                                } else {
                                    viewModel.addSelectedItem(item.kid, index)
                                }
                            }
                        )
                        if (enableEdit) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) {
                                        viewModel.addSelectedItem(item.kid, index)
                                    } else {
                                        viewModel.removeSelectedItem(item.kid)
                                    }
                                }
                            )
                        }

                    }
                }
            }
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                ListStateBox(
                    modifier = Modifier.padding(
                        bottom = bottomEdgePadding
                    ),
                    loading = listLoading,
                    finished = listFinished,
                    fail = listFail,
                    listData = list,
                ) {
                    viewModel.loadMore()
                }
            }
        }

        StickyHeaders(
            modifier = Modifier
                .fillMaxHeight(),
            state = listState,
            key = { item ->
                item.firstOrNull()?.let {
                    list.getOrNull(it.index)?.localDate
                }
            },
        ) {
            if (sideTimeline) {
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .padding(top = 10.dp, end = 5.dp)
                        .background(MaterialTheme.colorScheme.background),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val formatter = LocalDate.Format {
                        dayOfWeek(LocalDayOfWeekNames.CHINESE_FULL)
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            )
                            .size(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            textAlign = TextAlign.Center,
                            text = "${it.key.dayOfMonth}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(
                        modifier = Modifier.padding(top = 5.dp),
                        text = it.key.format(formatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            } else {
                val formatter = LocalDate.Format {
                    monthNumber()
                    chars("-")
                    dayOfMonth()
                    chars(" ")
                    dayOfWeek(LocalDayOfWeekNames.CHINESE_FULL)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = CircleShape,
                            )
                            .size(10.dp),
                    )
                    Text(
                        text = it.key.format(formatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

        }

    }
}
