package cn.a10miaomiao.bilimiao.compose

import cn.a10miaomiao.bilimiao.compose.ORIENTATION_LANDSCAPE
import cn.a10miaomiao.bilimiao.compose.ORIENTATION_PORTRAIT
import cn.a10miaomiao.bilimiao.compose.common.isCompactWindow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import cn.a10miaomiao.bilimiao.compose.animation.ksuCloseTransition
import cn.a10miaomiao.bilimiao.compose.animation.ksuOpenTransition
import cn.a10miaomiao.bilimiao.compose.animation.ksuPredictiveBackTransition
import cn.a10miaomiao.bilimiao.compose.animation.ksuOpenTransitionVertical
import cn.a10miaomiao.bilimiao.compose.animation.ksuCloseTransitionVertical
import androidx.navigation3.scene.Scene
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.datastore.appDataStore
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import cn.a10miaomiao.bilimiao.compose.common.BackHandler
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.LocalNavDisplayBackEnabled
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import cn.a10miaomiao.bilimiao.compose.base.BottomSheetState
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.pages.home.HomePage
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.DynamicPage
import cn.a10miaomiao.bilimiao.compose.pages.mine.ProfilePage
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchPage
import cn.a10miaomiao.bilimiao.compose.pages.video.VideoDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.community.MainReplyListPage
import cn.a10miaomiao.bilimiao.compose.pages.community.ReplyDetailListPage
import cn.a10miaomiao.bilimiao.compose.components.layout.M3EBottomBar
import cn.a10miaomiao.bilimiao.compose.platform.LocalPlatformContext
import cn.a10miaomiao.bilimiao.compose.platform.PlatformContext
import cn.a10miaomiao.bilimiao.compose.common.LocalContentInsets
import cn.a10miaomiao.bilimiao.compose.common.LocalEmitter
import cn.a10miaomiao.bilimiao.compose.common.LocalPageNavigation
import cn.a10miaomiao.bilimiao.compose.common.bottomSheetContentInsets
import cn.a10miaomiao.bilimiao.compose.common.emitter.SharedFlowEmitter
import cn.a10miaomiao.bilimiao.compose.common.foundation.animateTabSwitchTo
import cn.a10miaomiao.bilimiao.compose.common.navigation.BottomBarBackStack
import cn.a10miaomiao.bilimiao.compose.common.navigation.entriesFor
import cn.a10miaomiao.bilimiao.compose.common.navigation.BilibiliNavigation
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.navigation.rememberBottomBarBackStack
import cn.a10miaomiao.bilimiao.compose.components.layout.ComposeScaffoldPlayerLayoutState
import cn.a10miaomiao.bilimiao.compose.common.mypage.LocalPageConfigState
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfigState
import cn.a10miaomiao.bilimiao.compose.components.appbar.AppBarState
import cn.a10miaomiao.bilimiao.compose.components.appbar.LocalAppBarState
import cn.a10miaomiao.bilimiao.compose.components.dialogs.AutoSheetDialog
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialog
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialogState
import cn.a10miaomiao.bilimiao.compose.components.image.MyImagePreviewer
import cn.a10miaomiao.bilimiao.compose.components.image.provider.ImagePreviewerProvider
import cn.a10miaomiao.bilimiao.compose.components.layout.ComposeScaffold
import com.a10miaomiao.bilimiao.comm.store.AppStore
import com.a10miaomiao.bilimiao.comm.store.MessageStore
import com.a10miaomiao.bilimiao.comm.store.PlayerStore
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.compose.rememberInstance
import org.kodein.di.compose.subDI
import org.kodein.di.compose.withDI

