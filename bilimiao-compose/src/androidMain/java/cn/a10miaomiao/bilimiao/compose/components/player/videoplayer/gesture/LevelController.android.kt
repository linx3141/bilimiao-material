package cn.a10miaomiao.bilimiao.compose.components.player.videoplayer.gesture

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import com.a10miaomiao.bilimiao.comm.delegate.player.ActivityHolder
import kotlin.math.roundToInt

/**
 * 安卓端音量等级控制器：映射到系统媒体音量（STREAM_MUSIC）。
 */
actual fun createVolumeLevelController(): LevelController = object : LevelController {
    private val audioManager: AudioManager?
        get() = ActivityHolder.get()
            ?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val maxVolume: Int
        get() = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1

    override val level: Float
        get() {
            val am = audioManager ?: return 0f
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max <= 0) return 0f
            return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
        }

    override val range: ClosedRange<Float> = 0f..1f

    override val levelStep: Float
        get() = 1f / maxVolume

    override fun setLevel(level: Float) {
        val am = audioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (level.coerceIn(range) * max).roundToInt()
        // 不显示系统音量弹窗，数值交给播放器自己的手势浮窗反馈
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }
}

/**
 * 安卓端亮度等级控制器：调节当前 Activity 窗口亮度。
 * 窗口亮度未自定义时跟随系统亮度，读取系统亮度作为初始显示值。
 */
actual fun createBrightnessLevelController(): LevelController = object : LevelController {
    private val window: android.view.Window?
        get() = ActivityHolder.get()?.window

    override val level: Float
        get() {
            val activity = ActivityHolder.get() ?: return 0f
            val lp = activity.window.attributes
            if (lp.screenBrightness >= 0f) {
                return lp.screenBrightness.coerceIn(0f, 1f)
            }
            // 窗口跟随系统亮度：读取系统亮度作为当前显示值
            return runCatching {
                Settings.System.getInt(
                    activity.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                ) / 255f
            }.getOrDefault(0.5f)
        }

    override val range: ClosedRange<Float> = 0.01f..1f

    override val levelStep: Float = 0.01f

    override fun setLevel(level: Float) {
        val w = window ?: return
        val lp = w.attributes
        lp.screenBrightness = level.coerceIn(0.01f, 1f)
        w.attributes = lp
    }
}
