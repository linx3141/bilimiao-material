package com.a10miaomiao.bilimiao.comm.store

import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.datastore.appDataStore
import com.a10miaomiao.bilimiao.comm.platform.getMaterialYouColor
import com.a10miaomiao.bilimiao.comm.platform.setDarkMode
import com.a10miaomiao.bilimiao.comm.store.base.BaseStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kodein.di.DI

class AppStore(override val di: DI) :
    ViewModel(), BaseStore<AppStore.State> {

    data class ThemeSettingState (
        val color: Int,
        val type: Int = SettingConstants.THEME_TYPE_DYNAMIC_COLOR,
        val darkMode: Int = 0,
        val paletteStyle: String = "TONAL_SPOT",
        val colorSpec: String = "SPEC_2025",
        val enableNavigationBadge: Boolean = true,
        val enablePredictiveBack: Boolean = true,
        val pageScale: Float = 1f,
    )

    data class HomeSettingState (
        val showPopular: Boolean = true,
        val showRecommend: Boolean = true,
        val entryView: Int = SettingConstants.HOME_ENTRY_VIEW_DEFAULT,
    )

    data class State (
        var theme: ThemeSettingState? = null,
        var home: HomeSettingState = HomeSettingState(),
        var isLockScreenOrientationPortrait: Boolean = false,
    )

    override val stateFlow = MutableStateFlow(State())
    override fun copyState() = state.copy()

    override fun init() {
        super.init()
        viewModelScope.launch {
            val prefs = appDataStore.data.first()
            val themeType = prefs[SettingPreferences.ThemeType]
                ?: SettingConstants.THEME_TYPE_DYNAMIC_COLOR
            val themeColor = if (themeType == SettingConstants.THEME_TYPE_DYNAMIC_COLOR) {
                getMaterialYouColor()
            } else {
                (prefs[SettingPreferences.ThemeColor] ?: 0xFFFB7299).toInt()
            }
            setState {
                home = HomeSettingState(
                    showPopular = prefs[SettingPreferences.HomePopularShow] ?: true,
                    showRecommend = prefs[SettingPreferences.HomeRecommendShow] ?: true,
                    entryView = prefs[SettingPreferences.HomeEntryView] ?: SettingConstants.HOME_ENTRY_VIEW_DEFAULT
                )
                theme = ThemeSettingState(
                    color = themeColor,
                    type = themeType,
                    darkMode = prefs[SettingPreferences.ThemeDarkMode] ?: 0,
                    paletteStyle = prefs[SettingPreferences.ThemePaletteStyle] ?: "TONAL_SPOT",
                    colorSpec = prefs[SettingPreferences.ThemeColorSpec] ?: "SPEC_2025",
                    enableNavigationBadge = prefs[SettingPreferences.EnableNavigationBadge] ?: true,
                    enablePredictiveBack = prefs[SettingPreferences.EnablePredictiveBack] ?: true,
                    pageScale = prefs[SettingPreferences.PageScale] ?: 1f,
                )
                isLockScreenOrientationPortrait = prefs[SettingPreferences.IsLockScreenOrientationPortrait] ?: false
            }
        }
    }

    fun setDarkMode(mode: Int) {
        setState { theme = (theme ?: ThemeSettingState(color = 0xFFFB7299.toInt())).copy(darkMode = mode) }
        viewModelScope.launch {
            appDataStore.edit {
                it[SettingPreferences.ThemeDarkMode] = mode
            }
        }
        // 深色模式由 Compose 主题驱动（MainActivity 监听 appStore.theme 更新 isDarkTheme），
        // 不再调用 AppCompatDelegate.setDefaultNightMode，避免切换时 Activity relaunch
        // 导致返回首页/导航栈重置
    }
    fun setThemeColor(color: Long, type: Int) {
        // Material You 主题传入的是标记值 0x100000000，不能直接作为种子色，
        // 需要取当前系统动态颜色，保证运行时切换立即生效（与 init() 加载逻辑一致）
        val themeColor = if (type == SettingConstants.THEME_TYPE_DYNAMIC_COLOR) {
            getMaterialYouColor()
        } else {
            color.toInt()
        }
        setState { theme = (theme ?: ThemeSettingState(color = themeColor)).copy(color = themeColor, type = type) }
        viewModelScope.launch {
            appDataStore.edit {
                it[SettingPreferences.ThemeColor] = color
                it[SettingPreferences.ThemeType] = type
            }
        }
    }

    fun setThemeStyle(paletteStyle: String, colorSpec: String) {
        setState {
            theme = (theme ?: ThemeSettingState(color = 0xFFFB7299.toInt()))
                .copy(paletteStyle = paletteStyle, colorSpec = colorSpec)
        }
        viewModelScope.launch {
            appDataStore.edit {
                it[SettingPreferences.ThemePaletteStyle] = paletteStyle
                it[SettingPreferences.ThemeColorSpec] = colorSpec
            }
        }
    }

    fun setNavigationBadge(enable: Boolean) {
        setState {
            theme = (theme ?: ThemeSettingState(color = 0xFFFB7299.toInt()))
                .copy(enableNavigationBadge = enable)
        }
        viewModelScope.launch {
            appDataStore.edit {
                it[SettingPreferences.EnableNavigationBadge] = enable
            }
        }
    }

    fun setPredictiveBack(enable: Boolean, onApplied: () -> Unit = {}) {
        setState {
            theme = (theme ?: ThemeSettingState(color = 0xFFFB7299.toInt()))
                .copy(enablePredictiveBack = enable)
        }
        viewModelScope.launch {
            appDataStore.edit {
                it[SettingPreferences.EnablePredictiveBack] = enable
            }
            // DataStore 写入完成后回调（供重建页面即时生效前持久化状态，
            // 避免 recreate 后从 DataStore 读到旧值导致开关状态不同步）
            onApplied()
        }
    }

    fun setPageScale(scale: Float) {
        setState {
            theme = (theme ?: ThemeSettingState(color = 0xFFFB7299.toInt()))
                .copy(pageScale = scale)
        }
        viewModelScope.launch {
            appDataStore.edit {
                it[SettingPreferences.PageScale] = scale
            }
        }
    }

}
