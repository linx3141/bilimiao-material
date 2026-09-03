@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.community.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.CompositionLocalProvider
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bilibili.main.community.reply.v1.CursorReply
import bilibili.main.community.reply.v1.CursorReq
import bilibili.main.community.reply.v1.DetailListReq
import bilibili.main.community.reply.v1.DetailListScene
import bilibili.main.community.reply.v1.MainListReq
import bilibili.main.community.reply.v1.ReplyGRPC
import bilibili.main.community.reply.v1.ReplyInfo
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.entity.FlowPaginationInfo
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageListener
import cn.a10miaomiao.bilimiao.compose.common.mypage.rememberMyMenu
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.community.ReplyItemBox
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialogState
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.list.SwipeToRefresh
import cn.a10miaomiao.bilimiao.compose.components.miao.MiaoTitleBar
import cn.a10miaomiao.bilimiao.compose.pages.community.ReplyEditParams
import cn.a10miaomiao.bilimiao.compose.pages.community.components.ReplyEditPopup
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSpacePage
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.entity.video.VideoCommentReplyInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.mypage.myMenu
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance

private class ReplyDetailContentViewModel(
    override val di: DI,
    private val currentReply: ReplyInfo,
    private val onDeletedReply: (ReplyInfo) -> Unit = {},
) : ViewModel(), DIAware {

    private val pageNavigation: PageNavigation by instance()
    private val messageDialog: MessageDialogState by instance()

    private val userStore: UserStore by instance()

    var replyEditParams by androidx.compose.runtime.mutableStateOf<ReplyEditParams?>(null)
        private set

    fun closeReplyDialog() {
        replyEditParams = null
    }

    private var _sortOrder = MutableStateFlow(2)
    val sortOrder: StateFlow<Int> get() = _sortOrder
    val sortOrderList = listOf(
        2 to "按时间",
        3 to "按热度",
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> get() = _isRefreshing
    val list = FlowPaginationInfo<ReplyInfo>()
    private val _upMid = MutableStateFlow(-1L)
    val upMid: StateFlow<Long> get() = _upMid
    private var _cursor: CursorReply? = null

    init {
        loadData()
    }

    private fun addNewReply(reply: VideoCommentReplyInfo) {
        // TODO: 自定义通用Reply实体类
        refreshList()
    }

    private fun loadData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            list.loading.value = true
            val req = DetailListReq(
                oid = currentReply.oid,
                type = currentReply.type,
                root = currentReply.id,
                scene = DetailListScene.REPLY,
                cursor = CursorReq(
                    mode = bilibili.main.community.reply.v1.Mode.fromValue(sortOrder.value),
                    next = _cursor?.next ?: 0,
                )
            )
            val res = BiliGRPCHttp.request {
                ReplyGRPC.detailList(req)
            }.awaitCall()
            val listData = list.data.value.toMutableList()
            res.subjectControl?.let {
                _upMid.value = it.upMid
            }
            res.root?.let {
                listData.addAll(it.replies.filter { i1 ->
                    listData.indexOfFirst { i2 -> i1.id == i2.id } == -1
                })
            }
            list.data.value = listData
            _cursor = res.cursor
            if (res.cursor?.isEnd == true) {
                list.finished.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            list.fail.value = e.message ?: e.toString()
        } finally {
            list.loading.value = false
            _isRefreshing.value = false
        }
    }

    fun loadMore() {
        if (!this.list.finished.value && !this.list.loading.value) {
            loadData()
        }
    }

    fun refreshList(
        refreshing: Boolean = true,
    ) {
        list.reset()
        _cursor = null
        _isRefreshing.value = refreshing
        loadData()
    }

    fun likeReplyAt(index: Int) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val item = list.data.value[index]
            val isLike = item.replyControl?.action == 1L
            val newAction = if (isLike) 0 else 1
            val res = BiliApiService.commentApi
                .action(1, item.oid.toString(), item.id.toString(), newAction)
                .awaitCall()
                .json<MessageInfo>()
            if (res.isSuccess) {
                val likeNum = if (isLike) item.like - 1 else item.like + 1
                val newItem = item.copy(
                    replyControl = item.replyControl?.copy(
                        action = newAction.toLong(),
                    ),
                    like = likeNum,
                )
                val newList = list.data.value.toMutableList()
                newList[index] = newItem
                list.data.value = newList
            } else {
                GlobalToaster.show(res.message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalToaster.show("喵喵被搞坏了:" + e.message ?: e.toString())
        }
    }

    fun deleteReply(
        reply: ReplyInfo
    ) {
        messageDialog.open(
            title = "提示",
            text = "确定要删除这条评论：${reply.content?.message}",
            confirmButton = {
                TextButton(
                    onClick = {
                        messageDialog.close()
                        requestDeleteReply(reply)
                    },
                ) {
                    Text("确定")
                }
            },
            closeText = "取消",
            showClose = true,
        )
    }

    fun requestDeleteReply(
        reply: ReplyInfo
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                messageDialog.loading("正在删除")
            }
            val res = BiliApiService.commentApi
                .del(
                    type = reply.type.toInt(),
                    oid = reply.oid.toString(),
                    rpid = reply.id.toString(),
                )
                .awaitCall()
                .json<MessageInfo>()
            if (res.isSuccess) {
                withContext(Dispatchers.Main) {
                    GlobalToaster.show("删除成功")
                    messageDialog.close()
                    if (currentReply.id == reply.id) {
                        onDeletedReply(reply)
                    } else {
                        val newList = list.data.value.toMutableList()
                        val index = newList.indexOfFirst {
                            it.id == reply.id
                        }
                        if (index != -1) {
                            newList.removeAt(index)
                        }
                        list.data.value = newList
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    GlobalToaster.show(res.message)
                    messageDialog.close()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                messageDialog.alert(
                    title = "喵喵被搞坏了",
                    text = e.message ?: e.toString()
                )
            }
        }
    }

    fun setSortOrder(value: Int) {
        _sortOrder.value = value
        refreshList(false)
    }

    fun isLogin() = userStore.isLogin()

    fun toUserPage(mid: String) {
        pageNavigation.navigate(UserSpacePage(
            id = mid,
        ))
    }

    fun openReplyDialog(reply: ReplyInfo) {
        val params = ReplyEditParams(
            type = currentReply.type.toInt(),
            oid = reply.oid.toString(),
            root = currentReply.id.toString(),
            parent = reply.id.toString(),
            name = reply.member?.name ?: "",
        )
        replyEditParams = params
    }

    fun openReplyDialog() {
        val rid = currentReply.id
        val root = currentReply.root
        val params = ReplyEditParams(
            type = currentReply.type.toInt(),
            oid = currentReply.oid.toString(),
            root = if (root == 0L) rid.toString() else root.toString(),
            parent = rid.toString(),
            name = currentReply.member?.name ?: "",
        )
        miaoLogger() debug params
        replyEditParams = params
    }

    fun menuItemClick(item: MenuItemPropInfo) {
        when (val key = item.key) {
            in 0..10 -> {
                setSortOrder(key!!)
            }
            MenuKeys.send -> {
                openReplyDialog()
            }
            MenuKeys.delete -> {
                deleteReply(currentReply)
            }
        }
    }
}

