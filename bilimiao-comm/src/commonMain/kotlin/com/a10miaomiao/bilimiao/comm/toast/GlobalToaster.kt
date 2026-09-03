package com.a10miaomiao.bilimiao.comm.toast

import com.dokar.sonner.TextToastAction
import com.dokar.sonner.ToasterDefaults
import com.dokar.sonner.ToasterState
import kotlin.time.Duration

/**
 * 平台 Toast 实现：安卓端使用系统 Toast；桌面端无系统 Toast 时返回 false，
 * 由 [GlobalToaster] 回退到 sonner。
 */
expect fun platformShowToast(message: String, isLong: Boolean): Boolean

object GlobalToaster {
    private var _state: ToasterState? = null

    fun init(state: ToasterState) {
        _state = state
    }

    fun show(
        message: String,
        duration: Duration = ToasterDefaults.DurationShort,
    ) {
        if (!platformShowToast(message, duration == ToasterDefaults.DurationLong)) {
            _state?.show(message = message, duration = duration)
        }
    }

    fun showLong(
        message: String,
    ) {
        if (!platformShowToast(message, true)) {
            _state?.show(message = message, duration = ToasterDefaults.DurationLong)
        }
    }

    fun showWithAction(
        message: String,
        actionLabel: String,
        duration: Duration = ToasterDefaults.DurationLong,
        onAction: () -> Unit,
    ) {
        // 安卓系统 Toast 不支持操作按钮，仅显示文本；桌面端保留按钮
        if (!platformShowToast(message, duration == ToasterDefaults.DurationLong)) {
            _state?.show(
                message = message,
                duration = duration,
                action = TextToastAction(text = actionLabel) { onAction() },
            )
        }
    }
}
