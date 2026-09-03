package com.a10miaomiao.bilimiao.comm.toast

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.a10miaomiao.bilimiao.comm.platform.PlatformProviders

/**
 * 安卓端：使用系统 Toast 显示（需要在主线程调用，这里统一 post 到主线程）。
 */
actual fun platformShowToast(message: String, isLong: Boolean): Boolean {
    val context = PlatformProviders.context.platformContext as? Context ?: return false
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(
            context,
            message,
            if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
        ).show()
    }
    return true
}
