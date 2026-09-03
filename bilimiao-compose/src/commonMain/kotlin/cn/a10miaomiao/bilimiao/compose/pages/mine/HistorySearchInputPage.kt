package cn.a10miaomiao.bilimiao.compose.pages.mine

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.foundation.imePaddingAboveBottomBar
import cn.a10miaomiao.bilimiao.compose.common.localContentInsets
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.components.search.SearchHistoryList
import cn.a10miaomiao.bilimiao.compose.pages.search.createSearchHistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.kodein.di.compose.rememberInstance

/**
 * 历史记录搜索输入页：输入关键词后进入搜索历史记录列表页。
 * 与用户空间搜索输入页 [cn.a10miaomiao.bilimiao.compose.pages.user.UserSpaceSearchInputPage]
 * 同一套交互，搜索范围限定为历史记录。
 */
@Serializable
class HistorySearchInputPage(
    val initKeyword: String = "",
) : ComposePage {

    @Composable
    override fun Content() {
        val windowInsets = localContentInsets()
        val pageNavigation by rememberInstance<PageNavigation>()
        val scope = rememberCoroutineScope()
        val searchHistoryManager = remember { createSearchHistoryManager() }
        var keyword by remember { mutableStateOf(initKeyword) }
        var historyList by remember { mutableStateOf(listOf<String>()) }

        LaunchedEffect(Unit) {
            historyList = withContext(Dispatchers.IO) {
                searchHistoryManager.queryAllHistory("history")
            }
        }

        fun doSearch(text: String) {
            val trim = text.trim()
            if (trim.isNotEmpty()) {
                scope.launch(Dispatchers.IO) {
                    searchHistoryManager.deleteHistory(trim, "history")
                    searchHistoryManager.insertHistory(trim, "history")
                }
                pageNavigation.navigate(HistoryPage(keyword = trim))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePaddingAboveBottomBar()
                .padding(top = windowInsets.topDp.dp),
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                placeholder = {
                    Text(text = "搜索历史记录")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "搜索",
                    )
                },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        IconButton(onClick = { keyword = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "清空",
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        doSearch(keyword)
                    }
                ),
                shape = MaterialTheme.shapes.extraLarge,
            )
            SearchHistoryList(
                history = historyList,
                onKeywordClick = { doSearch(it) },
                onKeywordDelete = { text ->
                    scope.launch(Dispatchers.IO) {
                        searchHistoryManager.deleteHistory(text, "history")
                    }
                    historyList = historyList - text
                },
                onClearAll = {
                    scope.launch(Dispatchers.IO) {
                        searchHistoryManager.deleteAllHistory("history")
                    }
                    historyList = emptyList()
                },
            )
        }
    }
}
