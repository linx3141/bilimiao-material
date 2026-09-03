// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// desktop 平台实现：桌面端无多窗口/圆角信息
package androidx.navigation3.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
actual fun isInMultiWindowMode(): Boolean = false

@Composable
actual fun getRoundedCorner(): Dp = 0.dp
