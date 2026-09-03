package cn.a10miaomiao.bilimiao.compose.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.ORIENTATION_LANDSCAPE
import cn.a10miaomiao.bilimiao.compose.ORIENTATION_PORTRAIT
import cn.a10miaomiao.bilimiao.compose.PlayerFloatingLayoutState
import cn.a10miaomiao.bilimiao.compose.PlayerState
import cn.a10miaomiao.bilimiao.compose.StartViewState

import cn.a10miaomiao.bilimiao.compose.common.LocalContentInsets
import cn.a10miaomiao.bilimiao.compose.common.LocalPlayerState
import cn.a10miaomiao.bilimiao.compose.common.isCompactWindow
import cn.a10miaomiao.bilimiao.compose.common.ContentInsets
import cn.a10miaomiao.bilimiao.compose.common.toContentInsets
import cn.a10miaomiao.bilimiao.compose.components.appbar.AppBar
import cn.a10miaomiao.bilimiao.compose.components.appbar.AppBarHorizontal
import cn.a10miaomiao.bilimiao.compose.components.appbar.AppBarOrientation
import cn.a10miaomiao.bilimiao.compose.components.appbar.AppBarState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DrawerMaxWidth = 400
private const val DrawerScrimMaxAlpha = 0.4f
private const val DrawerSettleDurationMillis = 250
private const val DrawerVelocityThresholdFraction = 0.1f

private enum class ComposeDrawerValue {
    Closed,
    Open,
}

