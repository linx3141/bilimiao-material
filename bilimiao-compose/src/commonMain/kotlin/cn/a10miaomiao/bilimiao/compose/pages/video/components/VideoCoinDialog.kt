@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cn.a10miaomiao.bilimiao.compose.pages.video.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.common.foundation.annotatedText
import cn.a10miaomiao.bilimiao.compose.components.dialogs.AutoSheetDialog
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.pages.download.DownloadBangumiCreatePageViewModel.QualityInfo
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
class VideoCoinDialogState(
    val scope: CoroutineScope,
    val onChanged: (Int) -> Unit,
) {

    private var aid = ""

    private val _visible = mutableStateOf(false)
    val visible: Boolean get() = _visible.value

    private val _loading = mutableStateOf(false)
    val loading: Boolean get() = _loading.value

    private val _coinNum = mutableStateOf(0)
    val coinNum: Int get() = _coinNum.value

    private val _maxCoinNum = mutableStateOf(0)
    val maxCoinNum: Int get() = _maxCoinNum.value

    val snackbar = SnackbarHostState()

    fun requestCoin(num: Int) = scope.launch(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                _loading.value = true
            }
            val res = BiliApiService.videoAPI
                .coin(aid, num)
                .awaitCall()
                .json<MessageInfo>()
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    GlobalToaster.show("感谢投币")
                    dismiss()
                    onChanged(num)
                } else {
                    snackbar.showSnackbar(res.message)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                snackbar.showSnackbar(e.message ?: e.toString())
            }
        } finally {
            withContext(Dispatchers.Main) {
                _loading.value = false
            }
        }
    }

    fun confirmCoin() {
        requestCoin(coinNum)
    }

    fun setCoinNum(num: Int) {
        _coinNum.value = num
    }

    fun show(videoAid: String, copyright: Int) {
        aid = videoAid
        _maxCoinNum.value = if (copyright == 2) 1 else 2
        _coinNum.value = maxCoinNum
        _visible.value = true
    }

    fun dismiss() {
        _visible.value = false
    }
}

@Composable
fun VideoCoinRadioButton(
    modifier: Modifier = Modifier,
    num: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ElevatedAssistChip(
        onClick = onClick,
        modifier = modifier,
        label = {
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("给UP主投上 ")
                        withStyle(
                            style = SpanStyle(
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        ) {
                            append(num.toString())
                        }
                        append(" 枚硬币")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        leadingIcon = {
            RadioButton(
                selected = selected,
                onClick = onClick // null recommended for accessibility with screen readers
            )
        }
    )
}

@Composable
fun VideoCoinDialog(
    state: VideoCoinDialogState
) {
    if (state.visible) {
        AutoSheetDialog(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp),
            content = {
                // 弹窗高度由内容决定，不做全屏/固定高度
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = "请选择投币",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .fillMaxWidth()
                    )
                    // 投币选项：m3e 单列分段列表
                    val options = if (state.maxCoinNum > 1) listOf(1, 2) else listOf(1)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                    ) {
                        options.forEachIndexed { index, num ->
                            CompositionLocalProvider(
                                LocalListItemShapes provides segmentedItemShapes(
                                    index,
                                    options.size,
                                ),
                            ) {
                                val shapes = LocalListItemShapes.current
                                val selected = state.coinNum == num
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    shape = shapes?.shape ?: RoundedCornerShape(20.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceBright
                                    },
                                    onClick = { state.setCoinNum(num) },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "给up主投上${num}枚硬币",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    SnackbarHost(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        hostState = state.snackbar,
                    )
                    Row(
                        modifier = Modifier
                            .padding(
                                vertical = 5.dp,
                                horizontal = 12.dp
                            )
                    ) {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            onClick = state::confirmCoin,
                            enabled = !state.loading
                        ) {
                            Row {
                                if (state.loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(end = 5.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                                Text(text = "确定")
                            }
                        }
                    }
                }
            },
            onDismiss = state::dismiss
        )
    }
}