/**
 * 排序下拉菜单定位：菜单在按钮正下方、右对齐按钮右边缘（顶栏右上角），
 * 超出屏幕时收拢到可见范围内。
 */
private class SortMenuPositionProvider(
    private val anchorBounds: IntRect,
    private val spacingPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchor = this.anchorBounds
        val x = (anchor.right - popupContentSize.width)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = (anchor.bottom + spacingPx)
            .coerceAtMost(
                (windowSize.height - popupContentSize.height - spacingPx)
                    .coerceAtLeast(0),
            )
        return IntOffset(x = x, y = y)
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ReplyDetailContent(
    reply: ReplyInfo,
    innerPadding: PaddingValues,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    usePageConfig: Boolean = false,
    onCloseClick: () -> Unit = {},
    onDeletedReply: (ReplyInfo) -> Unit = {},
    onLikeReply: (ReplyInfo) -> Unit = {},
) {
    val userStore by rememberInstance<UserStore>()
    val userState by userStore.stateFlow.collectAsState()
    val viewModel = diViewModel(
        key = "reply-detail-${reply.id}"
    ) {
        ReplyDetailContentViewModel(
            di = it,
            currentReply = reply,
            onDeletedReply = onDeletedReply,
        )
    }

    val list by viewModel.list.data.collectAsState()
    val listLoading by viewModel.list.loading.collectAsState()
    val listFinished by viewModel.list.finished.collectAsState()
    val listFail by viewModel.list.fail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val upMid by viewModel.upMid.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val sortOrderList = viewModel.sortOrderList

    if (usePageConfig) {
        val memberName = reply.member?.name
        val memberId = reply.member?.mid
        val configId = PageConfig(
            title = if (memberName.isNullOrBlank())
                "评论详情"
            else
                "${memberName}\n的\n评论",
            menu = rememberMyMenu(sortOrder) {
                myItem {
                    key = MenuKeys.send
                    iconVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send
                    title = "回复评论"
                }
                if (userStore.isSelf(memberId?.toString() ?: "")) {
                    myItem {
                        key = MenuKeys.delete
                        iconVector = androidx.compose.material.icons.Icons.Outlined.Delete
                        title = "删除评论"
                    }
                }
                myItem {
                    key = MenuKeys.sort
                    iconVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort
                    title = sortOrderList
                        .find { it.first == sortOrder }
                        ?.second ?: "排序"
                    childMenu = myMenu {
                        checkable = true
                        checkedKey = sortOrder
                        sortOrderList.forEach {
                            myItem {
                                key = it.first
                                title = it.second
                            }
                        }
                    }
                }
            }
        )
        PageListener(
            configId = configId,
            onMenuItemClick = viewModel::menuItemClick
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        MiaoTitleBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
                ),
            icon = {
                IconButton(
                    onClick = onCloseClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp),
                    )
                }
            },
            title = {
                Text(
                    text = "评论详情",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            action = {
                if (!usePageConfig) {
                    IconButton(
                        onClick = viewModel::openReplyDialog
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "回复评论",
                        )
                    }
                    val expanded = remember {
                        mutableStateOf(false)
                    }
                    var sortAnchorBounds by remember { mutableStateOf(IntRect.Zero) }
                    // m3e 下拉菜单动画（与底栏/主题设置同款）
                    var menuPopupVisible by remember { mutableStateOf(false) }
                    val menuAnimatable = remember { Animatable(0f) }
                    LaunchedEffect(expanded.value) {
                        if (expanded.value) {
                            menuPopupVisible = true
                            menuAnimatable.snapTo(0f)
                            menuAnimatable.animateTo(1f, animationSpec = tween(durationMillis = 150))
                        } else {
                            menuAnimatable.animateTo(0f, animationSpec = tween(durationMillis = 150))
                            menuPopupVisible = false
                        }
                    }
                    Box {
                        IconButton(
                            onClick = {
                                expanded.value = true
                            },
                            modifier = Modifier.onGloballyPositioned { coords ->
                                val rect = coords.boundsInWindow()
                                sortAnchorBounds = IntRect(
                                    left = rect.left.roundToInt(),
                                    top = rect.top.roundToInt(),
                                    right = rect.right.roundToInt(),
                                    bottom = rect.bottom.roundToInt(),
                                )
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription = "列表排序",
                            )
                        }
                        if (menuPopupVisible) {
                            val spacingPx = with(LocalDensity.current) { 8.dp.toPx().roundToInt() }
                            Popup(
                                onDismissRequest = { expanded.value = false },
                                popupPositionProvider = SortMenuPositionProvider(
                                    anchorBounds = sortAnchorBounds,
                                    spacingPx = spacingPx,
                                ),
                                properties = PopupProperties(focusable = true),
                            ) {
                                val scale = 0.8f + 0.2f * menuAnimatable.value
                                val alpha = menuAnimatable.value
                                Box(
                                    modifier = Modifier
                                        .width(IntrinsicSize.Max)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            this.alpha = alpha
                                            transformOrigin = TransformOrigin(1f, 0f)
                                        },
                                ) {
                                    DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                                        viewModel.sortOrderList.forEachIndexed { index, item ->
                                            DropdownMenuItem(
                                                selected = item.first == sortOrder,
                                                onClick = {
                                                    viewModel.setSortOrder(item.first)
                                                    expanded.value = false
                                                },
                                                text = {
                                                    Text(text = item.second)
                                                },
                                                shapes = MenuDefaults.itemShape(
                                                    index = index,
                                                    count = viewModel.sortOrderList.size,
                                                ),
                                                selectedLeadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(
                                                            MenuDefaults.LeadingIconSize,
                                                        ),
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
        SwipeToRefresh(
            refreshing = isRefreshing,
            onRefresh = { viewModel.refreshList() },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                    ),
                contentPadding = PaddingValues(
                    bottom = innerPadding.calculateBottomPadding(),
                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                item {
                    // 顶栏与主题评论之间留出分段间距
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    val replyMid = reply.mid
                    CompositionLocalProvider(
                        LocalListItemShapes provides segmentedItemShapes(0, 1),
                    ) {
                        ReplyItemBox(
                            modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedElement(
                                        rememberSharedContentState("reply-${reply.id}"),
                                        animatedVisibilityScope,
                                    )
                                }
                            } else {
                                Modifier
                            }
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            item = reply,
                            isUpper = replyMid == upMid,
//                        showDelete = userState.isSelf(replyMid),
                            onAvatarClick = {
                                viewModel.toUserPage(reply.mid.toString())
                            },
                            onLikeClick = {
                                onLikeReply(reply)
                            },
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${reply.count}条回复",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                        )
                    }

                }
                itemsIndexed(
                    list,
                    key = { _, item -> item.id },
                ) { index, replyItem ->
                    val replyMid = replyItem.mid
                    CompositionLocalProvider(
                        LocalListItemShapes provides segmentedItemShapes(
                            index,
                            list.size,
                        ),
                    ) {
                        ReplyItemBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            item = replyItem,
                            isUpper = replyMid == upMid,
                            showDelete = userState.isSelf(replyMid),
                            onAvatarClick = {
                                viewModel.toUserPage(replyItem.mid.toString())
                            },
                            onLikeClick = {
                                viewModel.likeReplyAt(index)
                            },
                            onReplyClick = {
                                viewModel.openReplyDialog(replyItem)
                            },
                            onDeleteClick = {
                                viewModel.deleteReply(replyItem)
                            }
                        )
                    }
                }
                item() {
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


    viewModel.replyEditParams?.let { params ->
        ReplyEditPopup(
            params = params,
            onDismiss = viewModel::closeReplyDialog,
        )
    }

}
