package cn.a10miaomiao.bilimiao.compose.components.dyanmic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.assets.BilimiaoIcons
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.Common
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.common.Like
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.common.Likefill
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.common.Reply
import cn.a10miaomiao.bilimiao.compose.assets.bilimiaoicons.common.Share
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.DynamicDetailPage
import cn.a10miaomiao.bilimiao.compose.platform.LocalPlatformContext
import bilibili.app.dynamic.v2.DynamicGRPC
import bilibili.app.dynamic.v2.DynThumbReq
import bilibili.app.dynamic.v2.ThumbType
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.compose.rememberInstance


@Composable
fun DynamicModuleStatBox(
    stat: bilibili.app.dynamic.v2.ModuleStat,
    dynId: String = "",
    dynType: Long = 0L,
) {
    DynamicModuleStatBox(
        share = stat.repost,
        reply = stat.reply,
        like = stat.like,
        isLike = stat.likeInfo?.isLike == true,
        dynId = dynId,
        dynType = dynType,
    )
}

@Composable
fun DynamicModuleStatBox(
    share: Long,
    reply: Long,
    like: Long,
    isLike: Boolean,
    dynId: String = "",
    dynType: Long = 0L,
) {
    val pageNavigation: PageNavigation by rememberInstance()
    val userStore: UserStore by rememberInstance()
    val platformContext = LocalPlatformContext.current
    val scope = rememberCoroutineScope()

    var likeState by remember(like, isLike) {
        mutableStateOf(like to isLike)
    }
    var liking by remember { mutableStateOf(false) }

    fun doLike() {
        if (liking) return
        if (!userStore.isLogin()) {
            GlobalToaster.show("请先登录")
            return
        }
        if (dynId.isBlank()) {
            GlobalToaster.show("无法获取动态ID")
            return
        }
        val currentIsLike = likeState.second
        liking = true
        // 乐观更新点赞状态
        likeState = (likeState.first + if (currentIsLike) -1 else 1) to !currentIsLike
        scope.launch(Dispatchers.IO) {
            try {
                val req = DynThumbReq(
                    uid = userStore.state.info?.mid ?: 0L,
                    dynId = dynId,
                    dynType = dynType,
                    rid = "",
                    type = if (currentIsLike) ThumbType.CANCEL else ThumbType.THUMB,
                )
                BiliGRPCHttp.request {
                    DynamicGRPC.dynThumb(req)
                }.awaitCall()
                withContext(Dispatchers.Main) {
                    if (likeState.second) {
                        GlobalToaster.show("点赞成功")
                    } else {
                        GlobalToaster.show("已取消点赞")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // 失败回滚
                    likeState = (likeState.first + if (currentIsLike) 1 else -1) to currentIsLike
                    GlobalToaster.show(e.message ?: "操作失败")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    liking = false
                }
            }
        }
    }

    fun doReply() {
        if (dynId.isBlank()) {
            GlobalToaster.show("无法获取动态ID")
            return
        }
        pageNavigation.navigate(DynamicDetailPage(dynId))
    }

    fun doShare() {
        if (dynId.isBlank()) {
            GlobalToaster.show("无法获取动态ID")
            return
        }
        platformContext.shareText("https://t.bilibili.com/$dynId")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            onClick = ::doShare,
            contentPadding = PaddingValues(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    BilimiaoIcons.Common.Share,
                    contentDescription = "share",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 4.dp)
                        .size(16.dp)
                )
                Text(
                    text = NumberUtil.converString(share),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        TextButton(
            onClick = ::doReply,
            contentPadding = PaddingValues(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    BilimiaoIcons.Common.Reply,
                    contentDescription = "reply",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 4.dp)
                        .size(16.dp)
                )
                Text(
                    text = NumberUtil.converString(reply),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        TextButton(
            onClick = ::doLike,
            enabled = !liking,
            contentPadding = PaddingValues(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (likeState.second) {
                    Icon(
                        BilimiaoIcons.Common.Likefill,
                        contentDescription = "like",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                            .size(16.dp)
                    )
                } else {
                    Icon(
                        BilimiaoIcons.Common.Like,
                        contentDescription = "like",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 4.dp)
                            .size(16.dp)
                    )
                }
                Text(
                    text = NumberUtil.converString(likeState.first),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
