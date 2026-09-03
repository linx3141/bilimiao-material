@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.video.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import cn.a10miaomiao.bilimiao.compose.components.dialogs.AutoSheetDialog
import cn.a10miaomiao.bilimiao.compose.common.getImageFileName
import cn.a10miaomiao.bilimiao.compose.common.saveImageBytes
import coil3.compose.AsyncImage
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 封面预览弹窗：与下载/收藏/投币弹窗同款样式。
 * 展示封面图、标题，底部为"保存封面"按钮与 m3e 更多菜单。
 */
@Stable
class CoverImageDialogState(
    val scope: CoroutineScope,
) {

    private val _visible = mutableStateOf(false)
    val visible: Boolean get() = _visible.value

    private val _aid = mutableStateOf("")
    val aid: String get() = _aid.value

    private val _bvid = mutableStateOf("")
    val bvid: String get() = _bvid.value

    private val _title = mutableStateOf("")
    val title: String get() = _title.value

    private val _coverUrl = mutableStateOf("")
    val coverUrl: String get() = _coverUrl.value

    private val _saving = mutableStateOf(false)
    val saving: Boolean get() = _saving.value

    val snackbar = SnackbarHostState()

    var copyToClipboard: (String) -> Unit = {}

    var openMore: (() -> Unit)? = null

    fun show(
        aid: String,
        bvid: String,
        title: String,
        coverUrl: String,
    ) {
        _aid.value = aid
        _bvid.value = bvid
        _title.value = title
        _coverUrl.value = coverUrl
        _visible.value = true
    }

    fun saveCover() {
        if (coverUrl.isBlank() || saving) return
        _saving.value = true
        scope.launch(Dispatchers.Default) {
            try {
                val fileName = getImageFileName(coverUrl)
                val bytes = MiaoHttp(coverUrl).get().body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    saveImageBytes(fileName, bytes)
                } else {
                    GlobalToaster.show("封面下载失败")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalToaster.show("保存失败：${e.message ?: e.toString()}")
            } finally {
                _saving.value = false
            }
        }
    }

    fun copyCoverUrl() {
        if (coverUrl.isBlank()) return
        copyToClipboard(coverUrl)
        GlobalToaster.show("图片链接已复制到剪切板")
    }

    fun dismiss() {
        _visible.value = false
    }
}

@Composable
fun CoverImageDialog(
    state: CoverImageDialogState,
) {
    if (state.visible) {
        var expandedMenu by remember { mutableStateOf(false) }
        AutoSheetDialog(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp),
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = "查看封面 · ${state.aid}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .fillMaxWidth(),
                    )
                    AsyncImage(
                        model = state.coverUrl,
                        contentDescription = "封面",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                            .fillMaxWidth(),
                    )
                    SnackbarHost(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        hostState = state.snackbar,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = state::saveCover,
                            enabled = !state.saving,
                        ) {
                            Row {
                                if (state.saving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(end = 5.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                                Text(text = "保存封面")
                            }
                        }
                        // 更多菜单：m3e 下拉菜单（与底栏/播放器菜单一致）
                        Box {
                            var menuAnchorBounds by remember {
                                mutableStateOf(IntRect.Zero)
                            }
                            var menuPopupVisible by remember {
                                mutableStateOf(false)
                            }
                            val menuAnimatable = remember { Animatable(0f) }
                            LaunchedEffect(expandedMenu) {
                                if (expandedMenu) {
                                    menuPopupVisible = true
                                    menuAnimatable.snapTo(0f)
                                    menuAnimatable.animateTo(
                                        1f,
                                        animationSpec = tween(durationMillis = 150),
                                    )
                                } else {
                                    menuAnimatable.animateTo(
                                        0f,
                                        animationSpec = tween(durationMillis = 150),
                                    )
                                    menuPopupVisible = false
                                }
                            }
                            IconButton(
                                onClick = { expandedMenu = true },
                                colors = IconButtonDefaults.iconButtonColors()
                                    .copy(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    val rect = coords.boundsInWindow()
                                    menuAnchorBounds = IntRect(
                                        left = rect.left.roundToInt(),
                                        top = rect.top.roundToInt(),
                                        right = rect.right.roundToInt(),
                                        bottom = rect.bottom.roundToInt(),
                                    )
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "更多",
                                )
                            }
                            if (menuPopupVisible) {
                                val spacingPx = with(LocalDensity.current) {
                                    8.dp.toPx().roundToInt()
                                }
                                Popup(
                                    onDismissRequest = { expandedMenu = false },
                                    popupPositionProvider = CoverMenuPositionProvider(
                                        anchorBounds = menuAnchorBounds,
                                        spacingPx = spacingPx,
                                    ),
                                    properties = PopupProperties(focusable = true),
                                ) {
                                    val scope = rememberCoroutineScope()
                                    var openAnimationStarted by remember {
                                        mutableStateOf(false)
                                    }
                                    val scale = 0.8f + 0.2f * menuAnimatable.value
                                    val alpha = menuAnimatable.value
                                    Box(
                                        modifier = Modifier
                                            .width(IntrinsicSize.Max)
                                            .onGloballyPositioned {
                                                if (!openAnimationStarted) {
                                                    openAnimationStarted = true
                                                    scope.launch {
                                                        menuAnimatable.snapTo(0f)
                                                        menuAnimatable.animateTo(
                                                            1f,
                                                            animationSpec = tween(
                                                                durationMillis = 150,
                                                            ),
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
                                        DropdownMenuGroup(
                                            shapes = MenuDefaults.groupShapes(),
                                        ) {
                                            val items = listOf(
                                                Triple(
                                                    "自定义文件名",
                                                    false,
                                                    false,
                                                ),
                                                Triple(
                                                    "复制封面链接",
                                                    false,
                                                    true,
                                                ),
                                                Triple(
                                                    "查看更多",
                                                    false,
                                                    true,
                                                ),
                                            )
                                            items.forEachIndexed { index, (title, selected, enabled) ->
                                                DropdownMenuItem(
                                                    selected = selected,
                                                    enabled = enabled,
                                                    onClick = {
                                                        expandedMenu = false
                                                        when (title) {
                                                            "复制封面链接" -> state.copyCoverUrl()
                                                            "查看更多" -> state.openMore?.invoke()
                                                        }
                                                    },
                                                    text = {
                                                        Text(text = title)
                                                    },
                                                    shapes = MenuDefaults.itemShape(
                                                        index = index,
                                                        count = items.size,
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
            },
            onDismiss = state::dismiss,
        )
    }
}

/**
 * 封面菜单定位：菜单在按钮正上方弹出，右对齐按钮右边缘，
 * 与屏幕边缘保留与菜单到底栏同等的边距。
 */
private class CoverMenuPositionProvider(
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
        val margin = spacingPx
        val x = (anchor.right - popupContentSize.width)
            .coerceIn(
                margin,
                (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin),
            )
        val y = (anchor.top - popupContentSize.height - spacingPx)
            .coerceAtLeast(margin)
        return IntOffset(x = x, y = y)
    }
}
