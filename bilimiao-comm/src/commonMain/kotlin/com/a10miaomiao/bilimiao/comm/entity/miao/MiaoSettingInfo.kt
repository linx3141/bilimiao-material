package com.a10miaomiao.bilimiao.comm.entity.miao

import kotlinx.serialization.Serializable


/**
 * {“name”: "donate", "type": "pref", "title": "捐助",
 * "summary": "我在这里哦。",
 * "url": "alipayqr://platformapi/startapp?saId=10000007&qrcode=https://qr.alipay.com/FKX07587MLQPOBBKACENE1",
 * "backupUrl": "https://qr.alipay.com/FKX07587MLQPOBBKACENE1"
 * }
 * {“name”: "help", "type": "pref", "title": "帮助",
 * "summary": "世界太大，只能不停寻找。",
 * "url": "https://10miaomiao.cn/bilimiao/help.html"
 * }
 */
/**
 * 设置菜单
 */
@Serializable
data class MiaoSettingInfo(
    val type: String,
    val name: String,
    val title: String,
    val summary: String,
    val url: String,
    val backupUrl: String? = null,
)

/**
 * 本 fork 对服务端下发的设置菜单做的本地定制：
 * - 赞助/捐助类入口固定指向爱发电主页 [SPONSOR_URL]（替换服务端默认收款链接）；
 * - 移除 QQ 频道交流等推广/群聊入口。
 */
const val SPONSOR_URL = "https://ifdian.net/a/linx3141"

fun List<MiaoSettingInfo>.normalizeMiaoSettingList(): List<MiaoSettingInfo> =
    mapNotNull { item ->
        // 删除 QQ 频道/交流群等推广入口
        if (item.title.contains("QQ") || item.url.contains("qq.com")) {
            null
        } else if (
            // 赞助类入口：统一替换为爱发电主页
            item.title.contains("赞助") ||
            item.title.contains("捐助") ||
            item.title.contains("打赏") ||
            item.url.contains("afdian") ||
            item.url.contains("ifdian") ||
            item.url.contains("alipayqr") ||
            item.url.contains("qr.alipay")
        ) {
            item.copy(url = SPONSOR_URL)
        } else {
            item
        }
    }