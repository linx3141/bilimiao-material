package cn.a10miaomiao.bilimiao.compose.pages.search.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.store.RegionStore

data class MoreConditionsInfo(
    val timeType: Int,
    val durationList: List<Int>,
    val regionList: List<Int>
)

/**
 * 搜索综合页筛选条件状态。
 *
 * 筛选下拉菜单中的每次选择都会立即通过 [onChange] 生效并刷新搜索列表，
 * 不再使用原来的弹窗“确认/取消”流程。
 */
@Stable
internal class SearchFilterState(
    regionStore: RegionStore,
    val onChange: () -> Unit,
) {

    val timeTypeList = listOf(
        0 to "不限",
        1 to "最近一天",
        7 to "最近一周",
        180 to "最近半年",
    )
    private val _timeTypeSelected = mutableIntStateOf(0)
    val timeTypeSelected get() = _timeTypeSelected.intValue

    val durationList = listOf(
        0 to "不限",
        1 to "0-10分钟",
        2 to "10-30分钟",
        3 to "30-60分钟",
        4 to "60分钟+",
    )
    private val _durationSelectedList = mutableStateOf(listOf(0))
    val durationSelectedList get() = _durationSelectedList.value

    val regionList = listOf(
        0 to "不限",
        *regionStore.state.regions.map {
            it.tid to it.name
        }.toTypedArray()
    )
    private val _regionSelectedList = mutableStateOf(listOf(0))
    val regionSelectedList get() = _regionSelectedList.value

    val data get() = MoreConditionsInfo(
        timeType = timeTypeSelected,
        durationList = durationSelectedList,
        regionList = regionSelectedList,
    )

    fun handleSelectedTimeType(timeType: Int) {
        _timeTypeSelected.intValue = timeType
        onChange()
    }

    fun handleSelectedDuration(duration: Int) {
        if (duration == 0) {
            _durationSelectedList.value = listOf(0)
        } else if (durationSelectedList.indexOf(duration) == -1) {
            _durationSelectedList.value = listOf(
                *durationSelectedList.filter { it != 0 }.toTypedArray(), // 移除全部时长
                duration,
            )
        } else {
            _durationSelectedList.value = durationSelectedList.filter {
                it != duration
            }
            if (durationSelectedList.isEmpty()) {
                _durationSelectedList.value = listOf(0)
            }
        }
        onChange()
    }

    fun handleSelectedRegion(region: Int) {
        if (region == 0) {
            _regionSelectedList.value = listOf(0)
        } else if (regionSelectedList.indexOf(region) == -1) {
            _regionSelectedList.value = listOf(
                *regionSelectedList.filter { it != 0 }.toTypedArray(), // 移除全部分区
                region,
            )
        } else {
            _regionSelectedList.value = regionSelectedList.filter {
                it != region
            }
            if (regionSelectedList.isEmpty()) {
                _regionSelectedList.value = listOf(0)
            }
        }
        onChange()
    }
}

/**
 * 搜索综合页“筛选”下拉菜单。
 *
 * 与底栏其他选项一样使用 M3E 分组下拉菜单（参照 KernelSU manager），
 * 按“发布时间 / 内容时长 / 内容分区”分为三段，段与段之间有分组间距与
 * 独立的组标签+分割线；分区较多时菜单整体可上下滑动（参照 KSU 语言菜单）。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SearchFilterMenu(
    state: SearchFilterState,
    onDismiss: () -> Unit,
) {
    val groupCount = 3
    val checkIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
        )
    }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        // 发布时间（单选）
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 0, count = groupCount),
        ) {
            MenuDefaults.Label {
                Text("发布时间")
            }
            HorizontalDivider(
                modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
            )
            state.timeTypeList.forEachIndexed { index, (value, label) ->
                DropdownMenuItem(
                    selected = state.timeTypeSelected == value,
                    onClick = {
                        state.handleSelectedTimeType(value)
                        onDismiss()
                    },
                    text = { Text(label) },
                    shapes = MenuDefaults.itemShape(
                        index = index,
                        count = state.timeTypeList.size,
                    ),
                    selectedLeadingIcon = checkIcon,
                )
            }
        }

        Spacer(Modifier.height(MenuDefaults.GroupSpacing))

        // 内容时长（多选，保持菜单打开便于连续选择）
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 1, count = groupCount),
        ) {
            MenuDefaults.Label {
                Text("内容时长")
            }
            HorizontalDivider(
                modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
            )
            state.durationList.forEachIndexed { index, (value, label) ->
                DropdownMenuItem(
                    selected = state.durationSelectedList.indexOf(value) != -1,
                    onClick = {
                        state.handleSelectedDuration(value)
                    },
                    text = { Text(label) },
                    shapes = MenuDefaults.itemShape(
                        index = index,
                        count = state.durationList.size,
                    ),
                    selectedLeadingIcon = checkIcon,
                )
            }
        }

        Spacer(Modifier.height(MenuDefaults.GroupSpacing))

        // 内容分区（多选，保持菜单打开便于连续选择）
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 2, count = groupCount),
        ) {
            MenuDefaults.Label {
                Text("内容分区")
            }
            HorizontalDivider(
                modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
            )
            state.regionList.forEachIndexed { index, (value, label) ->
                DropdownMenuItem(
                    selected = state.regionSelectedList.indexOf(value) != -1,
                    onClick = {
                        state.handleSelectedRegion(value)
                    },
                    text = { Text(label) },
                    shapes = MenuDefaults.itemShape(
                        index = index,
                        count = state.regionList.size,
                    ),
                    selectedLeadingIcon = checkIcon,
                )
            }
        }
    }
}
