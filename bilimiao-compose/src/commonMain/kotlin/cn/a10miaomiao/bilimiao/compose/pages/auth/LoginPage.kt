package cn.a10miaomiao.bilimiao.compose.pages.auth

import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.auth.GeetestCallback
import cn.a10miaomiao.bilimiao.compose.common.auth.GeetestResult
import cn.a10miaomiao.bilimiao.compose.common.auth.GeetestVerifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.foundation.imePaddingAboveBottomBar
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialogState
import com.a10miaomiao.bilimiao.comm.BilimiaoCommCore
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.auth.LoginInfo
import com.a10miaomiao.bilimiao.comm.entity.auth.WebKeyInfo
import com.a10miaomiao.bilimiao.comm.entity.user.UserInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.a10miaomiao.bilimiao.comm.utils.getQueryKeyValueMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

@Serializable
class LoginPage : ComposePage {

    @Composable
    override fun Content() {
        val viewModel: LoginPageViewModel = diViewModel { LoginPageViewModel(it) }
        LaunchedEffect(Unit) {
            viewModel.checkLogin()
        }
        LoginPageContent(viewModel)
    }

}

private class LoginPageViewModel(
    override val di: DI,
) : ViewModel(), DIAware, GeetestCallback {

    private var verifyUrl = ""
    private var recaptchaToken = ""

    private val pageNavigation by instance<PageNavigation>()
    private val userStore by instance<UserStore>()
    private val geetestVerifier by instance<GeetestVerifier>()
    private val messageDialog by instance<MessageDialogState>()

    val loading = MutableStateFlow(false)
    val userName = MutableStateFlow("")
    val password = MutableStateFlow("")

    fun setUserName(value: String) {
        userName.value = value
    }

    fun setPassword(value: String) {
        password.value = value
    }

    fun startLogin(
        gt3Result: GeetestResult? = null,
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            if (userName.value.isBlank()) {
                messageDialog.alert("请输入用户名/邮箱/手机号")
                return@launch
            }
            if (password.value.isBlank()) {
                messageDialog.alert("请输入密码")
                return@launch
            }
            loading.value = true
            val webKey = getWebKey()
            val res = if (gt3Result == null) {
                // 不带验证码
                BiliApiService.authApi.oauth2Login(
                    username = userName.value,
                    passport = password.value,
                    key = webKey.key,
                    rhash = webKey.hash
                )
            } else {
                // 带验证码
                BiliApiService.authApi.oauth2Login(
                    username = userName.value,
                    passport = password.value,
                    key = webKey.key,
                    rhash = webKey.hash,
                    recaptchaToken = recaptchaToken,
                    geeValidate = gt3Result.geetest_validate,
                    geeSeccode = gt3Result.geetest_seccode,
                    geeChallenge = gt3Result.geetest_challenge,
                )
            }.awaitCall().json<ResponseData<LoginInfo.PasswordLoginInfo>>()
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    val loginInfo = res.requireData()
                    if (loginInfo.status == 0) {
                        BilimiaoCommCore.instance.saveAuthInfo(loginInfo.toLoginInfo())
                        authInfo()
                    } else if (loginInfo.url != null && "tmp_token=" in loginInfo.url) {
                        messageDialog.open(
                            title = "提示",
                            text = loginInfo.message,
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val params = UrlUtil.getQueryKeyValueMap(loginInfo.url)
                                        if (params.containsKey("tmp_token")
                                            && params.containsKey("request_id")
                                            && params.containsKey("source")
                                        ) {
                                            pageNavigation.navigate(
                                                TelVerifyPage(
                                                    code = params["tmp_token"] ?: "",
                                                    requestId = params["request_id"] ?: "",
                                                    source = params["source"] ?: ""
                                                )
                                            )
                                        } else {
                                            pageNavigation.launchWebBrowser(loginInfo.url)
                                        }
                                        messageDialog.close()
                                    }
                                ) {
                                    Text("请往验证")
                                }
                            }
                        )
                    } else {
                        messageDialog.open(
                            title = "登录失败，请稍后重试：" + loginInfo.status,
                            text = loginInfo.message,
                            confirmButton = {
                                if (!loginInfo.url.isNullOrBlank()) {
                                    TextButton(
                                        onClick = {
                                            pageNavigation.launchWebBrowser(loginInfo.url)
                                        }
                                    ) {
                                        Text("查看")
                                    }
                                }
                            }
                        )
                    }
                } else if (res.code == -105 && gt3Result == null) {
                    verifyUrl = res.data!!.url ?: ""
                    geetestVerifier.startVerification(this@LoginPageViewModel)
                } else {
                    messageDialog.alert(res.message)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            messageDialog.alert(e.message ?: e.toString())
        } finally {
            loading.value = false
        }
    }

    private suspend fun getWebKey(): WebKeyInfo {
        val res = BiliApiService.authApi
            .webKey()
            .awaitCall()
            .json<ResponseData<WebKeyInfo>>()
        if (res.isSuccess) {
            return res.requireData()
        }
        throw Exception(res.message)
    }

    private suspend fun authInfo() {
        val res = withContext(Dispatchers.IO) {
            BiliApiService.authApi
                .account()
                .awaitCall()
                .json<ResponseData<UserInfo>>()
        }
        if (res.isSuccess) {
            withContext(Dispatchers.Main) {
                userStore.setUserInfo(res.requireData())
                pageNavigation.popBackStack()
            }
        } else {
            throw Exception(res.message)
        }
    }

    fun checkLogin() {
        if (userStore.isLogin()) {
            pageNavigation.popBackStack()
        }
    }

    override suspend fun onResult(
        result: GeetestResult
    ): Boolean {
        startLogin(result)
        return true
    }

    override suspend fun getApiJson(): kotlinx.serialization.json.JsonObject? {
        val queryMap = UrlUtil.getQueryKeyValueMap(verifyUrl)
        if (queryMap.containsKey("recaptcha_token")) {
            recaptchaToken = queryMap["recaptcha_token"] ?: ""
            return buildJsonObject {
                put("success", 1)
                put("challenge", queryMap["gee_challenge"] ?: "")
                put("gt", queryMap["gee_gt"] ?: "")
            }
        } else {
            messageDialog.alert("加载验证码出现错误")
            return null
        }
    }

    fun toH5LoginPage() {
        pageNavigation.navigate(H5LoginPage())
    }

    fun toQrLogin() {
        pageNavigation.navigate(QrCodeLoginPage())
    }

    fun toSMSLogin() {
        pageNavigation.navigate(SMSLoginPage())
    }
}

