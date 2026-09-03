package cn.a10miaomiao.bilimiao.compose.pages.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.foundation.imePaddingAboveBottomBar
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.ExpressivePreferenceItem
import cn.a10miaomiao.bilimiao.compose.common.preference.LocalListItemShapes
import cn.a10miaomiao.bilimiao.compose.common.preference.segmentedItemShapes
import cn.a10miaomiao.bilimiao.compose.components.search.SearchHistoryList
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance
import kotlinx.serialization.Serializable

/**
 * 独立搜索页面：顶部搜索框 + 单列分段展示搜索历史。
 */
@Serializable
class SearchPage : ComposePage {

    @Composable
    override fun Content() {
        val viewModel: SearchInputViewModel = diViewModel { SearchInputViewModel(it) }
        val windowInsets = localContentInsets()
        val pageNavigation by rememberInstance<PageNavigation>()
        val history by viewModel.historyListFlow.collectAsState()
        var keyword by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // 与 KernelSU 输入页面一致：内容实时跟随键盘动画上移，
                // 每帧产生新帧保持屏幕高刷新率，键盘动画才流畅
                .imePaddingAboveBottomBar()
                .padding(top = windowInsets.topDp.dp),
        ) {
            SearchInputBox(
                keyword = keyword,
                onKeywordChange = { keyword = it },
                onSearch = {
                    val text = keyword.trim()
                    if (text.isNotEmpty()) {
                        viewModel.addSearchHistory(text)
                        pageNavigation.navigate(SearchResultPage(keyword = text))
                    }
                },
                onClear = { keyword = "" },
            )
            SearchHistoryList(
                history = history.map { it.text },
                onKeywordClick = { text ->
                    pageNavigation.navigate(SearchResultPage(keyword = text))
                },
                onKeywordDelete = viewModel::deleteSearchHistory,
                onClearAll = viewModel::deleteAllSearchHistory,
            )
        }
    }
}

@Composable
private fun SearchInputBox(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = keyword,
        onValueChange = onKeywordChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        placeholder = {
            Text(text = "搜索视频、UP主、番剧")
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "搜索",
            )
        },
        trailingIcon = {
            if (keyword.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "清空",
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = MaterialTheme.shapes.extraLarge,
    )
}
