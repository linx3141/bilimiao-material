@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.common.preference

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Material 3 Expressive 风格设置组件，参考 Android 16/17 系统设置与 KernelSU Manager：
 * - 设置项以分组（Segmented）呈现：组内相邻项间距与圆角极小，仅组首/组尾使用大圆角；
 * - 开关 thumb 内带图标（开启对勾 / 关闭叉号）；
 * - 可跳转子页面的项在右侧显示箭头。
 */
internal val ExpressiveCardShape = RoundedCornerShape(24.dp)
internal val ExpressiveIconContainerShape = RoundedCornerShape(16.dp)

/**
 * 当前列表项所在分组的形状信息；由 [ExpressiveSegmentedColumn] 提供，
 * 组件内部据此渲染为分段样式，未提供时回退为独立卡片。
 */
internal val LocalListItemShapes = compositionLocalOf<ListItemShapes?> { null }

/**
 * 计算分段列表项在分组中的形状。
 * 单项分组（count == 1）使用正常大圆角，避免与相邻项同组时的小圆角。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun segmentedItemShapes(
    index: Int,
    count: Int,
): ListItemShapes {
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    return if (count == 1) {
        shapes.copy(shape = MaterialTheme.shapes.large)
    } else {
        shapes
    }
}

/**
 * 计算横向分段列表项的形状（一行内多个项连体分段）。
 *
 * 与纵向 [segmentedItemShapes] 相反：首项左侧大圆角、末项右侧大圆角、
 * 中间项小圆角，相邻项间距由 [ListItemDefaults.SegmentedGap] 控制。
 * 供用户详情页主页等"一行分段"列表使用。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun horizontalSegmentedItemShapes(
    index: Int,
    count: Int,
): ListItemShapes {
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    val large = MaterialTheme.shapes.large
    val small = RoundedCornerShape(4.dp)
    val shape = when {
        count <= 1 -> large
        index == 0 -> RoundedCornerShape(
            topStart = large.topStart,
            bottomStart = large.bottomStart,
            topEnd = small.topEnd,
            bottomEnd = small.bottomEnd,
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = small.topStart,
            bottomStart = small.bottomStart,
            topEnd = large.topEnd,
            bottomEnd = large.bottomEnd,
        )
        else -> small
    }
    return shapes.copy(shape = shape)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun defaultSegmentedListItemColors(): ListItemColors =
    ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceBright,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
        supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

/**
 * Material 3 Expressive 开关：thumb 内带状态图标。
 * 开启时显示对勾，关闭时显示叉号，颜色使用 M3 开关 token 对应角色。
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val checkedIconColor = MaterialTheme.colorScheme.primary
    val uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedIconColor = checkedIconColor,
            uncheckedIconColor = uncheckedIconColor,
        ),
        thumbContent = {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.requiredSize(SwitchDefaults.IconSize),
                    tint = checkedIconColor,
                )
            } else {
                // 未选中状态：叉号（标准大小）
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.requiredSize(SwitchDefaults.IconSize),
                    tint = uncheckedIconColor,
                )
            }
        },
    )
}

/**
 * Material 3 Expressive 设置项。
 *
 * 在 [LocalListItemShapes] 提供时渲染为分段（Segmented）列表项（组内相邻小圆角、
 * 组首尾大圆角）；否则回退为独立圆角卡片。
 *
 * @param title 标题
 * @param summary 副标题
 * @param icon 前导图标
 * @param trailing 尾部内容（开关等）；为 null 且可点击时自动显示子页面箭头
 * @param onClick 点击回调；为 null 时不可点击
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressivePreferenceItem(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    summary: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val segmentedShapes = LocalListItemShapes.current
    if (segmentedShapes != null) {
        SegmentedListItem(
            onClick = onClick ?: {},
            shapes = segmentedShapes,
            modifier = modifier,
            enabled = enabled,
            leadingContent = icon,
            supportingContent = summary,
            trailingContent = trailing ?: if (onClick != null) {
                {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
            colors = defaultSegmentedListItemColors(),
            content = title,
        )
    } else {
        val contentAlpha = if (enabled) 1f else 0.38f
        val content: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(ExpressiveIconContainerShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.graphicsLayer { alpha = contentAlpha }) {
                            icon()
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Box(Modifier.graphicsLayer { alpha = contentAlpha }) {
                        title()
                    }
                    if (summary != null) {
                        Spacer(Modifier.height(2.dp))
                        Box(Modifier.graphicsLayer { alpha = contentAlpha }) {
                            CompositionLocalProvider(
                                LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                            ) {
                                summary()
                            }
                        }
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(12.dp))
                    trailing()
                } else if (onClick != null) {
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (onClick != null) {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = ExpressiveCardShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                onClick = onClick,
                enabled = enabled,
                content = content,
            )
        } else {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = ExpressiveCardShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                content = content,
            )
        }
    }
}

/**
 * 分段（Segmented）列表容器：组内项以 [ListItemDefaults.SegmentedGap] 间隔，
 * 每项根据所在位置获得对应圆角（组首/组尾大圆角，中间项小圆角）。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExpressiveSegmentedColumn(
    modifier: Modifier = Modifier,
    entries: List<@Composable () -> Unit>,
) {
    if (entries.isEmpty()) return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        entries.forEachIndexed { index, entry ->
            val shapes = segmentedItemShapes(index, entries.size)
            CompositionLocalProvider(
                LocalListItemShapes provides shapes,
            ) {
                entry()
            }
        }
    }
}

@DslMarker
annotation class PreferenceGroupMarker

/**
 * 设置分组作用域：在 [LazyListScope.preferenceGroup] 内收集设置项。
 */
