package com.a10miaomiao.bilimiao.comm.toast

/**
 * 桌面端：无系统 Toast，返回 false 由 GlobalToaster 回退到 sonner。
 */
actual fun platformShowToast(message: String, isLong: Boolean): Boolean = false
