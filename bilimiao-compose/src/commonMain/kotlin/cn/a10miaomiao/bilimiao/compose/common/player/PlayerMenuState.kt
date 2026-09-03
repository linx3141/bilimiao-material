package cn.a10miaomiao.bilimiao.compose.common.player

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntRect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放器原生 View 层与 Compose 层之间的下拉菜单桥接状态。
 *
 * 播放器控制栏（倍速/清晰度/更多）位于原生 View 中，无法直接使用 Compose 的
 * m3e 下拉菜单；由 [PlayerMenuHost]（Compose 层）观察本状态并渲染菜单，
 * 原生层只负责提交锚点位置与菜单项。
 */
@Stable
class PlayerMenuState {

    data class MenuItem(
        val title: String,
        val selected: Boolean = false,
        val enabled: Boolean = true,
        /** 子菜单：点击该项后切换为子菜单内容（如"画面比例"） */
        val children: List<MenuItem>? = null,
        /** Android drawable 资源 id，仅安卓端使用 */
        val iconRes: Int? = null,
        val onClick: (() -> Unit)? = null,
    )

    data class MenuRequest(
        val anchorBounds: IntRect,
        val items: List<MenuItem>,
    )

    private val _request = MutableStateFlow<MenuRequest?>(null)
    val request: StateFlow<MenuRequest?> = _request

    fun show(anchorBounds: IntRect, items: List<MenuItem>) {
        _request.value = MenuRequest(anchorBounds, items)
    }

    fun dismiss() {
        _request.value = null
    }
}
