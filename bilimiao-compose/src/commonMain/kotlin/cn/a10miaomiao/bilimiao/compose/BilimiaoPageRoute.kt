package cn.a10miaomiao.bilimiao.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.pages.BlankPage
import cn.a10miaomiao.bilimiao.compose.pages.TestPage
import cn.a10miaomiao.bilimiao.compose.pages.auth.H5LoginPage
import cn.a10miaomiao.bilimiao.compose.pages.auth.LoginPage
import cn.a10miaomiao.bilimiao.compose.pages.auth.QrCodeLoginPage
import cn.a10miaomiao.bilimiao.compose.pages.auth.SMSLoginPage
import cn.a10miaomiao.bilimiao.compose.pages.auth.TelVerifyPage
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.BangumiDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.BangumiEpisodesPage
import cn.a10miaomiao.bilimiao.compose.pages.community.MainReplyListPage
import cn.a10miaomiao.bilimiao.compose.pages.community.ReplyDetailListPage
import cn.a10miaomiao.bilimiao.compose.pages.download.DownloadBangumiCreatePage
import cn.a10miaomiao.bilimiao.compose.pages.download.DownloadDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.download.DownloadListPage
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.DynamicDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.DynamicOpusPage
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.DynamicPage
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.DynamicUpperPage
import cn.a10miaomiao.bilimiao.compose.pages.filter.FilterSettingPage
import cn.a10miaomiao.bilimiao.compose.pages.home.HomePage
import cn.a10miaomiao.bilimiao.compose.pages.lyric.LyricPage
import cn.a10miaomiao.bilimiao.compose.pages.message.MessagePage
import cn.a10miaomiao.bilimiao.compose.pages.mine.HistoryPage
import cn.a10miaomiao.bilimiao.compose.pages.mine.HistorySearchInputPage
import cn.a10miaomiao.bilimiao.compose.pages.mine.MyBangumiPage
import cn.a10miaomiao.bilimiao.compose.pages.mine.MyFollowPage
import cn.a10miaomiao.bilimiao.compose.pages.mine.WatchLaterPage
import cn.a10miaomiao.bilimiao.compose.pages.player.SendDanmakuPage
import cn.a10miaomiao.bilimiao.compose.pages.playlist.PlayListPage
import cn.a10miaomiao.bilimiao.compose.pages.rank.RankPage
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchResultPage
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchPage
import cn.a10miaomiao.bilimiao.compose.pages.mine.ProfilePage
import cn.a10miaomiao.bilimiao.compose.pages.setting.AboutPage
import cn.a10miaomiao.bilimiao.compose.pages.setting.DanmakuDisplaySettingPage
import cn.a10miaomiao.bilimiao.compose.pages.setting.DanmakuSettingPage
import cn.a10miaomiao.bilimiao.compose.pages.setting.HomeSettingPage
import cn.a10miaomiao.bilimiao.compose.pages.setting.ProxySettingPage
import cn.a10miaomiao.bilimiao.compose.pages.setting.SettingPage
import cn.a10miaomiao.bilimiao.compose.pages.setting.AutoStopTimerPage
import cn.a10miaomiao.bilimiao.compose.pages.setting.VideoSettingPage
import cn.a10miaomiao.bilimiao.compose.pages.setting.proxy.AddProxyServerPage
import cn.a10miaomiao.bilimiao.compose.pages.setting.proxy.EditProxyServerPage
import cn.a10miaomiao.bilimiao.compose.pages.setting.proxy.SelectProxyServerPage
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeRegionDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeSettingPage
import cn.a10miaomiao.bilimiao.compose.pages.user.BlackListPage
import cn.a10miaomiao.bilimiao.compose.pages.user.SearchFollowPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserBangumiPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserFavouriteDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserFavouritePage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserFollowPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserLikeArchivePage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserMedialistPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSeasonDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSpacePage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSpaceSearchInputPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSpaceSearchPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserFansPage
import cn.a10miaomiao.bilimiao.compose.pages.video.VideoDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.video.VideoPagesPage
import cn.a10miaomiao.bilimiao.compose.pages.video.VideoCoinPage
import cn.a10miaomiao.bilimiao.compose.pages.video.VideoAddFavoritePage
import cn.a10miaomiao.bilimiao.compose.pages.video.VideoDownloadPage
import cn.a10miaomiao.bilimiao.compose.pages.web.WebPage
import cn.a10miaomiao.bilimiao.compose.pages.setting.ThemeSettingPage
import androidx.navigation3.runtime.NavKey

/**
 * 所有页面的 NavEntry 注册表。
 * 在 entryProvider { } DSL 中调用 entries(this) 注册全部页面。
 * 深链接解析在 BilibiliNavigation 中独立实现，此处不声明 navDeepLink。
 */
object BilimiaoPageRoute {

