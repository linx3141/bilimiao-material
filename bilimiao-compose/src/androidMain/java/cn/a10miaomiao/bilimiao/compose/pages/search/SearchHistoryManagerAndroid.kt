package cn.a10miaomiao.bilimiao.compose.pages.search

import com.a10miaomiao.bilimiao.comm.db.createSearchHistoryDatabase
import com.a10miaomiao.bilimiao.comm.db.entity.SearchHistoryEntity

actual fun createSearchHistoryManager(): SearchHistoryManager {
    val db = createSearchHistoryDatabase()
    val dao = db.searchHistoryDao()
    return object : SearchHistoryManager {
        override suspend fun queryAllHistory(type: String): List<String> {
            return dao.queryAllHistory(type).map { it.keyword }
        }

        override suspend fun insertHistory(keyword: String, type: String) {
            dao.insertHistory(SearchHistoryEntity(keyword = keyword, type = type))
        }

        override suspend fun deleteHistory(keyword: String, type: String) {
            dao.deleteHistory(keyword, type)
        }

        override suspend fun deleteAllHistory(type: String) {
            dao.deleteAllHistory(type)
        }
    }
}
