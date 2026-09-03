package cn.a10miaomiao.bilimiao.compose.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.app.Activity
import android.content.pm.ApplicationInfo
import android.os.Build
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import cn.a10miaomiao.bilimiao.cover.CoverActivity
import org.lsposed.hiddenapibypass.HiddenApiBypass

class AndroidPlatformContext(
    private val context: Context,
) : PlatformContext {
    override fun openUrl(url: String) {
        try {
            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            // Fallback to default browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    override fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "分享"))
    }

    override fun openCoverImage(aid: String) {
        CoverActivity.launch(context, aid)
    }

    override fun applyPredictiveBack(enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return
        }
        // 与 KernelSU 一致：豁免后反射设置 onBackInvokedCallback 开关，并重建页面使即时生效。
        // 调用方需保证 DataStore 已写入完成（AppStore.setPredictiveBack 的 onApplied 回调）
        runCatching {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback"
            )
            val method = ApplicationInfo::class.java.getDeclaredMethod(
                "setEnableOnBackInvokedCallback",
                Boolean::class.javaPrimitiveType,
            )
            method.isAccessible = true
            method.invoke(context.applicationInfo, enable)
        }
        (context as? Activity)?.recreate()
    }
}
