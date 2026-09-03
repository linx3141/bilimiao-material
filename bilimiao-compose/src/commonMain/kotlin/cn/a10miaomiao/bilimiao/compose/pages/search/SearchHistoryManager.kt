package cn.a10miaomiao.bilimiao.compose.pages.search

interface SearchHistoryManager {
    /**
     * 查询指定类型的搜索历史。类型用于隔离不同搜索场景：
     * 全局搜索、历史记录搜索、单个 UP 主搜索（"up_$id"）互不共享。
     */
    suspend fun queryAllHistory(type: String = "video"): List<String>
    suspend fun insertHistory(keyword: String, type: String = "video")
    suspend fun deleteHistory(keyword: String, type: String = "video")
    suspend fun deleteAllHistory(type: String = "video")
}

expect fun createSearchHistoryManager(): SearchHistoryManager