@Composable
fun ComposeScaffold(
    startViewState: StartViewState,
    playerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    appBarState: AppBarState? = null,
    allowDrawerOpenGesture: Boolean = true,
    drawerContent: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val playerState = startViewState.playerState
    val showPlayer = playerState.showPlayer
    val fullScreenPlayer by playerState.fullScreenPlayer.collectAsState()
    val orientation = if (isCompactWindow()) ORIENTATION_PORTRAIT else ORIENTATION_LANDSCAPE
    val portraitPlayerLayoutState = playerState.portraitPlayerLayoutState
    val floatingPlayerLayoutState = playerState.floatingPlayerLayoutState
    val playerVideoRatio = playerState.playerVideoRatio
    val anchorBounds = playerState.anchorBounds
    val drawerState = startViewState.drawerState

    val appBarNestedScrollConnection = remember(appBarState, orientation) {
        if (appBarState == null) {
            null
        } else {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: androidx.compose.ui.geometry.Offset,
                    available: androidx.compose.ui.geometry.Offset,
                    source: NestedScrollSource,
                ): androidx.compose.ui.geometry.Offset {
                    if (appBarState.orientation != AppBarOrientation.Vertical) {
                        appBarState.showBar()
                        appBarState.showMenu()
                        return androidx.compose.ui.geometry.Offset.Zero
                    }
                    if (source != NestedScrollSource.UserInput) {
                        return androidx.compose.ui.geometry.Offset.Zero
                    }
                    if (abs(consumed.y) < 0.5f) {
                        return androidx.compose.ui.geometry.Offset.Zero
                    }
                    if (consumed.y < 0) {
                        appBarState.hideMenu()
                        appBarState.hideBar()
                    } else if (consumed.y > 0) {
                        appBarState.showBar()
                        appBarState.showMenu()
                    }
                    return androidx.compose.ui.geometry.Offset.Zero
                }
            }
        }
    }

    LaunchedEffect(appBarState?.orientation) {
        if (appBarState?.orientation == AppBarOrientation.Horizontal) {
            appBarState.showBar()
            appBarState.showMenu()
        }
    }

    // 布局骨架只响应系统栏，完全不读取 IME：
    // safeDrawing 包含 ime，若随输入法动画每帧变化，脚手架（常驻 Tab、底栏、播放器层）
    // 会在展开/收起动画期间每帧重新 measure 整棵布局树，造成输入卡顿
    // （与 KernelSU 一致：底栏固定在屏幕底部被键盘覆盖，IME 避让交给页面自身的 imePadding）。
    // 横屏时忽略摄像头打孔区域（displayCutout），让内容延伸到摄像头区域；
    // 竖屏保留完整 safeDrawing（摄像头与状态栏重叠，不能排除否则顶部 inset 丢失导致内容与状态栏重合）
    val isLandscape = LocalWindowInfo.current.containerSize.width >
        LocalWindowInfo.current.containerSize.height
    val rawWindowInsets = if (isLandscape) {
        WindowInsets.safeDrawing
            .exclude(WindowInsets.displayCutout)
            .exclude(WindowInsets.ime)
            .toContentInsets()
    } else {
        WindowInsets.safeDrawing
            .exclude(WindowInsets.ime)
            .toContentInsets()
    }
    val density = LocalDensity.current
    val densityFloat = density.density
    val playerLayoutState = remember(
        showPlayer,
        fullScreenPlayer,
        orientation,
        portraitPlayerLayoutState,
        floatingPlayerLayoutState,
        playerVideoRatio,
        anchorBounds,
    ) {
        ComposeScaffoldPlayerLayoutState(
            showPlayer = showPlayer,
            fullScreenPlayer = fullScreenPlayer,
            orientation = orientation,
            portraitState = portraitPlayerLayoutState,
            floatingState = floatingPlayerLayoutState,
            playerVideoRatio = playerVideoRatio,
            anchorBounds = anchorBounds,
        )
    }
    val drawerController = rememberComposeDrawerController(
        initialState = drawerState,
        onStateChanged = startViewState::setDrawerState,
    )
    var drawerMeasuredWidthPx by remember(orientation) { mutableIntStateOf(0) }

    LaunchedEffect(drawerMeasuredWidthPx) {
        drawerController.updateDrawerWidth(drawerMeasuredWidthPx.toFloat())
    }

    LaunchedEffect(drawerState, drawerMeasuredWidthPx) {
        if (drawerMeasuredWidthPx <= 0) {
            return@LaunchedEffect
        }
        when (drawerState) {
            StartViewState.DRAWER_STATE_EXPANDED -> drawerController.open()
            StartViewState.DRAWER_STATE_COLLAPSED -> drawerController.close()
        }
    }

    LaunchedEffect(
        drawerController.openFraction,
        drawerController.currentValue,
        drawerController.targetValue,
    ) {
        drawerController.syncWrapperState()
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val layoutResult = with(density) {
            calculateComposeScaffoldLayout(
                viewportWidth = maxWidth,
                viewportHeight = maxHeight,
                rawWindowInsets = rawWindowInsets,
                appBarState = appBarState,
                playerState = playerLayoutState,
            )
        }
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val leftEdgeWidthPx = with(density) { 40.dp.toPx() }
        val appBarGestureRect = layoutResult.appBarHorizontalBounds ?: layoutResult.appBarVerticalBounds
        val drawerWidth = if (maxWidth > DrawerMaxWidth.dp) DrawerMaxWidth.dp else maxWidth
        val scrimAlpha = drawerController.openFraction * DrawerScrimMaxAlpha
        val contentOffsetY = remember { Animatable(0f) }

        LaunchedEffect(layoutResult.contentBounds.top) {
            contentOffsetY.animateTo(
                targetValue = layoutResult.contentBounds.top,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
                ),
            )
        }
        val drawerGestureModifier = Modifier.anchoredDraggable(
            state = drawerController.state,
            orientation = Orientation.Horizontal,
            enabled = true,
            flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                state = drawerController.state,
                positionalThreshold = { distance -> distance * 0.5f },
            ),
        )
        val appBarDrawerGestureModifier = if (
            allowDrawerOpenGesture && drawerController.settledValue == ComposeDrawerValue.Closed
        ) {
            Modifier
                .pointerInput(startViewState) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        startViewState.setTouchStartTop(down.position.y, maxHeightPx.toInt(), densityFloat)
                    }
                }
                .then(drawerGestureModifier)
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(
                    allowDrawerOpenGesture,
                    drawerController.settledValue,
                    leftEdgeWidthPx,
                    appBarGestureRect,
                ) {
                    if (!allowDrawerOpenGesture || drawerController.settledValue != ComposeDrawerValue.Closed) {
                        return@pointerInput
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (!isDrawerOpenGestureStart(down.position, leftEdgeWidthPx, appBarGestureRect)) {
                            return@awaitEachGesture
                        }
                        startViewState.setTouchStartTop(down.position.y, maxHeightPx.toInt(), densityFloat)
                        val pointerId = down.id
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        var dragging = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                if (dragging) {
                                    drawerController.scope.launch {
                                        drawerController.settle(velocityTracker.calculateVelocity().x)
                                    }
                                }
                                break
                            }
                            if (!change.positionChanged()) {
                                continue
                            }
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val deltaX = change.position.x - down.position.x
                            val deltaY = change.position.y - down.position.y
                            if (!dragging) {
                                if (kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) || deltaX <= 0f) {
                                    break
                                }
                                dragging = true
                            }
                            val consumed = drawerController.state.dispatchRawDelta(deltaX - drawerController.dragConsumedDelta)
                            drawerController.dragConsumedDelta += consumed
                            if (consumed != 0f) {
                                change.consume()
                            }
                        }
                        drawerController.dragConsumedDelta = 0f
                    }
                }
        ) {
            SubcomposeLayout(modifier = Modifier.fillMaxSize()) { constraints ->
                val contentPlaceable = subcompose("content") {
                    CompositionLocalProvider(
                        LocalContentInsets provides layoutResult.contentInsets,
                        LocalPlayerState provides playerState,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            content()
                        }
                    }
                }.single().measure(contentConstraints(constraints, layoutResult.contentBounds))

                val verticalAppBarPlaceable = if (layoutResult.hasVerticalAppBar && appBarState != null) {
                    subcompose("appBarVertical") {
                        AnimatedVisibility(
                            visible = appBarState.barVisible,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                        ) {
                            AppBar(
                                title = appBarState.title,
                                canBack = appBarState.canBack,
                                showPointer = appBarState.showPointer,
                                pointerOrientation = appBarState.pointerOrientation,
                                showExchange = appBarState.showExchange,
                                menus = appBarState.menus,
                                isNavigationMenu = appBarState.isNavigationMenu,
                                checkedKey = appBarState.checkedKey,
                                menuExpanded = appBarState.menuExpanded,
                                appBarState = appBarState,
                                onBackClick = { appBarState._onBackClick?.invoke() },
                                onMenuClick = { appBarState._onMenuClick?.invoke() },
                                onMenuItemClick = { appBarState._onMenuItemClick?.invoke(it) },
                                onPointerClick = { appBarState._onPointerClick?.invoke() },
                                onExchangeClick = { appBarState._onExchangeClick?.invoke() },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(appBarDrawerGestureModifier),
                            )
                        }
                    }.single().measure(boundsConstraints(layoutResult.appBarVerticalBounds!!))
                } else {
                    null
                }

                val horizontalAppBarPlaceable = if (layoutResult.hasHorizontalAppBar && appBarState != null) {
                    subcompose("appBarHorizontal") {
                        AppBarHorizontal(
                            title = appBarState.title,
                            showBack = appBarState.canBack,
                            showPointer = appBarState.showPointer,
                            pointerOrientation = appBarState.pointerOrientation,
                            showExchange = appBarState.showExchange,
                            menus = appBarState.menus,
                            isNavigationMenu = appBarState.isNavigationMenu,
                            checkedKey = appBarState.checkedKey,
                            appBarState = appBarState,
                            onBackClick = { appBarState._onBackClick?.invoke() },
                            onMenuClick = { appBarState._onMenuClick?.invoke() },
                            onMenuItemClick = { appBarState._onMenuItemClick?.invoke(it) },
                            onPointerClick = { appBarState._onPointerClick?.invoke() },
                            onExchangeClick = { appBarState._onExchangeClick?.invoke() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }.single().measure(boundsConstraints(layoutResult.appBarHorizontalBounds!!))
                } else {
                    null
                }

                val playerPlaceable = if (layoutResult.hasPlayer) {
                    subcompose("player") {
                        PlayerLayer(
                            playerState = playerState,
                            playerContent = playerContent,
                            baseBounds = layoutResult.playerBounds!!,
                            portraitTopInset = rawWindowInsets.top,
                            safeDrawingInsets = rawWindowInsets,
                            viewportWidth = viewportWidth,
                            viewportHeight = viewportHeight,
                        )
                    }.single().measure(
                        constraints.copy(
                            minWidth = 0,
                            minHeight = 0,
                        )
                    )
                } else {
                    null
                }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    val contentRect = layoutResult.contentBounds
                    contentPlaceable.placeRelative(
                        contentRect.left.toInt(),
                        contentOffsetY.value.toInt(),
                    )

                    layoutResult.appBarHorizontalBounds?.let { rect ->
                        horizontalAppBarPlaceable?.placeRelative(rect.left.toInt(), rect.top.toInt())
                    }
                    layoutResult.appBarVerticalBounds?.let { rect ->
                        verticalAppBarPlaceable?.placeRelative(rect.left.toInt(), rect.top.toInt())
                    }
                    layoutResult.playerBounds?.let { rect ->
                        playerPlaceable?.placeRelative(rect.left.toInt(), rect.top.toInt())
                    }
                }
            }

            if (drawerController.isOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(drawerGestureModifier)
                        .background(Color.Black.copy(alpha = scrimAlpha))
                        .pointerInput(Unit) {
                            detectTapGestures {
                                drawerController.scope.launch {
                                    drawerController.close()
                                }
                            }
                        }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(drawerWidth)
                    .offset {
                        IntOffset(drawerController.currentOffset.roundToInt(), 0)
                    }
                    .then(
                        if (drawerMeasuredWidthPx == 0) Modifier.graphicsLayer { alpha = 0f } else Modifier
                    )
                    .onSizeChanged {
                        drawerMeasuredWidthPx = it.width
                    }
                    .then(drawerGestureModifier)
            ) {
                drawerContent()
            }
        }
    }
}

