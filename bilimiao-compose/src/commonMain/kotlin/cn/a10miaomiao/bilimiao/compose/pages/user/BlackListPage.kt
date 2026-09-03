package cn.a10miaomiao.bilimiao.compose.pages.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.entity.FlowPaginationInfo
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialogState
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.list.SwipeToRefresh
import cn.a10miaomiao.bilimiao.compose.components.user.UserInfoCard
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.user.BlackListInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.FilterStore
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

/**
 * 黑名单（已拉黑用户）列表页。
 *
 * 与本地"屏蔽UP主"列表双向对齐：
 * - 云端已拉黑但本地未屏蔽的用户，拉取列表时自动补入本地屏蔽（过滤其内容）；
 * - 在此移出黑名单的用户，同步移除本地屏蔽。
 */
@Serializable
class BlackListPage : ComposePage {

    @Composable
    override fun Content() {
        val viewModel = diViewModel { BlackListPageViewModel(it) }
        BlackListPageContent(viewModel = viewModel)
    }
}

private class BlackListPageViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val pageNavigation by instance<PageNavigation>()
    private val userStore by instance<UserStore>()
    private val filterStore by instance<FilterStore>()
    private val messageDialog by instance<MessageDialogState>()

    val isRefreshing = MutableStateFlow(false)
    val list = FlowPaginationInfo<BlackListInfo.Item>()

    init {
        loadData(1)
    }

    fun loadData(
        pageNum: Int = list.pageNum,
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            if (!userStore.isLogin()) {
                list.fail.value = "请先登录"
                return@launch
            }
            if (pageNum == 1) {
                list.reset()
            }
            list.loading.value = true
            val res = BiliApiService.userRelationApi
                .blacks(
                    pageNum = pageNum,
                    pageSize = list.pageSize,
                )
                .awaitCall()
                .json<ResponseData<BlackListInfo>>()
            if (res.isSuccess) {
                list.pageNum = pageNum
                val result = res.requireData()
                if (pageNum == 1) {
                    list.data.value = result.list
                } else {
                    list.data.value = mutableListOf<BlackListInfo.Item>().apply {
                        addAll(list.data.value)
                        addAll(result.list)
                    }
                }
                list.finished.value = result.list.size < list.pageSize
                // 本地/云端对齐：云端黑名单中本地未屏蔽的用户补入本地屏蔽，
                // 使其内容在推荐/动态等流中同样被过滤
                syncLocalFilter(result.list)
            } else {
                list.fail.value = res.message
            }
        } catch (e: Exception) {
            list.fail.value = "无法连接到御坂网络"
        } finally {
            list.loading.value = false
            isRefreshing.value = false
        }
    }

    private fun syncLocalFilter(items: List<BlackListInfo.Item>) {
        items.forEach { item ->
            val mid = item.mid.toLongOrNull() ?: return@forEach
            if (filterStore.filterUpper(mid)) {
                // 仅静默补齐本地屏蔽，不打扰用户
                filterStore.addUpper(mid, item.uname, showToast = false)
            }
        }
    }

    fun loadMore() {
        if (!list.finished.value && !list.loading.value) {
            loadData(list.pageNum + 1)
        }
    }

    fun refresh() {
        isRefreshing.value = true
        list.finished.value = false
        list.fail.value = ""
        loadData(1)
    }

    /**
     * 移出黑名单：确认后云端解除拉黑，并同步移除本地屏蔽。
     */
    fun removeBlack(
        index: Int,
    ) {
        val item = list.data.value.getOrNull(index) ?: return
        if (!userStore.isLogin()) {
            GlobalToaster.show("请先登录")
            return
        }
        messageDialog.open(
            title = "移出黑名单",
            text = "确定将「${item.uname}」移出黑名单？\n移出后本地将不再屏蔽其内容。",
            closeText = "取消",
            confirmButton = {
                TextButton(
                    onClick = {
                        messageDialog.close()
                        doRemove(item, index)
                    }
                ) {
                    Text("确定")
                }
            },
        )
    }

    private fun doRemove(
        item: BlackListInfo.Item,
        index: Int,
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val res = BiliApiService.userRelationApi
                .unblock(item.mid)
                .awaitCall().json<MessageInfo>()
            if (res.code == 0) {
                // 本地同步解除屏蔽（与云端对齐）
                item.mid.toLongOrNull()?.let {
                    filterStore.deleteUpper(it, showToast = false)
                }
                list.data.value = list.data.value.toMutableList().apply {
                    removeAt(index)
                }
                withContext(Dispatchers.Main) {
                    GlobalToaster.show("已移出黑名单")
                }
            } else {
                withContext(Dispatchers.Main) {
                    GlobalToaster.show(res.message)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                GlobalToaster.show("网络错误")
            }
            e.printStackTrace()
        }
    }

    fun toUserDetailPage(id: String) {
        pageNavigation.navigate(UserSpacePage(id))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BlackListPageContent(
    viewModel: BlackListPageViewModel,
) {
    val windowInsets = localContentInsets()

    val list by viewModel.list.data.collectAsState()
    val listLoading by viewModel.list.loading.collectAsState()
    val listFinished by viewModel.list.finished.collectAsState()
    val listFail by viewModel.list.fail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    PageConfig(
        title = "黑名单",
    )

    SwipeToRefresh(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(300.dp),
            modifier = Modifier.padding(
                start = windowInsets.leftDp.dp,
                end = windowInsets.rightDp.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Spacer(modifier = Modifier.height(windowInsets.topDp.dp))
            }

            itemsIndexed(
                list,
                key = { _, item -> item.mid },
                span = { _, _ -> GridItemSpan(maxLineSpan) },
            ) { index, item ->
                CompositionLocalProvider(
                    LocalListItemShapes provides segmentedItemShapes(
                        index,
                        list.size,
                    ),
                ) {
                    UserInfoCard(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        name = item.uname,
                        face = item.face,
                        sign = item.sign,
                        onClick = {
                            viewModel.toUserDetailPage(item.mid)
                        }
                    ) {
                        Button(
                            onClick = { viewModel.removeBlack(index) },
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(
                                vertical = 4.dp,
                                horizontal = 12.dp,
                            ),
                            modifier = Modifier
                                .sizeIn(
                                    minWidth = 40.dp,
                                    minHeight = 30.dp
                                )
                                .padding(0.dp),
                        ) {
                            Text(
                                text = "移出",
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                ListStateBox(
                    modifier = Modifier.padding(
                        bottom = windowInsets.bottom
                    ),
                    loading = listLoading,
                    finished = listFinished,
                    fail = listFail,
                    listData = list,
                ) {
                    viewModel.loadMore()
                }
            }
        }
    }
}
