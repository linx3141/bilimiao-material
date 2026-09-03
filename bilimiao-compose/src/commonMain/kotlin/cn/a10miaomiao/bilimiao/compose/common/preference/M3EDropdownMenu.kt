@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.common.preference

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
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
 * Material 3 Expressive 下拉选择菜单（设置项弹出）。
 *
 * 与 ThemeSettingPage 的 ExpressiveDropdownPreference / 底栏下拉菜单同一套实现：
 * Popup + DropdownMenuGroup 连体圆角、选中项勾选标记、展开/收起缩放淡入动画、
 * 菜单在设置项正上方弹出并靠屏幕右侧（保持 8dp 边距）。
 */
@Composable
fun <T> M3EDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    anchorBounds: IntRect,
    values: List<T>,
    selected: T,
    valueToText: (T) -> AnnotatedString,
    onSelected: (T) -> Unit,
) {
    var menuPopupVisible by remember { mutableStateOf(false) }
    val menuAnimatable = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var menuContentHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(expanded) {
        if (expanded) {
            menuPopupVisible = true
        } else {
            menuAnimatable.animateTo(0f, animationSpec = tween(durationMillis = 150))
            menuPopupVisible = false
        }
    }

    if (menuPopupVisible) {
        val spacingPx = with(LocalDensity.current) { 8.dp.toPx().roundToInt() }
        Popup(
            onDismissRequest = onDismiss,
            popupPositionProvider = DropdownPreferencePositionProvider(
                anchorBounds = anchorBounds,
                verticalSpacingPx = spacingPx,
                contentHeightPx = menuContentHeightPx,
            ),
            properties = PopupProperties(focusable = true),
        ) {
            // 菜单宽度由内容（最长文字）决定，不占满屏幕；
            // 展开动画延迟到弹窗内容首帧布局后触发
            var menuOpenAnimationStarted by remember(expanded) {
                mutableStateOf(false)
            }
            val scale = 0.8f + 0.2f * menuAnimatable.value
            val alpha = menuAnimatable.value
            Box(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .onSizeChanged { menuContentHeightPx = it.height }
                    .onGloballyPositioned {
                        if (expanded && !menuOpenAnimationStarted) {
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
                        // 向上弹出：从底部缩放展开（菜单从设置项上方长出来）
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
            ) {
                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                    values.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            selected = item == selected,
                            onClick = {
                                onDismiss()
                                onSelected(item)
                            },
                            text = { Text(valueToText(item)) },
                            shapes = MenuDefaults.itemShape(index, values.size),
                            selectedLeadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
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

/**
 * 下拉选择菜单定位：在设置项正上方弹出（向上展开）；
 * 上方空间不足时收拢到顶部边距内，水平靠屏幕右侧。
 */
internal class DropdownPreferencePositionProvider(
    private val anchorBounds: IntRect,
    private val verticalSpacingPx: Int,
    private val contentHeightPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchor = this.anchorBounds
        val horizontalMarginPx = verticalSpacingPx
        // 菜单靠屏幕右侧弹出，与屏幕右边缘保持与底栏下拉菜单同等的边距
        val x = (windowSize.width - popupContentSize.width - horizontalMarginPx)
            .coerceAtLeast(horizontalMarginPx)
        // 向上弹出：菜单底边紧贴设置项上方
        val y = (anchor.top - popupContentSize.height - verticalSpacingPx)
            .coerceAtLeast(horizontalMarginPx)
        return IntOffset(x = x, y = y)
    }
}
