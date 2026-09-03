package cn.a10miaomiao.bilimiao.compose.pages.setting

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.rememberPreferenceFlow
import cn.a10miaomiao.bilimiao.compose.components.preference.CustomSetsPreference
import cn.a10miaomiao.bilimiao.compose.components.preference.SliderIntPreference
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.datastore.appDataStore
import com.a10miaomiao.bilimiao.comm.store.PlayerStore
import kotlinx.serialization.Serializable
import cn.a10miaomiao.bilimiao.compose.common.preference.ProvidePreferenceLocals
import cn.a10miaomiao.bilimiao.compose.common.preference.MultiSelectListPreference
import cn.a10miaomiao.bilimiao.compose.common.preference.listPreference
import cn.a10miaomiao.bilimiao.compose.common.preference.preference
import cn.a10miaomiao.bilimiao.compose.common.preference.preferenceGroup
import cn.a10miaomiao.bilimiao.compose.common.preference.rememberPreferenceState
import cn.a10miaomiao.bilimiao.compose.common.preference.switchPreference
import org.kodein.di.compose.rememberInstance
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

@Serializable
class VideoSettingPage : ComposePage {

    // 与发送弹幕弹窗保持一致：不显示标题栏（关闭按钮 + 标题）
    override val showBottomSheetTitleBar: Boolean = false

    @Composable
    override fun Content() {
        val viewModel: VideoSettingPageViewModel = diViewModel { VideoSettingPageViewModel(it) }
        VideoSettingPageContent(viewModel)
    }
}

private class VideoSettingPageViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val pageNavigation by instance<PageNavigation>()

    private val fnvalSelection = mapOf(
        SettingConstants.PLAYER_FNVAL_DASH to AnnotatedString("dash(支持4K)"),
        SettingConstants.PLAYER_FNVAL_MP4 to AnnotatedString("mp4(不支持2K及以上)"),
    )

    fun fnvalSelectionName(value: Int) = fnvalSelection[value] ?: AnnotatedString(value.toString())
    val fnvalSelectionList = fnvalSelection.keys.toList()


    private val fullModeSelection = mapOf(
        SettingConstants.PLAYER_FULL_MODE_AUTO to AnnotatedString("跟随视频"),
        SettingConstants.PLAYER_FULL_MODE_UNSPECIFIED to AnnotatedString("跟随系统"),
        SettingConstants.PLAYER_FULL_MODE_SENSOR_LANDSCAPE to AnnotatedString("横向全屏(自动)"),
        SettingConstants.PLAYER_FULL_MODE_LANDSCAPE to AnnotatedString("横向全屏(固定方向1)"),
        SettingConstants.PLAYER_FULL_MODE_REVERSE_LANDSCAPE to AnnotatedString("横向全屏(固定方向2)"),
    )

    fun fullModeSelectionName(value: Int) =
        fullModeSelection[value] ?: AnnotatedString(value.toString())

    val fullModeSelectionList = fullModeSelection.keys.toList()


    private val openModeSelection = mapOf(
        SettingConstants.PLAYER_OPEN_MODE_AUTO_PLAY to AnnotatedString("无视频播放时，自动播放"),
        SettingConstants.PLAYER_OPEN_MODE_AUTO_REPLACE to AnnotatedString("正在播放时，自动替换播放"),
        SettingConstants.PLAYER_OPEN_MODE_AUTO_REPLACE_PAUSE to AnnotatedString("暂停播放时，自动替换播放"),
        SettingConstants.PLAYER_OPEN_MODE_AUTO_REPLACE_COMPLETE to AnnotatedString("完成播放时，自动替换播放"),
        SettingConstants.PLAYER_OPEN_MODE_AUTO_CLOSE to AnnotatedString("退出详情页时，自动关闭"),
        SettingConstants.PLAYER_OPEN_MODE_AUTO_FULL_SCREEN to AnnotatedString("设备竖屏状态时，自动全屏播放"),
        SettingConstants.PLAYER_OPEN_MODE_AUTO_FULL_SCREEN_LANDSCAPE to AnnotatedString("设备横屏状态时，自动全屏播放"),
    )

    fun openModeSelectionName(value: Int) =
        openModeSelection[value] ?: AnnotatedString(value.toString())

    val openModeSelectionList = openModeSelection.keys.toList()

    private val orderSelection = mapOf(
        SettingConstants.PLAYER_ORDER_LOOP to AnnotatedString("循环播放（有勾选下列选项时为列表循环，无勾选时为单个循环）"),
        SettingConstants.PLAYER_ORDER_NEXT_P to AnnotatedString("自动下一P"),
        SettingConstants.PLAYER_ORDER_NEXT_VIDEO to AnnotatedString("自动下一个视频"),
        SettingConstants.PLAYER_ORDER_NEXT_EPISODE to AnnotatedString("自动下一集（番剧）"),
    )

    fun orderSelectionName(value: Int) = orderSelection[value] ?: AnnotatedString(value.toString())
    val orderSelectionList = orderSelection.keys.toList()

    private val bottomProgressBarShowSelection = mapOf(
        SettingConstants.PLAYER_BOTTOM_PROGRESS_BAR_SHOW_IN_SMALL
                to AnnotatedString("小屏播放时，显示底部进度条"),
        SettingConstants.PLAYER_BOTTOM_PROGRESS_BAR_SHOW_IN_FULL
                to AnnotatedString("全屏播放时，显示底部进度条"),
        SettingConstants.PLAYER_BOTTOM_PROGRESS_BAR_SHOW_IN_PIP
                to AnnotatedString("画中画(应用外小窗)模式，显示底部进度条"),
    )

    fun bottomProgressBarShowName(value: Int) = bottomProgressBarShowSelection[value]
        ?: AnnotatedString(value.toString())

    val bottomProgressBarShowSelectionList = bottomProgressBarShowSelection.keys.toList()

    fun proxyClick() {
        pageNavigation.navigate(ProxySettingPage())
    }

    fun autoStopTimerClick() {
        pageNavigation.navigate(AutoStopTimerPage())
    }

}


