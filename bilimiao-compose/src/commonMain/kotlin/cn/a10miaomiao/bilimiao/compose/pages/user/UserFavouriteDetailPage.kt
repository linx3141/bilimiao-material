package cn.a10miaomiao.bilimiao.compose.pages.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.emitter.SharedFlowEmitter
import cn.a10miaomiao.bilimiao.compose.pages.user.content.UserFavouriteDetailContent
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.kodein.di.compose.rememberInstance

/**
 * 收藏夹详情页（手机窄屏下从收藏夹列表导航进入）。
 * 使用独立导航页，返回时与其他页面一致支持预测性返回手势与曲线动画。
 */
@Serializable
data class UserFavouriteDetailPage(
    private val id: String,
    private val title: String,
    private val keyword: String = "",
    private val type: String = "",
) : ComposePage {

    @Composable
    override fun Content() {
        val emitter: SharedFlowEmitter by rememberInstance()
        val scope = rememberCoroutineScope()
        UserFavouriteDetailContent(
            mediaId = id,
            mediaTitle = title,
            keyword = keyword,
            showTowPane = false,
            hideFirstPane = false,
            onChangeHideFirstPane = {},
            onClose = {},
            onRefresh = {
                // 详情页内修改（删除/取消收藏等）后通知列表页刷新
                if (type.isNotBlank()) {
                    scope.launch {
                        emitter.emit(EmitterAction.MediaListChanged(type))
                    }
                }
            },
        )
    }
}
