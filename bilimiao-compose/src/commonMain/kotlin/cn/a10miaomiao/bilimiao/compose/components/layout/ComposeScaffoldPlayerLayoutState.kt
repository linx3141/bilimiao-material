package cn.a10miaomiao.bilimiao.compose.components.layout

import androidx.compose.ui.geometry.Rect
import cn.a10miaomiao.bilimiao.compose.ORIENTATION_LANDSCAPE
import cn.a10miaomiao.bilimiao.compose.ORIENTATION_PORTRAIT
import cn.a10miaomiao.bilimiao.compose.PlayerFloatingLayoutState
import cn.a10miaomiao.bilimiao.compose.PlayerPortraitLayoutState

enum class PlayerDisplayMode {
    Hidden,
    EmbeddedPortrait,
    FloatingLandscape,
    Fullscreen,
    AnchorOverlay,
}

data class ComposeScaffoldPlayerLayoutState(
    val showPlayer: Boolean,
    val fullScreenPlayer: Boolean,
    val orientation: Int,
    val portraitState: PlayerPortraitLayoutState,
    val floatingState: PlayerFloatingLayoutState,
    val playerVideoRatio: Float,
    val anchorBounds: Rect? = null,
) {
    val displayMode: PlayerDisplayMode
        get() = when {
            !showPlayer -> PlayerDisplayMode.Hidden
            fullScreenPlayer -> PlayerDisplayMode.Fullscreen
            // 手机（紧凑窗口）竖屏统一使用顶部内嵌槽播放器：
            // 播放器画面固定占屏幕顶部，页面内容从画面下方布局滚动，
            // 页面返回动画只作用于播放器下方的内容区（与其它页面一致）。
            // 锚点覆盖（AnchorOverlay）仅用于平板/桌面等宽屏布局。
            orientation == ORIENTATION_PORTRAIT -> PlayerDisplayMode.EmbeddedPortrait
            anchorBounds != null -> PlayerDisplayMode.AnchorOverlay
            orientation == ORIENTATION_LANDSCAPE -> PlayerDisplayMode.FloatingLandscape
            else -> PlayerDisplayMode.Hidden
        }
}
