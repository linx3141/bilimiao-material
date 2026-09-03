package cn.a10miaomiao.bilimiao.compose.pages.community.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.emitter.SharedFlowEmitter
import cn.a10miaomiao.bilimiao.compose.common.isImeVisible
import cn.a10miaomiao.bilimiao.compose.components.dialogs.AutoSheetDialog
import cn.a10miaomiao.bilimiao.compose.pages.community.ReplyEditParams
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.user.UserEmoteInfo
import com.a10miaomiao.bilimiao.comm.entity.video.VideoCommentSendResultInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.compose.rememberInstance

/**
 * 回复编辑底部弹窗。
 *
 * 从底部弹出，输入法上方依次为：发布/表情工具栏、输入框；
 * 输入框上方为阴影遮罩，底下露出原评论页面。
 */
@Composable
fun ReplyEditPopup(
    params: ReplyEditParams,
    onDismiss: () -> Unit,
) {
    val emitter: SharedFlowEmitter by rememberInstance()
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf(TextFieldValue("")) }
    var loading by remember { mutableStateOf(false) }
    val snackbar = androidx.compose.material3.SnackbarHostState()
    val focusRequester = remember { FocusRequester() }
    val textEmpty by derivedStateOf { input.text.isEmpty() }
    var showEmojiGrid by remember { mutableStateOf(false) }

    fun inputEmoji(emoji: UserEmoteInfo) {
        val originalText = input.text
        val position = input.selection.min + emoji.text.length
        input = input.copy(
            text = originalText.substring(0, input.selection.min)
                    + emoji.text
                    + originalText.substring(input.selection.max),
            selection = TextRange(position)
        )
    }

    fun sendReply() {
        if (loading) return
        scope.launch(Dispatchers.IO) {
            try {
                val message = input.text
                withContext(Dispatchers.Main) {
                    loading = true
                }
                val res = BiliApiService.commentApi
                    .add(
                        message = if (params.parent != null
                            && params.parent != params.root) {
                            "回复 @${params.name} :$message"
                        } else {
                            message
                        },
                        type = params.type,
                        oid = params.oid,
                        root = params.root,
                        parent = params.parent,
                    )
                    .awaitCall()
                    .json<ResponseData<VideoCommentSendResultInfo>>(isLog = true)
                withContext(Dispatchers.Main) {
                    if (res.isSuccess) {
                        val result = res.requireData()
                        GlobalToaster.show(result.success_toast)
                        emitter.emit(EmitterAction.ReplyAdded(result.reply))
                        delay(300L)
                        onDismiss()
                    } else {
                        snackbar.showSnackbar(res.message)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                snackbar.showSnackbar(e.message ?: e.toString())
            } finally {
                withContext(Dispatchers.Main) {
                    loading = false
                }
            }
        }
    }

    val imeVisible = isImeVisible()
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(imeVisible) {
        if (showEmojiGrid && imeVisible) {
            showEmojiGrid = false
        }
    }
    LaunchedEffect(showEmojiGrid) {
        if (showEmojiGrid && imeVisible) {
            keyboardController?.hide()
        } else if (!showEmojiGrid && !imeVisible) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    AutoSheetDialog(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            ReplyTextField(
                visible = true,
                focusRequester = focusRequester,
                value = input,
                textEmpty = textEmpty,
                onValueChange = { input = it },
                onDone = { focusRequester.freeFocus() },
            )
            SnackbarHost(hostState = snackbar)
            ReplyTextToolbar(
                modifier = Modifier.padding(top = 5.dp),
                visibleEmoji = showEmojiGrid,
                loading = loading,
                onEmojiClick = {
                    showEmojiGrid = !showEmojiGrid
                },
                onSendClick = ::sendReply,
            )
            AnimatedVisibility(
                visible = showEmojiGrid,
            ) {
                EmojiGridBox(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .height(240.dp),
                    onInputEmoji = ::inputEmoji,
                )
            }
        }
    }
}
