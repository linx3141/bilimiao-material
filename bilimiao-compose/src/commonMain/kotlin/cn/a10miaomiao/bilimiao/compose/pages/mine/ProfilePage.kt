package cn.a10miaomiao.bilimiao.compose.pages.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.ExpressivePreferenceItem
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.pages.auth.LoginPage
import cn.a10miaomiao.bilimiao.compose.pages.download.DownloadListPage
import cn.a10miaomiao.bilimiao.compose.pages.message.MessagePage
import cn.a10miaomiao.bilimiao.compose.pages.setting.SettingPage
import cn.a10miaomiao.bilimiao.compose.pages.user.BlackListPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserFavouritePage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSpacePage
import cn.a10miaomiao.bilimiao.compose.components.user.UserLevelIcon
import com.a10miaomiao.bilimiao.comm.apis.UserApi
import com.a10miaomiao.bilimiao.comm.entity.user.UserInfo
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.user.SpaceInfo
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.store.MessageStore
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance

/**
 * 个人信息页：头像昵称 + 收藏/离线缓存/历史/订阅/稍后再看/消息/设置入口。
 */
@Serializable
class ProfilePage : ComposePage {

    @Composable
    override fun Content() {
        val viewModel: ProfilePageViewModel = diViewModel { ProfilePageViewModel(it) }
        val userStore by rememberInstance<UserStore>()
        val userState by userStore.stateFlow.collectAsState()
        val messageStore by rememberInstance<MessageStore>()
        val messageState by messageStore.stateFlow.collectAsState()
        val unreadCount = messageState.totalCount()
        val sign by viewModel.sign.collectAsState()
        LaunchedEffect(userState.info?.mid) {
            viewModel.loadSign()
        }
        ProfilePageContent(
            userInfo = userState.info,
            sign = sign,
            unreadCount = unreadCount,
            onUserClick = {
                val info = userState.info
                if (info != null) {
                    viewModel.toUserSpace(info.mid.toString())
                } else {
                    viewModel.toLogin()
                }
            },
            onEntryClick = viewModel::onEntryClick,
        )
    }
}

private class ProfilePageViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val pageNavigation by instance<PageNavigation>()
    private val userStore by instance<UserStore>()
    private val _sign = MutableStateFlow<String?>(null)
    val sign: StateFlow<String?> get() = _sign

    fun loadSign() {
        val mid = userStore.state.info?.mid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = UserApi().space(mid.toString())
                    .awaitCall()
                    .json<ResponseData<SpaceInfo>>()
                _sign.value = res.data?.card?.sign
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toUserSpace(mid: String) {
        pageNavigation.navigate(UserSpacePage(id = mid))
    }

    fun toLogin() {
        pageNavigation.navigate(LoginPage())
    }

    fun onEntryClick(index: Int) {
        when (index) {
            0 -> {
                val mid = userStore.state.info?.mid
                if (mid != null) {
                    pageNavigation.navigate(UserFavouritePage(mid = mid.toString()))
                } else {
                    GlobalToaster.show("请先登录")
                }
            }
            1 -> pageNavigation.navigate(DownloadListPage())
            2 -> pageNavigation.navigate(HistoryPage())
            3 -> pageNavigation.navigate(MyFollowPage())
            4 -> pageNavigation.navigate(BlackListPage())
            5 -> pageNavigation.navigate(WatchLaterPage())
            6 -> pageNavigation.navigate(MessagePage())
            7 -> pageNavigation.navigate(SettingPage())
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfilePageContent(
    userInfo: UserInfo?,
    sign: String?,
    unreadCount: Int,
    onUserClick: () -> Unit,
    onEntryClick: (Int) -> Unit,
) {
    val windowInsets = localContentInsets()
    val entries = listOf(
        Triple("收藏夹", Icons.Filled.Star, 0),
        Triple("离线缓存", Icons.Filled.Download, 1),
        Triple("历史记录", Icons.Filled.History, 2),
        Triple("关注列表", Icons.Filled.Person, 3),
        Triple("黑名单", Icons.Filled.Block, 4),
        Triple("稍后再看", Icons.Filled.PlaylistPlay, 5),
        Triple("消息", Icons.Filled.Notifications, 6),
        Triple("设置", Icons.Filled.Settings, 7),
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = windowInsets.topDp.dp,
            end = 12.dp,
            bottom = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "user") {
            // 用户卡片：与搜索页 up 主列表卡片一致，独立（不与下方菜单相连）
            androidx.compose.runtime.CompositionLocalProvider(
                LocalListItemShapes provides segmentedItemShapes(0, 1),
            ) {
                val segmentedShapes = LocalListItemShapes.current
                Surface(
                    // 与下方菜单列表保持明显间距，体现独立卡片
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = segmentedShapes!!.shape,
                    color = MaterialTheme.colorScheme.surfaceBright,
                    onClick = onUserClick,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = userInfo?.face?.takeIf { it.isNotBlank() }
                                ?.let { UrlUtil.autoHttps(it) + "@200w_200h" },
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = userInfo?.name ?: "点击登录",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                UserLevelIcon(
                                    modifier = Modifier
                                        .padding(start = 5.dp)
                                        .size(20.dp, 15.dp),
                                    level = userInfo?.level ?: 0,
                                )
                            }
                            if (userInfo != null) {
                                Row {
                                    Text(
                                        text = NumberUtil.converString(userInfo.follower) + "粉丝",
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Spacer(modifier = Modifier.width(15.dp))
                                    Text(
                                        text = NumberUtil.converString(userInfo.following) + "关注",
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Spacer(modifier = Modifier.width(15.dp))
                                    Text(
                                        text = NumberUtil.converString(userInfo.dynamic) + "动态",
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                                if (!sign.isNullOrBlank()) {
                                    Text(
                                        text = sign,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.outline,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        itemsIndexed(
            entries,
            key = { _, entry -> entry.first },
        ) { index, entry ->
            androidx.compose.runtime.CompositionLocalProvider(
                LocalListItemShapes provides segmentedItemShapes(
                    index,
                    entries.size,
                ),
            ) {
                ExpressivePreferenceItem(
                    title = {
                        Text(text = entry.first)
                    },
                    icon = {
                        val iconContent: @Composable () -> Unit = {
                            Icon(
                                imageVector = entry.second,
                                contentDescription = entry.first,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (entry.third == 6 && unreadCount > 0) {
                            // "消息"图标角标（与底栏"我的"角标一致）
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ) {
                                        Text(
                                            text = if (unreadCount > 99) {
                                                "99+"
                                            } else {
                                                unreadCount.toString()
                                            },
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                },
                            ) {
                                iconContent()
                            }
                        } else {
                            iconContent()
                        }
                    },
                    onClick = { onEntryClick(entry.third) },
                )
            }
        }
    }
}
