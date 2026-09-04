package cn.a10miaomiao.bilimiao.compose.pages.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a10miaomiao.bilimiao.comm.mypage.SearchConfigInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.kodein.di.DI
import org.kodein.di.DIAware

class SearchInputViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    val historyListFlow = MutableStateFlow(listOf<SuggestInfo>())
    val historyList get() = historyListFlow
    val suggestListFlow = MutableStateFlow(listOf<SuggestInfo>())
    val suggestList get() = suggestListFlow.value

    var config: SearchConfigInfo? = null
    var searchMode = 0 // 0为全站搜索，1为页面自身搜索

    private val searchHistoryManager = createSearchHistoryManager()

    /** 联想请求 Job：连续输入时取消上一个请求，避免旧响应覆盖新输入 */
    private var suggestJob: Job? = null

    /** 联想输入防抖时长 */
    private val suggestDebounceMs = 200L

    init {
        updateHistoryList()
    }

    private fun updateHistoryList() {
        // 历史按“最近搜索时间”倒序：每次搜索先删旧记录再插入新行，
        // 按 id 倒序即最近搜索在前，同词只保留一条并自动置顶
        historyListFlow.value = runBlocking { searchHistoryManager.queryAllHistory() }.map {
            SuggestInfo(
                text = it,
                type = SuggestType.HISTORY,
                value = it,
            )
        }
    }

    /**
     * 输入变化时加载搜索联想（建议）。
     *
     * 输入为空时清空联想（页面此时显示搜索历史）；输入非空时
     * 防抖后请求 B 站 suggest 接口，响应仅当输入未再变化时生效。
     */
    fun loadSuggestData(keyword: String) {
        suggestJob?.cancel()
        if (keyword.isBlank()) {
            suggestListFlow.value = emptyList()
            return
        }
        suggestJob = viewModelScope.launch(Dispatchers.IO) {
            delay(suggestDebounceMs)
            val requestKeyword = keyword
            suggestListFlow.value = getInitSuggestData(requestKeyword)
            try {
                val res = BiliApiService.searchApi.suggestList(requestKeyword).awaitCall()
                val jsonStr = res.body!!.string()
                val jsonObj = Json.parseToJsonElement(jsonStr).jsonObject
                val jsonArray = jsonObj["result"]?.jsonObject?.get("tag")?.jsonArray
                if (jsonArray != null) {
                    suggestListFlow.value = getInitSuggestData(requestKeyword).apply {
                        for (element in jsonArray) {
                            val value = element.jsonObject["value"]?.jsonPrimitive?.content
                                ?: continue
                            add(
                                SuggestInfo(
                                    text = value,
                                    value = value,
                                    type = SuggestType.TEXT
                                )
                            )
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 输入已变化导致本请求被取消：直接结束，不覆盖新结果
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getInitSuggestData(
        keyword: String
    ) = mutableListOf<SuggestInfo>().apply {
        if (isNumeric(keyword)) {
            add(
                SuggestInfo(
                    text = "AV$keyword",
                    type = SuggestType.AV,
                    value = keyword,
                )
            )
            add(
                SuggestInfo(
                    text = "SS$keyword",
                    type = SuggestType.SS,
                    value = keyword,
                )
            )
        }
    }

    /**
     * 记录一次搜索：同关键词先去重删除再插入（新记录 id 最大，
     * 列表按 id 倒序即最近搜索置顶），随后刷新历史列表。
     */
    fun addSearchHistory(text: String) {
        runBlocking {
            searchHistoryManager.deleteHistory(text)
            searchHistoryManager.insertHistory(text)
        }
        updateHistoryList()
    }

    fun deleteSearchHistory(text: String) {
        runBlocking { searchHistoryManager.deleteHistory(text) }
        updateHistoryList()
    }

    fun deleteAllSearchHistory() {
        runBlocking { searchHistoryManager.deleteAllHistory() }
        updateHistoryList()
    }

    fun isNumeric(s: String): Boolean {
        return s.all { it.isDigit() }
    }

    enum class SuggestType {
        TEXT, // 普通文本
        SEARCH, // 直接搜索
        AV, // 视频ID，AV号跳转
        SS, // 番剧ID，SS号跳转
        HISTORY, // 历史搜索
    }

    data class SuggestInfo(
        val text: String, // 显示文字
        val type: SuggestType,
        val value: String,
    )
}
