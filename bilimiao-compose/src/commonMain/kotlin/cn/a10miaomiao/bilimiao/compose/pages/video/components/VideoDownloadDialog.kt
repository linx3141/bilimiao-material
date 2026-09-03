@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.video.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import cn.a10miaomiao.bilimiao.compose.common.toColorInt
import androidx.lifecycle.viewModelScope
import bilibili.app.archive.v1.Arc
import bilibili.app.archive.v1.Page
import cn.a10miaomiao.bilimiao.compose.components.dialogs.AutoSheetDialog
import cn.a10miaomiao.bilimiao.compose.common.download.DownloadManager
import cn.a10miaomiao.bilimiao.compose.common.download.entry.BiliDownloadEntryInfo
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Stable
class VideoDownloadDialogState(
    val scope: CoroutineScope,
) {
    private var downloadManager: DownloadManager? = null
    private var videoBvid = ""

    private val _visible = mutableStateOf(false)
    val visible: Boolean get() = _visible.value

    private val _loading = mutableStateOf(false)
    val loading: Boolean get() = _loading.value

    private val _list = mutableStateOf(listOf<Page>())
    val list: List<Page> get() = _list.value

    private val _arcData = MutableStateFlow<Arc?>(null)
    val arcData get() = _arcData.value

    private val _checkedMap = mutableStateMapOf<Long,Int>() // 已选中
    val checkedMap: Map<Long,Int> get() = _checkedMap
    val checkedSize: Int get() = _checkedMap.size

    private val _downloadedSet = mutableStateOf(setOf<Long>()) // 已下载
    val downloadedSet: Set<Long> get() = _downloadedSet.value

    private val _qualityList = mutableStateOf(listOf<Pair<Int, String>>()) // Quality: Description
    val qualityList: List<Pair<Int, String>> get() = _qualityList.value

    private val _quality = mutableIntStateOf(0)
    val quality get () = _quality.intValue
    val description get() = qualityList.find { it.first == quality }?.second ?: "未选择"

    val snackbar = SnackbarHostState()

    fun show(
        manager: DownloadManager,
        bvid: String,
        videoArc: Arc,
        videoPages: List<Page>,
    ) {
        downloadManager = manager
        _visible.value = true
        _list.value = videoPages
        _arcData.value = videoArc
        _downloadedSet.value = getDownloadedList(
            manager,
            videoPages.map { it.cid }.toSet()
        )
        videoBvid = bvid
        if (qualityList.isEmpty() && videoPages.isNotEmpty()) {
            val videoAid = videoArc.aid.toString()
            getAcceptQuality(videoAid, videoPages[0].cid.toString())
        }
    }

    private fun getDownloadedList(
        manager: DownloadManager,
        cidSet: Set<Long>,
    ): Set<Long> {
        return manager
            .downloadList
            .mapNotNull { it.entry.source?.cid ?: it.entry.page_data?.cid }
            .filter { cidSet.contains(it) }
            .toSet()
    }

    private fun getAcceptQuality(
        aid: String,
        cid: String,
    ) = scope.launch(Dispatchers.IO) {
        try {
            val res = BiliApiService.playerAPI.getVideoPalyUrl(
                aid, cid, 64, fnval = 4048
            )
            val acceptDescription = res.accept_description
            _qualityList.value = res.accept_quality.mapIndexed { index, q ->
                q to (acceptDescription.getOrNull(index) ?: q.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkedChange(cid: Long, index: Int) {
        if (checkedMap.contains(cid)) {
            _checkedMap.remove(cid)
        } else {
            _checkedMap[cid] = index
        }
    }

    fun setQuality(quality: Int) {
        _quality.intValue = quality
    }

    private fun showSnackbar(message: String) {
        scope.launch {
            snackbar.showSnackbar(message)
        }
    }

    fun startDownload() {
        if (quality == 0) {
            showSnackbar("请选择画质")
            return
        }
        val manager = downloadManager
        if (manager == null) {
            showSnackbar("下载服务异常")
            return
        }
        val videoArc = arcData
        if (videoArc == null) {
            showSnackbar("缺少视频信息")
            return
        }
        checkedMap.forEach { c ->
            var page = list.getOrNull(c.value)
            if (page?.cid != c.key) {
                page = list.find { it.cid == c.key }
            }
            if (page != null) {
                downloadVideo(
                    manager,
                    videoArc,
                    page,
                )
            }
        }
        GlobalToaster.show("成功创建${checkedSize}条记录")
        dismiss()
        _checkedMap.clear()
    }

    private fun downloadVideo(
        manager: DownloadManager,
        videoArc: Arc,
        page: Page,
    ) {
        val pageData = BiliDownloadEntryInfo.PageInfo(
            cid = page.cid,
            page = page.page,
            from = page.from,
            part = page.part,
            vid = page.vid,
            has_alias = false,
            tid = 0,
            width = 0,
            height = 0,
            rotate = 0,
            download_title = "视频已缓存完成",
            download_subtitle = videoArc.title
        )
        val currentTime = System.currentTimeMillis()
        val biliVideoEntry = BiliDownloadEntryInfo(
            media_type = 2,
            has_dash_audio = true,
            is_completed = false,
            total_bytes = 0,
            downloaded_bytes = 0,
            title = videoArc.title,
            type_tag = quality.toString(),
            cover = videoArc.pic,
            prefered_video_quality = quality,
            quality_pithy_description = description,
            guessed_total_bytes = 0,
            total_time_milli = 0,
            danmaku_count = 1000,
            time_update_stamp = currentTime,
            time_create_stamp = currentTime,
            can_play_in_advance = true,
            interrupt_transform_temp_file = false,
            avid = videoArc.aid,
            spid = 0,
            season_id = null,
            ep = null,
            source = null,
            bvid = videoBvid,
            owner_id = videoArc.author?.mid ?: 0L,
            page_data = pageData
        )
        manager.createDownload(biliVideoEntry)
    }

    fun dismiss() {
        _visible.value = false
    }
}

@Composable
fun VideoDownloadItem(
    page: Page,
    enabled: Boolean,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val segmentedShapes = LocalListItemShapes.current
    Box(
        modifier = if (segmentedShapes == null) {
            modifier.padding(
                vertical = 5.dp,
                horizontal = 10.dp,
            )
        } else {
            modifier
        },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = segmentedShapes?.shape ?: RoundedCornerShape(10.dp),
            color = if (segmentedShapes != null) {
                MaterialTheme.colorScheme.surfaceBright
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            onClick = { onCheckedChange?.invoke(!checked) },
            enabled = enabled && onCheckedChange != null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = page.part,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row {
                        Text(
                            text = "P${page.page}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = NumberUtil.converDuration(page.duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Checkbox(
                    enabled = enabled,
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
        }
    }
}

@Composable
fun VideoDownloadDialog(
    state: VideoDownloadDialogState,
) {
    if (state.visible) {
        var expandedQualityMenu by remember {
            mutableStateOf(false)
        }
        AutoSheetDialog(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp),
            content = {
                // 弹窗高度由分P列表内容决定；列表超高时内部滚动
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = "请选择分P下载",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .fillMaxWidth()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                    ) {
                        state.list.forEachIndexed { index, item ->
                            val isEnabled = !state.downloadedSet.contains(item.cid)
                            val isChecked = if (isEnabled) {
                                state.checkedMap.containsKey(item.cid)
                            } else {
                                true
                            }
                            CompositionLocalProvider(
                                LocalListItemShapes provides segmentedItemShapes(
                                    index,
                                    state.list.size,
                                ),
                            ) {
                                VideoDownloadItem(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    page = item,
                                    enabled = isEnabled,
                                    checked = isChecked,
                                    onCheckedChange = {
                                        state.checkedChange(item.cid, index)
                                    },
                                )
                            }
                        }
                    }
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
                        Box {
                            var qualityAnchorBounds by remember {
                                mutableStateOf(IntRect.Zero)
                            }
                            // 菜单 Popup 显示状态与动画（与底栏/评论排序下拉菜单一致）
                            var qualityMenuPopupVisible by remember {
                                mutableStateOf(false)
                            }
                            val qualityMenuAnimatable = remember { Animatable(0f) }
                            LaunchedEffect(expandedQualityMenu) {
                                if (expandedQualityMenu) {
                                    qualityMenuPopupVisible = true
                                    qualityMenuAnimatable.snapTo(0f)
                                    qualityMenuAnimatable.animateTo(
                                        1f,
                                        animationSpec = tween(durationMillis = 150),
                                    )
                                } else {
                                    qualityMenuAnimatable.animateTo(
                                        0f,
                                        animationSpec = tween(durationMillis = 150),
                                    )
                                    qualityMenuPopupVisible = false
                                }
                            }
                            Button(
                                onClick = { expandedQualityMenu = true },
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    val rect = coords.boundsInWindow()
                                    qualityAnchorBounds = IntRect(
                                        left = rect.left.roundToInt(),
                                        top = rect.top.roundToInt(),
                                        right = rect.right.roundToInt(),
                                        bottom = rect.bottom.roundToInt(),
                                    )
                                },
                            ) {
                                Text(text = "画质：" + state.description)
                            }
                            if (qualityMenuPopupVisible) {
                                val spacingPx = with(LocalDensity.current) {
                                    8.dp.toPx().roundToInt()
                                }
                                Popup(
                                    onDismissRequest = { expandedQualityMenu = false },
                                    popupPositionProvider = QualityMenuPositionProvider(
                                        anchorBounds = qualityAnchorBounds,
                                        spacingPx = spacingPx,
                                    ),
                                    properties = PopupProperties(focusable = true),
                                ) {
                                    // 菜单宽度由内容决定，整体淡入 + 从按钮方向缩放展开
                                    val scale = 0.8f + 0.2f * qualityMenuAnimatable.value
                                    val alpha = qualityMenuAnimatable.value
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
                                            state.qualityList.forEachIndexed { index, item ->
                                                DropdownMenuItem(
                                                    selected = item.first == state.quality,
                                                    onClick = {
                                                        expandedQualityMenu = false
                                                        state.setQuality(item.first)
                                                    },
                                                    text = {
                                                        Text(text = item.second)
                                                    },
                                                    shapes = MenuDefaults.itemShape(
                                                        index = index,
                                                        count = state.qualityList.size,
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
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = state::startDownload,
                            enabled = state.checkedMap.isNotEmpty(),
                        ) {
                            Text(text = "开始下载(${state.checkedSize})")
                        }
                    }
                }
            },
            onDismiss = state::dismiss
        )
    }
}

/**
 * 画质下拉菜单定位：菜单在按钮正上方（弹窗底部按钮上方空间充足），
 * 左对齐按钮左边缘，与屏幕上下边缘保留与菜单到底栏同等的边距。
 */
private class QualityMenuPositionProvider(
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
        val x = anchor.left
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = (anchor.top - popupContentSize.height - spacingPx)
            .coerceAtLeast(spacingPx)
        return IntOffset(x = x, y = y)
    }
}