class MainComposeNavigator(
    internal val launchUrl: (String) -> Unit,
    internal val scannerLauncher: (callback: (result: String) -> Unit) -> Boolean = { false },
    internal val onClose: () -> Unit = {},
    val topLevelRoutes: Set<NavKey> = setOf<NavKey>(
        HomePage,
        DynamicPage(),
        SearchPage(),
        ProfilePage(),
    ),
    val startRoute: NavKey = HomePage,
) {
    private var bottomBar: BottomBarBackStack? = null

    /**
     * PageNavigation 实例。在 MainComposeHost 组合期间通过 attach() 设置，
     * 保证 LocalPageNavigation provides 时已就绪（避免 by lazy 在 attach 前被访问）。
     */
    private var pageNavigationImpl: PageNavigation? = null

    val pageNavigation: PageNavigation
        get() = pageNavigationImpl ?: error("PageNavigation not attached; MainComposeHost must compose first")

    val uriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            if (!BilibiliNavigation.navigationTo(pageNavigation, uri)) {
                BilibiliNavigation.navigationToWeb(pageNavigation, uri)
            }
        }
    }

    internal fun attach(bottomBar: BottomBarBackStack, pageNavigation: PageNavigation) {
        this.bottomBar = bottomBar
        this.pageNavigationImpl = pageNavigation
    }

    fun navigateByUri(deepLink: String): Boolean {
        return pageNavigation.navigateByUri(deepLink)
    }

    fun navigate(page: ComposePage) {
        pageNavigation.navigate(page)
    }

    fun canPopBackStack(): Boolean {
        return pageNavigation.canPopBackStack()
    }

    fun popBackStack(): Boolean {
        return pageNavigation.popBackStack()
    }

    /**
     * 播放列表自动连播到下一个视频时，把当前视频详情页原地替换为新视频的详情页。
     * 只在当前可见页面是视频详情页时生效，其他页面保持不动。
     * @return 是否实际发生了页面替换
     */
    fun replaceCurrentVideoPage(id: String): Boolean {
        val bb = bottomBar ?: return false
        val top = bb.currentBackStack.lastOrNull()
        if (top !is VideoDetailPage) return false
        bb.currentBackStack.removeLastOrNull()
        bb.currentBackStack.add(VideoDetailPage(id = id))
        return true
    }

    fun goBackHome() {
        val bb = bottomBar ?: return
        if (bb.topLevelRoute != startRoute) {
            bb.topLevelRoute = startRoute
        }
    }
}

