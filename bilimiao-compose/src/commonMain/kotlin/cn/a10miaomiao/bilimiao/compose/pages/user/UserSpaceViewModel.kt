package cn.a10miaomiao.bilimiao.compose.pages.user

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialogState
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.BangumiDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.mine.MyBangumiPage
import cn.a10miaomiao.bilimiao.compose.pages.mine.MyFollowPage
import com.a10miaomiao.bilimiao.comm.apis.UserApi
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.user.SpaceInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.FilterStore
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import cn.a10miaomiao.bilimiao.compose.common.foundation.springAnimateToPage

class UserSpaceViewModel(
    override val di: DI,
    val vmid: String,
    val archiveViewModel: UserArchiveViewModel,
) : ViewModel(), DIAware {

    private val pageNavigation by instance<PageNavigation>()
    private val messageDialog by instance<MessageDialogState>()
    val userStore: UserStore by instance()
    val filterStore: FilterStore by instance()

    var openUrl: (String) -> Unit = {}
    var copyToClipboard: (String) -> Unit = {}
    var shareText: (String) -> Unit = {}

    private val _loading = MutableStateFlow(false);
    val loading: StateFlow<Boolean> get() = _loading

    private val _fail = MutableStateFlow<Any?>(null)
    val fail: StateFlow<Any?> get() = _fail

    private val _detailData = MutableStateFlow<SpaceInfo?>(null)
    val detailData: StateFlow<SpaceInfo?> get() = _detailData

    private val _isFollow = MutableStateFlow(false)
    val isFollow: StateFlow<Boolean> get() = _isFollow

    private val _isFiltered = mutableStateOf(!filterStore.filterUpper(vmid))
    val isFiltered get() = _isFiltered.value

    val isSelf get() = userStore.isSelf(vmid)

    val tabs = listOf(
        UserSpacePageTabs.Index(this),
        UserSpacePageTabs.Dynamic(vmid),
        UserSpacePageTabs.Archive(archiveViewModel),
    )

    val pagerState = PagerState{ tabs.size }
    val currentPage get() = pagerState.currentPage

    init {
        if (vmid.isNotBlank()) {
            loadData()
        }
    }

    suspend fun changeTab(index: Int, animate: Boolean = false) {
        if (animate) {
            pagerState.springAnimateToPage(index)
        } else {
            pagerState.scrollToPage(index)
        }
    }

    fun loadData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            _loading.value = true
            _fail.value = null
            val res = BiliApiService
                .userApi
                .space(vmid)
                .awaitCall()
                .json<ResponseData<SpaceInfo>>()
            if (res.code == 0) {
                val result = res.requireData()
                _detailData.value = result
                _isFollow.value = result.card.relation.is_follow == 1
            } else {
                _fail.value = res.message
                GlobalToaster.show(res.message)
            }
        } catch (e: Exception) {
            _fail.value = e
            GlobalToaster.show("网络错误")
            e.printStackTrace()
        } finally {
            _loading.value = false
        }
    }

    /**
     * 取消屏蔽该UP主：移除本地屏蔽，并同步把用户移出 B 站黑名单（需登录）。
     * 与 PiliPlus 一致：先确认再执行。
     */
    fun filterUpperDelete () {
        val info = detailData.value
        if (info == null) {
            GlobalToaster.show("请等待信息加载完成")
            return
        }
        messageDialog.open(
            title = "取消屏蔽该UP主",
            text = "确定将「${info.card.name}」移出黑名单？\n" +
                "移出后本地不再过滤其内容，对方可重新与你互动。",
            closeText = "取消",
            confirmButton = {
                TextButton(
                    onClick = {
                        messageDialog.close()
                        doFilterUpperDelete()
                    }
                ) {
                    Text("确定")
                }
            },
        )
    }

    private fun doFilterUpperDelete() {
        filterStore.deleteUpper(vmid.toLong(), showToast = false)
        _isFiltered.value = false
        if (!userStore.isLogin()) {
            GlobalToaster.show("已取消屏蔽")
            return
        }
        // 同步移出 B 站黑名单
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = BiliApiService.userRelationApi
                    .unblock(vmid)
                    .awaitCall().json<MessageInfo>()
                if (res.code == 0) {
                    GlobalToaster.show("已取消屏蔽（已移出黑名单）")
                } else {
                    GlobalToaster.show("已取消本地屏蔽，移出黑名单失败：${res.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalToaster.show("已取消本地屏蔽，移出黑名单失败")
            }
        }
    }

    /**
     * 屏蔽该UP主：加入本地屏蔽列表，并同步拉黑用户（需登录，自动解除关注关系）。
     */
    fun filterUpperAdd () {
        val info = detailData.value
        if (info == null) {
            GlobalToaster.show("请等待信息加载完成")
            return
        }
        // 未登录时无法拉黑，直接本地屏蔽即可
        if (!userStore.isLogin()) {
            filterStore.addUpper(info.card.mid.toLong(), info.card.name, showToast = false)
            _isFiltered.value = true
            GlobalToaster.show("未登录，仅本地屏蔽")
            return
        }
        messageDialog.open(
            title = "屏蔽该UP主",
            text = "确定将「${info.card.name}」加入黑名单？\n" +
                "加入黑名单后将自动解除关注关系，禁止其与你互动或查看你的空间，并在本地屏蔽其内容。",
            closeText = "取消",
            confirmButton = {
                TextButton(
                    onClick = {
                        messageDialog.close()
                        doFilterUpperAdd(info)
                    }
                ) {
                    Text("确定")
                }
            },
        )
    }

    private fun doFilterUpperAdd(info: SpaceInfo) {
        filterStore.addUpper(info.card.mid.toLong(), info.card.name, showToast = false)
        _isFiltered.value = true
        // 云端拉黑：加入 B 站黑名单
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = BiliApiService.userRelationApi
                    .block(info.card.mid)
                    .awaitCall().json<MessageInfo>()
                if (res.code == 0) {
                    // 拉黑后 B 站会自动解除关注关系
                    _isFollow.value = false
                    GlobalToaster.show("已屏蔽该UP主（已加入黑名单）")
                } else {
                    GlobalToaster.show("已本地屏蔽，拉黑失败：${res.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalToaster.show("已本地屏蔽，拉黑失败")
            }
        }
    }

    fun getUserSpaceUrl (): String {
        return "https://space.bilibili.com/${vmid}"
    }

    fun attention() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val data = detailData.value ?: return@launch
            val mode = if (isFollow.value) { 2 } else { 1 }
            val res = BiliApiService.userRelationApi
                .modify(vmid, mode)
                .awaitCall().json<MessageInfo>()
            if (res.code == 0) {
                _isFollow.value = mode == 1
                GlobalToaster.show(if (mode == 1) {
                    "关注成功"
                } else {
                    "已取消关注"
                })
            } else {
                GlobalToaster.show(res.message)
            }
        } catch (e: Exception) {
            GlobalToaster.show("网络错误")
            e.printStackTrace()
        }
    }

    fun toFans() {
        pageNavigation.navigate(UserFansPage(vmid))
    }

    fun toFollow() {
        if (isSelf) {
            pageNavigation.navigate(MyFollowPage())
        } else {
            pageNavigation.navigate(UserFollowPage(vmid))
        }
    }

    fun showLikeInfo() {
        val detailInfo = detailData.value ?: return
        messageDialog.alert(
            title = detailInfo.card.name,
            text = "${detailInfo.card.likes.skr_tip}：${detailInfo.card.likes.like_num}"
        )
    }

    fun toBangumiFollow() {
        if (isSelf) {
            pageNavigation.navigate(MyBangumiPage())
        } else {
            pageNavigation.navigate(UserBangumiPage(vmid))
        }
    }


    fun toLikeArchive() {
        pageNavigation.navigate(UserLikeArchivePage(vmid))
    }

    fun toVideoDetail(item: SpaceInfo.ArchiveItem) {
        pageNavigation.navigateToVideoInfo(item.param)
    }

    fun toBangumiDetail(item: SpaceInfo.SeasonItem) {
        pageNavigation.navigate(BangumiDetailPage(
            id = item.param
        ))
    }

    fun toFavouriteList() {
        pageNavigation.navigate(UserFavouritePage(
            mid = vmid
        ))
    }

    fun toFavouriteDetail(item: SpaceInfo.Favourite2Item) {
        pageNavigation.navigate(UserFavouriteDetailPage(
            id = item.media_id,
            title = item.title
        ))
    }

    fun menuItemClick(item: MenuItemPropInfo) {
        when (item.key) {
            // 取消屏蔽
            1 -> filterUpperDelete()
            // 屏蔽
            2 -> filterUpperAdd()
            // 用浏览器打开
            3 -> {
                val url = getUserSpaceUrl()
                openUrl(url)
            }
            // 复制链接
            4 -> {
                val text = getUserSpaceUrl()
                copyToClipboard(text)
                GlobalToaster.show("已复制：$text")
            }
            // 分享
            5 -> {
                val info = detailData.value
                val url = getUserSpaceUrl()
                shareText(info?.card?.name + " " + url)
            }
            11, 12 -> {
                archiveViewModel.changeRankOrder(item.action ?: "")
            }
            MenuKeys.follow -> {
                attention()
            }
        }
    }

    fun searchSelfPage(keyword: String) {
        pageNavigation.navigate(UserSpaceSearchPage(
            id = vmid,
            keyword = keyword,
        ))
    }

}