@Composable
private fun LoginPageContent(
    viewModel: LoginPageViewModel
) {
    PageConfig(title = "登录BILIBILI")
    val windowInsets = localContentInsets()

    val loading by viewModel.loading.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val password by viewModel.password.collectAsState()

    val passwordFocusRequester = remember { FocusRequester() }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 登录页内容轻（无列表），用实时 imePadding：内容区贴着键盘顶部同步收缩，
            // 键盘动画期间不会在内容下方露出背景块，也不会有列表重排卡顿
            .imePaddingAboveBottomBar()
            .padding(horizontal = 16.dp)
            .padding(top = windowInsets.topDp.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            Text(
                text = "登录Bilibili",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(40.dp))
            // 标准 Material 3 Expressive 输入框（与搜索页一致：OutlinedTextField + 大圆角）
            OutlinedTextField(
                value = userName,
                onValueChange = viewModel::setUserName,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(text = "用户名/邮箱/手机号")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        passwordFocusRequester.requestFocus()
                    },
                ),
                shape = MaterialTheme.shapes.extraLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            // 密码框：M3E 输入框 + 明文/密文切换
            OutlinedTextField(
                value = password,
                onValueChange = viewModel::setPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester),
                placeholder = {
                    Text(text = "密码")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "隐藏密码"
                            } else {
                                "显示密码"
                            },
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        viewModel.startLogin()
                    },
                ),
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                shape = MaterialTheme.shapes.extraLarge,
            )
        }
        // 按钮固定在输入框下方：键盘展开时内容整体位于键盘上方（imePadding 收缩），
        // 按钮不会闪现到别处
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = viewModel::startLogin,
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    modifier = Modifier.padding(horizontal = 5.dp),
                    text = "登录",
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = viewModel::toSMSLogin) {
                    Text(text = "手机号登录")
                }
                Spacer(modifier = Modifier.width(24.dp))
                TextButton(onClick = viewModel::toQrLogin) {
                    Text(text = "二维码登录")
                }
            }
        }
    }
}
