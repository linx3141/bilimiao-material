package cn.a10miaomiao.bilimiao.compose.common.emitter

import com.a10miaomiao.bilimiao.comm.entity.video.VideoCommentReplyInfo

sealed class EmitterAction {
    data class DoubleClickTab(
        val tab: String,
    ): EmitterAction()

    /** 投币完成 */
    data class CoinChanged(
        val num: Int,
    ): EmitterAction()

    /** 收藏状态变化（0=取消收藏，1=已收藏） */
    data class FavoriteChanged(
        val state: Int,
    ): EmitterAction()

    /** 回复发送成功 */
    data class ReplyAdded(
        val reply: VideoCommentReplyInfo,
    ): EmitterAction()

    /** 收藏夹内容发生变化（详情页操作后通知列表刷新） */
    data class MediaListChanged(
        val type: String,
    ): EmitterAction()
}
