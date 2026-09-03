package cn.a10miaomiao.bilimiao.compose.pages.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.emitter.SharedFlowEmitter
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigator
import cn.a10miaomiao.bilimiao.compose.pages.video.components.VideoCoinRadioButton
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.toast.GlobalToaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

@Serializable
data class VideoCoinPage(
    val aid: String,
    val copyright: Int,
) : ComposePage {

    @Composable
    override fun Content() {
        val viewModel = diViewModel(key = "coin-$aid") {
            VideoCoinViewModel(it, aid, copyright)
        }
        VideoCoinPageContent(viewModel)
    }
}

class VideoCoinViewModel(
    override val di: DI,
    private val aid: String,
    copyright: Int,
) : ViewModel(), DIAware {

    private val pageNavigator by instance<PageNavigator>()
    private val emitter by instance<SharedFlowEmitter>()

    var loading by mutableStateOf(false)
        private set
    var coinNum by mutableIntStateOf(0)
        private set
    var maxCoinNum by mutableIntStateOf(0)
        private set

    val snackbar = SnackbarHostState()

    init {
        maxCoinNum = if (copyright == 2) 1 else 2
        coinNum = maxCoinNum
    }

    fun selectCoinNum(num: Int) {
        coinNum = num
    }

    fun confirmCoin() {
        if (loading) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    loading = true
                }
                val res = BiliApiService.videoAPI
                    .coin(aid, coinNum)
                    .awaitCall()
                    .json<MessageInfo>()
                withContext(Dispatchers.Main) {
                    if (res.isSuccess) {
                        GlobalToaster.show("感谢投币")
                        emitter.emit(EmitterAction.CoinChanged(coinNum))
                        pageNavigator.popBackStack()
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
                    loading = false
                }
            }
        }
    }
}

@Composable
private fun VideoCoinPageContent(
    viewModel: VideoCoinViewModel,
) {
    PageConfig(title = "投币")

    val windowInsets = localContentInsets()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = windowInsets.topDp.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "请选择投币",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
            )
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    VideoCoinRadioButton(
                        num = 1,
                        selected = viewModel.coinNum == 1,
                        onClick = {
                            viewModel.selectCoinNum(1)
                        }
                    )
                    if (viewModel.maxCoinNum > 1) {
                        Spacer(modifier = Modifier.height(20.dp))
                        VideoCoinRadioButton(
                            num = 2,
                            selected = viewModel.coinNum == 2,
                            onClick = {
                                viewModel.selectCoinNum(2)
                            }
                        )
                    }
                }
                SnackbarHost(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    hostState = viewModel.snackbar,
                )
            }
            Row(
                modifier = Modifier
                    .padding(
                        vertical = 5.dp,
                        horizontal = 10.dp
                    )
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = viewModel::confirmCoin,
                    enabled = !viewModel.loading
                ) {
                    Row {
                        if (viewModel.loading) {
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
    }
}
