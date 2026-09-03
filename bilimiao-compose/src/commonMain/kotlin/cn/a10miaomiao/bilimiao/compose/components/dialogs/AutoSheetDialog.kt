package cn.a10miaomiao.bilimiao.compose.components.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.common.isCompactWindow

@Composable
fun AutoSheetDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onPreDismiss: (() -> Boolean)? = null,
    content: @Composable () -> Unit
) {
    val direction = if (isCompactWindow()) {
        DirectionState.BOTTOM
    } else {
        DirectionState.NONE
    }

    AnyPopDialog(
        onDismiss = onDismiss,
        onPreDismiss = onPreDismiss,
        content = {
            val shape: Shape = if (direction == DirectionState.NONE) {
                MaterialTheme.shapes.extraLarge
            } else {
                // 底部弹窗：仅顶部大圆角（底部贴屏幕边缘）
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .safeDrawingPadding()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .then(modifier),
            ) {
                content()
            }
        },
        isActiveClose = false,
        properties = AnyPopDialogProperties(
            direction = direction,
        ),
    )
}
