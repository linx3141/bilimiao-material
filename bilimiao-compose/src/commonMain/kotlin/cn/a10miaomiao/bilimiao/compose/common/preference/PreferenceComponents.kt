package cn.a10miaomiao.bilimiao.compose.common.preference

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhanghai.compose.preference.listPreference as zhListPreference
import cn.a10miaomiao.bilimiao.compose.components.dialogs.FullScreenDialogProperties

// 桥接适配器：将本项目的 Preferences 适配为 me.zhanghai.compose.preference.Preferences
private class ZHPrefsAdapter(
    val common: Preferences,
) : me.zhanghai.compose.preference.Preferences {
    override fun <T> get(key: String): T? = common.get(key)
    override fun asMap(): Map<String, Any> = common.asMap()
    override fun toMutablePreferences(): me.zhanghai.compose.preference.MutablePreferences {
        val commonMutable = common.toMutablePreferences()
        return ZHMutablePrefsAdapter(commonMutable)
    }
}

private class ZHMutablePrefsAdapter(
    val common: MutablePreferences,
) : me.zhanghai.compose.preference.MutablePreferences {
    override fun <T> get(key: String): T? = common.get(key)
    override fun asMap(): Map<String, Any> = common.asMap()
    override fun toMutablePreferences(): me.zhanghai.compose.preference.MutablePreferences =
        ZHMutablePrefsAdapter(common.toMutablePreferences())
    override fun <T> set(key: String, value: T?) = common.set(key, value)
    override fun clear() = common.clear()
}

/**
 * 将本项目的 [MutableStateFlow]<[Preferences]> 转换为库的
 * [MutableStateFlow]<[me.zhanghai.compose.preference.Preferences]>。
 */
@Composable
private fun rememberZHPrefsFlow(
    commonFlow: MutableStateFlow<Preferences>,
): MutableStateFlow<me.zhanghai.compose.preference.Preferences> {
    val zhFlow = remember {
        MutableStateFlow<me.zhanghai.compose.preference.Preferences>(
            ZHPrefsAdapter(commonFlow.value)
        )
    }
    LaunchedEffect(commonFlow) {
        // 项目层 -> zhanghai 层：外部更新（DataStore 读取结果等）同步给偏好组件
        commonFlow.collect {
            zhFlow.value = ZHPrefsAdapter(it)
        }
    }
    LaunchedEffect(commonFlow) {
        // zhanghai 层 -> 项目层：rememberPreferenceState 等组件的写入同步回项目层，
        // 使 DataStorePreferenceFlow 的写回逻辑能拿到可变的 Preferences 并持久化
        zhFlow.collect { zh ->
            val common = when (zh) {
                is ZHPrefsAdapter -> zh.common
                is ZHMutablePrefsAdapter -> zh.common
                else -> null
            }
            if (common is MutableDataStorePreferences) {
                commonFlow.value = common
            }
        }
    }
    return zhFlow
}

@Composable
fun ProvidePreferenceLocals(
    flow: MutableStateFlow<Preferences>? = null,
    content: @Composable () -> Unit,
) {
    if (flow != null) {
        val zhFlow = rememberZHPrefsFlow(flow)
        me.zhanghai.compose.preference.ProvidePreferenceLocals(
            flow = zhFlow,
            content = content,
        )
    } else {
        me.zhanghai.compose.preference.ProvidePreferenceLocals(
            content = content,
        )
    }
}

@Composable
fun <T> rememberPreferenceState(
    key: String,
    defaultValue: T,
): MutableState<T> {
    return me.zhanghai.compose.preference.rememberPreferenceState(key, defaultValue)
}

enum class ListPreferenceType {
    ALERT_DIALOG,
    DROPDOWN_MENU,
}

