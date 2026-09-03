package cn.a10miaomiao.bilimiao.compose.pages.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.isImeVisible
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigator
import cn.a10miaomiao.bilimiao.compose.pages.community.components.EmojiGridBox
import cn.a10miaomiao.bilimiao.compose.pages.community.components.ReplyTextField
import cn.a10miaomiao.bilimiao.compose.pages.community.components.ReplyTextToolbar
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerDelegate
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.entity.user.UserEmoteInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

@Serializable
class SendDanmakuPage : ComposePage {

    // 与发送评论弹窗保持一致：不显示标题栏（关闭按钮 + 标题）
    override val showBottomSheetTitleBar: Boolean = false

    @Composable
    override fun Content() {
        val viewModel: SendDanmakuViewModel = diViewModel {
            SendDanmakuViewModel(it)
        }
        SendDanmakuPageContent(viewModel)
    }
}

internal data class SelectItemInfo<T>(
    val label: String,
    val value: T,
)

internal class SendDanmakuViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val pageNavigation by instance<PageNavigator>()
    private val playerDelegate by instance<BasePlayerDelegate>()

    val focusRequester = FocusRequester()

    internal val danmakuTypeList = listOf<SelectItemInfo<Int>>(
        SelectItemInfo("滚动", 1),
        SelectItemInfo("顶部", 5),
        SelectItemInfo("底部", 4)
    )

    internal val danmakuColorList = listOf<SelectItemInfo<Int>>(
        SelectItemInfo("#FFFFFF", 0xFFFFFF),
        SelectItemInfo("#FE0302", 0xFE0302),
        SelectItemInfo("#FF7204", 0xFF7204),
        SelectItemInfo("#FFAA02", 0xFFAA02),
        SelectItemInfo("#FFD302", 0xFFD302),
        SelectItemInfo("#FFFF00", 0xFFFF00),
        SelectItemInfo("#A0EE00", 0xA0EE00),
        SelectItemInfo("#00CD00", 0x00CD00),
        SelectItemInfo("#019899", 0x019899),
        SelectItemInfo("#4266BE", 0x4266BE),
        SelectItemInfo("#89D5FF", 0x89D5FF),
        SelectItemInfo("#CC0273", 0xCC0273),
        SelectItemInfo("#222222", 0x222222),
        SelectItemInfo("#9B9B9B", 0x9B9B9B),
    )

    internal val danmakuTextSizeList = listOf<SelectItemInfo<Float>>(
        SelectItemInfo("默认", 25f),
        SelectItemInfo("较小", 18f),
    )

    val loading = MutableStateFlow(false)
    val danmakuType = MutableStateFlow(1)
    val danmakuText = MutableStateFlow("")
    val danmakuColor = MutableStateFlow(0xFFFFFF)
    val danmakuTextSize = MutableStateFlow(25f)

    fun setDanmakuTextTypeValue(value: Int) {
        danmakuType.value = value
    }

    fun setDanmakuTextValue(value: String) {
        danmakuText.value = value
    }

    fun setDanmakuTextColorValue(value: Int) {
        danmakuColor.value = value
    }

    fun setDanmakuTextSizeValue(value: Float) {
        danmakuTextSize.value = value
    }

    fun sendDanmaku() {
        val text = danmakuText.value.replace("\n", " ")
        if (text.isBlank()) {
            GlobalToaster.show("请输入弹幕内容")
            return
        }
        if (text.length > 50) {
            GlobalToaster.show("弹幕内容字数过多")
            return
        }

        val type = danmakuType.value
        val color = danmakuColor.value
        val textSize = danmakuTextSize.value

        val sourceIds = playerDelegate.getSourceIds()
        val currentPosition = playerDelegate.currentPosition()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                loading.value = true
                val res = BiliApiService.playerAPI.sendDamaku(
                    aid = sourceIds.aid,
                    oid = sourceIds.cid,
                    msg = text,
                    mode = type,
                    fontsize = textSize.toInt(),
                    color = color,
                    progress = currentPosition,
                ).awaitCall().json<MessageInfo>()
                withContext(Dispatchers.Main) {
                    if (res.isSuccess) {
                        GlobalToaster.show("发送成功")
                        playerDelegate.sendDanmaku(
                            type,
                            text,
                            textSize,
                            color,
                            currentPosition
                        )
                        pageNavigation.popBackStack()
                    } else {
                        GlobalToaster.show(res.message)
                    }
                }
                loading.value = false
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    GlobalToaster.show(e.message ?: e.toString())
                }
                loading.value = false
            }
        }
    }

    fun requestFocus() {
        focusRequester.requestFocus()
    }

    fun freeFocus() {
        focusRequester.freeFocus()
    }
}


