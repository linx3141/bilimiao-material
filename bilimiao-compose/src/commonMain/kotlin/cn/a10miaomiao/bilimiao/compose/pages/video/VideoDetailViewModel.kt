package cn.a10miaomiao.bilimiao.compose.pages.video

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bilibili.app.archive.v1.Page
import bilibili.app.view.v1.ViewGRPC
import bilibili.app.view.v1.ViewReply
import bilibili.app.view.v1.ViewReq
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.emitter.SharedFlowEmitter
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.StartViewState
import cn.a10miaomiao.bilimiao.compose.pages.playlist.PlayListPage
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchResultPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSeasonDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSpacePage
import cn.a10miaomiao.bilimiao.compose.pages.video.components.CoverImageDialogState
import cn.a10miaomiao.bilimiao.compose.pages.video.components.VideoAddFavoriteDialogState
import cn.a10miaomiao.bilimiao.compose.pages.video.components.VideoCoinDialogState
import cn.a10miaomiao.bilimiao.compose.pages.video.components.VideoDownloadDialogState
import cn.a10miaomiao.bilimiao.compose.common.download.DownloadManager
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.datastore.mapPreferences
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerDelegate
import com.a10miaomiao.bilimiao.comm.delegate.player.VideoPlayerSource
import com.a10miaomiao.bilimiao.comm.delegate.player.createVideoPlayerSource
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.entity.player.PlayListFrom
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.FilterStore
import com.a10miaomiao.bilimiao.comm.store.PlayListStore
import com.a10miaomiao.bilimiao.comm.store.PlayerStore
import com.a10miaomiao.bilimiao.comm.store.UserLibraryStore
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import kotlinx.serialization.Serializable
import kotlin.collections.mapNotNull

