package cn.a10miaomiao.bilimiao.compose.pages.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.foundation.imePaddingAboveBottomBar
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.components.search.SearchHistoryList
import cn.a10miaomiao.bilimiao.compose.components.search.SearchSuggestList
import org.kodein.di.compose.rememberInstance
import kotlinx.serialization.Serializable

/**
 * 独立搜索页面：顶部搜索框 + 联想/历史。
 *
 * - 输入框为空：展示搜索历史（最近搜索在前，按上次搜索时间排序）；
 * - 输入关键字（开始搜索）：不再显示历史，展示搜索联想（如输入 AB
 *   展示 ABC/ABS/ABD…，纯数字时附 AV/SS 快捷词）；
 * - 提交搜索或点击任意联想/历史项：该词写入历史并置顶，随后进入结果页。
 */
@Serializable
class SearchPage : ComposePage {

    @Composable
    override fun Content() {
        val viewModel: SearchInputViewModel = diViewModel { SearchInputViewModel(it) }
        val windowInsets = localContentInsets()
        val pageNavigation by rememberInstance<PageNavigation>()
        val history by viewModel.historyListFlow.collectAsState()
        val suggestList by viewModel.suggestListFlow.collectAsState()
        var keyword by remember { mutableStateOf("") }

        fun search(text: String) {
            val trimText = text.trim()
            if (trimText.isEmpty()) return
            // 每次搜索都更新历史：同词去重并置顶（按上次搜索时间排序）
            viewModel.addSearchHistory(trimText)
            pageNavigation.navigate(SearchResultPage(keyword = trimText))
        }

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
                onKeywordChange = { text ->
                    keyword = text
                    // 开始输入即加载联想；清空时联想随之清空，页面回到历史视图
                    viewModel.loadSuggestData(text)
                },
                onSearch = { search(keyword) },
                onClear = {
                    keyword = ""
                    viewModel.loadSuggestData("")
                },
            )
            if (keyword.isBlank()) {
                // 未开始输入：搜索历史（最近搜索置顶）
                SearchHistoryList(
                    history = history.map { it.text },
                    onKeywordClick = { text -> search(text) },
                    onKeywordDelete = viewModel::deleteSearchHistory,
                    onClearAll = viewModel::deleteAllSearchHistory,
                )
            } else {
                // 开始搜索：展示联想建议，历史让位
                SearchSuggestList(
                    suggest = suggestList.map { it.text },
                    onKeywordClick = { text -> search(text) },
                )
            }
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
