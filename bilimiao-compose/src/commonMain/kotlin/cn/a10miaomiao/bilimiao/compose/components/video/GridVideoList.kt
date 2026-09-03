package cn.a10miaomiao.bilimiao.compose.components.video

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
/**
 * 多列分段网格中某张卡片的圆角。
 * 通过判断四个方向（上/下/左/右）是否存在相邻卡片决定圆角：
 * 某角的外侧两个方向都没有相邻卡片时使用大圆角，否则小圆角。
 * 这样无论一行/多行、最后一行是否排满，圆角都正确（与分组是否独立无关）。
 *
 * @param seq 卡片在组内的序号（0 起）
 * @param groupSize 组内卡片总数
 * @param colCount 网格列数
 */
fun gridSegmentedShape(
    seq: Int,
    groupSize: Int,
    colCount: Int,
): Shape {
    val large = 24.dp
    val small = 4.dp
    val row = seq / colCount
    val col = seq % colCount
    val hasUp = row > 0 && (row - 1) * colCount + col < groupSize
    val hasDown = (row + 1) * colCount + col < groupSize
    val hasLeft = col > 0
    val hasRight = (seq + 1) < groupSize && (seq + 1) / colCount == row
    return RoundedCornerShape(
        topStart = if (!hasUp && !hasLeft) large else small,
        topEnd = if (!hasUp && !hasRight) large else small,
        bottomStart = if (!hasDown && !hasLeft) large else small,
        bottomEnd = if (!hasDown && !hasRight) large else small,
    )
}

/**
 * 按窗口宽度与最小列宽计算列数（列宽由屏幕大小/dpi 决定，与 GridCells.Adaptive 等价）。
 */
@Composable
fun rememberGridColumnCount(minColumnWidth: Dp): Int {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = windowInfo.containerSize.width / density.density
    return (widthDp / minColumnWidth.value).toInt().coerceAtLeast(1)
}
