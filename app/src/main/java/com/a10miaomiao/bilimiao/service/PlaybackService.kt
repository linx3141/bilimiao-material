package com.a10miaomiao.bilimiao.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.os.Build
import androidx.annotation.OptIn
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.a10miaomiao.bilimiao.MainActivity
import com.a10miaomiao.bilimiao.R
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
        const val ACTION_PLAY = "cn.a10miaomiao.bilimiao.action.PLAY"
        const val ACTION_PAUSE = "cn.a10miaomiao.bilimiao.action.PAUSE"

        var instance: PlaybackService? = null
            private set

        /** 服务尚未创建时暂存的播放器（启动竞态） */
        private var pendingPlayer: ExoPlayer? = null

        /** 服务尚未创建时暂存的标题（避免丢失元数据） */
        private var pendingTitle: String? = null

        /** 服务尚未创建时暂存的封面 */
        private var pendingCover: Bitmap? = null

        /** 由 app 层在播放器就绪时调用：绑定 ExoPlayer 到媒体会话 */
        fun attachPlayer(player: ExoPlayer) {
            instance?.bindPlayer(player) ?: run { pendingPlayer = player }
        }

        /** 更新媒体标题（服务未就绪时暂存，onCreate 后应用） */
        fun updateMediaMeta(title: String) {
            val service = instance
            if (service != null) {
                service.applyMeta(title)
            } else {
                pendingTitle = title
            }
        }

        /** 更新封面图（媒体通知 largeIcon） */
        fun updateMediaCover(bitmap: Bitmap?) {
            val service = instance
            if (service != null) {
                service.applyCover(bitmap)
            } else {
                pendingCover = bitmap
            }
        }
    }

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var metadataItem: MediaItem? = null
    private var coverBitmap: Bitmap? = null

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
        pendingTitle?.let { applyMeta(it) }
        pendingTitle = null
        pendingCover?.let { applyCover(it) }
        pendingCover = null
        serviceScope.launch {
            initPlayerSetting()
        }
    }

    private val playbackListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            startForegroundMediaNotification()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            startForegroundMediaNotification()
        }
    }

    /** 绑定实际播放器：媒体会话/通知控制都转发给 [PlayerDelegateImpl] 保持一致 */
    private fun bindPlayer(player: ExoPlayer) {
        // 同一实例可能随每次 openPlayer 重复上报，避免释放正在使用的播放器
        if (exoPlayer !== player) {
            exoPlayer?.removeListener(playbackListener)
            exoPlayer?.release()
            exoPlayer = player
            player.addListener(playbackListener)
        }
        if (metadataItem == null) {
            metadataItem = buildMetaItem("bilimiao")
        }
        if (showNotification) {
            mediaSession?.player = MyForwardingPlayer(player)
            startForegroundMediaNotification()
        }
    }

    private fun buildMetaItem(title: String): MediaItem {
        val metaBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist("bilimiao")
        // 把封面作为 artwork 数据写入会话元数据，系统媒体 UI 据此显示封面
        coverBitmap?.let { bmp ->
            val bos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 88, bos)
            metaBuilder.setArtworkData(bos.toByteArray())
        }
        return MediaItem.Builder()
            .setMediaId("bilimiao")
            .setMediaMetadata(metaBuilder.build())
            .build()
    }

    /** 重设会话播放器触发一次事件，让系统刷新媒体元数据/通知 */
    private fun refreshSessionMedia() {
        val exo = exoPlayer
        if (exo != null && mediaSession?.player is MyForwardingPlayer) {
            mediaSession?.player = MyForwardingPlayer(exo)
        }
    }

    /** 更新当前媒体的标题（媒体通知展示） */
    private fun applyMeta(title: String) {
        metadataItem = buildMetaItem(title)
        startForegroundMediaNotification()
        refreshSessionMedia()
    }

    /** 更新封面图并刷新通知/会话元数据 */
    private fun applyCover(bitmap: Bitmap?) {
        coverBitmap = bitmap
        val cur = metadataItem?.mediaMetadata?.title?.toString()
        if (cur != null) {
            metadataItem = buildMetaItem(cur)
        }
        startForegroundMediaNotification()
        refreshSessionMedia()
    }

    // region 前台服务（媒体样式通知，直接由系统渲染媒体控件）

    /** 通知渠道（API 26+） */
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    "bilimiao_media",
                    "媒体播放",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun serviceIntent(action: String): PendingIntent =
        PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, PlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * 前台媒体通知：不依赖 media3 的通知发布管线，直接用 MediaStyle
     * 绑定 MediaSession token，系统据此渲染标准媒体控件（播放/暂停/进度条）。
     * 每次播放状态/标题变化都调用以刷新。
     */
    private fun startForegroundMediaNotification() {
        ensureChannel()
        val session = mediaSession
        val playing = (session?.player?.isPlaying == true)
        val meta = metadataItem?.mediaMetadata
        val title = meta?.title?.toString() ?: "bilimiao"
        // 平台媒体通知：MediaStyle 绑定平台 MediaSession token，
        // 系统渲染标准媒体控件（含进度拖动），不依赖 media3 通知管线
        @Suppress("DEPRECATION")
        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, "bilimiao_media")
        } else {
            Notification.Builder(this)
        }
        builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("正在播放")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 1,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        coverBitmap?.let { builder.setLargeIcon(it) }
        if (session != null && Build.VERSION.SDK_INT >= 21) {
            builder.setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.platformToken)
                    .setShowActionsInCompactView(0)
            )
            builder.addAction(
                if (playing) {
                    android.R.drawable.ic_media_pause
                } else {
                    android.R.drawable.ic_media_play
                },
                if (playing) "暂停" else "播放",
                serviceIntent(if (playing) ACTION_PAUSE else ACTION_PLAY)
            )
        }
        startForeground(1, builder.build())
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // 处理媒体通知按钮
        when (intent?.action) {
            ACTION_PLAY -> mediaSession?.player?.play()
            ACTION_PAUSE -> mediaSession?.player?.pause()
        }
        // 立即进入前台（媒体通知），避免 ForegroundServiceDidNotStartInTimeException
        startForegroundMediaNotification()
        return START_STICKY
    }

    // endregion

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
        exoPlayer?.removeListener(playbackListener)
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
            // 优先用缓存的元数据项向系统呈现标题；未设置时回退到
            // 播放器真实媒体项（保证媒体通知有内容、不会因空项不发布）
            return metadataItem ?: super.getCurrentMediaItem()
        }

        override fun stop() {
            playerDelegate?.closePlayer() ?: super.stop()
        }
    }

}