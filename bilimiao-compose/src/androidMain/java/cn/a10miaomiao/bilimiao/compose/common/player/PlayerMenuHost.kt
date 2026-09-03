@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.common.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 播放器 m3e 下拉菜单宿主：渲染 [PlayerMenuState] 提交的菜单，
 * 实现与底栏下拉菜单一致（DropdownMenuGroup 连体圆角、选中项取色高亮 +
 * 勾选标记、展开/收起淡入缩放动画、子菜单切换）。
 */
@Composable
fun PlayerMenuHost(state: PlayerMenuState) {
    val request by state.request.collectAsState()
    var menuPopupVisible by remember { mutableStateOf(false) }
    val menuAnimatable = remember { Animatable(0f) }
    // 当前显示的菜单内容（点击"画面比例"等带子菜单的项时切换）
    var currentItems by remember { mutableStateOf(request?.items ?: emptyList()) }
    var currentAnchor by remember { mutableStateOf(request?.anchorBounds ?: IntRect.Zero) }
    // 菜单内容变化（如切换子菜单）时触发重新定位
    var contentHeightVersion by remember { mutableStateOf(0) }
    val windowHeight = LocalWindowInfo.current.containerSize.height

    LaunchedEffect(request) {
        val currentRequest = request
        if (currentRequest != null) {
            currentItems = currentRequest.items
            currentAnchor = currentRequest.anchorBounds
            contentHeightVersion++
            menuPopupVisible = true
            menuAnimatable.snapTo(0f)
        } else {
            menuAnimatable.animateTo(0f, animationSpec = tween(durationMillis = 150))
            menuPopupVisible = false
        }
    }

    if (menuPopupVisible) {
        val spacingPx = with(LocalDensity.current) { 8.dp.toPx().roundToInt() }
        // 锚点位于窗口上半部分时菜单在下方弹出，否则在上方弹出
        val placement = if (currentAnchor.top > windowHeight / 2) {
            PlayerMenuPlacement.Above
        } else {
            PlayerMenuPlacement.Below
        }
        Popup(
            onDismissRequest = { state.dismiss() },
            popupPositionProvider = PlayerMenuPositionProvider(
                anchorBounds = currentAnchor,
                verticalSpacingPx = spacingPx,
                contentHeightVersion = contentHeightVersion,
                placement = placement,
            ),
            properties = PopupProperties(focusable = true),
        ) {
            val scope = rememberCoroutineScope()
            var openAnimationStarted by remember { mutableStateOf(false) }
            val scale = 0.8f + 0.2f * menuAnimatable.value
            val alpha = menuAnimatable.value
            Box(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .onSizeChanged { contentHeightVersion = it.height }
                    .onGloballyPositioned {
                        // 弹窗窗口完成首帧布局后再播放展开动画（与底栏下拉菜单一致）
                        if (!openAnimationStarted) {
                            openAnimationStarted = true
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
                        transformOrigin = if (placement == PlayerMenuPlacement.Above) {
                            TransformOrigin(0.5f, 1f)
                        } else {
                            TransformOrigin(0.5f, 0f)
                        }
                    },
            ) {
                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                    currentItems.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            selected = item.selected,
                            enabled = item.enabled,
                            onClick = {
                                val children = item.children
                                if (children != null) {
                                    // 切换到子菜单（如"画面比例"），保持弹窗打开并重新定位
                                    currentItems = children
                                    contentHeightVersion++
                                } else {
                                    item.onClick?.invoke()
                                    state.dismiss()
                                }
                            },
                            text = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            leadingIcon = item.iconRes?.let { res ->
                                {
                                    Icon(
                                        painter = painterResource(res),
                                        contentDescription = null,
                                    )
                                }
                            },
                            shapes = MenuDefaults.itemShape(
                                index = index,
                                count = currentItems.size,
                            ),
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

private enum class PlayerMenuPlacement {
    Above,
    Below,
}

/**
 * 播放器菜单定位：水平居中于锚点按钮，垂直在按钮上方或下方弹出；
 * 左右与屏幕边缘保留与菜单到底栏同等的边距。
 */
private class PlayerMenuPositionProvider(
    private val anchorBounds: IntRect,
    private val verticalSpacingPx: Int,
    private val contentHeightVersion: Int,
    private val placement: PlayerMenuPlacement,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchor = this.anchorBounds
        val margin = verticalSpacingPx
        val x = (anchor.left + anchor.width / 2 - popupContentSize.width / 2)
            .coerceIn(
                margin,
                (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin),
            )
        val y = if (placement == PlayerMenuPlacement.Above) {
            (anchor.top - popupContentSize.height - verticalSpacingPx)
                .coerceAtLeast(margin)
        } else {
            (anchor.bottom + verticalSpacingPx)
                .coerceAtMost(
                    (windowSize.height - popupContentSize.height - margin)
                        .coerceAtLeast(margin),
                )
        }
        return IntOffset(x = x, y = y)
    }
}
