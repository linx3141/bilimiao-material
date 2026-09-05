package com.a10miaomiao.bilimiao.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.a10miaomiao.bilimiao.MainActivity
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.datastore.appDataStore
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerDelegate
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService(), MediaSession.Callback {

    companion object {
        var instance: PlaybackService? = null
            private set

        /** 服务尚未创建时暂存的播放器（启动竞态） */
        private var pendingPlayer: ExoPlayer? = null

        /** 由 app 层在播放器就绪时调用：绑定 ExoPlayer 到媒体会话 */
        fun attachPlayer(player: ExoPlayer) {
            instance?.bindPlayer(player) ?: run { pendingPlayer = player }
        }
    }

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var metadataItem: MediaItem? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var showNotification = true
    private var playerDelegate: BasePlayerDelegate? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeSessionAndPlayer()
        pendingPlayer?.let { bindPlayer(it) }
        pendingPlayer = null
        serviceScope.launch {
            initPlayerSetting()
        }
    }

    /** 绑定实际播放器：媒体会话/通知控制都转发给 [PlayerDelegateImpl] 保持一致 */
    private fun bindPlayer(player: ExoPlayer) {
        // 同一实例可能随每次 openPlayer 重复上报，避免释放正在使用的播放器
        if (exoPlayer !== player) {
            exoPlayer?.release()
            exoPlayer = player
        }
        if (showNotification) {
            mediaSession?.player = MyForwardingPlayer(player)
        }
    }

    /** 更新当前媒体的标题/封面（媒体通知展示） */
    fun updateMediaMeta(title: String, coverUrl: String) {
        metadataItem = MediaItem.Builder()
            .setMediaId("bilimiao")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist("bilimiao")
                    .build()
            )
            .build()
        // 通知在下一次会话事件时按 getCurrentMediaItem 刷新
    }

    private suspend fun initPlayerSetting() {
        var isInitial = true
        appDataStore.data.map {
            it[SettingPreferences.PlayerNotification] ?: true
        }.collect {
            showNotification = it
            if (isInitial) {
                isInitial = false
            } else {
                if (showNotification) {
                    exoPlayer?.let { bindPlayer(it) }
                } else {
                    mediaSession?.player = defaultExoPlayer()
                }
            }
        }
    }

    private fun initializeSessionAndPlayer() {
        val player = defaultExoPlayer()
        val intent = Intent(this, MainActivity::class.java)
        val pIntent: PendingIntent = PendingIntent.getActivity(
            this,
            1,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(this)
            .setSessionActivity(pIntent)
            .build()
        exoPlayer = player
    }

    fun setPlayerDelegate(delegate: BasePlayerDelegate) {
        playerDelegate = delegate
    }

    private fun defaultExoPlayer() = ExoPlayer.Builder(this).build()

    // The user dismissed the app from the recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {
//        val player = mediaSession?.player!!
//        if (!player.playWhenReady
//            || player.mediaItemCount == 0
//            || player.playbackState == Player.STATE_ENDED) {
//            // Stop the service if not playing, continue playing in the background
//            // otherwise.
//            stopSelf()
//        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        instance = null
        super.onDestroy()
        miaoLogger() debug "PlaybackService.onDestroy"
    }

    @OptIn(UnstableApi::class)
    inner class MyForwardingPlayer(player: Player) : ForwardingPlayer(player) {
        override fun play() {
            val delegate = playerDelegate
            if (delegate != null && !delegate.isPlaying()) {
                delegate.resume()
            } else {
                super.play()
            }
        }

        override fun pause() {
            val delegate = playerDelegate
            if (delegate != null && delegate.isPlaying()) {
                delegate.pause()
            } else {
                super.pause()
            }
        }

        override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
            val delegate = playerDelegate
            if (delegate != null) {
                delegate.seekTo(positionMs)
            } else {
                super.seekTo(mediaItemIndex, positionMs)
            }
        }

        override fun getCurrentMediaItem(): MediaItem? {
            // 用我们缓存的元数据项向系统呈现标题；播放行为不受影响
            return metadataItem ?: super.getCurrentMediaItem()
        }

        override fun stop() {
            playerDelegate?.closePlayer() ?: super.stop()
        }
    }

}