private fun isDrawerOpenGestureStart(
    position: Offset,
    leftEdgeWidthPx: Float,
    appBarGestureRect: Rect?,
): Boolean {
    return position.x <= leftEdgeWidthPx || appBarGestureRect?.contains(position) == true
}

private class ComposeDrawerController(
    initialState: Int,
    private val onStateChanged: (Int) -> Unit,
    val scope: CoroutineScope,
) {
    private var drawerWidthPx by mutableFloatStateOf(0f)
    private var lastDispatchedState by mutableIntStateOf(initialState)
    private var programmaticChange by mutableStateOf(false)
    var dragConsumedDelta by mutableFloatStateOf(0f)

    val state = AnchoredDraggableState(
        initialValue = initialState.toDrawerValue(),
    )

    val currentValue: ComposeDrawerValue
        get() = state.currentValue

    val targetValue: ComposeDrawerValue
        get() = state.targetValue

    val settledValue: ComposeDrawerValue
        get() = state.settledValue

    val isAnimationRunning: Boolean
        get() = state.isAnimationRunning

    val currentOffset: Float
        get() = if (drawerWidthPx <= 0f || state.offset.isNaN()) -drawerWidthPx else state.requireOffset()

    val openFraction: Float
        get() = if (drawerWidthPx <= 0f) 0f else ((drawerWidthPx + currentOffset) / drawerWidthPx).coerceIn(0f, 1f)

    val isOpen: Boolean
        get() = openFraction > 0f || settledValue == ComposeDrawerValue.Open || isAnimationRunning

    suspend fun updateDrawerWidth(widthPx: Float) {
        if (widthPx <= 0f || drawerWidthPx == widthPx) {
            return
        }
        drawerWidthPx = widthPx
        state.updateAnchors(
            DraggableAnchors {
                ComposeDrawerValue.Closed at -drawerWidthPx
                ComposeDrawerValue.Open at 0f
            }
        )
    }

    suspend fun open() {
        if (drawerWidthPx <= 0f || targetValue == ComposeDrawerValue.Open) {
            return
        }
        programmaticChange = true
        state.animateTo(
            ComposeDrawerValue.Open,
            animationSpec = tween(durationMillis = DrawerSettleDurationMillis),
        )
        programmaticChange = false
        dispatchState(StartViewState.DRAWER_STATE_EXPANDED)
    }

    suspend fun close() {
        if (drawerWidthPx <= 0f || targetValue == ComposeDrawerValue.Closed) {
            return
        }
        programmaticChange = true
        state.animateTo(
            ComposeDrawerValue.Closed,
            animationSpec = tween(durationMillis = DrawerSettleDurationMillis),
        )
        programmaticChange = false
        dispatchState(StartViewState.DRAWER_STATE_COLLAPSED)
    }

    suspend fun settle(velocity: Float) {
        if (drawerWidthPx <= 0f) {
            return
        }
        val threshold = drawerWidthPx * DrawerVelocityThresholdFraction
        val target = when {
            velocity >= threshold -> ComposeDrawerValue.Open
            velocity <= -threshold -> ComposeDrawerValue.Closed
            openFraction >= 0.5f -> ComposeDrawerValue.Open
            else -> ComposeDrawerValue.Closed
        }
        programmaticChange = true
        dispatchState(StartViewState.DRAWER_STATE_SETTLING)
        state.animateTo(
            target,
            animationSpec = tween(durationMillis = DrawerSettleDurationMillis),
        )
        programmaticChange = false
        dispatchState(
            if (target == ComposeDrawerValue.Open) {
                StartViewState.DRAWER_STATE_EXPANDED
            } else {
                StartViewState.DRAWER_STATE_COLLAPSED
            }
        )
    }

    fun syncWrapperState() {
        if (programmaticChange) {
            return
        }
        when {
            isAnimationRunning -> {
                dispatchState(StartViewState.DRAWER_STATE_SETTLING)
            }
            settledValue == ComposeDrawerValue.Open && targetValue == ComposeDrawerValue.Open -> {
                dispatchState(StartViewState.DRAWER_STATE_EXPANDED)
            }
            settledValue == ComposeDrawerValue.Closed && targetValue == ComposeDrawerValue.Closed -> {
                dispatchState(StartViewState.DRAWER_STATE_COLLAPSED)
            }
            else -> {
                dispatchState(StartViewState.DRAWER_STATE_DRAGGING)
            }
        }
    }

    private fun dispatchState(state: Int) {
        if (lastDispatchedState == state) {
            return
        }
        lastDispatchedState = state
        onStateChanged(state)
    }
}