class VideoDetailViewModel(
    override val di: DI,
    id: String,
    private val autoPlay: Boolean = false,
) : ViewModel(), DIAware {

    private val pageNavigation by instance<PageNavigation>()
    private val basePlayerDelegate by instance<BasePlayerDelegate>()

    var openUrl: (String) -> Unit = {}
    var copyToClipboard: (String) -> Unit = {}
    var shareText: (String) -> Unit = {}
    var openCoverImage: (String) -> Unit = {}

    private val filterStore: FilterStore by instance()
    private val playerStore: PlayerStore by instance()
    private val playListStore: PlayListStore by instance()
    private val userStore: UserStore by instance()
    private val userLibraryStore: UserLibraryStore by instance()
    private val emitter: SharedFlowEmitter by instance()
    private val downloadManager: DownloadManager by instance()
    private val startViewState: StartViewState by instance()

    /** 投币/收藏/下载弹窗状态（弹窗样式与发送评论弹窗一致） */
    val coinDialogState = VideoCoinDialogState(viewModelScope) {
        viewModelScope.launch { emitter.emit(EmitterAction.CoinChanged(it)) }
    }
    val favoriteDialogState = VideoAddFavoriteDialogState(viewModelScope) {
        viewModelScope.launch {
            emitter.emit(EmitterAction.FavoriteChanged(it))
        }
    }
    val downloadDialogState = VideoDownloadDialogState(viewModelScope)

    /** 封面预览弹窗状态（与下载/收藏/投币弹窗同款样式） */
    val coverDialogState = CoverImageDialogState(viewModelScope).also { state ->
        state.openMore = {
            if (state.bvid.isNotBlank()) {
                pageNavigation.navigate(VideoDetailPage(state.bvid))
            }
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> get() = _isRefreshing
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading
    private val _fail = MutableStateFlow<Any?>(null)
    val fail: StateFlow<Any?> get() = _fail
    private val _detailData = MutableStateFlow<ViewReply?>(null)
    val detailData: StateFlow<ViewReply?> get() = _detailData

    /** 是否充电专属视频（gRPC 接口的 rights 不带该标志，需配合 web 接口 is_upower_exclusive 判定） */
    val isChargeVideo = MutableStateFlow(false)

    // 自动连播合集（默认关闭）
    private val _isAutoPlaySeason = mutableStateOf(false)
    val isAutoPlaySeason get() = _isAutoPlaySeason.value

    // 此ViewModel启动播放的视频Aid
    private var videoAidToPlay = ""

    // 本页面注册到 StartViewState 的视频 aid，页面离开组合时按值注销
    private var registeredPageAid: String? = null

    // 强制自动播放一次（收藏夹自动连播点击进入详情页时使用）
    private var forceAutoPlayOnce = autoPlay

    private var _id = id

    init {
        loadData()
        viewModelScope.launch {
            val emitter by instance<SharedFlowEmitter>()
            emitter.collectAction<EmitterAction.CoinChanged> {
                updateCoinState(it.num)
            }
        }
        viewModelScope.launch {
            val emitter by instance<SharedFlowEmitter>()
            emitter.collectAction<EmitterAction.FavoriteChanged> {
                updateFavoriteState(it.state)
            }
        }
    }

    fun onBackPressed() {
        viewModelScope.launch(Dispatchers.Main) {
            closePlayerIfNeeded()
            runCatching {
                pageNavigation.popBackStack()
            }
        }
    }

    /**
     * 页面进入组合时重新注册当前视频页 aid。
     * 返回导航复用同一 ViewModel（不重新加载数据）时，
     * 页面离开组合会注销注册，这里在重新进入时补注册。
     */
    fun registerPage() {
        val pageAid = detailData.value?.getArcData()?.aid?.toString().orEmpty()
        if (pageAid.isNotEmpty()) {
            registeredPageAid = pageAid
            startViewState.currentVideoPageAid = pageAid
        }
    }

    /**
     * 页面退出（返回手势完成、页面离开组合）时调用：按设置关闭当前视频播放器。
     * 返回导航本身由导航层处理，以支持预测性返回手势动画。
     */
    fun onPageDispose() {
        // 只有当前注册值仍属于本页面时才清空，避免两层视频详情页切换时误删新页面的注册
        if (startViewState.currentVideoPageAid == registeredPageAid) {
            startViewState.currentVideoPageAid = null
        }
        viewModelScope.launch(Dispatchers.Main) {
            closePlayerIfNeeded()
        }
    }

    private suspend fun closePlayerIfNeeded() {
        if (basePlayerDelegate.isOpened()
            && basePlayerDelegate.getSourceIds().aid == videoAidToPlay) {
            val openMode = SettingPreferences.mapPreferences {
                it[SettingPreferences.PlayerOpenMode] ?: SettingConstants.PLAYER_OPEN_MODE_DEFAULT
            }
            if (openMode and SettingConstants.PLAYER_OPEN_MODE_AUTO_CLOSE != 0) {
                basePlayerDelegate.closePlayer()
            }
        }
    }

    fun changeVideo(id: String) {
        _id = id
        loadData()
    }

    fun loadData() = viewModelScope.launch {
        try {
            _loading.value = true
            _fail.value = null
            val req = if (_id.startsWith("BV")) {
                ViewReq(
                    bvid = _id,
                )
            } else {
                ViewReq(
                    aid = _id.toLong(),
                )
            }
            val res = BiliGRPCHttp.request {
                ViewGRPC.view(req)
            }.awaitCall()
            _detailData.value = res
            // 在自动播放前同步注册当前页面的视频 aid，
            // 避免"忽略返回手势"关闭时，详情页自动开播被误判为新页面而关掉播放器
            registerPage()
            // 诊断：输出 gRPC 详情接口解码出的充电相关字段，便于确认标识判定
            val chargeArc = res.getArcData()
            val rightsCharge = chargeArc?.rights?.let {
                it.ugcPay == 1 || it.arcPay == 1
            } == true
            isChargeVideo.value = rightsCharge
            miaoLogger() debug "充电标识-详情: aid=${chargeArc?.aid} " +
                "ugcPay=${chargeArc?.rights?.ugcPay} " +
                "arcPay=${chargeArc?.rights?.arcPay} " +
                "payFreeWatch=${chargeArc?.rights?.payFreeWatch}"
            if (!rightsCharge) {
                loadChargeFromWeb()
            }
            autoStartPlay()
        } catch (e: Exception) {
            e.printStackTrace()
            _fail.value = e
        } finally {
            _isRefreshing.value = false
            _loading.value = false
        }
    }

    /**
     * 与 PiliPlus 一致：gRPC 详情接口不带充电标识，
     * 改用 web 视图接口的 is_upower_exclusive（及 rights.ugc_pay/arc_pay）兜底判定。
     */
    private fun loadChargeFromWeb() = viewModelScope.launch {
        val bvid = _detailData.value?.getBvid() ?: return@launch
        try {
            val res = MiaoHttp.request {
                url = "https://api.bilibili.com/x/web-interface/view?bvid=$bvid"
            }.awaitCall().json<ChargeViewInfo>()
            val data = res.data
            val charge = data?.is_upower_exclusive == true
                || data?.rights?.ugc_pay == 1
                || data?.rights?.arc_pay == 1
            miaoLogger() debug "充电标识-详情web: bvid=$bvid " +
                "is_upower_exclusive=${data?.is_upower_exclusive} " +
                "ugc_pay=${data?.rights?.ugc_pay} arc_pay=${data?.rights?.arc_pay}"
            if (charge) {
                isChargeVideo.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Serializable
    private data class ChargeViewInfo(
        val code: Int = 0,
        val data: ChargeViewData? = null,
    ) {
        @Serializable
        data class ChargeViewData(
            val is_upower_exclusive: Boolean = false,
            val rights: ChargeRights? = null,
        )

        @Serializable
        data class ChargeRights(
            val ugc_pay: Int = 0,
            val arc_pay: Int = 0,
        )
    }

    private fun autoStartPlay() = viewModelScope.launch(Dispatchers.Main) {
        val arcData = detailData.value?.getArcData() ?: return@launch
        if (basePlayerDelegate.getSourceIds().aid == arcData.aid.toString()) {
            // 同个视频不替换播放
            return@launch
        }
        if (forceAutoPlayOnce) {
            // 收藏夹自动连播点击进入：不受全局"播放器自动控制"影响，直接开播一次
            forceAutoPlayOnce = false
            playVideo()
            return@launch
        }
        val openMode = SettingPreferences.mapPreferences {
            it[SettingPreferences.PlayerOpenMode] ?: SettingConstants.PLAYER_OPEN_MODE_DEFAULT
        }
        if (basePlayerDelegate.isOpened()) {
            if (basePlayerDelegate.isPlaying()) {
                // 自动替换正在播放的视频
                if (openMode and SettingConstants.PLAYER_OPEN_MODE_AUTO_REPLACE != 0) {
                    playVideo()
                }
            } else if (basePlayerDelegate.isPause()) {
                // 自动替换暂停的视频
                if (openMode and SettingConstants.PLAYER_OPEN_MODE_AUTO_REPLACE_PAUSE != 0) {
                    playVideo()
                }
            } else {
                // 自动替换完成的视频
                if (openMode and SettingConstants.PLAYER_OPEN_MODE_AUTO_REPLACE_COMPLETE != 0) {
                    playVideo()
                }
            }
        } else {
            // 自动播放新视频
            if (openMode and SettingConstants.PLAYER_OPEN_MODE_AUTO_PLAY != 0) {
                playVideo()
            }
        }
    }


    fun playVideo() {
        val detail = detailData.value ?: return
        val pages = detail.getPages()
        val history = detail.history
        if (pages.isNotEmpty()) {
            val page = history?.let { h ->
                pages.find { it.cid == h.cid }
            } ?: pages[0] ?: return
            playVideo(page)
        }
    }
    fun playVideo(page: Page) {
        val detail = detailData.value ?: return
        val arc = detail.getArcData() ?: return
        val author = arc.author ?: return
        videoAidToPlay = arc.aid.toString()
        val viewPages = detail.getPages()
        val ugcSeason = detail.getUgcSeasonData()
        val title = if (viewPages.size > 1) {
            page.part
        } else {
            arc.title
        }
        val cid = page.cid
        val isAutoPlaySeason = this.isAutoPlaySeason
        if (isAutoPlaySeason && ugcSeason != null) {
            // 将合集加入播放列表
            val playListFromId = (playListStore.state.from as? PlayListFrom.Season)?.seasonId
                ?: (playListStore.state.from as? PlayListFrom.Section)?.seasonId
            if (playListFromId != ugcSeason.id.toString() ||
                !playListStore.state.inListForAid(arc.aid.toString())) {
                // 当前播放列表来源不是当前合集或视频不在播放列表中时，创建新播放列表
                // 以合集创建播放列表
                val index = if (ugcSeason.sections.size > 1) {
                    ugcSeason.sections.indexOfFirst { section ->
                        section.episodes.indexOfFirst { it.aid == arc.aid } != -1
                    }
                } else { 0 }
                playListStore.setPlayList(ugcSeason, index)
            }
        } else if (!playListStore.state.inListForAid(arc.aid.toString())) {
            // 当前视频不在播放列表中时，如果未正在播放或播放列表为空则创建新的播放列表，否则将视频加入列表尾部
            if (playListStore.state.items.isEmpty()
                || playerStore.state.aid.isEmpty()) {
                // 以当前视频创建新的播放列表
                val playListItem = playListStore.run {
                    arc.toPlayListItem(viewPages)
                }
                playListStore.setPlayList(
                    name = arc.title,
                    from = playListItem.from,
                    items = listOf(
                        playListItem,
                    )
                )
            } else {
                // 将视频添加到播放列表末尾
                playListStore.addItem(playListStore.run {
                    arc.toPlayListItem(viewPages)
                })
            }
        }

        // 播放视频
        basePlayerDelegate.openPlayer(
            createVideoPlayerSource(
                mainTitle = arc.title,
                title = title,
                coverUrl = arc.pic,
                aid = arc.aid.toString(),
                id = cid.toString(),
                ownerId = author.mid.toString(),
                ownerName = author.name,
            ).apply {
                pages = viewPages
                    .map {
                        VideoPlayerSource.PageInfo(
                            cid = it.cid.toString(),
                            title = it.part,
                        )
                    }
                defaultPlayerSource.run {
                    val history = detailData.value?.history
                    if (history != null) {
                        lastPlayCid = history.cid.toString()
                        lastPlayTime = history.progress * 1000L
                    }
                    val dimension = arc.dimension
                    if (dimension != null) {
                        width = dimension.width.toInt()
                        height = dimension.height.toInt()
                    }
                }
            }
        )
    }

    /**
     * 添加至稍后再看
     */
    fun addVideoHistoryToview() = viewModelScope.launch(Dispatchers.IO) {
        if (!userStore.isLogin()) {
            GlobalToaster.show("请先登录")
            return@launch
        }
        try {
            val arcData = detailData.value?.getArcData() ?: return@launch
            val res = BiliApiService.userApi
                .videoToviewAdd(arcData.aid.toString())
                .awaitCall()
                .json<MessageInfo>()
            if (res.code == 0) {
                GlobalToaster.show("已添加至稍后再看")
                userLibraryStore.appendWatchLater(
                    UserLibraryStore.WatchLaterInfo(
                        aid = arcData.aid,
                        title = arcData.title,
                        cover = arcData.pic,
                    )
                )
            } else {
                GlobalToaster.show(res.message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalToaster.show(e.toString())
        }
    }

    fun ViewReply.getArcData(): bilibili.app.archive.v1.Arc? {
        return arc ?: activitySeason?.arc
    }

    fun ViewReply.getReqUserData(): bilibili.app.view.v1.ReqUser? {
        return activitySeason?.reqUser ?: reqUser
    }

    fun ViewReply.getUgcSeasonData(): bilibili.app.view.v1.UgcSeason? {
        return ugcSeason ?: activitySeason?.ugcSeason
    }

    fun ViewReply.getPages(): List<bilibili.app.archive.v1.Page> {
        return (activitySeason?.pages ?: pages).mapNotNull { it.page }
    }

    fun ViewReply.getBvid(): String {
        return activitySeason?.bvid ?: bvid
    }

    fun getBvid(): String {
        return detailData.value?.getBvid() ?: ""
    }

    private fun updateArcAndReqUser(
        arc: bilibili.app.archive.v1.Arc?,
        reqUser: bilibili.app.view.v1.ReqUser?,
    ) {
        val videoDetail = detailData.value ?: return
        val activitySeason = videoDetail.activitySeason
        if (activitySeason != null) {
            _detailData.value = videoDetail.copy(
                activitySeason = activitySeason.copy(
                    arc = arc,
                    reqUser = reqUser,
                ),
            )
        } else {
            _detailData.value = videoDetail.copy(
                arc = arc,
                reqUser = reqUser,
            )
        }
    }

    private fun updateCoinState(state: Int) {
        val videoDetail = detailData.value ?: return
        var videoArc = videoDetail.getArcData()
        var reqUser = videoDetail.getReqUserData()
        val stat = videoArc?.stat
        videoArc = videoArc?.copy(
            stat = stat?.copy(
                coin = stat.coin + state,
            )
        )
        reqUser = reqUser?.copy(
            coin = state,
        )
        updateArcAndReqUser(videoArc, reqUser)
    }

    private fun updateFavoriteState(state: Int) {
        val videoDetail = detailData.value ?: return
        var videoArc = videoDetail.getArcData()
        var reqUser = videoDetail.getReqUserData()
        val stat = videoArc?.stat
        if (state == 0) {
            videoArc = videoArc?.copy(
                stat = stat?.copy(
                    fav = stat.fav - 1,
                )
            )
            reqUser = reqUser?.copy(
                favorite = state,
            )
        } else if (state == 1) {
            videoArc = videoArc?.copy(
                stat = stat?.copy(
                    fav = stat.fav + 1,
                )
            )
            reqUser = reqUser?.copy(
                favorite = state,
            )
        }
        updateArcAndReqUser(videoArc, reqUser)
    }

    private fun updateLikeState(state: Int) {
        val videoDetail = detailData.value ?: return
        var videoArc = videoDetail.arc ?: videoDetail.activitySeason?.arc
        var reqUser = videoDetail.getReqUserData()
        val stat = videoArc?.stat
        if (state == 0) {
            videoArc = videoArc?.copy(
                stat = stat?.copy(
                    like = stat.like - 1,
                )
            )
            reqUser = reqUser?.copy(
                like = state,
            )
        } else if (state == 1) {
            videoArc = videoArc?.copy(
                stat = stat?.copy(
                    like = stat.like + 1,
                )
            )
            reqUser = reqUser?.copy(
                like = state,
            )
        }
        updateArcAndReqUser(videoArc, reqUser)
    }

    /**
     * 点赞/取消点赞
     */
    fun requestLike(
        arc: bilibili.app.archive.v1.Arc,
        reqUser: bilibili.app.view.v1.ReqUser?,
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (!userStore.isLogin()) {
            GlobalToaster.show("请先登录")
            return@launch
        }
        try {
            val res = BiliApiService.videoAPI
                .like(
                    aid = arc.aid.toString(),
                    dislike = reqUser?.dislike ?: 0,
                    like = reqUser?.like ?: 0,
                )
                .awaitCall()
                .json<MessageInfo>()
            if (res.isSuccess) {
                val state = if (reqUser?.like == 1) 0 else 1
                if (state == 1) {
                    GlobalToaster.show("点赞成功")
                } else {
                    GlobalToaster.show("已取消点赞")
                }
                updateLikeState(state)
            } else {
                GlobalToaster.show(res.message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalToaster.show(e.message ?: e.toString())
        }
    }

    fun updateIsAutoPlaySeason(isChecked: Boolean) {
        _isAutoPlaySeason.value = isChecked
    }

    fun openVideoPages() {
        val arc = detailData.value?.getArcData() ?: return
        pageNavigation.navigate(VideoPagesPage(arc.aid.toString()))
    }

    fun openCoverActivity() {
        val arc = detailData.value?.getArcData() ?: return
        coverDialogState.show(
            aid = arc.aid.toString(),
            bvid = getBvid(),
            title = arc.title,
            coverUrl = UrlUtil.autoHttps(arc.pic),
        )
    }

    fun toUserPage(mid: String) {
        pageNavigation.navigate(UserSpacePage(
            id = mid,
        ))
    }

    fun toVideoPage(aid: String) {
        pageNavigation.navigate(VideoDetailPage(
            id = aid,
        ))
    }

    fun toSearchPage(keyword: String) {
        pageNavigation.navigate(SearchResultPage(
            keyword = keyword,
        ))
    }

    fun toPlayListPage() {
        pageNavigation.navigate(PlayListPage())
    }

    fun toUgcSeasonPage(seasonId: String, seasonTitle: String) {
        pageNavigation.navigate(UserSeasonDetailPage(
            id = seasonId,
            title = seasonTitle,
        ))
    }

    fun openCoinDialog(aid: String, copyright: Int) {
        if (!userStore.isLogin()) {
            GlobalToaster.show("请先登录")
            return
        }
        coinDialogState.show(aid, copyright)
    }

    fun openAddFavoriteDialog(aid: String) {
        if (!userStore.isLogin()) {
            GlobalToaster.show("请先登录")
            return
        }
        favoriteDialogState.show(aid)
    }

    fun openDownloadDialog() {
        val videoDetail = detailData.value ?: return
        val videoArc = videoDetail.getArcData() ?: return
        downloadDialogState.show(
            manager = downloadManager,
            bvid = videoDetail.getBvid(),
            videoArc = videoArc,
            videoPages = videoDetail.getPages(),
        )
    }

    fun openShare(id: String, title: String) {
        val url = "http://www.bilibili.com/video/$id"
        shareText("$title $url")
    }

    fun copyPlainText(label: String, text: String) {
        copyToClipboard(text)
    }

    fun menuItemClick(item: MenuItemPropInfo) {
        val videoDetail = detailData.value ?: return
        val videoArc = videoDetail.getArcData() ?: return
        val viewPages = videoDetail.getPages()
        when (item.key) {
            MenuKeys.download -> {
                openDownloadDialog()
            }
            MenuKeys.favourite -> {
                openAddFavoriteDialog(videoArc.aid.toString())
            }
            1 -> {
                // 分享
                openShare(videoDetail.getBvid(), videoArc.title)
            }
            2 -> {
                // 浏览器打开
                val url = "http://www.bilibili.com/video/${videoDetail.getBvid()}"
                pageNavigation.launchWebBrowser(url)
            }
            3 -> {
                // 复制链接
                val text = "http://www.bilibili.com/video/${videoDetail.getBvid()}"
                copyPlainText("URL", text)
                GlobalToaster.show("已复制：$text")
            }
            4 -> {
                // 复制AV号
                val text = "av${videoArc.aid}"
                copyPlainText("URL", text)
                GlobalToaster.show("已复制：$text")
            }
            5 -> {
                // 复制BV号
                val text = videoDetail.getBvid()
                copyPlainText("URL", text)
                GlobalToaster.show("已复制：$text")
            }
            6 -> {
                // 保存封面
                openCoverActivity()
            }
            11 -> {
                // 添加至下一个播放
                val current = playerStore.getPlayListCurrentPosition()
                if (current != -1) {
                    playListStore.run {
                        addItem(
                            videoArc.toPlayListItem(viewPages),
                            current + 1
                        )
                    }
                    GlobalToaster.show("已添加至下一个播放")
                } else {
                    GlobalToaster.show("添加失败，找不到正在播放的视频")
                }
            }
            12 -> {
                // 添加至最后一个播放
                playListStore.run {
                    addItem(
                        videoArc.toPlayListItem(viewPages),
                        state.items.size,
                    )
                }
                GlobalToaster.show("已添加至最后一个播放")
            }
            13 -> {
                // 添加至稍后再看
                addVideoHistoryToview()
            }
        }
    }
}
