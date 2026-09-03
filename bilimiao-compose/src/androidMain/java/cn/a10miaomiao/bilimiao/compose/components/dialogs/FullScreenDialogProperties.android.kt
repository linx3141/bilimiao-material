package cn.a10miaomiao.bilimiao.compose.components.dialogs

import androidx.compose.ui.window.DialogProperties

actual val FullScreenDialogProperties = DialogProperties(
    decorFitsSystemWindows = false,
)

actual fun fullScreenDialogProperties(
    dismissOnBackPress: Boolean,
): DialogProperties = DialogProperties(
    dismissOnBackPress = dismissOnBackPress,
    dismissOnClickOutside = false,
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)
