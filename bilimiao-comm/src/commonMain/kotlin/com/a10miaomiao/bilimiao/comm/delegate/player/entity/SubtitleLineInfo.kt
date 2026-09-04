package com.a10miaomiao.bilimiao.comm.delegate.player.entity

/**
 * 已解析的单条 CC 字幕（时间轴单位为毫秒，与播放位置一致）。
 */
data class SubtitleLineInfo(
    val fromMs: Long,
    val toMs: Long,
    val text: String,
)
