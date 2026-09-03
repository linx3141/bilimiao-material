package com.a10miaomiao.bilimiao.comm.entity.user

import kotlinx.serialization.Serializable

/**
 * B站黑名单（已拉黑用户）列表
 * 接口：/x/relation/blacks
 */
@Serializable
data class BlackListInfo(
    val list: List<Item> = emptyList(),
    val total: Int = 0,
) {
    @Serializable
    data class Item(
        val mid: String = "",
        val uname: String = "",
        val face: String = "",
        val sign: String = "",
        val mtime: Long = 0L,
    )
}
