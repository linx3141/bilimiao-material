package cn.a10miaomiao.bilimiao.compose.base

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

/**
 * 所有页面的基类接口，同时作为 Nav3 的 NavKey。
 * 子类需为 data class 或 object，并建议加 @Serializable 以支持 rememberNavBackStack。
 */
interface ComposePage : NavKey {

    @Composable
    fun Content()

    /**
     * 以底部弹窗方式打开时，是否显示标题栏（左上角关闭按钮 + 标题）。
     * 例如发送弹幕页需要与发送评论弹窗保持一致的布局，不显示标题栏。
     */
    val showBottomSheetTitleBar: Boolean
        get() = true

}
