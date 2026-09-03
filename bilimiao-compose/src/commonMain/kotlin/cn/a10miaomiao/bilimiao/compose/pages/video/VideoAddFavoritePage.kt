package cn.a10miaomiao.bilimiao.compose.pages.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.emitter.SharedFlowEmitter
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigator
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.pages.video.components.VideoFavoriteItem
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.media.MediaListInfo
import com.a10miaomiao.bilimiao.comm.entity.media.MediaResponseInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

@Serializable
data class VideoAddFavoritePage(
    val aid: String,
) : ComposePage {

    @Composable
    override fun Content() {
        val viewModel = diViewModel(key = "favorite-$aid") {
            VideoAddFavoriteViewModel(it, aid)
        }
        VideoAddFavoritePageContent(viewModel)
    }
}

class VideoAddFavoriteViewModel(
    override val di: DI,
    private val aid: String,
) : ViewModel(), DIAware {

    private val pageNavigator by instance<PageNavigator>()
    private val emitter by instance<SharedFlowEmitter>()

    var loading by mutableStateOf(false)
        private set
    var listLoading by mutableStateOf(false)
        private set
    var listFail by mutableStateOf("")
        private set

    val list = mutableStateListOf<MediaListInfo>()
    val selectedMap = mutableStateMapOf<String, Boolean>()
    val snackbar = SnackbarHostState()

    init {
        viewModelScope.launch {
            loadData()
        }
    }

    suspend fun loadData() {
        try {
            withContext(Dispatchers.Main) {
                listLoading = true
                listFail = ""
            }
            val res = BiliApiService.videoAPI
                .favoriteCreated(aid)
                .awaitCall()
                .json<ResponseData<MediaResponseInfo>>()
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    list.clear()
                    list.addAll(res.requireData().list)
                } else {
                    listFail = res.message
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                listFail = e.message ?: e.toString()
            }
        } finally {
            withContext(Dispatchers.Main) {
                listLoading = false
            }
        }
    }

    fun checkedChange(key: String, isChecked: Boolean) {
        selectedMap[key] = isChecked
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    loading = true
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
                        val state = when {
                            favIds.size - delIds.size + addIds.size == 0 -> 0
                            favIds.isEmpty() -> 1
                            else -> null
                        }
                        if (state != null) {
                            emitter.emit(EmitterAction.FavoriteChanged(state))
                        }
                        GlobalToaster.show("操作成功")
                        pageNavigator.popBackStack()
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
                    loading = false
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoAddFavoritePageContent(
    viewModel: VideoAddFavoriteViewModel,
) {
    PageConfig(title = "收藏")

    val windowInsets = localContentInsets()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = windowInsets.topDp.dp),
    ) {
        Text(
            text = "请选择收藏夹",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        )
        Box(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                itemsIndexed(viewModel.list, { _, item -> item.id }) { index, item ->
                    val isChecked = if (item.fav_state == 1) {
                        viewModel.selectedMap[item.id] ?: true
                    } else {
                        viewModel.selectedMap[item.id] ?: false
                    }
                    CompositionLocalProvider(
                        LocalListItemShapes provides segmentedItemShapes(
                            index,
                            viewModel.list.size,
                        ),
                    ) {
                        VideoFavoriteItem(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            title = item.title,
                            count = item.media_count,
                            checked = isChecked,
                            onCheckedChange = { viewModel.checkedChange(item.id, it) }
                        )
                    }
                }
            }
            if (viewModel.list.isEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (viewModel.listLoading) {
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
                    } else if (viewModel.listFail.isNotBlank()) {
                        Text(
                            viewModel.listFail,
                            modifier = Modifier.padding(start = 5.dp),
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            SnackbarHost(
                modifier = Modifier.align(Alignment.BottomCenter),
                hostState = viewModel.snackbar,
            )
        }
        Row(
            modifier = Modifier
                .padding(
                    vertical = 5.dp,
                    horizontal = 10.dp
                )
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = viewModel::confirmFavorite,
                enabled = !viewModel.loading
            ) {
                Row {
                    if (viewModel.loading) {
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
}
