/*
 * 本页面 UI 改编自 KernelSU manager 的 Material 配色页面
 * （ColorPaletteScreenMaterial.kt）：预览卡片、色块选择、模式切换、
 * 配色风格与色彩规范选择。
 *   https://github.com/tiann/KernelSU
 * 原作者：weishu (KernelSU 项目)，依据 GNU GPL v3.0 许可证使用与修改。
 *
 * KernelSU: Copyright (C) 2022-2026 KernelSU 开发者
 * 本文件亦在 GPL-3.0 下发布。
 */
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.setting

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import androidx.lifecycle.ViewModel
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.components.dialogs.AutoSheetDialog
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.flow.stateMap
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.ExpressivePreferenceItem
import cn.a10miaomiao.bilimiao.compose.common.preference.ExpressiveSegmentedColumn
import cn.a10miaomiao.bilimiao.compose.common.preference.ExpressiveSwitch
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.platform.LocalPlatformContext
import cn.a10miaomiao.bilimiao.compose.pages.setting.components.ExpressiveToggleButton
import cn.a10miaomiao.bilimiao.compose.pages.setting.components.MonetColorButton
import cn.a10miaomiao.bilimiao.compose.pages.setting.components.MonetThemePreviewCard
import cn.a10miaomiao.bilimiao.compose.pages.setting.components.expressiveToggleButtonColors
import cn.a10miaomiao.bilimiao.compose.pages.setting.components.keyColorOptions
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.datastore.appDataStore
import com.a10miaomiao.bilimiao.comm.platform.getMaterialYouColor
import com.a10miaomiao.bilimiao.comm.store.AppStore
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

@Serializable
class ThemeSettingPage : ComposePage {

    @Composable
    override fun Content() {
        val viewModel: ThemeSettingPageViewModel = diViewModel { ThemeSettingPageViewModel(it) }
        ThemeSettingPageContent(viewModel)
    }

}

private class ThemeSettingPageViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val appStore by instance<AppStore>()
    private val pageNavigation by instance<PageNavigation>()

    val darkModeList = listOf(
        0 to "跟随系统",
        1 to "浅色",
        2 to "深色",
        6 to "深色OLED",
    )
    val darkModeIcon = mapOf(
        0 to Icons.Filled.Brightness4,
        1 to Icons.Filled.Brightness7,
        2 to Icons.Filled.Brightness3,
        6 to Icons.Filled.Brightness1,
    )
    val darkModeListSize get() = darkModeList.size

    val materialYouColor get() = getMaterialYouColor()

    val paletteStyleList = PaletteStyle.entries
    val colorSpecList = ColorSpec.SpecVersion.entries

    val themeState = appStore.stateFlow.stateMap {
        it.theme ?: AppStore.ThemeSettingState(
            color = 0xFFFB7299.toInt(),
        )
    }

    fun setDarkMode(mode: Int) {
        appStore.setDarkMode(mode)
    }

    fun setThemeColor(color: Long) {
        val type = when (color) {
            0x100000000 -> SettingConstants.THEME_TYPE_DYNAMIC_COLOR
            else -> SettingConstants.THEME_TYPE_DEFAULT
        }
        appStore.setThemeColor(color, type)
    }

    fun setThemeStyle(paletteStyle: String, colorSpec: String) {
        appStore.setThemeStyle(paletteStyle, colorSpec)
    }

    fun setNavigationBadge(enable: Boolean) {
        appStore.setNavigationBadge(enable)
    }

    fun setPredictiveBack(enable: Boolean, onApplied: () -> Unit = {}) {
        appStore.setPredictiveBack(enable, onApplied)
    }

    fun setPageScale(scale: Float) {
        appStore.setPageScale(scale)
    }
}