@Composable
private fun VideoSettingPageContent(
    viewModel: VideoSettingPageViewModel
) {
    PageConfig(
        title = "播放设置"
    )
    val windowInsets = localContentInsets()
    val playerStore: PlayerStore by rememberInstance()

    val dataStore = remember {
        appDataStore
    }

    ProvidePreferenceLocals(
        flow = rememberPreferenceFlow(dataStore)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = windowInsets.leftDp.dp,
                    end = windowInsets.rightDp.dp,
                )
        ) {
            item("top") {
                Spacer(
                    modifier = Modifier.height(windowInsets.topDp.dp)
                )
            }
            preferenceGroup(
                key = "player",
                title = {
                    Text("播放器设置")
                }
            ) {
                switchPreference(
                    key = SettingPreferences.PlayerBackground.name,
                    title = {
                        Text("后台播放")
                    },
                    summary = {
                        Text("遇到困难时，不要停下来.")
                    },
                    defaultValue = true,
                )
                switchPreference(
                    key = SettingPreferences.PlayerAudioFocus.name,
                    title = {
                        Text("占用音频焦点")
                    },
                    summary = {
                        Text("关闭后可以与其它APP同时播放")
                    },
                    defaultValue = true,
                )
                switchPreference(
                    key = SettingPreferences.PlayerIgnoreBackGesture.name,
                    title = {
                        Text("忽略返回手势")
                    },
                    summary = {
                        Text("开启后，返回手势会跳过播放器")
                    },
                    defaultValue = false,
                )
            }

            preferenceGroup(
                key = "source",
                title = {
                    Text("视频源设置")
                }
            ) {
                listPreference(
                    key = SettingPreferences.PlayerFnval.name,
                    title = {
                        Text("视频格式选择")
                    },
                    summary = {
                        Text("不能播放时，换个格式试试吧")
                    },
                    defaultValue = SettingConstants.PLAYER_FNVAL_DASH,
                    values = viewModel.fnvalSelectionList,
                    valueToText = viewModel::fnvalSelectionName
                )
                preference(
                    key = SettingPreferences.PlayerProxy.name,
                    title = {
                        Text("区域限制设置")
                    },
                    summary = {
                        Text("滴，出差卡")
                    },
                    onClick = viewModel::proxyClick
                )
            }

            preferenceGroup(
                key = "control",
                title = {
                    Text("播放控制设置")
                }
            ) {
                switchPreference(
                    key = SettingPreferences.PlayerNotification.name,
                    title = {
                        Text("显示通知栏播放器控制器")
                    },
                    summary = {
                        if (it) {
                            Text(text = "播放时才会显示")
                        } else {
                            Text(text = "这个家里已经没有你的位置啦！")
                        }
                    },
                    defaultValue = true,
                )
                item {
                    val state = rememberPreferenceState(
                        SettingPreferences.PlayerOpenMode.name,
                        SettingConstants.PLAYER_OPEN_MODE_DEFAULT,
                    )
                    val value = state.value
                    val intSet = remember(value, viewModel.openModeSelectionList) {
                        viewModel.openModeSelectionList.filter {
                            value and it == it
                        }.toSet()
                    }
                    @Suppress("UNCHECKED_CAST")
                    MultiSelectListPreference(
                        value = intSet as Set<Any>,
                        onValueChange = {
                            state.value = (it as Set<Int>).fold(0) { acc, i ->
                                acc or i
                            }
                        },
                        values = viewModel.openModeSelectionList,
                        title = {
                            Text("播放器自动控制")
                        },
                        summary = {
                            Text("打开或关闭视频详情时自动进行的操作")
                        },
                        valueToText = { viewModel.openModeSelectionName(it as Int) },
                    )
                }
                item {
                    val state = rememberPreferenceState(
                        SettingPreferences.PlayerOrder.name,
                        SettingConstants.PLAYER_ORDER_DEFAULT,
                    )
                    val value = state.value
                    val intSet = remember(value, viewModel.orderSelectionList) {
                        viewModel.orderSelectionList.filter {
                            value and it == it
                        }.toSet()
                    }
                    @Suppress("UNCHECKED_CAST")
                    MultiSelectListPreference(
                        value = intSet as Set<Any>,
                        onValueChange = {
                            state.value = (it as Set<Int>).fold(0) { acc, i ->
                                acc or i
                            }
                        },
                        values = viewModel.orderSelectionList,
                        title = {
                            Text("播放器播放顺序")
                        },
                        summary = {
                            Text("可以多个选项组合选择")
                        },
                        valueToText = { viewModel.orderSelectionName(it as Int) },
                    )
                }
                switchPreference(
                    key = SettingPreferences.PlayerOrderRandom.name,
                    title = {
                        Text("随机播放")
                    },
                    summary = {
                        Text("播放完一个视频后，随机播放下一个视频，单个视频循环时无效")
                    },
                    defaultValue = false,
                )
                listPreference(
                    key = SettingPreferences.PlayerFullMode.name,
                    title = {
                        Text("全屏播放屏幕方向")
                    },
                    summary = {
                        Text("可以在播放器长按全屏按钮召唤此选项")
                    },
                    defaultValue = SettingConstants.PLAYER_FULL_MODE_AUTO,
                    values = viewModel.fullModeSelectionList,
                    valueToText = viewModel::fullModeSelectionName
                )
                item {
                    val state = rememberPreferenceState(
                        SettingPreferences.PlayerBottomProgressBarShow.name,
                        0,
                    )
                    val value = state.value
                    val intSet = remember(value, viewModel.bottomProgressBarShowSelectionList) {
                        viewModel.bottomProgressBarShowSelectionList.filter {
                            value and it == it
                        }.toSet()
                    }
                    @Suppress("UNCHECKED_CAST")
                    MultiSelectListPreference(
                        value = intSet as Set<Any>,
                        onValueChange = {
                            state.value = (it as Set<Int>).fold(0) { acc, i ->
                                acc or i
                            }
                        },
                        values = viewModel.bottomProgressBarShowSelectionList,
                        title = {
                            Text("底部进度条显示控制")
                        },
                        valueToText = { viewModel.bottomProgressBarShowName(it as Int) },
                    )
                }
                item {
                    CustomSetsPreference(
                        state = rememberPreferenceState(
                            SettingPreferences.PlayerSpeedValues.name,
                            SettingConstants.PLAYER_SPEED_SETS,
                        ),
                        title = {
                            Text("自定义倍速菜单")
                        },
                        valueText = {
                            Text(
                                text = it + "倍速",
                                modifier = Modifier.widthIn(min = 48.dp),
                                textAlign = TextAlign.Center,
                            )
                        },
                        valueCanEdit = {
                            it !in SettingConstants.PLAYER_SPEED_SETS
                        },
                        canAdd = {
                            it.size < 10
                        }
                    )
                }
                preference(
                    key = "auto_stop_duration",
                    title = {
                        Text("播放器定时关闭")
                    },
                    summary = {
                        Text("视频播放的时长，而不是实际经过的时间")
                    },
                    onClick = viewModel::autoStopTimerClick
                )
            }

            preferenceGroup(
                key = "small",
                title = {
                    Text(text = "横屏状态小屏设置")
                }
            ) {
                switchPreference(
                    key = SettingPreferences.PlayerSmallDraggable.name,
                    title = {
                        Text(text = "小屏时整个播放器可拖拽")
                    },
                    summary = {
                        if (it) {
                            Text(text = "已启用，播放时可拖拽小屏播放器")
                        } else {
                            Text(text = "启用后，小屏状态时播放器手势无效")
                        }
                    },
                    defaultValue = false,
                )
                item {
                    SliderIntPreference(
                        state = rememberPreferenceState(
                            SettingPreferences.PlayerSmallShowArea.name,
                            480,
                        ),
                        title = {
                            Text(text = "小屏时播放面积")
                        },
                        valueRange = 150..600,
                        valueText = {
                            Text(text = it.toString())
                        }
                    )
                }
                item {
                    SliderIntPreference(
                        state = rememberPreferenceState(
                            SettingPreferences.PlayerHoldShowArea.name,
                            130,
                        ),
                        title = {
                            Text(text = "小屏挂起后播放面积")
                        },
                        valueRange = 100..300,
                        valueText = {
                            Text(text = it.toString())
                        }
                    )
                }
            }

            preferenceGroup(
                key = "subtitle",
                title = {
                    Text("字幕设置")
                }
            ) {
                switchPreference(
                    key = SettingPreferences.PlayerSubtitleShow.name,
                    title = {
                        Text("字幕显示")
                    },
                    summary = {
                        if (it) {
                            Text("字幕功能已打开")
                        } else {
                            Text("字幕功能已关闭")
                        }
                    },
                    defaultValue = true,
                )
                switchPreference(
                    key = SettingPreferences.PlayerAiSubtitleShow.name,
                    title = {
                        Text("AI字幕显示")
                    },
                    summary = {
                        Text("此AI字幕是指UP主手动生成的AI字幕，并非每个视频都有")
                    },
                    defaultValue = false,
                )
            }

            item("bottom") {
                Spacer(
                    modifier = Modifier.height(
                        windowInsets.bottom
                    )
                )
            }
        }
    }
}
