package cn.a10miaomiao.bilimiao.compose.pages.dynamic

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.foundation.animateTabSwitchTo
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageListener
import cn.a10miaomiao.bilimiao.compose.common.mypage.rememberMyMenu
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.common.toWindowInsets
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.components.DynamicMiniUpperList
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.components.DynamicMostVisitedContent
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.components.DynamicPageScaffold
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.components.DynamicUpperList
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.content.DynamicAllListContent
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.content.DynamicVideoListContent
import cn.a10miaomiao.bilimiao.compose.pages.home.HomePage
import cn.a10miaomiao.bilimiao.compose.pages.home.content.HomePopularContent
import cn.a10miaomiao.bilimiao.compose.pages.home.content.HomeRecommendContent
import cn.a10miaomiao.bilimiao.compose.pages.home.content.HomeTimeMachineContent
import com.a10miaomiao.bilimiao.comm.mypage.MenuActions
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.kodein.di.compose.rememberInstance

@Serializable
class DynamicPage : ComposePage {

    @Composable
    override fun Content() {
        val viewModel: DynamicViewModel = diViewModel { DynamicViewModel(it) }
        DynamicPageContent(viewModel)
    }
}

@Composable
private fun DynamicPageContent(
    viewModel: DynamicViewModel
) {
    val pageConfigId = PageConfig(
        title = "bilimiao\n-\n动态",
        menu = rememberMyMenu {
            checkable = true
            checkedKey = MenuKeys.dynamic

            myItem {
                key = MenuKeys.home
                title = "首页"
                iconVector = androidx.compose.material.icons.Icons.Default.Home
            }
            myItem {
                key = MenuKeys.dynamic
                title = "动态"
                iconVector = androidx.compose.material.icons.Icons.Default.Icecream
            }
            myItem {
                key = MenuKeys.searchInHome
                title = "搜索"
                iconVector = androidx.compose.material.icons.Icons.Default.Search
                action = MenuActions.search
            }
        }
    )
    PageListener(
        configId = pageConfigId,
        onMenuItemClick = viewModel::menuItemClick,
    )

    val windowInsets = localContentInsets()

    val upperList by viewModel.upList.collectAsState()
    val pageNavigation by rememberInstance<PageNavigation>()
    // 选择 UP 主：打开独立 UP 主动态页（NavDisplay 子页面），
    // 预测性返回与打开/关闭转场动画由导航层处理（与视频详情页一致）
    fun openUpperPage(up: bilibili.app.dynamic.v2.UpListItem) {
        pageNavigation.navigate(
            DynamicUpperPage(
                uid = up.uid,
                face = up.face,
                name = up.name,
            )
        )
    }

    val saveableStateHolder = rememberSaveableStateHolder()
    DynamicPageScaffold(
        allContent = {
            saveableStateHolder.SaveableStateProvider(key = PageTabIds.DynamicAll) {
                DynamicAllListContent(
                    dynamicViewModel = viewModel
                )
            }
        },
        videoContent = {
            saveableStateHolder.SaveableStateProvider(key = PageTabIds.DynamicVideo) {
                DynamicVideoListContent()
            }
        },
        mostVisitedContent = {
            saveableStateHolder.SaveableStateProvider(key = PageTabIds.DynamicMostVisited) {
                DynamicMostVisitedContent(
                    upperList = upperList,
                    onSelected = ::openUpperPage,
                )
            }
        },
        upperList = { maxWidth ->
            if (maxWidth > 72.dp) {
                DynamicUpperList(
                    modifier = Modifier
                        .width(maxWidth)
                        .fillMaxHeight(),
                    safePadding = PaddingValues(
                        top = windowInsets.topDp.dp,
                        bottom = windowInsets.bottom
                    ),
                    upperList = upperList,
                    onSelected = ::openUpperPage,
                )
            } else {
                DynamicMiniUpperList(
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight(),
                    upperList = upperList,
                    onSelected = ::openUpperPage,
                )
            }
        },
    )
}
