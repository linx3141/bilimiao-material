/*
 * 预测性返回开关的应用逻辑改编自 KernelSU manager
 * （KernelSUApplication.onCreate 中对 enableOnBackInvokedCallback 的设置）：
 *   https://github.com/tiann/KernelSU
 * 原作者：weishu (KernelSU 项目)，依据 GNU GPL v3.0 许可证使用与修改。
 */
package com.a10miaomiao.bilimiao

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.datastore.appDataStore
import com.a10miaomiao.bilimiao.comm.delegate.theme.ThemeDelegate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.mikaelzero.mojito.Mojito
import net.mikaelzero.mojito.loader.glide.GlideImageLoader
import net.mikaelzero.mojito.view.sketch.SketchImageLoadFactory
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.concurrent.thread


class Bilimiao: Application() {

    companion object {
        const val APP_NAME = "bilimiao"
        lateinit var app: Bilimiao
        lateinit var commApp: BilimiaoCommApp
    }

    init {
        app = this
        commApp = BilimiaoCommApp(this)
    }

    override fun onCreate() {
        super.onCreate()
        AppCrashHandler.getInstance(this)
        commApp.onCreate()
        setDefaultNightMode()
        applyPredictiveBackSetting()
        initImageLoader()
        Mojito.initialize(
            GlideImageLoader.with(this),
            SketchImageLoadFactory()
        )
    }

    /**
     * 全局图片加载器：统一的内存/磁盘缓存，图片跨页面共享，
     * 避免每次进入列表都重新解码/下载封面。
     */
    private fun initImageLoader() {
        SingletonImageLoader.setSafe(
            SingletonImageLoader.Factory { context ->
                ImageLoader.Builder(context)
                    .memoryCache {
                        MemoryCache.Builder()
                            .maxSizePercent(context, 0.25)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(context.cacheDir.resolve("bilimiao_image_cache"))
                            .maxSizeBytes(512L * 1024 * 1024)
                            .build()
                    }
                    .build()
            }
        )
    }

    /**
     * 应用预测性返回手势开关（Android 14+）。
     * 关闭时禁用 onBackInvokedCallback，退回传统返回；开启时启用，
     * 配合系统预测性返回动画（Android 15+ 的边缘滑动返回）。
     */
    private fun applyPredictiveBackSetting() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return
        }
        // 异步读取设置，避免阻塞主线程启动（DataStore 首次读取涉及磁盘 IO）
        thread(name = "predictive-back-init") {
            val enable = runBlocking {
                appDataStore.data.first()[SettingPreferences.EnablePredictiveBack] ?: true
            }
            Handler(Looper.getMainLooper()).post {
                applyPredictiveBackCallback(enable)
            }
        }
    }

    private fun applyPredictiveBackCallback(enable: Boolean) {
        HiddenApiBypass.addHiddenApiExemptions(
            "Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback"
        )
        runCatching {
            val method = ApplicationInfo::class.java.getDeclaredMethod(
                "setEnableOnBackInvokedCallback",
                Boolean::class.javaPrimitiveType,
            )
            method.isAccessible = true
            method.invoke(applicationInfo, enable)
        }
    }

    private fun setDefaultNightMode() {
        val mode = ThemeDelegate.getNightMode(this)
        if (mode == 0) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else if (mode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else if (mode == 2) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}
