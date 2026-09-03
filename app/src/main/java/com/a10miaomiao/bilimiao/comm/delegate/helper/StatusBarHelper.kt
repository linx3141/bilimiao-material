package com.a10miaomiao.bilimiao.comm.delegate.helper

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager

class StatusBarHelper(
    val activity: Activity,
) {

    var isShowNavigation = true
        set(value) {
            field = value
            update()
        }
    var isShowStatus = true
        set(value) {
            field = value
            update()
        }
    var isLightStatusBar = true
        set(value) {
            field = value
            update()
        }

    init {
        // 全透明状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window.run {
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                statusBarColor = Color.TRANSPARENT
                navigationBarColor = Color.TRANSPARENT
            }
        }
        // AOSP edge-to-edge：移除导航栏/状态栏的对比色 scrim（系统叠加的阴影）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.window.isNavigationBarContrastEnforced = false
            activity.window.isStatusBarContrastEnforced = false
        }
        // 横屏时布局延伸到摄像头打孔区域，避免出现灰色条（摄像头挡不住什么）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity.window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    fun update () {
        var uiFlags = if (isShowStatus) {
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        } else {
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
        uiFlags = uiFlags or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (!isShowNavigation) {
            uiFlags = uiFlags or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            // 仅全屏（如播放器）时使用粘性沉浸，避免默认状态导航条被系统 scrim 覆盖
            uiFlags = uiFlags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        // 根据当前实际生效的主题判断深色模式（跟随系统时 configuration.uiMode 反映系统状态），
        // 深色主题下状态栏前景色固定为白色
        if (isLightStatusBar && !isSystemInDark()) {
            uiFlags = uiFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            uiFlags = uiFlags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        activity.window.decorView.systemUiVisibility = uiFlags
    }

    /**
     * 当前是否处于深色主题（与 ThemeDelegate.isSystemInDark 一致）
     */
    private fun isSystemInDark (): Boolean {
        val uiMode = activity.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    fun getStatusBarHeight (): Int {
        var statusBarHeight = 0
        //获取status_bar_height资源的ID
        val resourceId: Int = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            //根据资源ID获取响应的尺寸值
            statusBarHeight = activity.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

}