fun LazyListScope.preferenceCategory(
    key: String,
    title: @Composable () -> Unit,
) {
    item(key = key, contentType = "preferenceCategory") {
        Box(
            modifier = Modifier.padding(
                start = 20.dp,
                end = 16.dp,
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
}

fun LazyListScope.switchPreference(
    key: String,
    defaultValue: Boolean,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    summary: @Composable ((Boolean) -> Unit)? = null,
    enabled: () -> Boolean = { true },
) {
    item(key = key, contentType = "switchPreference") {
        val checked = rememberPreferenceState(key, defaultValue)
        val isEnabled = enabled()
        ExpressivePreferenceItem(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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

fun LazyListScope.preference(
    key: String,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    summary: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    item(key = key, contentType = "preference") {
        ExpressivePreferenceItem(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            title = title,
            summary = summary,
            icon = icon,
            enabled = enabled,
            onClick = onClick,
        )
    }
}

fun LazyListScope.sliderPreference(
    key: String,
    defaultValue: Float,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    valueRange: ClosedRange<Float> = 0f..1f,
    valueSteps: Int = 0,
    enabled: () -> Boolean = { true },
    summary: @Composable ((Float) -> Unit)? = null,
    valueText: @Composable ((Float) -> Unit)? = null,
) {
    item(key = key, contentType = "sliderPreference") {
        val value = rememberPreferenceState(key, defaultValue)
        val isEnabled = enabled()
        ExpressiveSliderItem(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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

fun <T> LazyListScope.listPreference(
    key: String,
    defaultValue: T,
    modifier: Modifier = Modifier,
    type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU,
    title: @Composable () -> Unit,
    summary: @Composable ((T) -> Unit)? = null,
    values: List<T>,
    valueToText: (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
) {
    zhListPreference(
        key = key,
        defaultValue = defaultValue,
        modifier = modifier,
        type = when (type) {
            ListPreferenceType.ALERT_DIALOG -> me.zhanghai.compose.preference.ListPreferenceType.ALERT_DIALOG
            ListPreferenceType.DROPDOWN_MENU -> me.zhanghai.compose.preference.ListPreferenceType.DROPDOWN_MENU
        },
        title = { title() },
        summary = summary,
        values = values,
        valueToText = { valueToText(it) },
    )
}

// Composable preference components used by custom preference files
@Composable
fun Preference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    summary: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    ExpressivePreferenceItem(
        modifier = modifier,
        title = title,
        summary = summary,
        icon = icon,
        onClick = onClick,
    )
}

@Composable
fun SliderPreference(
    value: Float,
    onValueChange: (Float) -> Unit,
    sliderValue: Float,
    onSliderValueChange: (Float) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedRange<Float> = 0f..1f,
    valueSteps: Int = 0,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    valueText: @Composable ((Float) -> Unit)? = null,
) {
    ExpressiveSliderItem(
        modifier = modifier,
        title = title,
        icon = icon,
        summary = summary,
        valueText = valueText?.let { textLambda -> { textLambda(sliderValue) } },
        value = sliderValue,
        onValueChange = onSliderValueChange,
        onValueChangeFinished = { onValueChange(sliderValue) },
        valueRange = valueRange,
        valueSteps = valueSteps,
        enabled = enabled,
    )
}

@Composable
fun MultiSelectListPreference(
    value: Set<Any>,
    onValueChange: (Set<Any>) -> Unit,
    values: List<Any>,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    valueToText: (Any) -> AnnotatedString = { AnnotatedString(it.toString()) },
) {
    var showDialog by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(value) }

    ExpressivePreferenceItem(
        modifier = modifier,
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        onClick = {
            selected = value
            showDialog = true
        },
        trailing = {
            Text(
                text = value.map { valueToText(it) }.joinToString("、") { it.text },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(0.45f),
                maxLines = 1,
            )
        },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { title() },
            text = {
                Column {
                    values.forEach { itemValue ->
                        val isSelected = itemValue in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = isSelected,
                                    onValueChange = { checked ->
                                        selected = if (checked) {
                                            selected + itemValue
                                        } else {
                                            selected - itemValue
                                        }
                                    },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(valueToText(itemValue))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(selected)
                        showDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("取消")
                }
            },
            properties = FullScreenDialogProperties,
        )
    }
}
