package com.a10miaomiao.bilimiao.comm.mypage

import androidx.compose.ui.graphics.vector.ImageVector

data class MenuItemPropInfo (
    var key: Int? = null,
    var action: String? = null,
    var title: String? = null,
    var subTitle: String? = null,
    var iconVector: ImageVector? = null,
    var visibility: Int = 0,
    /** 操作项本身的激活状态（如已关注、菜单展开等），用于底栏选中态显示 */
    var selected: Boolean = false,
    var childMenu: MyPageMenu? = null,
    var contentDescription: String? = null,
) {

}