/**
 * 发送弹幕页面：仿照发送评论弹窗的布局，
 * 输入框 + 弹幕选项（位置/字体大小/颜色）+ 表情/发送工具栏。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SendDanmakuPageContent(
    viewModel: SendDanmakuViewModel
) {
    val loading = viewModel.loading.collectAsState().value
    val danmakuType = viewModel.danmakuType.collectAsState().value
    val danmakuColor = viewModel.danmakuColor.collectAsState().value
    val danmakuTextSize = viewModel.danmakuTextSize.collectAsState().value

    var input by remember { mutableStateOf(TextFieldValue("")) }
    val textEmpty by derivedStateOf { input.text.isEmpty() }
    var showEmojiGrid by remember { mutableStateOf(false) }
    val imeVisible = isImeVisible()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.requestFocus()
    }
    LaunchedEffect(imeVisible) {
        if (showEmojiGrid && imeVisible) {
            showEmojiGrid = false
        }
    }
    LaunchedEffect(showEmojiGrid) {
        if (showEmojiGrid && imeVisible) {
            keyboardController?.hide()
        } else if (!showEmojiGrid && !imeVisible) {
            viewModel.requestFocus()
            keyboardController?.show()
        }
    }

    fun inputEmoji(emoji: UserEmoteInfo) {
        val originalText = input.text
        val position = input.selection.min + emoji.text.length
        input = input.copy(
            text = originalText.substring(0, input.selection.min)
                    + emoji.text
                    + originalText.substring(input.selection.max),
            selection = TextRange(position),
        )
        viewModel.setDanmakuTextValue(input.text)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ReplyTextField(
            visible = true,
            focusRequester = viewModel.focusRequester,
            value = input,
            textEmpty = textEmpty,
            placeholder = "请输入弹幕内容",
            onValueChange = {
                input = it
                viewModel.setDanmakuTextValue(it.text)
            },
            onDone = viewModel::freeFocus,
        )
        DanmakuOptionRow(
            label = "弹幕位置",
            options = viewModel.danmakuTypeList,
            selected = danmakuType,
            onSelect = viewModel::setDanmakuTextTypeValue,
        )
        DanmakuOptionRow(
            label = "字体大小",
            options = viewModel.danmakuTextSizeList,
            selected = danmakuTextSize,
            onSelect = viewModel::setDanmakuTextSizeValue,
        )
        DanmakuColorRow(
            options = viewModel.danmakuColorList,
            selected = danmakuColor,
            onSelect = viewModel::setDanmakuTextColorValue,
        )
        ReplyTextToolbar(
            modifier = Modifier.padding(top = 5.dp),
            visibleEmoji = showEmojiGrid,
            loading = loading,
            sendText = "发送",
            onEmojiClick = {
                showEmojiGrid = !showEmojiGrid
            },
            onSendClick = {
                if (!loading) {
                    viewModel.sendDanmaku()
                }
            },
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

@Composable
private fun <T> DanmakuOptionRow(
    label: String,
    options: List<SelectItemInfo<T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label：",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(end = 10.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(options) { option ->
                FilterChip(
                    selected = selected == option.value,
                    onClick = {
                        onSelect(option.value)
                    },
                    label = {
                        Text(text = option.label)
                    },
                )
            }
        }
    }
}

@Composable
private fun DanmakuColorRow(
    options: List<SelectItemInfo<Int>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "弹幕颜色：",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(end = 10.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(options) { option ->
                FilterChip(
                    selected = selected == option.value,
                    onClick = {
                        onSelect(option.value)
                    },
                    label = {
                        Text(
                            text = option.label,
                            modifier = Modifier.background(
                                Color(option.value.toLong() or 0xFF000000)
                            ),
                        )
                    },
                )
            }
        }
    }
}
