package cn.a10miaomiao.bilimiao.compose.pages.time

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.pages.time.components.*
import cn.a10miaomiao.bilimiao.compose.components.dialogs.AutoSheetDialog
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import com.a10miaomiao.bilimiao.comm.store.TimeSettingStore
import com.a10miaomiao.bilimiao.comm.store.model.DateModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

@Serializable
class TimeSettingPage : ComposePage {

    @Composable
    override fun Content() {
        val viewModel = diViewModel { TimeSettingViewMode(it) }
        TimeSettingPageContent(viewModel)
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimeSettingPageContent(
    viewModel: TimeSettingViewMode,
) {
    PageConfig(title = "时光姬-时间线设置")

    val windowInsets = localContentInsets()

    val cardIndex = viewModel.cardIndex.collectAsState().value
    val customTime by viewModel.customTime.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(windowInsets.topDp.dp))

            // M3E 单列分段列表：两张连体卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                // 卡片1：按月份选择（点击展开年月选择器）
                CompositionLocalProvider(
                    LocalListItemShapes provides segmentedItemShapes(0, 2),
                ) {
                    val shapes = LocalListItemShapes.current
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes?.shape ?: RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceBright,
                        onClick = { viewModel.setCurrentCardAsMonth(true) },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                text = "按月份选择",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (cardIndex == TimeSettingStore.TIME_TYPE_MONTH) {
                                MonthPicker(viewModel)
                            }
                        }
                    }
                }
                // 卡片2：自定义时间（标题直接显示时间范围，点击弹出设置弹窗）
                CompositionLocalProvider(
                    LocalListItemShapes provides segmentedItemShapes(1, 2),
                ) {
                    val shapes = LocalListItemShapes.current
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes?.shape ?: RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceBright,
                        onClick = {
                            viewModel.setCurrentCardAsCustom(true)
                            showDialog = true
                        },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                text = "自定义时间: " +
                                    (if (customTime.timeFrom.year != -1) {
                                        customTime.timeFrom.getValue("-")
                                    } else {
                                        "未选择"
                                    }) +
                                    " 至 " +
                                    (if (customTime.timeTo.year != -1) {
                                        customTime.timeTo.getValue("-")
                                    } else {
                                        "未选择"
                                    }),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(windowInsets.bottom + 50.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .padding(bottom = windowInsets.bottom),
            contentAlignment = Alignment.BottomStart
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::save,
            ) {
                Text(
                    text = "确定",
                    fontSize = 20.sp,
                    lineHeight = 20.sp
                )
            }
        }

    }

    if (showDialog) {
        CustomTimeDialog(
            viewModel = viewModel,
            onDismiss = { showDialog = false },
        )
    }
}

/**
 * 按月份选择：年份前后切换 + 月份网格，选中年份月份直接生效
 * （该月 1 日 至 该月最后一天）。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MonthPicker(
    viewModel: TimeSettingViewMode,
) {
    val monthTime by viewModel.monthTime.collectAsState()
    val timeFrom = monthTime.timeFrom
    val maxYear = viewModel.maxDate.year
    var year by remember(timeFrom.year) { mutableIntStateOf(timeFrom.year) }
    var month by remember(timeFrom.month) { mutableIntStateOf(timeFrom.month) }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 年份切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                enabled = year > 2009,
                onClick = { year-- },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上一年",
                )
            }
            Text(
                text = "${year}年",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(
                enabled = year < maxYear,
                onClick = { year++ },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下一年",
                )
            }
        }
        // 月份网格（4 行 x 3 列）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (row in 0..3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (col in 1..3) {
                        val m = row * 3 + col
                        if (m <= 12) {
                            val selected = m == month
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                onClick = {
                                    month = m
                                    viewModel.setMonthTime(year, m)
                                },
                            ) {
                                Text(
                                    text = "${m}月",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    textAlign = TextAlign.Center,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Text(
            text = "已选择：${timeFrom.year}年${timeFrom.month}月（" +
                "${timeFrom.getValue("-")} 至 ${monthTime.timeTo.getValue("-")}）",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/**
 * 自定义时间弹窗（参考投币弹窗）：内容两个 M3E 分段卡片分别设置起始/截止时间，
 * 最底部放"设置时间"按钮。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CustomTimeDialog(
    viewModel: TimeSettingViewMode,
    onDismiss: () -> Unit,
) {
    val customTime by viewModel.customTime.collectAsState()
    val timeFrom = customTime.timeFrom
    val timeTo = customTime.timeTo
    // null=不弹日期选择，true=设置起始时间，false=设置截止时间
    var pickingDate by remember { mutableStateOf<Boolean?>(null) }

    AutoSheetDialog(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = "自定义时间",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .fillMaxWidth(),
            )
            // 两个 M3E 分段卡片：设置起始/截止时间
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                DateSettingCard(
                    index = 0,
                    count = 2,
                    title = "设置起始时间",
                    summary = if (timeFrom.year != -1) {
                        timeFrom.getValue("-")
                    } else {
                        "未选择"
                    },
                    onClick = { pickingDate = true },
                )
                DateSettingCard(
                    index = 1,
                    count = 2,
                    title = "设置截止时间",
                    summary = if (timeTo.year != -1) {
                        timeTo.getValue("-")
                    } else {
                        "未选择"
                    },
                    onClick = { pickingDate = false },
                )
            }
            // 底部按钮（与投币弹窗一致）
            Row(
                modifier = Modifier
                    .padding(
                        vertical = 5.dp,
                        horizontal = 12.dp,
                    ),
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onClick = onDismiss,
                ) {
                    Text("设置时间")
                }
            }
        }
    }

    pickingDate?.let { isFrom ->
        val current = if (isFrom) timeFrom else timeTo
        val initialMillis = if (current.year != -1) {
            current.toLocalDate()
                .atStartOfDayIn(TimeZone.UTC)
                .epochSeconds * 1000
        } else {
            kotlin.time.Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .atStartOfDayIn(TimeZone.UTC)
                .epochSeconds * 1000
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.fromEpochMilliseconds(utcTimeMillis)
                        .toLocalDateTime(TimeZone.UTC).date
                    return date >= viewModel.minDate.toLocalDate() &&
                        date <= viewModel.maxDate.toLocalDate()
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC).date
                            val dm = DateModel().setDate(date)
                            if (isFrom) {
                                viewModel.setCustomTime(dm, timeTo)
                            } else {
                                viewModel.setCustomTime(timeFrom, dm)
                            }
                        }
                        pickingDate = null
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = null }) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
            )
        }
    }
}

/** M3E 风格日期设置卡片（弹窗内使用，分段圆角）。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DateSettingCard(
    index: Int,
    count: Int,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    CompositionLocalProvider(
        LocalListItemShapes provides segmentedItemShapes(index, count),
    ) {
        val shapes = LocalListItemShapes.current
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = shapes?.shape ?: RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceBright,
            onClick = onClick,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