@Composable
fun MainComposeHost(
    navigator: MainComposeNavigator,
    hostDi: DI,
    startViewState: StartViewState,
    appState: AppStore.State,
    pageConfigState: PageConfigState,
    emitter: SharedFlowEmitter,
    messageDialogState: MessageDialogState,
    bottomSheetState: BottomSheetState,
    platformContext: PlatformContext,
    playerContent: (@Composable () -> Unit)? = null,
    playerMenuHost: (@Composable () -> Unit)? = null,
    onBackClick: () -> Unit,
    onPlayerBackPressed: () -> Unit = {},
    onClosePlayer: () -> Unit = {},
    initialDeepLink: String? = null,
    onInitialDeepLinkConsumed: () -> Unit = {},
    onReady: () -> Unit = {},
) {
    val bottomBar = rememberBottomBarBackStack(
        startRoute = navigator.startRoute,
        topLevelRoutes = navigator.topLevelRoutes,
    )
    val pageNavigation = remember(bottomBar) {
        PageNavigation(
            bottomBar = bottomBar,
            launchUrl = { url -> navigator.launchUrl(url) },
            scannerLauncher = navigator.scannerLauncher,
            onClose = navigator.onClose,
        )
    }
    // attach 必须在首帧 measure 之前完成：ComposeScaffold 的 SubcomposeLayout 会在 measure
    // 阶段同步组合首页，HomePageViewModel 构造时会通过 Kodein 解析 PageNavigation。
    // LaunchedEffect 的协程体是异步调度（可能在 measure 之后才执行），会触发
    // "PageNavigation not attached" 崩溃，因此这里改用 SideEffect（applyChanges 阶段同步执行）。
    SideEffect {
        navigator.attach(bottomBar, pageNavigation)
    }
    LaunchedEffect(Unit) {
        onReady()
    }
    val appBarState = remember { AppBarState() }
    val pageConfig = pageConfigState.collectConfigAsState().value
    val bottomSheetPage by bottomSheetState.page.collectAsState()
    val playerState = startViewState.playerState
    val orientation = if (isCompactWindow()) ORIENTATION_PORTRAIT else ORIENTATION_LANDSCAPE
    val showPlayer = playerState.showPlayer
    val fullScreenPlayer by playerState.fullScreenPlayer.collectAsState()
    val playerActive = showPlayer || fullScreenPlayer
    // "忽略返回手势"：开启后返回手势跳过播放器，直接导航
    val ignoreBackGesture by appDataStore.data
        .map { it[SettingPreferences.PlayerIgnoreBackGesture] ?: false }
        .collectAsState(initial = false)
    val allowDrawerOpenGesture = bottomSheetPage == null && !fullScreenPlayer
    val portraitPlayerLayoutState = playerState.portraitPlayerLayoutState
    val floatingPlayerLayoutState = playerState.floatingPlayerLayoutState
    val playerLayoutState = remember(
        showPlayer,
        fullScreenPlayer,
        orientation,
        portraitPlayerLayoutState,
        floatingPlayerLayoutState,
        playerState.playerVideoRatio,
        playerState.anchorBounds,
    ) {
        ComposeScaffoldPlayerLayoutState(
            showPlayer = showPlayer,
            fullScreenPlayer = fullScreenPlayer,
            orientation = orientation,
            portraitState = portraitPlayerLayoutState,
            floatingState = floatingPlayerLayoutState,
            playerVideoRatio = playerState.playerVideoRatio,
            anchorBounds = playerState.anchorBounds,
        )
    }
    LaunchedEffect(initialDeepLink, bottomBar) {
        initialDeepLink?.let {
            if (navigator.navigateByUri(it)) {
                onInitialDeepLinkConsumed()
            }
        }
    }
    LaunchedEffect(pageConfig, orientation) {
        val menus = pageConfig.menu?.items?.map { item ->
            cn.a10miaomiao.bilimiao.compose.components.appbar.MenuItemData.fromPropInfo(item)
        } ?: emptyList()
        appBarState.title = pageConfig.title
        appBarState.menus = menus
        appBarState.canBack = pageConfig.menu?.checkable != true
        appBarState.isNavigationMenu = pageConfig.menu?.checkable == true
        appBarState.checkedKey = pageConfig.menu?.takeIf { it.checkable }?.checkedKey
        appBarState.orientation = if (orientation == ORIENTATION_LANDSCAPE) {
            cn.a10miaomiao.bilimiao.compose.components.appbar.AppBarOrientation.Horizontal
        } else {
            cn.a10miaomiao.bilimiao.compose.components.appbar.AppBarOrientation.Vertical
        }
        appBarState.syncExpandedMenusWith(menus)
        appBarState.showBar()
        appBarState.showMenu()
    }
    LaunchedEffect(onBackClick) {
        appBarState.setOnBackClickListener(onBackClick)
        appBarState.setOnMenuClickListener {
            startViewState.openDrawer()
        }
        appBarState.setOnMenuItemClickListener {
            pageConfigState.onMenuItemClick(it.toPropInfo())
        }
    }
    LaunchedEffect(startViewState, pageConfigState) {
        pageConfigState.openSearch = {
            // 有页面级搜索（如用户主页搜索当前用户内容）时走页面搜索，
            // 否则进入全局搜索输入页；push 到当前栈，保留当前页面
            // （搜索结果页点"继续搜索"后返回时能回到原来的搜索结果页）
            val searchConfig = pageConfigState.currentConfig.search
            val keyword = searchConfig?.keyword ?: ""
            if (searchConfig?.name.isNullOrBlank()) {
                bottomBar.pushToCurrent(SearchPage())
            } else {
                pageConfigState.onSearchSelfPage(keyword)
            }
        }
    }

    CompositionLocalProvider(
        LocalPlatformContext provides platformContext,
        LocalPageConfigState provides pageConfigState,
        LocalPageNavigation provides pageNavigation,
        LocalEmitter provides emitter,
        LocalUriHandler provides navigator.uriHandler,
        LocalAppBarState provides appBarState,
    ) {
        withDI(di = hostDi) {
            BilimiaoTheme(appState = appState) {
                val toasterState = rememberToasterState()
                LaunchedEffect(toasterState) {
                    GlobalToaster.init(toasterState)
                }
                // 未读消息数（"我的"底栏角标）
                val messageStore by rememberInstance<MessageStore>()
                val messageState by messageStore.stateFlow.collectAsState()
                val unreadCount = messageState.totalCount()
                val playerStore by rememberInstance<PlayerStore>()
                val playerStoreState by playerStore.stateFlow.collectAsState()
                val playingAid = playerStoreState.aid
                val currentTopPage = bottomBar.currentBackStack.lastOrNull()
                val currentVideoPageAid = startViewState.currentVideoPageAid
                // 当前正在播放视频的详情页 + "忽略返回手势"关闭 + 未全屏：
                // 返回交给 NavDisplay 处理，以获得预测性返回手势动画（页面滑出预览），
                // 播放器在返回提交时关闭；
                // 全屏播放时返回始终先退出全屏（见 playerBackHandler），不进入该预测性 pop 分支
                val isVideoPageBackCase = playerActive && !ignoreBackGesture &&
                    !fullScreenPlayer &&
                    currentTopPage is VideoDetailPage &&
                    currentVideoPageAid == playingAid &&
                    bottomBar.currentBackStack.size > 1
                // "忽略返回手势"关闭（默认）时：正在播放且当前页面不是
                // 正在播放视频的详情页/评论页，则关闭播放器
                // 以页面/注册状态变化为触发（不含播放器开关），避免"稍后再看"等
                // 页面直接开播时被误判为"打开新页面"而立即关闭
                LaunchedEffect(
                    ignoreBackGesture,
                    currentTopPage,
                    currentVideoPageAid,
                ) {
                    if (!ignoreBackGesture && playerActive) {
                        val isCommentPage = currentTopPage is MainReplyListPage ||
                            currentTopPage is ReplyDetailListPage
                        val isCurrentVideoPage = currentTopPage is VideoDetailPage &&
                            (currentVideoPageAid == playingAid
                                || currentVideoPageAid.isNullOrBlank())
                        if (!isCommentPage && !isCurrentVideoPage) {
                            onClosePlayer()
                        }
                    }
                }
                ImagePreviewerProvider(
                    previewer = { state, innerPadding ->
                        MyImagePreviewer(state, innerPadding)
                    }
                ) {
                    var bottomSelectedIndex by remember { mutableIntStateOf(0) }
                    LaunchedEffect(bottomBar.topLevelRoute) {
                        bottomSelectedIndex = when (bottomBar.topLevelRoute::class) {
                            HomePage::class -> 0
                            DynamicPage::class -> 1
                            SearchPage::class -> 2
                            ProfilePage::class -> 3
                            else -> 0
                        }
                    }
                    ComposeScaffold(
                        startViewState = startViewState,
                        playerContent = playerContent,
                        appBarState = null,
                        allowDrawerOpenGesture = false,
                    ) {
                        // 底栏项目随页面变化：展示当前页面的操作按钮（如有）
                        val actionMenus = if (appBarState.isNavigationMenu) {
                            emptyList()
                        } else {
                            appBarState.menus
                        }
                        val bottomBarContent: @Composable () -> Unit = {
                            M3EBottomBar(
                                vertical = isWideScreen(),
                                selectedIndex = bottomSelectedIndex,
                                onSelect = { index ->
                                    bottomSelectedIndex = index
                                    when (index) {
                                        0 -> bottomBar.navigate(HomePage)
                                        1 -> bottomBar.navigate(DynamicPage())
                                        2 -> bottomBar.navigate(SearchPage())
                                        3 -> bottomBar.navigate(ProfilePage())
                                    }
                                },
                                actionMenus = actionMenus,
                                onActionClick = { menu ->
                                    pageConfigState.onMenuItemClick(menu.toPropInfo())
                                },
                                // 页面自定义菜单内容（如搜索筛选）由页面注册、底栏按 key 取用
                                customMenuContent = { menu ->
                                    pageConfigState.customMenuContents[menu.key]
                                },
                                // "导航栏角标"开关控制：关闭时传 0，不显示角标
                                profileBadgeCount =
                                    if (appState.theme?.enableNavigationBadge != false) {
                                        unreadCount
                                    } else {
                                        0
                                    },
                            )
                        }
                        // NavDisplay 返回提交回调：从正在播放视频的详情页返回时，
                        // 先关闭播放器再 pop（预测性返回手势动画由 NavDisplay 执行）
                        val onVideoPageBack: () -> Unit = {
                            val top = bottomBar.currentBackStack.lastOrNull()
                            if (!ignoreBackGesture && playerActive && top is VideoDetailPage
                                && startViewState.currentVideoPageAid == playingAid
                            ) {
                                onClosePlayer()
                            }
                            bottomBar.pop()
                        }
                        // 播放器相关的返回优先级高于导航：只要播放器显示（全屏/浮窗/固定上方），
                        // 返回键都先关闭播放器，避免返回手势穿透到上一级页面。
                        // 用 NavigationBackHandler 注册预测性返回回调（普通 BackHandler 在手势
                        // 返回时会被 NavDisplay 的预测性回调抢先命中导致穿透）；
                        // 同时下方用 LocalNavDisplayBackEnabled 禁用 NavDisplay 的返回处理做双保险。
                        // 注意：必须组合在 MyNavHost（NavDisplay 的返回处理）之后，后注册先消费
                        val playerBackHandler: @Composable () -> Unit = {
                            val playerGestureState =
                                rememberNavigationEventState(NavigationEventInfo.None)
                            NavigationBackHandler(
                                state = playerGestureState,
                                // 全屏播放：返回总是先退出全屏（与"忽略返回手势"开关无关，
                                // 也优先于详情页预测性返回），避免返回手势穿透到背后的页面，
                                // 或把播放器与视频详情页一起退出；
                                // 非全屏时：开启"忽略返回手势"不再拦截，返回交给导航处理；
                                // 当前视频页返回已交给 NavDisplay（带预测性动画），这里不再拦截
                                isBackEnabled = playerActive &&
                                    (fullScreenPlayer ||
                                        (!ignoreBackGesture && !isVideoPageBackCase)),
                                onBackCompleted = {
                                    if (fullScreenPlayer) {
                                        // 全屏播放时返回：只退出全屏，回到小窗播放器 +
                                        // 视频详情页（与开关关闭时的行为一致），
                                        // 不关闭播放器、不弹导航栈
                                        onPlayerBackPressed()
                                        return@NavigationBackHandler
                                    }
                                    val top = bottomBar.currentBackStack.lastOrNull()
                                    // "忽略返回手势"关闭时，从正在播放视频的详情页返回：
                                    // 关闭播放器并直接回到上一级页面
                                    if (top is VideoDetailPage
                                        && startViewState.currentVideoPageAid == playingAid
                                    ) {
                                        onClosePlayer()
                                        bottomBar.pop()
                                    } else {
                                        onPlayerBackPressed()
                                    }
                                },
                            )
                        }
                        if (isWideScreen()) {
                            // 平板/宽屏：左侧竖排导航 + 右侧内容
                            Row(modifier = Modifier.fillMaxSize()) {
                                // 竖排导航常驻（与 KernelSU SideRail 一致），
                                // 不随输入法隐藏，避免页面切换时底栏闪烁/消失
                                bottomBarContent()
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        CompositionLocalProvider(
                                            LocalNavDisplayBackEnabled provides
                                                (!playerActive ||
                                                    (ignoreBackGesture && !fullScreenPlayer)
                                                    || isVideoPageBackCase),
                                        ) {
                                            MyNavHost(
                                                bottomBar = bottomBar,
                                                onBack = onVideoPageBack,
                                            )
                                        }
                                    }
                                    playerBackHandler()
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CompositionLocalProvider(
                                        LocalNavDisplayBackEnabled provides
                                            (!playerActive ||
                                                (ignoreBackGesture && !fullScreenPlayer)
                                                || isVideoPageBackCase),
                                    ) {
                                        MyNavHost(
                                            bottomBar = bottomBar,
                                            onBack = onVideoPageBack,
                                        )
                                    }
                                }
                                // 底栏常驻：输入法弹出时由键盘自然覆盖，不做显隐切换。
                                // 之前用 isImeVisible() 条件移除/AnimatedVisibility 滑出时，
                                // IME 动画期间内容区高度跳变会让聚焦的输入框反复重建/重获焦点
                                // （日志表现为 Autofill + show(ime()) 每 200ms 循环），
                                // 输入法展开动画被持续打断导致卡顿。
                                bottomBarContent()
                                playerBackHandler()
                            }
                        }
                    }
                    if (bottomSheetPage != null) {
                        MyBottomSheet(
                            page = bottomSheetPage!!,
                            onClose = bottomSheetState::close,
                        )
                    }
                }
                MessageDialog(messageDialogState)
                playerMenuHost?.invoke()
                Toaster(
                    state = toasterState,
                    alignment = Alignment.BottomCenter,
                    richColors = true,
                )
            }
        }
    }
}

