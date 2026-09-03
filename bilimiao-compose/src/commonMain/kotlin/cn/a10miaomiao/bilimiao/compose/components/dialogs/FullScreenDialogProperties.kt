package cn.a10miaomiao.bilimiao.compose.components.dialogs

import androidx.compose.ui.window.DialogProperties

/**
 * 弹窗窗口属性：允许弹窗内容延伸到系统栏区域，
 * 使弹窗背景阴影/遮罩能完整覆盖全屏（状态栏与导航栏区域）。
 * Android 通过 decorFitsSystemWindows=false 实现；桌面端无系统栏概念，返回默认属性。
 */
expect val FullScreenDialogProperties: DialogProperties

/**
 * AnyPopDialog 使用的全屏弹窗窗口属性。
 * 除返回键行为外参数固定：不响应点击外部关闭、使用自定义宽度、延伸到系统栏区域。
 */
expect fun fullScreenDialogProperties(
    dismissOnBackPress: Boolean,
): DialogProperties