    fun entries(scope: EntryProviderScope<NavKey>) {
        // 通用
        scope.entry<BlankPage> { PageSurface { it.Content() } }
        scope.entry<TestPage> { PageSurface { it.Content() } }

        // home
        scope.entry<HomePage> { PageSurface { it.Content() } }

        // search
        scope.entry<SearchResultPage> { PageSurface { it.Content() } }
        scope.entry<SearchPage> { PageSurface { it.Content() } }
        scope.entry<ProfilePage> { PageSurface { it.Content() } }

        // auth
        scope.entry<LoginPage> { PageSurface { it.Content() } }
        scope.entry<QrCodeLoginPage> { PageSurface { it.Content() } }
        scope.entry<TelVerifyPage> { PageSurface { it.Content() } }
        scope.entry<H5LoginPage> { PageSurface { it.Content() } }
        scope.entry<SMSLoginPage> { PageSurface { it.Content() } }

        // video
        scope.entry<VideoDetailPage> { PageSurface { it.Content() } }
        scope.entry<VideoPagesPage> { PageSurface { it.Content() } }
        scope.entry<VideoCoinPage> { PageSurface { it.Content() } }
        scope.entry<VideoAddFavoritePage> { PageSurface { it.Content() } }
        scope.entry<VideoDownloadPage> { PageSurface { it.Content() } }

        // bangumi
        scope.entry<BangumiDetailPage> { PageSurface { it.Content() } }
        scope.entry<BangumiEpisodesPage> { PageSurface { it.Content() } }

        // dynamic
        scope.entry<DynamicPage> { PageSurface { it.Content() } }
        scope.entry<DynamicDetailPage> { PageSurface { it.Content() } }
        scope.entry<DynamicOpusPage> { PageSurface { it.Content() } }
        scope.entry<DynamicUpperPage> { PageSurface { it.Content() } }

        // rank
        scope.entry<RankPage> { PageSurface { it.Content() } }

        // download
        scope.entry<DownloadListPage> { PageSurface { it.Content() } }
        scope.entry<DownloadDetailPage> { PageSurface { it.Content() } }
        scope.entry<DownloadBangumiCreatePage> { PageSurface { it.Content() } }

        // filter
        scope.entry<FilterSettingPage> { PageSurface { it.Content() } }

        // message
        scope.entry<MessagePage> { PageSurface { it.Content() } }

        // player
        scope.entry<SendDanmakuPage> { PageSurface { it.Content() } }

        // playlist
        scope.entry<PlayListPage> { PageSurface { it.Content() } }

        // setting
        scope.entry<SettingPage> { PageSurface { it.Content() } }
        scope.entry<HomeSettingPage> { PageSurface { it.Content() } }
        scope.entry<ThemeSettingPage> { PageSurface { it.Content() } }
        scope.entry<VideoSettingPage> { PageSurface { it.Content() } }
        scope.entry<AutoStopTimerPage> { PageSurface { it.Content() } }
        scope.entry<DanmakuSettingPage> { PageSurface { it.Content() } }
        scope.entry<DanmakuDisplaySettingPage> { PageSurface { it.Content() } }
        scope.entry<ProxySettingPage> { PageSurface { it.Content() } }
        scope.entry<AddProxyServerPage> { PageSurface { it.Content() } }
        scope.entry<EditProxyServerPage> { PageSurface { it.Content() } }
        scope.entry<SelectProxyServerPage> { PageSurface { it.Content() } }
        scope.entry<AboutPage> { PageSurface { it.Content() } }

        // time
        scope.entry<TimeSettingPage> { PageSurface { it.Content() } }
        scope.entry<TimeRegionDetailPage> { PageSurface { it.Content() } }

        // mine
        scope.entry<MyBangumiPage> { PageSurface { it.Content() } }
        scope.entry<MyFollowPage> { PageSurface { it.Content() } }
        scope.entry<HistoryPage> { PageSurface { it.Content() } }
        scope.entry<HistorySearchInputPage> { PageSurface { it.Content() } }
        scope.entry<WatchLaterPage> { PageSurface { it.Content() } }

        // user
        scope.entry<UserSpacePage> { PageSurface { it.Content() } }
        scope.entry<UserFansPage> { PageSurface { it.Content() } }
        scope.entry<BlackListPage> { PageSurface { it.Content() } }
        scope.entry<UserSpaceSearchInputPage> { PageSurface { it.Content() } }
        scope.entry<UserSpaceSearchPage> { PageSurface { it.Content() } }
        scope.entry<UserFollowPage> { PageSurface { it.Content() } }
        scope.entry<SearchFollowPage> { PageSurface { it.Content() } }
        scope.entry<UserBangumiPage> { PageSurface { it.Content() } }
        scope.entry<UserLikeArchivePage> { PageSurface { it.Content() } }
        scope.entry<UserFavouritePage> { PageSurface { it.Content() } }
        scope.entry<UserFavouriteDetailPage> { PageSurface { it.Content() } }
        scope.entry<UserSeasonDetailPage> { PageSurface { it.Content() } }
        scope.entry<UserMedialistPage> { PageSurface { it.Content() } }

        // community
        scope.entry<MainReplyListPage> { PageSurface { it.Content() } }
        scope.entry<ReplyDetailListPage> { PageSurface { it.Content() } }

        // lyric
        scope.entry<LyricPage> { PageSurface { it.Content() } }

        // web
        scope.entry<WebPage> { PageSurface { it.Content() } }
    }
}

/**
 * 页面统一背景：避免页面内容加载中/过渡期间背景透明，
 * 所有页面在进入/返回动画时都有一层 surface 兜底。
 */
@Composable
private fun PageSurface(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        content()
    }
}