/**
 * 平板 UI 判定：横屏（宽 > 高）即使用平板布局（左侧竖栏），竖屏使用手机布局（底部横栏）。
 */
@Composable
private fun isWideScreen(): Boolean {
    // 用窗口实际尺寸判断横竖屏，旋转后能即时更新（LocalConfiguration 在 configChanges 场景下可能滞后）
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current.density
    val widthDp = windowInfo.containerSize.width / density
    val heightDp = windowInfo.containerSize.height / density
    return widthDp > heightDp
}

@Composable
fun MyNavHost(
    bottomBar: BottomBarBackStack,
    onBack: () -> Unit = { bottomBar.pop() },
) {
    val wideScreen = isWideScreen()
    val entryProvider = entryProvider {
        BilimiaoPageRoute.entries(this)
    }
    // 常驻式 Tab：与 KernelSU Manager 一致，四个 Tab 用 Pager 保持组合，
    // 切换时整体平移（手机横向 / 平板纵向），任意次数打断都从当前位置继续，
    // 不会像 AnimatedContent 双页过渡那样丢弃中间页面。
    // 注意：必须使用 bottomBar.backStacks 里的规范实例（保持插入顺序），
    // 不能用新建的 DynamicPage()/SearchPage()/ProfilePage() 实例，
    // 否则 entriesFor 按实例查 backStacks map 会抛 NoSuchElementException。
    val tabs = remember { bottomBar.backStacks.keys.toList() }
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOfFirst {
            it::class == bottomBar.topLevelRoute::class
        }.coerceAtLeast(0),
        pageCount = { tabs.size },
    )
    // 冷启动优化：首帧只组合当前 Tab（beyondViewportPageCount = 0），
    // 首帧渲染后与 KernelSU 一致补齐全部 4 个 Tab（beyondViewportPageCount = 3），
    // 保证任意 Tab 切换都直接平移、不会现场组合目标页造成卡顿；
    // 整树重绘开销由每个 Tab 的 graphicsLayer 图层缓存吸收。
    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        contentReady = true
    }
    val beyondViewportCount = if (contentReady) 3 else 0
    // 底栏/导航切换 Tab：由 topLevelRoute 驱动 Pager 滚动（打断由 Pager 处理）
    LaunchedEffect(bottomBar.topLevelRoute) {
        val target = tabs.indexOfFirst {
            it::class == bottomBar.topLevelRoute::class
        }
        if (target < 0) return@LaunchedEffect
        // 与 KernelSU Manager 完全一致：按像素距离 animateScrollBy 滚动，
        // 会平滑经过中间所有页；不能用 animateScrollToPage——它距离太远时会
        // 预跳到邻近页再动画，造成"先闪现到中间页"。
        // 首页内时光姬/推荐/热门等 Tab 通过 animateTabSwitchTo 复用同一套动画。
        pagerState.animateTabSwitchTo(target)
    }
    // Tab 级返回（与 KernelSU MainScreenBackHandler 一致）：当前 Tab 根部且不是启动 Tab 时，
    // 返回切回启动 Tab，由 Pager 执行平移动画；子页面返回由当前 Tab 的 NavDisplay 优先处理
    //（组合更深、后注册，BackHandler 后注册先消费），这里只兜底根页面场景。
    val tabLevelBackEnabled = bottomBar.topLevelRoute != bottomBar.startRoute &&
        bottomBar.currentBackStack.size <= 1
    BackHandler(enabled = tabLevelBackEnabled) {
        bottomBar.pop()
    }
    val pagerModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceContainer)
    if (wideScreen) {
        // 平板：上下平移（与左侧竖排导航互不干扰）
        VerticalPager(
            state = pagerState,
            modifier = pagerModifier,
            beyondViewportPageCount = beyondViewportCount,
            userScrollEnabled = false,
        ) { index ->
            // 每个 Tab 独立 layer 缓存：输入法动画/交互触发整树重绘时，
            // 非当前 Tab 直接用缓存的图层（GPU 合成），只有当前 Tab 重新记录绘制命令，
            // 避免 4 个常驻 Tab 每帧重绘造成的主线程 80~100ms 掉帧
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { },
            ) {
                TabNavDisplay(
                    bottomBar = bottomBar,
                    route = tabs[index],
                    entryProvider = entryProvider,
                    onBack = onBack,
                    // 与 KernelSU 一致：用 settledPage 判断当前页，
                    // 动画落定前返回处理仍属于上一页，避免中途切换
                    isCurrent = pagerState.settledPage == index,
                    wideScreen = true,
                )
            }
        }
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = pagerModifier,
            beyondViewportPageCount = beyondViewportCount,
            userScrollEnabled = false,
        ) { index ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { },
            ) {
                TabNavDisplay(
                    bottomBar = bottomBar,
                    route = tabs[index],
                    entryProvider = entryProvider,
                    onBack = onBack,
                    isCurrent = pagerState.settledPage == index,
                    wideScreen = false,
                )
            }
        }
    }
}

