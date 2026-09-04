package cn.a10miaomiao.bilimiao.compose.components.player.videoplayer.gesture

/**
 * 桌面端暂无系统音量/亮度调节能力：保持 no-op（与上游一致）。
 * 后续如需支持可在 JVM 平台接入系统音量控制。
 */
actual fun createVolumeLevelController(): LevelController = NoOpLevelController

actual fun createBrightnessLevelController(): LevelController = NoOpLevelController