@Composable
private fun ThemeSettingPageContent(
    viewModel: ThemeSettingPageViewModel
) {
    PageConfig(
        title = "主题设置"
    )
    val windowInsets = localContentInsets()
    val themeState by viewModel.themeState.collectAsState()
    val platformContext = LocalPlatformContext.current
    var pageScale by remember(themeState.pageScale) {
        mutableFloatStateOf(themeState.pageScale)
    }

    val paletteStyle = try {
        PaletteStyle.valueOf(themeState.paletteStyle)
    } catch (_: Exception) {
        PaletteStyle.TonalSpot
    }
    val colorSpec = try {
        ColorSpec.SpecVersion.valueOf(themeState.colorSpec)
    } catch (_: Exception) {
        ColorSpec.SpecVersion.SPEC_2025
    }
    val isDark = when (themeState.darkMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val isDynamic = themeState.type == SettingConstants.THEME_TYPE_DYNAMIC_COLOR
    val keyColor = if (isDynamic) 0 else themeState.color

    Column(
        modifier = Modifier
            .padding(
                top = windowInsets.topDp.dp,
                start = windowInsets.leftDp.dp,
                end = windowInsets.rightDp.dp,
            )
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonetThemePreviewCard(
            keyColor = keyColor,
            isDark = isDark,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MonetColorButton(
                    color = Color.Unspecified,
                    isSelected = isDynamic,
                    isDark = isDark,
                    paletteStyle = paletteStyle,
                    colorSpec = colorSpec,
                    onClick = {
                        viewModel.setThemeColor(0x100000000)
                    }
                )
            }
            items(keyColorOptions) { color ->
                MonetColorButton(
                    color = Color(color),
                    isSelected = !isDynamic && themeState.color == color,
                    isDark = isDark,
                    paletteStyle = paletteStyle,
                    colorSpec = colorSpec,
                    onClick = {
                        viewModel.setThemeColor(color.toLong())
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "深色模式",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                viewModel.darkModeList.forEachIndexed { index, mode ->
                    ExpressiveToggleButton(
                        checked = themeState.darkMode == mode.first,
                        onCheckedChange = {
                            if (it) {
                                viewModel.setDarkMode(mode.first)
                            }
                        },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            viewModel.darkModeList.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.weight(1f),
                        colors = expressiveToggleButtonColors(),
                    ) {
                        Icon(
                            imageVector = viewModel.darkModeIcon.getValue(mode.first),
                            contentDescription = mode.second,
                        )
                    }
                }
            }

            ExpressiveSegmentedColumn(
                modifier = Modifier.padding(top = 4.dp),
                entries = listOf(
                    {
                        ExpressiveDropdownPreference(
                            title = "配色风格",
                            summary = paletteStyle.name,
                            icon = Icons.Rounded.Style,
                            items = viewModel.paletteStyleList.map { it.name },
                            selectedIndex = viewModel.paletteStyleList.indexOf(paletteStyle)
                                .coerceAtLeast(0),
                            onSelected = { index ->
                                viewModel.setThemeStyle(
                                    viewModel.paletteStyleList[index].name,
                                    themeState.colorSpec,
                                )
                            },
                        )
                    },
                    {
                        ExpressiveDropdownPreference(
                            title = "色彩规范",
                            summary = colorSpec.name,
                            icon = Icons.Rounded.DesignServices,
                            items = viewModel.colorSpecList.map { it.name },
                            selectedIndex = viewModel.colorSpecList.indexOf(colorSpec)
                                .coerceAtLeast(0),
                            onSelected = { index ->
                                viewModel.setThemeStyle(
                                    themeState.paletteStyle,
                                    viewModel.colorSpecList[index].name,
                                )
                            },
                        )
                    },
                    {
                        ExpressivePreferenceItem(
                            title = { Text("导航栏角标") },
                            summary = { Text("在导航栏显示未读角标") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Pin,
                                    contentDescription = null,
                                )
                            },
                            trailing = {
                                ExpressiveSwitch(
                                    checked = themeState.enableNavigationBadge,
                                    onCheckedChange = viewModel::setNavigationBadge,
                                )
                            },
                        )
                    },
                    {
                        ExpressivePreferenceItem(
                            title = { Text("预测性返回手势") },
                            summary = { Text("启用预测性返回动画") },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.MenuOpen,
                                    contentDescription = null,
                                )
                            },
                            trailing = {
                                ExpressiveSwitch(
                                    checked = themeState.enablePredictiveBack,
                                    onCheckedChange = { enable ->
                                        // 与 KernelSU 一致：DataStore 写入完成后反射设置
                                        // onBackInvokedCallback 并重建页面，使开关即时生效；
                                        // 先持久化再 recreate，避免重建后读到旧状态
                                        viewModel.setPredictiveBack(enable) {
                                            platformContext.applyPredictiveBack(enable)
                                        }
                                    },
                                )
                            },
                        )
                    },
                    {
                        ExpressivePreferenceItem(
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            text = "页面缩放",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "调整页面显示比例",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = "${(pageScale * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            summary = {
                                Slider(
                                    value = pageScale,
                                    onValueChange = { pageScale = it },
                                    onValueChangeFinished = { viewModel.setPageScale(pageScale) },
                                    valueRange = 0.8f..1.1f,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                        )
                    },
                ),
            )
        }
        // 滚动内容底部间距：随内容滚动，避免最后一项直接贴到底栏
        Spacer(modifier = Modifier.height(16.dp))
    }

}