/**
 * 单个 Tab 的导航宿主：独立的 [NavDisplay]，渲染该 Tab 自己的 backstack。
 * 非当前 Tab 保持组合以保留状态（ViewModel/滚动位置），但通过
 * [LocalNavDisplayBackEnabled] 关闭其系统返回处理，避免返回键误触其他 Tab。
 */
@Composable
private fun TabNavDisplay(
    bottomBar: BottomBarBackStack,
    route: NavKey,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    onBack: () -> Unit,
    isCurrent: Boolean,
    wideScreen: Boolean,
) {
    val entries = bottomBar.entriesFor(route, entryProvider)
    // 合并外层开关（如播放器显示时禁用 NavDisplay 返回），
    // 避免内层 isCurrent 覆盖掉外层的 false
    val parentBackEnabled = LocalNavDisplayBackEnabled.current
    CompositionLocalProvider(
        LocalNavDisplayBackEnabled provides (isCurrent && parentBackEnabled),
    ) {
        NavDisplay(
            entries = entries,
            onBack = onBack,
            transitionSpec = {
                if (wideScreen) {
                    // 平板子页面：新页从下方覆盖上来（KSU 同款曲线 + 1/4 视差）
                    ksuOpenTransitionVertical()
                } else {
                    // 打开页面动画（与 KernelSU Manager 一致）：新页从右侧全宽滑入
                    ksuOpenTransition()
                }
            },
            popTransitionSpec = {
                if (wideScreen) {
                    // 平板子页面返回：当前页向下滑出，上一页从上方视差滑入
                    ksuCloseTransitionVertical()
                } else {
                    // 退出页面动画（与 KernelSU Manager 一致）
                    ksuCloseTransition()
                }
            },
            predictivePopTransitionSpec = {
                if (wideScreen) {
                    // 平板预测性返回：方向固定为垂直（上一页从上方滑入）
                    ksuCloseTransitionVertical()
                } else {
                    // 子页面预测性返回（与 KernelSU Manager 一致）：方向固定左边缘
                    ksuPredictiveBackTransition()
                }
            },
        )
    }
}

