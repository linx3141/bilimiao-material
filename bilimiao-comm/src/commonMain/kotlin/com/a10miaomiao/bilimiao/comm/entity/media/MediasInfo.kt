package com.a10miaomiao.bilimiao.comm.entity.media

import kotlinx.serialization.Serializable

@Serializable
data class MediasInfo(
    val id: String,
    val cover: String,
    val ctime: Long,
    val duration: Long,
    val title: String,
    /** 稿件 bvid（收藏夹接口返回 bvid，供逐条补充充电判定） */
    val bvid: String = "",
    val upper: MediaUpperInfo,
    val cnt_info: CntInfo,
    val ugc: Ugc? = null,
    val ogv: Ogv? = null,
    /** 稿件属性位 */
    val attr: Int = 0,
    /** UGC 付费（充电专属） */
    val ugc_pay: Int = 0,
    /** 稿件角标（含“充电专属”） */
    val badges: List<BadgeInfo>? = null,
) {

    @Serializable
    data class BadgeInfo(
        val text: String? = null,
    )

    /** 是否充电专属视频 */
    val isChargeVideo: Boolean
        get() = ugc_pay == 1 ||
            badges?.any { it.text == "充电专属" } == true

    @Serializable
    data class CntInfo(
        val coin: Int = 0,
        val collect: Int = 0,
        val danmaku: String,
        val play: String,
        val reply: Int = 0,
        val share: Int = 0,
        val thumb_down: Int = 0,
        val thumb_up: Int = 0
    )
    @Serializable
    data class Ugc(
        val first_cid: String,
    )
    @Serializable
    data class Ogv(
        val season_id: String,
    )
}
