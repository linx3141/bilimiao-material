@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.dynamic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ModalDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bilibili.app.archive.middleware.v1.PlayerArgs
import bilibili.app.dynamic.v2.DynDetailReq
import bilibili.app.dynamic.v2.DynamicGRPC
import bilibili.app.dynamic.v2.DynamicItem
import bilibili.app.dynamic.v2.DynamicType
import bilibili.app.dynamic.v2.Module.ModuleItem
import bilibili.app.dynamic.v2.OpusDetailReq
import bilibili.app.dynamic.v2.OpusGRPC
import bilibili.app.dynamic.v2.OpusItem
import bilibili.app.dynamic.v2.Paragraph
import bilibili.app.dynamic.v2.PicParagraph
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.ContentInsets
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.dyanmic.DynamicModuleStatBox
import cn.a10miaomiao.bilimiao.compose.components.miao.MiaoCard
import cn.a10miaomiao.bilimiao.compose.components.community.ReplyItemBox
import cn.a10miaomiao.bilimiao.compose.components.dyanmic.DynamicModuleBox
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.status.BiliFailBox
import cn.a10miaomiao.bilimiao.compose.components.status.BiliLoadingBox
import cn.a10miaomiao.bilimiao.compose.pages.community.MainReplyListPageContent
import cn.a10miaomiao.bilimiao.compose.pages.community.MainReplyViewModel
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.network.BiliGRPCHttp
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import pbandk.decodeFromByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance

@Serializable
data class DynamicDetailPage(
    private val id: String,
) : ComposePage {

    @Composable
    override fun Content() {
        val viewModel = diViewModel(key = "dynamic$id") {
            DynamicDetailPageViewModel(it, id)
        }
        DynamicDetailPageContent(viewModel)
    }

}

private class DynamicDetailPageViewModel(
    override val di: DI,
    val dynId: String,
) : ViewModel(), DIAware {

    val userStore: UserStore by instance()

    private val _loading = MutableStateFlow(false);
    val loading: StateFlow<Boolean> get() = _loading

    private val _fail = MutableStateFlow<Any?>(null)
    val fail: StateFlow<Any?> get() = _fail

    private val _detailData = MutableStateFlow<DynamicItem?>(null)
    val detailData: StateFlow<DynamicItem?> get() = _detailData
    init {
        if (dynId.isNotBlank()) {
            loadData()
        }
    }

    fun loadData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            _loading.value = true
            _fail.value = null
            val req = DynDetailReq(
                uid = userStore.state.info?.mid ?: 0L,
                dynamicId = dynId,
                shareId = "dt.opus-detail.0.0.pv",
                shareMode = 3,
                localTime = 8,
                playerArgs = PlayerArgs(
                    qn = 32,
                    fnval = 400,
                )
            )
            val res = BiliGRPCHttp.request {
                DynamicGRPC.dynDetail(req)
            }.awaitCall()
            _detailData.value = res.item
        } catch (e: Exception) {
            _fail.value = e
            GlobalToaster.show("网络错误")
            e.printStackTrace()
        } finally {
            _loading.value = false
        }
    }

    fun menuItemClick(item: MenuItemPropInfo) {
        when (item.key) {
            MenuKeys.home -> {
            }
        }
    }
}


@Composable
private fun DynamicDetailPageContent(
    viewModel: DynamicDetailPageViewModel
) {
    val windowInsets = localContentInsets()

    val detailData = viewModel.detailData.collectAsState().value

    AnimatedContent(
        modifier = Modifier.fillMaxSize(),
        targetState = detailData == null,
        label = "DynamicDetailPageContent",
        transitionSpec = {
            // Follow M3 Clean fades
            val fadeIn = fadeIn(
                tween(),
            )
            val fadeOut = fadeOut()
            fadeIn.togetherWith(fadeOut)
        }
    ) {
        if (it || detailData == null) {
            DynamicDetailPageLoadingContent(
                loading = viewModel.loading.collectAsState().value,
                fail = viewModel.fail.collectAsState().value,
                innerPadding = windowInsets.toPaddingValues()
            )
        } else {
            DynamicDetailPageDetailContent(
                viewModel = viewModel,
                windowInsets = windowInsets,
                detailData = detailData,
            )
        }
    }
}

