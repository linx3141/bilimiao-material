/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation3.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene

/**
 * Local provider of [AnimatedContentScope] to [NavEntry.Content].
 *
 * This does not have a default value since the AnimatedContentScope is provided at runtime by
 * AnimatedContent.
 */
val LocalNavAnimatedContentScope: ProvidableCompositionLocal<AnimatedContentScope> =
    compositionLocalOf {
        // no default, we need an AnimatedContent to get the AnimatedContentScope
        throw IllegalStateException(
            "Unexpected access to LocalNavAnimatedContentScope. You should only " +
                    "access LocalNavAnimatedContentScope inside a NavEntry passed " +
                    "to NavDisplay that is not in a OverlayScene."
        )
    }

/**
 * 预测性返回手势时是否对下层页面应用阴影（变暗）。
 * 默认全部应用；调用方可在特定场景（如底栏 Tab 平级切换）关闭。
 */
val LocalDimOnPredictiveBack: ProvidableCompositionLocal<(Scene<*>, Scene<*>) -> Boolean> =
    compositionLocalOf { { _, _ -> true } }

/**
 * 是否允许当前 [NavDisplay] 参与系统返回处理。
 * 多 Tab 常驻场景（Pager）下，非当前 Tab 的 NavDisplay 仍保持组合以保留状态，
 * 但不应响应返回键；通过该 Local 关闭其返回处理。
 */
val LocalNavDisplayBackEnabled: ProvidableCompositionLocal<Boolean> =
    compositionLocalOf { true }

/**
 * 当前场景是否正处于页面过渡动画（打开/返回/预测性返回）中被移出/位移。
 *
 * 过渡动画会逐帧改变场景内布局位置；播放器锚点（PlayerAnchorBox）等
 * “悬浮于页面之上、不应跟随页面位移”的内容可据此在动画期间冻结自身位置，
 * 动画结束后自动恢复。
 */
val LocalSceneDisplacing: ProvidableCompositionLocal<Boolean> =
    compositionLocalOf { false }
