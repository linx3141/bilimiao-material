package cn.a10miaomiao.bilimiao.compose.platform

import androidx.compose.runtime.staticCompositionLocalOf

interface PlatformContext {
    fun openUrl(url: String)
    fun copyToClipboard(text: String)
    fun shareText(text: String)
    fun openCoverImage(aid: String) {}
    /**
     * 预测性返回手势开关变更时即时应用（安卓端立即生效并重建页面；
     * 桌面端无此概念，默认空实现）。
     */
    fun applyPredictiveBack(enable: Boolean) {}
}

val LocalPlatformContext = staticCompositionLocalOf<PlatformContext> {
    error("PlatformContext not provided")
}
