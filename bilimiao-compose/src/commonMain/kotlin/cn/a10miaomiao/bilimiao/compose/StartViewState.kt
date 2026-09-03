package cn.a10miaomiao.bilimiao.compose

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.min

class StartViewState(
    fullScreenPlayer: StateFlow<Boolean> = MutableStateFlow(false),
) {

    val playerState = PlayerState(fullScreenPlayer)

    /**
     * 当前可见的视频详情页对应的视频 aid。
     * 由 VideoDetailViewModel 在详情数据加载成功后注册、页面离开组合时注销，
     * 用于判断当前页面是否为正在播放视频的详情页（"忽略返回手势"关闭时的联动逻辑）。
     */
    private val _currentVideoPageAid = mutableStateOf<String?>(null)
    var currentVideoPageAid: String?
        get() = _currentVideoPageAid.value
        set(value) {
            _currentVideoPageAid.value = value
        }

    private val _drawerState = mutableStateOf(DRAWER_STATE_COLLAPSED)
    val drawerState get() = _drawerState.value

    private val _drawerOpen = mutableStateOf(false)
    val drawerOpen get() = _drawerOpen.value

    private val _touchStart = mutableFloatStateOf(0f)
    val touchStart get() = _touchStart.floatValue

    fun setTouchStartTop(topHeightPx: Float, windowHeightPx: Int, density: Float) {
        var topHeightDp = topHeightPx / density
        val windowHeightDp = windowHeightPx / density
        topHeightDp = min(topHeightDp - 200, windowHeightDp - 400)
        topHeightDp = max(topHeightDp, 0f)
        _touchStart.value = topHeightDp
    }

    fun openDrawer() {
        _drawerOpen.value = true
        _drawerState.value = DRAWER_STATE_EXPANDED
    }

    fun closeDrawer() {
        _drawerOpen.value = false
        _drawerState.value = DRAWER_STATE_COLLAPSED
    }

    fun setDrawerState(state: Int) {
        _drawerState.value = state
        _drawerOpen.value = state != DRAWER_STATE_COLLAPSED
    }

    fun isDrawerOpen(): Boolean {
        return _drawerOpen.value
    }

    companion object {
        const val DRAWER_STATE_DRAGGING = 1
        const val DRAWER_STATE_SETTLING = 2
        const val DRAWER_STATE_EXPANDED = 3
        const val DRAWER_STATE_COLLAPSED = 4
    }

}
