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

/**
 * 场景过渡动画（打开/返回/预测性返回）是否正在进行（任意场景）。
 *
 * 过渡期间，被“唤醒”来渲染预览/目标内容的场景组合（如预测性返回时下层页面
 * 会临时重新进入组合）只是动画参与方，不应产生真实的页面注册/注销副作用
 * （如详情页 aid 注册被预览页覆盖导致返回逻辑错乱），据此跳过。
 */
val LocalSceneTransitioning: ProvidableCompositionLocal<Boolean> =
    compositionLocalOf { false }