@Composable
private fun rememberComposeDrawerController(
    initialState: Int,
    onStateChanged: (Int) -> Unit,
): ComposeDrawerController {
    val scope = rememberCoroutineScope()
    return remember(scope, onStateChanged) {
        ComposeDrawerController(
            initialState = initialState,
            onStateChanged = onStateChanged,
            scope = scope,
        )
    }
}

private fun Int.toDrawerValue(): ComposeDrawerValue {
    return if (this == StartViewState.DRAWER_STATE_EXPANDED) {
        ComposeDrawerValue.Open
    } else {
        ComposeDrawerValue.Closed
    }
}

private fun boundsConstraints(bounds: Rect): Constraints {
    val width = bounds.width.toInt().coerceAtLeast(0)
    val height = bounds.height.toInt().coerceAtLeast(0)
    return Constraints.fixed(width, height)
}

private fun contentConstraints(rootConstraints: Constraints, bounds: Rect): Constraints {
    val width = bounds.width.toInt().coerceAtLeast(0)
    val height = bounds.height.toInt().coerceAtLeast(0)
    return rootConstraints.copy(
        minWidth = width,
        maxWidth = width,
        minHeight = height,
        maxHeight = height,
    )
}

@Composable
internal fun PlayerLayer(
    playerState: PlayerState,
    playerContent: (@Composable () -> Unit)?,
    baseBounds: Rect,
    portraitTopInset: Dp,
    safeDrawingInsets: ContentInsets = ContentInsets(),
    viewportWidth: Dp = 0.dp,
    viewportHeight: Dp = 0.dp,
) {
    val orientation = if (isCompactWindow()) ORIENTATION_PORTRAIT else ORIENTATION_LANDSCAPE
    val density = LocalDensity.current
    val fullScreenPlayer by playerState.fullScreenPlayer.collectAsState()

    if (playerContent == null) {
        return
    }

    var currentWidth by remember { mutableStateOf(with(density) { baseBounds.width.toDp() }) }
    var currentHeight by remember { mutableStateOf(with(density) { baseBounds.height.toDp() }) }
    val scope = rememberCoroutineScope()
    var offsetX by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }
    var isDragging by remember { mutableStateOf(false) }
    // 浮窗外层容器在窗口（root）中的位置，用于缩放时把触控点换算成绝对坐标
    var windowPos by remember { mutableStateOf(Offset.Zero) }
    // 挂起（收起）状态：挂起前保存的浮窗布局，用于点击按钮恢复
    var isHoldUp by remember { mutableStateOf(false) }
    var savedFloatingState by remember { mutableStateOf<PlayerFloatingLayoutState?>(null) }

    val displayMode = when {
        !playerState.showPlayer -> PlayerDisplayMode.Hidden
        fullScreenPlayer -> PlayerDisplayMode.Fullscreen
        playerState.anchorBounds != null -> PlayerDisplayMode.AnchorOverlay
        orientation == ORIENTATION_PORTRAIT -> PlayerDisplayMode.EmbeddedPortrait
        orientation == ORIENTATION_LANDSCAPE -> PlayerDisplayMode.FloatingLandscape
        else -> PlayerDisplayMode.Hidden
    }
    val screenWidth = viewportWidth
    val screenHeight = viewportHeight
    // 播放器下方白条的边距与触摸层高度、屏幕左右边缘挂起判定阈值
    val handleSpacing = 4.dp
    val handleBarHeight = 24.dp
    val edgeThreshold = 48.dp

    /**
     * 浮窗整体（含下方白条）在垂直方向多占的高度。
     * 挂起时不显示白条，因此高度为 0。
     */
    fun floatingHandleExtent(): Dp =
        if (displayMode == PlayerDisplayMode.FloatingLandscape && !isHoldUp) {
            handleSpacing + handleBarHeight
        } else {
            0.dp
        }

    fun clampFloatingOffset(
        ox: Dp,
        oy: Dp,
        w: Dp,
        h: Dp,
    ): Pair<Dp, Dp> {
        val minX = safeDrawingInsets.left
        val minY = safeDrawingInsets.top
        val maxX = (screenWidth - safeDrawingInsets.right - w).coerceAtLeast(minX)
        val maxY = (screenHeight - safeDrawingInsets.bottom - h - floatingHandleExtent())
            .coerceAtLeast(minY)
        return ox.coerceIn(minX, maxX) to oy.coerceIn(minY, maxY)
    }

    fun updateFloatingState() {
        playerState.updateFloatingPlayerLayoutState(
            playerState.floatingPlayerLayoutState.copy(
                widthPx = with(density) { currentWidth.toPx() },
                heightPx = with(density) { currentHeight.toPx() },
                offsetXPx = with(density) { offsetX.toPx() },
                offsetYPx = with(density) { offsetY.toPx() },
                initialized = true,
            )
        )
    }

    /**
     * 挂起（收起）播放器浮窗：缩小到固定宽度（保持比例），
     * 根据触控点所在半边贴到屏幕左/右边缘。
     *
     * @param touchScreenX 松手时触控点在屏幕坐标系的 x（px）
     */
    fun holdUpToEdge(touchScreenX: Float) {
        savedFloatingState = playerState.floatingPlayerLayoutState
        val holdWidth = 200.dp
        val holdHeight =
            (holdWidth * (currentHeight / currentWidth)).coerceAtLeast(80.dp)
        isHoldUp = true
        val toLeft = touchScreenX < with(density) { screenWidth.toPx() } / 2f
        val targetX = if (toLeft) {
            safeDrawingInsets.left
        } else {
            screenWidth - safeDrawingInsets.right - holdWidth
        }
        currentWidth = holdWidth
        currentHeight = holdHeight
        val (finalX, finalY) = clampFloatingOffset(
            targetX,
            offsetY,
            holdWidth,
            holdHeight,
        )
        offsetX = finalX
        offsetY = finalY
        updateFloatingState()
        playerState.setHoldUp(true)
    }

    /**
     * 恢复挂起前的浮窗尺寸和位置。
     */
    fun restoreHoldUp() {
        val saved = savedFloatingState
        savedFloatingState = null
        isHoldUp = false
        if (saved != null && saved.initialized) {
            currentWidth = with(density) { saved.widthPx.toDp() }
            currentHeight = with(density) { saved.heightPx.toDp() }
            val (targetX, targetY) = clampFloatingOffset(
                with(density) { saved.offsetXPx.toDp() },
                with(density) { saved.offsetYPx.toDp() },
                currentWidth,
                currentHeight,
            )
            offsetX = targetX
            offsetY = targetY
        }
        updateFloatingState()
        playerState.setHoldUp(false)
    }

    suspend fun animateToSafeArea() {
        val (targetX, targetY) = clampFloatingOffset(offsetX, offsetY, currentWidth, currentHeight)
        if (targetX == offsetX && targetY == offsetY) return
        val targetOffsetXPx = with(density) { targetX.toPx() }
        val targetOffsetYPx = with(density) { targetY.toPx() }
        val startOffsetXPx = with(density) { offsetX.toPx() }
        val startOffsetYPx = with(density) { offsetY.toPx() }
        coroutineScope {
            launch {
                animate(
                    initialValue = startOffsetXPx,
                    targetValue = targetOffsetXPx,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
                    ),
                ) { value, _ ->
                    offsetX = with(density) { value.toDp() }
                }
            }
            launch {
                animate(
                    initialValue = startOffsetYPx,
                    targetValue = targetOffsetYPx,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
                    ),
                ) { value, _ ->
                    offsetY = with(density) { value.toDp() }
                }
            }
        }
        updateFloatingState()
    }

    LaunchedEffect(baseBounds, displayMode, playerState.portraitPlayerLayoutState, if (isDragging) null else playerState.floatingPlayerLayoutState) {
        val defaultWidth = with(density) { baseBounds.width.toDp() }
        val defaultHeight = with(density) { baseBounds.height.toDp() }
        when (displayMode) {
            PlayerDisplayMode.Hidden,
            PlayerDisplayMode.Fullscreen,
            PlayerDisplayMode.EmbeddedPortrait,
            PlayerDisplayMode.AnchorOverlay,
            -> {
                currentWidth = defaultWidth
                currentHeight = defaultHeight
                offsetX = 0.dp
                offsetY = 0.dp
            }
            PlayerDisplayMode.FloatingLandscape -> {
                // 拖动/缩放进行中：baseBounds 变化会触发本协程重启，
                // 此时不应恢复本地尺寸/位置（否则与手指状态竞争导致闪烁）
                if (isDragging) return@LaunchedEffect
                val floatingState = playerState.floatingPlayerLayoutState
                if (floatingState.initialized) {
                    currentWidth = with(density) { floatingState.widthPx.toDp() }
                    currentHeight = with(density) { floatingState.heightPx.toDp() }
                    val (clampedX, clampedY) = clampFloatingOffset(
                        with(density) { floatingState.offsetXPx.toDp() },
                        with(density) { floatingState.offsetYPx.toDp() },
                        currentWidth,
                        currentHeight,
                    )
                    offsetX = clampedX
                    offsetY = clampedY
                    if (with(density) { clampedX.toPx() } != floatingState.offsetXPx || with(density) { clampedY.toPx() } != floatingState.offsetXPx) {
                        playerState.updateFloatingPlayerLayoutState(
                            floatingState.copy(
                                offsetXPx = with(density) { clampedX.toPx() },
                                offsetYPx = with(density) { clampedY.toPx() },
                            )
                        )
                    }
                } else {
                    currentWidth = defaultWidth
                    currentHeight = defaultHeight
                    val (initX, initY) = clampFloatingOffset(
                        screenWidth - currentWidth,
                        0.dp,
                        currentWidth,
                        currentHeight,
                    )
                    offsetX = initX
                    offsetY = initY
                    playerState.updateFloatingPlayerLayoutState(
                        floatingState.copy(
                            defaultWidthPx = with(density) { currentWidth.toPx() },
                            defaultHeightPx = with(density) { currentHeight.toPx() },
                            widthPx = with(density) { currentWidth.toPx() },
                            heightPx = with(density) { currentHeight.toPx() },
                            offsetXPx = with(density) { offsetX.toPx() },
                            offsetYPx = with(density) { offsetY.toPx() },
                            initialized = true,
                        )
                    )
                }
            }
        }
    }

    // 浮窗主体不再全区域拦截触摸：只有白条拖动区、底部缩放区由 Compose 处理，
    // 其余区域放行给 AndroidView（播放器控件、音量/亮度手势等）。
    val modifier = if (displayMode == PlayerDisplayMode.FloatingLandscape) {
        Modifier.offset(x = offsetX, y = offsetY)
    } else {
        Modifier
    }

    if (displayMode == PlayerDisplayMode.FloatingLandscape) {
        // 横屏浮动播放器：播放器 + 下方白条（挂起时隐藏白条）。
        // 外层 Box 向外扩展 overlay：缩放热区放在播放器外围，不遮挡播放器控件。
        val overlay = 24.dp
        val totalHeight = currentHeight + floatingHandleExtent()
        Box(
            modifier = Modifier
                .offset(x = offsetX - overlay, y = offsetY - overlay)
                .size(currentWidth + overlay * 2, totalHeight + overlay * 2)
                .onGloballyPositioned { windowPos = it.positionInRoot() },
        ) {
            if (!isHoldUp) {
                // 播放器外围整圈缩放热区：在圈内任意位置拖动都可缩放。
                // 按手指相对播放器的方位固定对边：拖左/上侧时右/下边缘不动，
                // 拖右/下侧时左/上边缘不动；播放器内部放行触摸。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downPos = down.position
                                val px = with(density) { overlay.toPx() }
                                val w = with(density) { currentWidth.toPx() }
                                val h = with(density) { currentHeight.toPx() }
                                val inInner = downPos.x >= px && downPos.x <= px + w &&
                                    downPos.y >= px && downPos.y <= px + h
                                if (inInner) {
                                    // 播放器内部：放行，不消费（交给播放器控件）
                                    return@awaitEachGesture
                                }
                                down.consume()
                                // 绝对坐标基准：按下时外层容器在窗口中的位置，
                                // 之后用 offset 变化推算容器当前位置（避免节点坐标反馈衰减）
                                val downWindowX = windowPos.x
                                val downWindowY = windowPos.y
                                val downOffsetX = offsetX
                                val downOffsetY = offsetY
                                // 按下时播放器四条边在窗口坐标系的位置
                                val playerLeftPx = downWindowX + px
                                val playerTopPx = downWindowY + px
                                val playerRightPx = playerLeftPx + w
                                val playerBottomPx = playerTopPx + h
                                // 手指相对播放器中心方位：决定固定对边与缩放方向
                                val toLeft = downPos.x < px + w / 2
                                val toTop = downPos.y < px + h / 2
                                val pointerId = down.id
                                var dragging = false
                                isDragging = true
                                val minWidth = 200.dp
                                val minHeight = 112.dp
                                // 上限为屏幕可视区域（随设备/横竖屏动态变化），
                                // 避免浮窗拖到固定值（如 450dp）就卡住
                                val maxWidth = (screenWidth - safeDrawingInsets.left -
                                    safeDrawingInsets.right).coerceAtLeast(minWidth)
                                val maxHeight = (screenHeight - safeDrawingInsets.top -
                                    safeDrawingInsets.bottom - floatingHandleExtent())
                                    .coerceAtLeast(minHeight)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                    if (!change.pressed) {
                                        isDragging = false
                                        break
                                    }
                                    if (!change.positionChanged()) continue
                                    // 容器随 offset 移动：用 offset 变化推算当前容器窗口位置
                                    val nodeDX = with(density) { (offsetX - downOffsetX).toPx() }
                                    val nodeDY = with(density) { (offsetY - downOffsetY).toPx() }
                                    // 手指绝对（窗口）坐标
                                    val fingerX = downWindowX + nodeDX + change.position.x
                                    val fingerY = downWindowY + nodeDY + change.position.y
                                    if (!dragging) {
                                        if (
                                            abs(fingerX - (downWindowX + downPos.x)) < 2f &&
                                            abs(fingerY - (downWindowY + downPos.y)) < 2f
                                        ) continue
                                        dragging = true
                                    }
                                    // 水平：角点跟手，对边固定
                                    if (toLeft) {
                                        currentWidth = with(density) { (playerRightPx - fingerX).toDp() }
                                            .coerceIn(minWidth, maxWidth)
                                        offsetX = with(density) { playerRightPx.toDp() } - currentWidth
                                    } else {
                                        currentWidth = with(density) { (fingerX - playerLeftPx).toDp() }
                                            .coerceIn(minWidth, maxWidth)
                                    }
                                    // 垂直：角点跟手，对边固定
                                    if (toTop) {
                                        currentHeight = with(density) { (playerBottomPx - fingerY).toDp() }
                                            .coerceIn(minHeight, maxHeight)
                                        offsetY = with(density) { playerBottomPx.toDp() } - currentHeight
                                    } else {
                                        currentHeight = with(density) { (fingerY - playerTopPx).toDp() }
                                            .coerceIn(minHeight, maxHeight)
                                    }
                                    updateFloatingState()
                                    change.consume()
                                }
                            }
                        },
                )
            }
            // 浮窗内容（播放器 + 白条），偏移 overlay 使浮窗左上角仍位于 offsetX/offsetY
            Column(
                modifier = Modifier
                    .offset(x = overlay, y = overlay)
                    .width(currentWidth),
            ) {
                // 播放器内容
                Box(
                    modifier = Modifier.size(currentWidth, currentHeight),
                ) {
                    playerContent()
                    if (isHoldUp) {
                        // 挂起状态：覆盖整个播放器，点击取消挂起，按住拖动挂起小窗
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        down.consume()
                                        val pointerId = down.id
                                        var dragging = false
                                        isDragging = true
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                            if (!change.pressed) {
                                                isDragging = false
                                                if (!dragging) {
                                                    // 点击挂起小窗：取消挂起，恢复原尺寸
                                                    restoreHoldUp()
                                                } else {
                                                    // 拖动结束：保持挂起状态，只回弹到安全区内
                                                    scope.launch { animateToSafeArea() }
                                                }
                                                break
                                            }
                                            if (!change.positionChanged()) continue
                                            val pan = change.position - change.previousPosition
                                            if (!dragging) {
                                                if (abs(pan.x) < 2f && abs(pan.y) < 2f) continue
                                                dragging = true
                                            }
                                            // 增量式：浮窗位移 = 手指位移，完全跟手
                                            offsetX += pan.x.toDp()
                                            offsetY += pan.y.toDp()
                                            updateFloatingState()
                                            change.consume()
                                        }
                                    }
                                },
                        )
                    }
                }
                if (!isHoldUp) {
                    // 白条：位于播放器下方（外侧），正常边距
                    Spacer(modifier = Modifier.height(handleSpacing))
                    // 触控层：宽度为播放器宽度的 1/4，高度恢复 24dp，水平居中
                    val barWidth = currentWidth / 4
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(barWidth)
                            .height(handleBarHeight)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    // 消费按下事件：点击白条区域不传给视频（避免误暂停）
                                    down.consume()
                                    val pointerId = down.id
                                    var dragging = false
                                    isDragging = true
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                        if (!change.pressed) {
                                            isDragging = false
                                            if (dragging) {
                                                // 松手判定：触控点在屏幕左右边缘区域则收起，
                                                // 否则回弹到安全区（继续正常放置）
                                                // 白条层水平居中，需加上左偏移才是屏幕坐标
                                                val barLeftOffset = (currentWidth - barWidth) / 2
                                                val touchScreenX =
                                                    with(density) { offsetX.toPx() } +
                                                        with(density) { barLeftOffset.toPx() } +
                                                        change.position.x
                                                val edgePx = edgeThreshold.toPx()
                                                if (
                                                    touchScreenX <= edgePx ||
                                                    touchScreenX >= with(density) { screenWidth.toPx() } - edgePx
                                                ) {
                                                    holdUpToEdge(touchScreenX)
                                                } else {
                                                    scope.launch { animateToSafeArea() }
                                                }
                                            }
                                            break
                                        }
                                        if (!change.positionChanged()) continue
                                        val pan = change.position - change.previousPosition
                                        if (!dragging) {
                                            if (abs(pan.x) < 2f && abs(pan.y) < 2f) continue
                                            dragging = true
                                        }
                                        // 增量式：浮窗位移 = 手指位移，完全跟手
                                        offsetX += pan.x.toDp()
                                        offsetY += pan.y.toDp()
                                        updateFloatingState()
                                        change.consume()
                                    }
                                }
                            },
                    ) {
                        // 白色拖动条（视觉手柄，居中，宽=触控层宽，高 5dp）
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(Color.White.copy(alpha = 0.93f)),
                        )
                    }
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .then(
                if (baseBounds.width == Float.POSITIVE_INFINITY || baseBounds.height == Float.POSITIVE_INFINITY) {
                    Modifier.fillMaxSize()
                } else if (displayMode == PlayerDisplayMode.EmbeddedPortrait) {
                    Modifier
                        .background(Color.Black)
                        .padding(top = portraitTopInset)
                        .size(currentWidth, currentHeight)
                } else if (displayMode == PlayerDisplayMode.AnchorOverlay) {
                    Modifier
                        .background(Color.Black)
                        .size(currentWidth, currentHeight)
                } else {
                    Modifier.size(currentWidth, currentHeight)
                }
            )
            .then(modifier)
    ) {
        CompositionLocalProvider(LocalPlayerState provides playerState) {
            playerContent()
        }
    }
}
