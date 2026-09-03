package cn.a10miaomiao.bilimiao.compose.components.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import cn.a10miaomiao.bilimiao.compose.components.appbar.MenuItemData
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Material 3 Expressive 底部导航栏。
 *
 * 传入 [actionMenus] 时，底栏项目替换为该页面的操作项（如视频页的
 * 添加至/收藏/下载/更多）；不传时显示固定的主导航：首页 / 动态 / 搜索 / 我的。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3EBottomBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    actionMenus: List<MenuItemData> = emptyList(),
    onActionClick: (MenuItemData) -> Unit = {},
    profileBadgeCount: Int = 0,
    vertical: Boolean = false,
    /**
     * 自定义菜单内容提供器：按菜单项返回其自定义下拉菜单内容。
     * 提供器本身不是 @Composable，但返回的内容是 @Composable 的，
     * 便于在 onClick 等非组合上下文判断菜单是否可展开。
     */
    customMenuContent: ((menu: MenuItemData) -> (@Composable (onDismiss: () -> Unit) -> Unit)?)? = null,
) {
    var expandedMenu by remember { mutableStateOf<MenuItemData?>(null) }
    // 记录"本次外部点击已由弹窗 dismiss 收起菜单"：
    // 点击展开的按钮时，弹窗的外部点击 dismiss 与按钮 onClick 会先后触发，
    // 若 dismiss 先执行，onClick 不应再把菜单切回去（否则会出现"收起又弹出"，
    // 并可能把弹窗窗口带入外部点击/返回都失效的异常状态）
    var outsideDismissPending by remember { mutableStateOf(false) }
    // 记录每个操作按钮在窗口中的位置，供下拉菜单定位使用
    val menuAnchorBounds = remember { mutableStateMapOf<MenuItemData, IntRect>() }
    // 弹窗窗口虽然按屏幕坐标定位，但 WindowManager 会把窗口钳制在可见显示区内
    // （状态栏以下）。因此限制菜单最大高度时需要减去状态栏顶部偏移，
    // 否则高菜单（如搜索筛选）即使按公式定位，也会被钳制下移、底端压进底栏。
    val displayTopOffsetPx = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this)
    }

    if (vertical) {
        // 竖排（平板/宽屏）：左侧竖栏；子页面显示页面操作按钮，主页面显示四个主 Tab
        // 顶部保留状态栏高度（与其他顶栏一致），其余方向延伸到摄像头区域不被留空
        NavigationRail(
            modifier = modifier,
            windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
            // 与 KernelSU 底栏取色一致：surfaceContainer
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            if (actionMenus.isNotEmpty()) {
                actionMenus.forEach { menu ->
                    NavigationRailItem(
                        selected = expandedMenu == menu || menu.selected,
                        onClick = {
                            val hasCustomMenu =
                                customMenuContent?.invoke(menu) != null
                            if (menu.childMenu.isNullOrEmpty() && !hasCustomMenu) {
                                onActionClick(menu)
                            } else {
                                val next = if (outsideDismissPending) {
                                    outsideDismissPending = false
                                    null
                                } else if (expandedMenu == menu) {
                                    null
                                } else {
                                    menu
                                }
                                expandedMenu = next
                            }
                        },
                        icon = {
                            if (menu.iconVector != null) {
                                Icon(
                                    imageVector = menu.iconVector,
                                    contentDescription = menu.contentDescription ?: menu.title,
                                )
                            } else {
                                Text(
                                    text = menu.title.take(1),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        },
                        label = {
                            Text(text = menu.title)
                        },
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val rect = coords.boundsInWindow()
                            menuAnchorBounds[menu] = IntRect(
                                left = rect.left.roundToInt(),
                                top = rect.top.roundToInt(),
                                right = rect.right.roundToInt(),
                                bottom = rect.bottom.roundToInt(),
                            )
                        },
                    )
                }
            } else {
                val items: List<Triple<ImageVector, String, Int>> = listOf(
                    Triple(Icons.Filled.Home, "首页", 0),
                    Triple(Icons.Filled.DynamicFeed, "动态", 1),
                    Triple(Icons.Filled.Search, "搜索", 2),
                    Triple(Icons.Filled.Person, "我的", 3),
                )
                items.forEach { (icon, label, index) ->
                    NavigationRailItem(
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        icon = {
                            if (index == 3 && profileBadgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                        ) {
                                            Text(
                                                text = if (profileBadgeCount > 99) {
                                                    "99+"
                                                } else {
                                                    profileBadgeCount.toString()
                                                },
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                )
                            }
                        },
                        label = {
                            Text(text = label)
                        },
                    )
                }
            }
        }
    } else {
        // 与 KernelSU 底栏取色一致：surfaceContainer
        NavigationBar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
        // 主导航项 ↔ 页面操作项切换时做平滑过渡，
        // 避免页面切换时底栏内容瞬间替换造成的视觉跳动；
        // 操作项内部的小变化（如关注状态、排序标题）不触发整栏动画。
        AnimatedContent(
            targetState = actionMenus.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                fadeIn(tween(durationMillis = 200)) togetherWith
                    fadeOut(tween(durationMillis = 120))
            },
            label = "bottomBarContent",
        ) { isActionMenu ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isActionMenu) {
                    actionMenus.forEach { menu ->
                        NavigationBarItem(
                            // 菜单展开或操作项自身为激活状态（如已关注）时显示选中态
                            selected = expandedMenu == menu || menu.selected,
                            onClick = {
                                // 带子菜单或自定义菜单内容的操作项：点击展开下拉菜单
                                val hasCustomMenu =
                                    customMenuContent?.invoke(menu) != null
                                if (menu.childMenu.isNullOrEmpty() && !hasCustomMenu) {
                                    onActionClick(menu)
                                } else {
                                    val next = if (outsideDismissPending) {
                                        // 本次点击已由弹窗 dismiss 收起，保持关闭
                                        outsideDismissPending = false
                                        null
                                    } else if (expandedMenu == menu) {
                                        null
                                    } else {
                                        menu
                                    }
                                    expandedMenu = next
                                }
                            },
                            icon = {
                                if (menu.iconVector != null) {
                                    Icon(
                                        imageVector = menu.iconVector,
                                        contentDescription = menu.contentDescription ?: menu.title,
                                    )
                                } else {
                                    Text(
                                        text = menu.title.take(1),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            },
                            label = {
                                Text(text = menu.title)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    val rect = coords.boundsInWindow()
                                    menuAnchorBounds[menu] = IntRect(
                                        left = rect.left.roundToInt(),
                                        top = rect.top.roundToInt(),
                                        right = rect.right.roundToInt(),
                                        bottom = rect.bottom.roundToInt(),
                                    )
                                },
                        )
                    }
                } else {
                    val items: List<Triple<ImageVector, String, Int>> = listOf(
                        Triple(Icons.Filled.Home, "首页", 0),
                        Triple(Icons.Filled.DynamicFeed, "动态", 1),
                        Triple(Icons.Filled.Search, "搜索", 2),
                        Triple(Icons.Filled.Person, "我的", 3),
                    )
                    items.forEach { (icon, label, index) ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { onSelect(index) },
                            icon = {
                                if (index == 3 && profileBadgeCount > 0) {
                                    // "我的"未读消息角标
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                            ) {
                                                Text(text = if (profileBadgeCount > 99) {
                                                    "99+"
                                                } else {
                                                    profileBadgeCount.toString()
                                                }, color = MaterialTheme.colorScheme.onPrimary)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                    )
                                }
                            },
                            label = {
                                Text(text = label)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
    }

    // 带子菜单的操作项：在所属按钮正上方弹出 M3E 下拉菜单
    // （水平居中于按钮中轴线，宽度由最长菜单项文字决定，不超出屏幕）
    val expandedMenuValue = expandedMenu
    val expandedChildren = expandedMenuValue?.childMenu
    val hasCustomMenu = expandedMenuValue?.let {
        customMenuContent?.invoke(it)
    } != null
    val isMenuExpanded = expandedMenuValue != null &&
        (!expandedChildren.isNullOrEmpty() || hasCustomMenu)
    // 记录最后展开的菜单内容与所属按钮，供收起动画期间继续渲染与定位
    var lastMenuChildren by remember { mutableStateOf<List<MenuItemData>?>(null) }
    var lastAnchorMenu by remember { mutableStateOf<MenuItemData?>(null) }
    if (isMenuExpanded) {
        lastMenuChildren = expandedChildren
        lastAnchorMenu = expandedMenuValue
    }

    // 菜单 Popup 显示状态与动画：
    // 展开时先显示 Popup 再播放淡入；收起时等淡出动画播放完毕后再移除 Popup，
    // 保证收起也有动画（与 M3E DropdownMenuPopup 行为一致）
    var menuPopupVisible by remember { mutableStateOf(false) }
    val menuAnimatable = remember { Animatable(0f) }
    // 记录菜单内容实际高度：异步加载（如搜索分区列表）导致内容变高时，
    // 用它触发重新创建定位器，强制 Popup 按新尺寸重新定位
    var menuContentHeightPx by remember { mutableStateOf(0) }
    LaunchedEffect(isMenuExpanded) {
        if (isMenuExpanded) {
            menuPopupVisible = true
            menuAnimatable.snapTo(0f)
            // 展开动画改由弹窗内容首帧布局后触发（见 Popup 内 onGloballyPositioned），
            // 避免重内容菜单（如搜索筛选）窗口首帧渲染慢于动画时长导致动画不可见
            outsideDismissPending = false
        } else {
            menuAnimatable.animateTo(0f, animationSpec = tween(durationMillis = 150))
            menuPopupVisible = false
            // 收起动画结束后清除 dismiss 残留标记，
            // 避免"仅 dismiss 触发（按钮 onClick 未触发）"的场景残留脏标记
            outsideDismissPending = false
        }
    }

    if (menuPopupVisible) {
        // 菜单与底栏之间的垂直间距，让阴影有呼吸空间（同时用作左右屏幕边缘的边距）
        val menuSpacingPx = with(LocalDensity.current) { 8.dp.toPx().roundToInt() }
        Popup(
            onDismissRequest = {
                // 外部点击：记录本次已由 dismiss 处理，供按钮 onClick 竞态时保持一致
                val wasOpen = expandedMenu != null
                outsideDismissPending = outsideDismissPending || wasOpen
                expandedMenu = null
            },
            popupPositionProvider = MenuPopupPositionProvider(
                anchorBoundsMap = menuAnchorBounds,
                expandedMenu = lastAnchorMenu,
                verticalSpacingPx = menuSpacingPx,
                contentHeightVersion = menuContentHeightPx,
                placement = if (vertical) {
                    MenuPopupPlacement.Right
                } else {
                    MenuPopupPlacement.Above
                },
            ),
            properties = PopupProperties(focusable = true),
        ) {
            val scope = rememberCoroutineScope()
            // 按"是否展开"而不是菜单实例做 key：收起动画期间 expandedMenu 变为 null，
            // 若按实例做 key 会重置标记，导致 onGloballyPositioned 在收起阶段再次启动
            // 展开动画，展开动画会取消收起动画（Animatable 互斥），
            // 使 menuPopupVisible=false 永远不执行、弹窗窗口无法销毁
            var menuOpenAnimationStarted by remember(isMenuExpanded) {
                mutableStateOf(false)
            }
            val scale = 0.8f + 0.2f * menuAnimatable.value
            val alpha = menuAnimatable.value
            val expandedCustomMenuContent = lastAnchorMenu?.let {
                customMenuContent?.invoke(it)
            }
            // 自定义菜单（如搜索筛选）需要限制高度，内容内部滚动：
            // 手机横排时不超过按钮上方空间；平板竖排时菜单在竖栏右侧，
            // 高度用整个屏幕垂直可用空间，避免只留按钮上方的一小段
            val anchorBounds = lastAnchorMenu?.let { menuAnchorBounds[it] }
            val menuMaxHeightPx = if (anchorBounds != null) {
                if (vertical) {
                    val windowHeight = LocalWindowInfo.current.containerSize.height
                    (windowHeight - displayTopOffsetPx - menuSpacingPx * 2)
                        .coerceAtLeast(1)
                } else {
                    (anchorBounds.top - displayTopOffsetPx - menuSpacingPx)
                        .coerceAtLeast(1)
                }
            } else {
                Int.MAX_VALUE
            }
            val menuMaxHeight = with(LocalDensity.current) {
                menuMaxHeightPx.toDp()
            }
            // 动画作用于外层 Box（含菜单框与内容），展开/收起均整体缩放+淡入淡出，
            // 与 M3E/KernelSU 的 DropdownMenuPopup 行为一致
            Box(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .then(
                        if (expandedCustomMenuContent != null) {
                            Modifier.heightIn(max = menuMaxHeight)
                        } else {
                            Modifier
                        }
                    )
                    .onSizeChanged { menuContentHeightPx = it.height }
                    .onGloballyPositioned {
                        // 弹窗窗口完成首帧布局后再播放展开动画：
                        // 否则重内容菜单首帧渲染慢，动画播放完才显示第一帧，
                        // 看起来就像“没有动画”
                        if (isMenuExpanded && !menuOpenAnimationStarted) {
                            menuOpenAnimationStarted = true
                            scope.launch {
                                menuAnimatable.snapTo(0f)
                                menuAnimatable.animateTo(
                                    1f,
                                    animationSpec = tween(durationMillis = 150),
                                )
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
            ) {
                if (expandedCustomMenuContent != null) {
                    // 自定义菜单内容（如搜索筛选的多段分组菜单），由内容自行组织分组与分割
                    expandedCustomMenuContent({ expandedMenu = null })
                } else {
                    DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                        lastMenuChildren.orEmpty().forEachIndexed { index, child ->
                            DropdownMenuItem(
                                selected = child.key == lastAnchorMenu?.checkedKey,
                                onClick = {
                                    expandedMenu = null
                                    onActionClick(child)
                                },
                                text = {
                                    Text(text = child.title)
                                },
                                shapes = MenuDefaults.itemShape(
                                    index = index,
                                    count = lastMenuChildren.orEmpty().size,
                                ),
                                // 选中的菜单项显示勾选标记（参照 KernelSU manager，GPL-3.0）
                                selectedLeadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(MenuDefaults.LeadingIconSize),
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

/**
 * 下拉菜单定位：水平居中于所属按钮（中轴线对齐），垂直在按钮正上方；
 * 超出屏幕时向内收拢（不超出屏幕的前提下尽量居中）。
 * 左右两侧保留与 [verticalSpacingPx]（菜单到底栏间距）同等的边距，
 * 避免边缘按钮展开的菜单紧贴屏幕边缘。
 */
private enum class MenuPopupPlacement {
    Above,
    Right,
}

private class MenuPopupPositionProvider(
    private val anchorBoundsMap: Map<MenuItemData, IntRect>,
    private val expandedMenu: MenuItemData?,
    private val verticalSpacingPx: Int,
    private val contentHeightVersion: Int,
    private val placement: MenuPopupPlacement = MenuPopupPlacement.Above,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchor = expandedMenu?.let { anchorBoundsMap[it] } ?: return IntOffset.Zero
        // 左右屏幕边缘与菜单到底栏间距保持一致的边距
        val horizontalMarginPx = verticalSpacingPx
        val maxX = (windowSize.width - popupContentSize.width - horizontalMarginPx * 2)
            .coerceAtLeast(0)
        val (x, y) = when (placement) {
            MenuPopupPlacement.Above -> {
                // 菜单在锚点正上方、水平居中（手机横排底栏）
                val px = anchor.center.x - popupContentSize.width / 2
                val py = (anchor.top - popupContentSize.height - verticalSpacingPx)
                    .coerceAtLeast(0)
                px to py
            }
            MenuPopupPlacement.Right -> {
                // 平板竖排：菜单放在竖栏右侧，垂直居中于按钮，与竖栏保持正常边距
                val px = (anchor.right + verticalSpacingPx)
                    .coerceAtMost(
                        (windowSize.width - popupContentSize.width - horizontalMarginPx)
                            .coerceAtLeast(0),
                    )
                val py = (anchor.center.y - popupContentSize.height / 2)
                    .coerceIn(
                        0,
                        (windowSize.height - popupContentSize.height).coerceAtLeast(0),
                    )
                px to py
            }
        }
        return IntOffset(
            x = x.coerceIn(horizontalMarginPx, horizontalMarginPx + maxX),
            y = y,
        )
    }
}