@Composable
fun MyBottomSheet(
    page: ComposePage,
    onClose: () -> Unit,
) {
    val parentPageNavigation by rememberInstance<PageNavigation>()
    // BottomSheet 独立 backstack，简化：复用父级 PageNavigation
    // （sheet 内部页面调用 navigate 会进入主 backstack，行为与旧版略有差异，后续可优化）
    val pageNavigation = parentPageNavigation
    val pageConfigState = remember { PageConfigState() }
    subDI(
        diBuilder = {
            bindSingleton(overrides = true) { pageNavigation }
            bindSingleton<cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigator>(
                overrides = true
            ) { pageNavigation }
        }
    ) {
        CompositionLocalProvider(
            LocalContentInsets provides bottomSheetContentInsets(
                titleBarHeight = if (page.showBottomSheetTitleBar) {
                    48.dp
                } else {
                    0.dp
                },
            ),
            LocalPageConfigState provides pageConfigState,
            LocalPageNavigation provides pageNavigation,
        ) {
            AutoSheetDialog(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .heightIn(max = 500.dp),
                content = {
                    page.Content()
                    if (page.showBottomSheetTitleBar) {
                        MyBottomSheetTitleBar(pageConfigState, onClose)
                    }
                },
                onDismiss = onClose,
                // 返回键直接关闭底部弹窗；不要再 pop 主导航栈，
                // 否则会先返回上一层页面（如视频详情）而弹窗不关闭
                onPreDismiss = { false },
            )
        }
    }
}

@Composable
fun MyBottomSheetTitleBar(
    state: PageConfigState,
    onClose: () -> Unit,
) {
    val config = state.collectConfigAsState()
    Box(
        modifier = Modifier
            .height(48.dp)
            .padding(horizontal = 10.dp)
            .fillMaxWidth(),
    ) {
        IconButton(
            onClick = onClose,
            colors = IconButtonDefaults.iconButtonColors()
                .copy(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                        .copy(alpha = 0.75f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "close"
            )
        }

        AnimatedContent(
            modifier = Modifier
                .align(Alignment.Center),
            targetState = config.value.title,
            contentKey = { it },
            label = "BottomSheetTitle",
        ) { title ->
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer
                            .copy(alpha = 0.75f)
                    )
                    .padding(vertical = 2.dp, horizontal = 10.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                text = title.replace("\n", " "),
            )
        }
    }
}
