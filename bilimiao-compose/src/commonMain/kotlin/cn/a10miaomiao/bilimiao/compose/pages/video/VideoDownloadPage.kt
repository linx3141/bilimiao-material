@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bilibili.app.archive.v1.Arc
import bilibili.app.archive.v1.Page
import bilibili.app.view.v1.ViewGRPC
import bilibili.app.view.v1.ViewReq
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.download.DownloadManager
import cn.a10miaomiao.bilimiao.compose.common.download.entry.BiliDownloadEntryInfo
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigator
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.pages.video.components.VideoDownloadItem
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

@Serializable
data class VideoDownloadPage(
    val aid: String,
    val bvid: String,
) : ComposePage {

    @Composable
    override fun Content() {
        val viewModel = diViewModel(key = "download-$aid") {
            VideoDownloadViewModel(it, aid, bvid)
        }
        VideoDownloadPageContent(viewModel)
    }
}

class VideoDownloadViewModel(
    override val di: DI,
    private val aid: String,
    private val bvid: String,
) : ViewModel(), DIAware {

    private val pageNavigator by instance<PageNavigator>()
    private val downloadManager by instance<DownloadManager>()

    var loading by mutableStateOf(true)
        private set

    private var arcData: Arc? = null
    internal val pageList = mutableStateListOf<Page>()
    val checkedMap = mutableStateMapOf<Long, Int>()
    val checkedSize: Int get() = checkedMap.size

    internal val qualityList = mutableStateListOf<Pair<Int, String>>()
    var quality by mutableIntStateOf(0)
        private set
    val description: String
        get() = qualityList.find { it.first == quality }?.second ?: "未选择"

    val snackbar = SnackbarHostState()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadDetail()
        }
    }

    private suspend fun loadDetail() {
        try {
            val req = if (bvid.startsWith("BV")) {
                ViewReq(bvid = bvid)
            } else {
                ViewReq(aid = aid.toLong())
            }
            val res = BiliGRPCHttp.request {
                ViewGRPC.view(req)
            }.awaitCall()
            val arc = res.getArcData()
            val pages = res.getPages()
            withContext(Dispatchers.Main) {
                arcData = arc
                pageList.clear()
                pageList.addAll(pages)
                if (qualityList.isEmpty() && pages.isNotEmpty()) {
                    val videoAid = arc?.aid.toString()
                    getAcceptQuality(videoAid, pages[0].cid.toString())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar(e.message ?: e.toString())
        } finally {
            withContext(Dispatchers.Main) {
                loading = false
            }
        }
    }

    private fun getAcceptQuality(
        aid: String,
        cid: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val res = BiliApiService.playerAPI.getVideoPalyUrl(
                aid, cid, 64, fnval = 4048
            )
            val acceptDescription = res.accept_description
            val list = res.accept_quality.mapIndexed { index, q ->
                q to (acceptDescription.getOrNull(index) ?: q.toString())
            }
            withContext(Dispatchers.Main) {
                qualityList.clear()
                qualityList.addAll(list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkedChange(cid: Long, index: Int) {
        if (checkedMap.contains(cid)) {
            checkedMap.remove(cid)
        } else {
            checkedMap[cid] = index
        }
    }

    fun selectQuality(quality: Int) {
        this.quality = quality
    }

    private fun showSnackbar(message: String) {
        viewModelScope.launch {
            snackbar.showSnackbar(message)
        }
    }

    fun startDownload() {
        if (quality == 0) {
            showSnackbar("请选择画质")
            return
        }
        val videoArc = arcData
        if (videoArc == null) {
            showSnackbar("缺少视频信息")
            return
        }
        checkedMap.forEach { c ->
            var page = pageList.getOrNull(c.value)
            if (page?.cid != c.key) {
                page = pageList.find { it.cid == c.key }
            }
            if (page != null) {
                downloadVideo(
                    videoArc,
                    page,
                )
            }
        }
        GlobalToaster.show("成功创建${checkedSize}条记录")
        pageNavigator.popBackStack()
        checkedMap.clear()
    }

    private fun downloadVideo(
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
            bvid = bvid,
            owner_id = videoArc.author?.mid ?: 0L,
            page_data = pageData
        )
        downloadManager.createDownload(biliVideoEntry)
    }
}

private fun bilibili.app.view.v1.ViewReply.getArcData(): Arc? {
    return arc ?: activitySeason?.arc
}

private fun bilibili.app.view.v1.ViewReply.getPages(): List<Page> {
    return (activitySeason?.pages ?: pages).mapNotNull { it.page }
}

private fun bilibili.app.view.v1.ViewReply.getBvid(): String {
    return activitySeason?.bvid ?: bvid
}

@Composable
private fun VideoDownloadPageContent(
    viewModel: VideoDownloadViewModel,
) {
    PageConfig(title = "下载")

    val windowInsets = localContentInsets()
    var expandedQualityMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 页面打开时已由 ViewModel init 加载
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = windowInsets.topDp.dp),
    ) {
        Text(
            text = "请选择分P下载",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        )
        Box(
            modifier = Modifier.weight(1f)
        ) {
            if (viewModel.loading) {
                Text(
                    text = "加载中...",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    items(viewModel.pageList.size, { it }) { index ->
                        val item = viewModel.pageList[index]
                        val isChecked = viewModel.checkedMap.containsKey(item.cid)
                        CompositionLocalProvider(
                            LocalListItemShapes provides segmentedItemShapes(
                                index,
                                viewModel.pageList.size,
                            ),
                        ) {
                            VideoDownloadItem(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                page = item,
                                enabled = true,
                                checked = isChecked,
                                onCheckedChange = { viewModel.checkedChange(item.cid, index) }
                            )
                        }
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
                .padding(5.dp)
        ) {
            Box() {
                Button(
                    modifier = Modifier.padding(end = 5.dp),
                    onClick = { expandedQualityMenu = true },
                ) {
                    Text(text = "画质：" + viewModel.description)
                }
                DropdownMenu(
                    expanded = expandedQualityMenu,
                    onDismissRequest = { expandedQualityMenu = false },
                ) {
                    viewModel.qualityList.forEach {
                        DropdownMenuItem(
                            onClick = {
                                expandedQualityMenu = false
                                viewModel.selectQuality(it.first)
                            },
                            text = {
                                Text(text = it.second)
                            }
                        )
                    }
                }
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = viewModel::startDownload,
                enabled = viewModel.checkedMap.isNotEmpty(),
            ) {
                Text(text = "开始下载(${viewModel.checkedSize})")
            }
        }
    }
}
