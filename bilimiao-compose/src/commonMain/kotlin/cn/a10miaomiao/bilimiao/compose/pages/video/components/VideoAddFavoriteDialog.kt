@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.video.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.common.toColorInt
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.components.dialogs.AutoSheetDialog
import cn.a10miaomiao.bilimiao.compose.pages.community.ReplyEditParams
import cn.a10miaomiao.bilimiao.compose.pages.download.EpisodeItem
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.bangumi.EpisodeInfo
import com.a10miaomiao.bilimiao.comm.entity.media.MediaListInfo
import com.a10miaomiao.bilimiao.comm.entity.media.MediaResponseInfo
import com.a10miaomiao.bilimiao.comm.entity.player.PlayListFrom
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Stable
class VideoAddFavoriteDialogState(
    val scope: CoroutineScope,
    val onChanged: (Int) -> Unit,
) {

    var aid: String = ""
        private set

    private val _visible = mutableStateOf(false)
    val visible: Boolean get() = _visible.value

    private val _loading = mutableStateOf(false)
    val loading: Boolean get() = _loading.value

    private val _list = mutableStateListOf<MediaListInfo>()
    val list: List<MediaListInfo> get() = _list

    private val _listLoading = mutableStateOf(false)
    val listLoading: Boolean get() = _listLoading.value

    private val _listFail = mutableStateOf("")
    val listFail: String get() = _listFail.value

    private val _selectedMap = mutableStateMapOf<String, Boolean>()
    val selectedMap: Map<String, Boolean> get() = _selectedMap

    val snackbar = SnackbarHostState()

    suspend fun loadData() {
        try {
            withContext(Dispatchers.Main) {
                _listLoading.value = true
                _listFail.value = ""
            }
            val res = BiliApiService.videoAPI
                .favoriteCreated(aid)
                .awaitCall()
                .json<ResponseData<MediaResponseInfo>>()
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    _list.clear()
                    _list.addAll(res.requireData().list)
                } else {
                    _listFail.value = res.message
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                _listFail.value = e.message ?: e.toString()
            }
        } finally {
            withContext(Dispatchers.Main) {
                _listLoading.value = false
            }
        }
    }

    private fun requestFavorite(
        favIds: List<String>,
        addIds: List<String>,
        delIds: List<String>,
    ) = scope.launch(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                _loading.value = true
            }
            val res = BiliApiService.videoAPI
                .favoriteDeal(
                    aid = aid,
                    addIds = addIds,
                    delIds = delIds,
                )
                .awaitCall()
                .json<MessageInfo>()
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    // 按最终操作结果更新详情页收藏状态：
                    // 有新增收藏夹 -> 已收藏；仅移出收藏夹 -> 取消收藏
                    when {
                        addIds.isNotEmpty() -> onChanged(1)
                        delIds.isNotEmpty() -> onChanged(0)
                    }
                    aid = ""
                    GlobalToaster.show("操作成功")
                    dismiss()
                } else {
                    snackbar.showSnackbar(res.message)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                snackbar.showSnackbar(e.message ?: e.toString())
            }
        } finally {
            withContext(Dispatchers.Main) {
                _loading.value = false
            }
        }
    }

    fun confirmFavorite() {
        val favIds = list
            .filter { it.fav_state == 1 }
            .map { it.id }
        val addIds = list
            .filter { it.fav_state != 1 && (selectedMap[it.id] ?: false) }
            .map { it.id }
        val delIds = list
            .filter { it.fav_state == 1 && !(selectedMap[it.id] ?: true) }
            .map { it.id }
        if (addIds.isEmpty() && delIds.isEmpty()) {
            // 没有做任何更改：不发请求，直接关闭弹窗（空参数会触发接口"参数错误"）
            dismiss()
            return
        }
        requestFavorite(favIds, addIds, delIds)
    }

    fun show(videoAid: String) {
        if (videoAid != aid || list.isEmpty()) {
            scope.launch {
                loadData()
            }
            aid = videoAid
        }
        _visible.value = true
    }

    fun checkedChange(key: String, isChecked: Boolean) {
        _selectedMap[key] = isChecked
    }

    fun dismiss() {
        _visible.value = false
    }

}

@Composable
fun VideoFavoriteItem(
    title: String,
    count: Int,
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
            enabled = onCheckedChange != null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${count}个内容",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
        }
    }
}

@Composable
fun VideoAddFavoriteDialog(
    state: VideoAddFavoriteDialogState,
) {
    if (state.visible) {
        AutoSheetDialog(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp),
            content = {
                // 弹窗高度由列表内容决定；列表超高时内部滚动
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = "请选择收藏夹",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .fillMaxWidth()
                    )
                    if (state.list.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                        ) {
                            state.list.forEachIndexed { index, item ->
                                val isChecked = if (item.fav_state == 1) {
                                    state.selectedMap[item.id] ?: true
                                } else {
                                    state.selectedMap[item.id] ?: false
                                }
                                CompositionLocalProvider(
                                    LocalListItemShapes provides segmentedItemShapes(
                                        index,
                                        state.list.size,
                                    ),
                                ) {
                                    VideoFavoriteItem(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        title = item.title,
                                        count = item.media_count,
                                        checked = isChecked,
                                        onCheckedChange = {
                                            state.checkedChange(item.id, it)
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (state.listLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp,
                                )
                                Text(
                                    "加载中",
                                    modifier = Modifier.padding(start = 5.dp),
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 14.sp,
                                )
                            } else if (state.listFail.isNotBlank()) {
                                Text(
                                    state.listFail,
                                    modifier = Modifier.padding(start = 5.dp),
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 14.sp,
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
                            .padding(
                                vertical = 5.dp,
                                horizontal = 12.dp
                            )
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = state::confirmFavorite,
                            enabled = !state.loading
                        ) {
                            Row {
                                if (state.loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(end = 5.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                                Text(text = "完成")
                            }
                        }
                    }
                }
            },
            onDismiss = state::dismiss
        )
    }
}