@Composable
private fun DynamicDetailPageLoadingContent(
    loading: Boolean,
    fail: Any?,
    innerPadding: PaddingValues,
) {
    PageConfig(
        title = "动态详情"
    )
    if (loading) {
        BiliLoadingBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    } else if (fail != null) {
        BiliFailBox(
            e = fail,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
private fun DynamicDetailPageDetailContent(
    viewModel: DynamicDetailPageViewModel,
    windowInsets: ContentInsets,
    detailData: DynamicItem,
) {
    val extend = detailData.extend
    val oid: String
    val commentType: Int
    val originDyn = extend?.sourceContent?.value?.array?.let { bytes ->
        runCatching {
            bilibili.app.dynamic.v2.DynamicItem.Companion.decodeFromByteArray(bytes)
        }.getOrNull()
    }
    val originBusinessId = originDyn?.extend?.businessId
        ?.takeIf { it.isNotBlank() }
    if (detailData.cardType == DynamicType.AV) {
        // 视频动态：评论区即视频评论区（type=1, oid=视频aid）
        oid = extend?.businessId?.takeIf { it.isNotBlank() } ?: extend?.dynIdStr.orEmpty()
        commentType = 1
    } else if (detailData.cardType == DynamicType.FORWARD) {
        if (extend?.opusSummary != null) {
            // opus 图文动态：评论 type=17，oid=动态 id
            oid = extend?.businessId?.takeIf { it.isNotBlank() }
                ?: extend?.dynIdStr.orEmpty()
            commentType = 17
        } else {
            // 普通转发动态：评论在原动态上，oid=原动态业务 id（视频为 aid），type 按原动态类型
            oid = originBusinessId
                ?: extend?.origDynIdStr?.takeIf { it.isNotBlank() }
                ?: extend?.businessId?.takeIf { it.isNotBlank() }
                ?: extend?.dynIdStr.orEmpty()
            commentType = when (extend?.origDynType) {
                DynamicType.AV -> 1
                DynamicType.DRAW -> 17
                else -> 11
            }
        }
    } else if (detailData.cardType == DynamicType.DRAW) {
        // 图文（相册/opus）动态：评论 type=17
        oid = extend?.businessId?.takeIf { it.isNotBlank() } ?: extend?.dynIdStr.orEmpty()
        commentType = 17
    } else {
        // 文字/转发等普通动态：评论 oid=业务方id，type=11（已验证 oid=businessId, type=11 有效）
        oid = extend?.businessId?.takeIf { it.isNotBlank() } ?: extend?.dynIdStr.orEmpty()
        commentType = 11
    }
    val replyViewModel = diViewModel(
        key = "dynamic.reply.${oid}"
    ) {
        MainReplyViewModel(
            it, oid,
            type = commentType,
            extra = "{\"spmid\":\"dt.dt-detail.0.0\",\"from_spmid\":\"\"}",
            filterTagName = "全部"
        )
    }

    val buttomModule = remember(detailData) {
        detailData.modules.lastOrNull()?.let {
            val moduleItem = it.moduleItem
            if (moduleItem is ModuleItem.ModuleButtom) moduleItem.value
            else null
        }
    }

    val origName = detailData.extend?.origName

    MainReplyListPageContent(
        viewModel = replyViewModel,
        pageTitle = origName?.let {
            "${it}\n的\n动态详情"
        } ?: "动态详情",
        headerContent = {
            item {
                Column(
                    modifier = Modifier
                        .padding(bottom = 5.dp),
                ) {
                    // 主动态：与动态列表页一致的卡片（up主/图/文/三个按键）
                    CompositionLocalProvider(
                        LocalListItemShapes provides segmentedItemShapes(0, 1),
                    ) {
                        MiaoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        ) {
                            for(module in detailData.modules) {
                                DynamicModuleBox(
                                    module = module,
                                    item = viewModel.detailData.value,
                                )
                            }
                            // 详情接口的统计在 buttom 模块，补上分享/评论/点赞三个按键
                            buttomModule?.moduleStat?.let { stat ->
                                DynamicModuleStatBox(
                                    stat = stat,
                                    dynId = extend?.dynIdStr ?: "",
                                    dynType = extend?.dynType ?: 0L,
                                )
                            }
                        }
                    }
                }
            }
            item {
                val moduleStat = buttomModule?.moduleStat
                Text(
                    modifier = Modifier
                        .padding(
                            top = 10.dp,
                            bottom = 5.dp,
                            start = 10.dp,
                            end = 10.dp,
                        ),
                    text = if (moduleStat == null) "全部评论"
                    else "全部评论(${moduleStat.reply})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                HorizontalDivider()
            }
        }
    )
}