@PreferenceGroupMarker
class PreferenceGroupScope {
    internal val entries = mutableListOf<@Composable () -> Unit>()

    fun item(content: @Composable () -> Unit) {
        entries.add(content)
    }
}

/**
 * 设置分组：一个标题 + 一组连体圆角（Segmented）设置项，对齐 Android 16/17 系统设置。
 */
fun LazyListScope.preferenceGroup(
    key: String,
    title: @Composable (() -> Unit)? = null,
    content: PreferenceGroupScope.() -> Unit,
) {
    item(key = key, contentType = "preferenceGroup") {
        val scope = PreferenceGroupScope().apply(content)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            if (title != null) {
                Box(
                    modifier = Modifier.padding(
                        start = 4.dp,
                        end = 0.dp,
                        top = 20.dp,
                        bottom = 8.dp,
                    )
                ) {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.labelLarge,
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        title()
                    }
                }
            }
            ExpressiveSegmentedColumn(entries = scope.entries)
        }
    }
}

fun PreferenceGroupScope.switchPreference(
    key: String,
    defaultValue: Boolean,
    title: @Composable () -> Unit,
    summary: @Composable ((Boolean) -> Unit)? = null,
    enabled: () -> Boolean = { true },
) {
    item {
        val checked = rememberPreferenceState(key, defaultValue)
        val isEnabled = enabled()
        ExpressivePreferenceItem(
            title = title,
            summary = summary?.let { summaryLambda -> { summaryLambda(checked.value) } },
            enabled = isEnabled,
            onClick = { checked.value = !checked.value },
            trailing = {
                ExpressiveSwitch(
                    checked = checked.value,
                    onCheckedChange = null,
                    enabled = isEnabled,
                )
            },
        )
    }
}

fun PreferenceGroupScope.preference(
    key: String,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    summary: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    item {
        ExpressivePreferenceItem(
            modifier = modifier,
            title = title,
            summary = summary,
            icon = icon,
            enabled = enabled,
            onClick = onClick,
        )
    }
}

