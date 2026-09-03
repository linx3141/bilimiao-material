@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.components.image

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
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
import cn.a10miaomiao.bilimiao.compose.common.fetchOriginalImageBytes
import cn.a10miaomiao.bilimiao.compose.common.getImageFileName
import cn.a10miaomiao.bilimiao.compose.common.saveImageBytes
import cn.a10miaomiao.bilimiao.compose.components.image.previewer.ImagePreviewer
import cn.a10miaomiao.bilimiao.compose.components.image.previewer.defaultPreviewBackground
import cn.a10miaomiao.bilimiao.compose.components.image.provider.ImagePreviewerState
import cn.a10miaomiao.bilimiao.compose.components.zoomable.previewer.TransformLayerScope
import cn.a10miaomiao.bilimiao.compose.platform.LocalPlatformContext
import cn.a10miaomiao.bilimiao.compose.platform.PlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.AsyncImagePainter
import com.a10miaomiao.bilimiao.comm.utils.MiaoLogger
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import cn.a10miaomiao.bilimiao.compose.components.dialogs.FullScreenDialogProperties

private class MyImagePreviewerController(
    val coroutineScope: CoroutineScope,
    val platformContext: PlatformContext,
    val imagePreviewerState: ImagePreviewerState,
) {
    val isDownloading = mutableStateOf(false)

    private fun getCurrentImageUrl(): String {
        val page = imagePreviewerState.previewerState.currentPage
        val model = imagePreviewerState.imageModels[page]
        return model.originalUrl
    }

    fun saveImageFile() {
        val imageUrl = getCurrentImageUrl()
        isDownloading.value = true
        coroutineScope.launch(Dispatchers.Default) {
            try {
                val fileName = getImageFileName(imageUrl)
                val bytes = fetchOriginalImageBytes(imageUrl)
                if (bytes != null && bytes.isNotEmpty()) {
                    saveImageBytes(fileName, bytes)
                } else {
                    GlobalToaster.show("原图下载失败")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalToaster.show("原图下载失败")
            } finally {
                isDownloading.value = false
            }
        }
    }

    fun copyImageUrl() {
        val imageUrl = getCurrentImageUrl()
        platformContext.copyToClipboard(imageUrl)
        GlobalToaster.show("图片链接已复制到剪切板")
    }

    fun cancelDownloading() {
        isDownloading.value = false
    }
}

@Composable
fun MyImagePreviewer(
    imagePreviewerState: ImagePreviewerState,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    MiaoLogger("ImagePreviewer").d(
        "open" to "models=${imagePreviewerState.imageModels.size}",
    )
    val coroutineScope = rememberCoroutineScope()
    val platformContext = LocalPlatformContext.current
    val controller = remember(imagePreviewerState) {
        MyImagePreviewerController(coroutineScope, platformContext, imagePreviewerState)
    }
    var showMoreMenu by remember { mutableStateOf(false) }

    ImagePreviewer(
        contentPadding = contentPadding,
        state = imagePreviewerState.previewerState,
        imageLoader = { page ->
            val model = imagePreviewerState.imageModels[page]
            // 加载原图（高清）；状态读取已修复，原图加载正常
            val imageUrl = model.originalUrl
            MiaoLogger("ImagePreviewer").d("loader page=$page" to imageUrl.take(120))
            val painterState = remember { mutableStateOf<Painter?>(null) }
            var loadingLogged by remember { mutableStateOf(false) }
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
            ) {
                // Coil3 的 painter.state 是 StateFlow，需取 .value 才是加载状态
                when (val state = painter.state.value) {
                    is AsyncImagePainter.State.Success -> {
                        loadingLogged = false
                        painterState.value = painter
                        imagePreviewerState.onImageLoaded(page)
                        MiaoLogger("ImagePreviewer").d("loaded" to imageUrl.take(120))
                    }
                    is AsyncImagePainter.State.Error -> {
                        MiaoLogger("ImagePreviewer").d(
                            "error" to (state.result.throwable?.message ?: "unknown"),
                            "url" to imageUrl.take(120),
                        )
                    }
                    is AsyncImagePainter.State.Loading -> {
                        if (!loadingLogged) {
                            loadingLogged = true
                            MiaoLogger("ImagePreviewer").d("loading" to imageUrl.take(120))
                        }
                    }
                    else -> {}
                }
            }
            return@ImagePreviewer Pair(
                painterState.value,
                Size(model.width, model.height)
            )
        },
        previewerLayer = TransformLayerScope(
            background = defaultPreviewBackground,
            foreground = {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(
                            WindowInsets.safeDrawing
                                .only(WindowInsetsSides.Bottom + WindowInsetsSides.End)
                                .asPaddingValues()
                        )
                        .padding(bottom = 40.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CapsuleIconButton(
                            icon = Icons.Default.Download,
                            contentDescription = "保存图片",
                            onClick = controller::saveImageFile,
                        )
                        Box {
                            var moreAnchorBounds by remember { mutableStateOf(IntRect.Zero) }
                            CapsuleIconButton(
                                icon = Icons.Default.MoreVert,
                                contentDescription = "更多",
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    val rect = coords.boundsInWindow()
                                    moreAnchorBounds = IntRect(
                                        left = rect.left.roundToInt(),
                                        top = rect.top.roundToInt(),
                                        right = rect.right.roundToInt(),
                                        bottom = rect.bottom.roundToInt(),
                                    )
                                },
                            )
                            // m3e 下拉菜单（与视频详情页底栏更多菜单同款）
                            var menuPopupVisible by remember { mutableStateOf(false) }
                            val menuAnimatable = remember { Animatable(0f) }
                            LaunchedEffect(showMoreMenu) {
                                if (showMoreMenu) {
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
                            if (menuPopupVisible) {
                                val menuSpacingPx =
                                    with(LocalDensity.current) { 8.dp.toPx().roundToInt() }
                                Popup(
                                    onDismissRequest = { showMoreMenu = false },
                                    popupPositionProvider = MoreMenuPopupPositionProvider(
                                        anchorBounds = moreAnchorBounds,
                                        verticalSpacingPx = menuSpacingPx,
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
                                                transformOrigin = TransformOrigin(0.5f, 1f)
                                            },
                                    ) {
                                        DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                                            // 独立菜单项使用大圆角（与单条分段卡片一致）
                                            val itemShapes = MenuDefaults.itemShape(
                                                index = 0,
                                                count = 1,
                                            )
                                            val largeShape = MaterialTheme.shapes.large
                                            DropdownMenuItem(
                                                selected = false,
                                                modifier = Modifier.clip(largeShape),
                                                shapes = itemShapes.copy(
                                                    shape = largeShape,
                                                    selectedShape = largeShape,
                                                ),
                                                onClick = {
                                                    showMoreMenu = false
                                                    controller.copyImageUrl()
                                                },
                                                text = {
                                                    Text(text = "复制图片链接")
                                                },
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
            },
        ),
    )
    if (controller.isDownloading.value) {
        AlertDialog(
            onDismissRequest = controller::cancelDownloading,
            confirmButton = {
                TextButton(onClick = controller::cancelDownloading) {
                    Text("取消")
                }
            },
            title = {
                Text("正在下载图片")
            },
            text = {
                LinearProgressIndicator()
            },
            properties = FullScreenDialogProperties,
        )
    }
}

@Composable
private fun CapsuleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * 更多菜单定位：水平居中于所属按钮（中轴线对齐），垂直在按钮正上方；
 * 左右两侧保留与菜单到底栏间距同等的边距，避免边缘按钮展开的菜单紧贴屏幕边缘。
 * 与底栏更多菜单（M3EBottomBar.MenuPopupPositionProvider）行为一致。
 */
private class MoreMenuPopupPositionProvider(
    private val anchorBounds: IntRect,
    private val verticalSpacingPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchor = this.anchorBounds
        val x = anchor.center.x - popupContentSize.width / 2
        val y = (anchor.top - popupContentSize.height - verticalSpacingPx)
            .coerceAtLeast(0)
        val horizontalMarginPx = verticalSpacingPx
        val maxX = (windowSize.width - popupContentSize.width - horizontalMarginPx * 2)
            .coerceAtLeast(0)
        return IntOffset(
            x = x.coerceIn(horizontalMarginPx, horizontalMarginPx + maxX),
            y = y,
        )
    }
}
