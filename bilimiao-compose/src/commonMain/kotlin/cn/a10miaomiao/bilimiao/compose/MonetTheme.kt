/*
 * 本文件改编自 KernelSU manager 的莫奈取色逻辑：
 *   https://github.com/tiann/KernelSU (manager/app/src/main/java/me/weishu/kernelsu/ui/theme/ThemeExt.kt)
 * 原作者：weishu (KernelSU 项目)
 * 依据 GNU GPL v3.0 许可证使用与修改，遵循相同的 GPL-3.0 协议。
 *
 * KernelSU: Copyright (C) 2022-2026 KernelSU 开发者
 * 本文件亦在 GPL-3.0 下发布。
 */
package cn.a10miaomiao.bilimiao.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

/** 系统动态色种子缓存（壁纸不变时结果不变，避免每次组合重新生成） */
private val dynamicSeedCache = mutableMapOf<Boolean, Color>()

/** 莫奈配色全局缓存：同一种子/明暗/风格/规范只计算一次（主题页大量按钮共享） */
private val colorSchemeCache = mutableMapOf<String, ColorScheme>()

/** OLED 模式：表面色阶全部替换为纯黑（与 KernelSU 一致） */
fun ColorScheme.amoledBackground(amoled: Boolean): ColorScheme =
    if (!amoled) this
    else copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color.Black,
        surfaceContainer = Color.Black,
        surfaceContainerHigh = Color.Black,
        surfaceContainerHighest = Color.Black,
    )

/** SPEC_2025 色彩规范仅在部分配色风格下生效（与 KernelSU 一致） */
val PaletteStyle.supportsSpec2025: Boolean
    get() = this == PaletteStyle.TonalSpot ||
            this == PaletteStyle.Neutral ||
            this == PaletteStyle.Vibrant ||
            this == PaletteStyle.Expressive

/** 当所选配色风格不支持 SPEC_2025 时回退到 SPEC_2021（与 KernelSU 一致） */
fun ColorSpec.SpecVersion.effectiveFor(style: PaletteStyle): ColorSpec.SpecVersion =
    if (this == ColorSpec.SpecVersion.SPEC_2025 && !style.supportsSpec2025) {
        ColorSpec.SpecVersion.SPEC_2021
    } else {
        this
    }

/**
 * 基于 KernelSU 的 rememberKernelSUColorScheme 改编：
 * 使用 materialkolor 生成莫奈取色配色，支持配色风格（Tonal Spot / Neutral / Vibrant / Expressive）
 * 与色彩规范（SPEC_2021 / SPEC_2025）选择。
 */
@Composable
fun rememberBilimiaoColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
): ColorScheme {
    // Color.Unspecified 表示使用系统动态色作种子（与 KernelSU 一致）
    val seed = if (seedColor == Color.Unspecified) {
        dynamicSeedCache.getOrPut(isDark) {
            rememberSystemDynamicColorScheme(isDark).primary
        }
    } else {
        seedColor
    }
    val cacheKey = "$seed-$isDark-$isAmoled-$paletteStyle-$colorSpec"
    // 与 KernelSU Material 主题完全一致的取色实现
    return remember(seed, isDark, isAmoled, paletteStyle, colorSpec) {
        colorSchemeCache.getOrPut(cacheKey) {
            dynamicColorScheme(
                seedColor = seed,
                isDark = isDark,
                isAmoled = isAmoled,
                style = paletteStyle,
                specVersion = colorSpec.effectiveFor(paletteStyle),
            ).amoledBackground(isAmoled)
        }
    }
}