fun PreferenceGroupScope.sliderPreference(
    key: String,
    defaultValue: Float,
    title: @Composable () -> Unit,
    valueRange: ClosedRange<Float> = 0f..1f,
    valueSteps: Int = 0,
    enabled: () -> Boolean = { true },
    summary: @Composable ((Float) -> Unit)? = null,
    valueText: @Composable ((Float) -> Unit)? = null,
) {
    item {
        val value = rememberPreferenceState(key, defaultValue)
        val isEnabled = enabled()
        ExpressiveSliderItem(
            title = title,
            summary = summary?.let { summaryLambda -> { summaryLambda(value.value) } },
            valueText = valueText?.let { textLambda -> { textLambda(value.value) } },
            value = value.value,
            onValueChange = { value.value = it },
            valueRange = valueRange,
            valueSteps = valueSteps,
            enabled = isEnabled,
        )
    }
}

fun <T> PreferenceGroupScope.listPreference(
    key: String,
    defaultValue: T,
    type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU,
    title: @Composable () -> Unit,
    summary: @Composable ((T) -> Unit)? = null,
    values: List<T>,
    valueToText: (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
) {
    item {
        val value = rememberPreferenceState(key, defaultValue)
        var expanded by remember { mutableStateOf(false) }
        var anchorBounds by remember { mutableStateOf(IntRect.Zero) }
        Box {
            ExpressivePreferenceItem(
                title = title,
                summary = summary?.let { summaryLambda -> { summaryLambda(value.value) } },
                onClick = { expanded = true },
                trailing = {
                    Text(
                        text = valueToText(value.value),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier.onGloballyPositioned { coords ->
                    val rect = coords.boundsInWindow()
                    anchorBounds = IntRect(
                        left = rect.left.roundToInt(),
                        top = rect.top.roundToInt(),
                        right = rect.right.roundToInt(),
                        bottom = rect.bottom.roundToInt(),
                    )
                },
            )
            M3EDropdownMenu(
                expanded = expanded,
                onDismiss = { expanded = false },
                anchorBounds = anchorBounds,
                values = values,
                selected = value.value,
                valueToText = valueToText,
                onSelected = { value.value = it },
            )
        }
    }
}

/**
 * 滑杆设置项（分段/独立卡片自适应）。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExpressiveSliderItem(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    valueText: @Composable ((Float) -> Unit)? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedRange<Float>,
    valueSteps: Int,
    enabled: Boolean,
) {
    val segmentedShapes = LocalListItemShapes.current
    if (segmentedShapes != null) {
        SegmentedListItem(
            onClick = {},
            shapes = segmentedShapes,
            modifier = modifier,
            leadingContent = icon,
            colors = defaultSegmentedListItemColors(),
            supportingContent = summary,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            title()
                        }
                        if (valueText != null) {
                            Spacer(Modifier.width(12.dp))
                            valueText(value)
                        }
                    }
                    Slider(
                        value = value,
                        onValueChange = onValueChange,
                        onValueChangeFinished = onValueChangeFinished,
                        valueRange = valueRange.start..valueRange.endInclusive,
                        steps = valueSteps,
                        enabled = enabled,
                    )
                }
            },
        )
    } else {
        val contentAlpha = if (enabled) 1f else 0.38f
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = ExpressiveCardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(ExpressiveIconContainerShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.graphicsLayer { alpha = contentAlpha }) {
                                icon()
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Box(Modifier.graphicsLayer { alpha = contentAlpha }) {
                            title()
                        }
                        if (summary != null) {
                            Spacer(Modifier.height(2.dp))
                            Box(Modifier.graphicsLayer { alpha = contentAlpha }) {
                                CompositionLocalProvider(
                                    LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                                ) {
                                    summary()
                                }
                            }
                        }
                    }
                    if (valueText != null) {
                        Spacer(Modifier.width(12.dp))
                        valueText(value)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    valueRange = valueRange.start..valueRange.endInclusive,
                    steps = valueSteps,
                    enabled = enabled,
                )
            }
        }
    }
}
