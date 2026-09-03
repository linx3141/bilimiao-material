@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.video.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import cn.a10miaomiao.bilimiao.compose.components.miao.MiaoTitleBar
import cn.a10miaomiao.bilimiao.compose.pages.community.MainReplyViewModel
import kotlin.math.roundToInt

@Composable
fun VideoReplyTitleBar(
    modifier: Modifier = Modifier,
    viewModel: MainReplyViewModel,
    count: Int = -1,
) {
    val sortOrder by viewModel.sortOrder.collectAsState()
    val expanded = remember {
        mutableStateOf(false)
    }
    MiaoTitleBar(
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "视频评论",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (count > 0) {
                    Text(
                        text = "($count)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        action = {
            IconButton(
                onClick = viewModel::openReplyDialog
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Comment,
                    contentDescription = "发布评论",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            var sortAnchorBounds by remember { mutableStateOf(IntRect.Zero) }
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
                        contentDescription = "列表排序",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (menuPopupVisible) {
                    val spacingPx = with(LocalDensity.current) { 8.dp.toPx().roundToInt() }
                    Popup(
                        onDismissRequest = { expanded.value = false },
                        popupPositionProvider = ReplySortMenuPositionProvider(
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
    )
}

/**
 * 排序下拉菜单定位：菜单在按钮正下方、右对齐按钮右边缘（顶栏右上角）。
 */
private class ReplySortMenuPositionProvider(
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
