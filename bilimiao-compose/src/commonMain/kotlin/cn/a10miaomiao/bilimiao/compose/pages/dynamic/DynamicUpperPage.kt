package cn.a10miaomiao.bilimiao.compose.pages.dynamic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import bilibili.app.dynamic.v2.UpListItem
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.content.DynamicUpperContent
import kotlinx.serialization.Serializable

/**
 * UP 主个人动态页：从动态页的"最常访问"或侧栏进入，
 * 作为独立导航页面打开（与视频详情页一致），预测性返回/转场动画由导航层处理。
 */
@Serializable
class DynamicUpperPage(
    val uid: Long,
    val face: String = "",
    val name: String = "",
) : ComposePage {

    @Composable
    override fun Content() {
        PageConfig(title = if (name.isBlank()) "UP主动态" else name)
        val upper = remember(uid, face, name) {
            UpListItem(
                uid = uid,
                face = face,
                name = name,
            )
        }
        DynamicUpperContent(upper = upper)
    }
}