/**
 * m3e 下拉选择设置项（仿 KernelSU 的 OverlayDropdownPreference）：
 * 点击设置项弹出 m3e 下拉菜单，选中项显示勾选标记。
 */
@Composable
private fun ExpressiveDropdownPreference(
    title: String,
    summary: String,
    icon: ImageVector,
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorBounds by remember { mutableStateOf(IntRect.Zero) }
    // 菜单 Popup 显示状态与动画（与底栏下拉菜单一致）：展开先显示再淡入缩放，
    // 收起等动画播完再移除 Popup
    var menuPopupVisible by remember { mutableStateOf(false) }
    val menuAnimatable = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    // 记录菜单内容实际高度：向上弹出定位需要内容高度，
    // 内容高度变化时触发重新定位
    var menuContentHeightPx by remember { mutableStateOf(0) }
    LaunchedEffect(expanded) {
        if (expanded) {
            menuPopupVisible = true
        } else {
            menuAnimatable.animateTo(0f, animationSpec = tween(durationMillis = 150))
            menuPopupVisible = false
        }
    }
    Box {
        ExpressivePreferenceItem(
            title = { Text(title) },
            summary = { Text(summary) },
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            },
            onClick = { expanded = true },
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
        if (menuPopupVisible) {
            val spacingPx = with(LocalDensity.current) { 8.dp.toPx().roundToInt() }
            Popup(
                onDismissRequest = { expanded = false },
                popupPositionProvider = DropdownPreferencePositionProvider(
                    anchorBounds = anchorBounds,
                    verticalSpacingPx = spacingPx,
                    contentHeightPx = menuContentHeightPx,
                ),
                properties = PopupProperties(focusable = true),
            ) {
                // 菜单宽度由内容（最长文字）决定，不占满屏幕；
                // 展开动画延迟到弹窗内容首帧布局后触发（与搜索底栏筛选菜单一致）：
                // 避免重内容菜单首帧渲染慢于动画时长导致动画不可见
                var menuOpenAnimationStarted by remember(expanded) {
                    mutableStateOf(false)
                }
                val scale = 0.8f + 0.2f * menuAnimatable.value
                val alpha = menuAnimatable.value
                Box(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .onSizeChanged { menuContentHeightPx = it.height }
                        .onGloballyPositioned {
                            if (expanded && !menuOpenAnimationStarted) {
                                menuOpenAnimationStarted = true
                                scope.launch {
                                    menuAnimatable.snapTo(0f)
                                    menuAnimatable.animateTo(
                                        1f,
                                        animationSpec = tween(durationMillis = 150),
                                    )
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            // 向上弹出：从底部缩放展开（菜单从设置项上方长出来）
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        },
                ) {
                    DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                        items.forEachIndexed { index, item ->
                            DropdownMenuItem(
                                selected = index == selectedIndex,
                                onClick = {
                                    expanded = false
                                    onSelected(index)
                                },
                                text = { Text(item) },
                                shapes = MenuDefaults.itemShape(index, items.size),
                                selectedLeadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 下拉选择菜单定位：在设置项正上方弹出（向上展开）；
 * 上方空间不足时收拢到顶部边距内，水平靠屏幕右侧（与底栏下拉菜单同款边距）。
 */
private class DropdownPreferencePositionProvider(
    private val anchorBounds: IntRect,
    private val verticalSpacingPx: Int,
    private val contentHeightPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchor = this.anchorBounds
        val horizontalMarginPx = verticalSpacingPx
        // 菜单靠屏幕右侧弹出，与屏幕右边缘保持与底栏下拉菜单同等的边距
        val x = (windowSize.width - popupContentSize.width - horizontalMarginPx)
            .coerceAtLeast(horizontalMarginPx)
        // 向上弹出：菜单底边紧贴设置项上方
        val y = (anchor.top - popupContentSize.height - verticalSpacingPx)
            .coerceAtLeast(horizontalMarginPx)
        return IntOffset(x = x, y = y)
    }
}
