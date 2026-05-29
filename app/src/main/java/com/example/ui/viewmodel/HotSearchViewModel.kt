package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.model.HotPlatform
import com.example.data.model.HotSearchItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState {
    object Loading : UiState
    data class Success(
        val items: List<HotSearchItem>,
        val filteredCount: Int,
        val updateTime: String?
    ) : UiState
    data class Error(val message: String, val lastSuccessItems: List<HotSearchItem>? = null) : UiState
}

class HotSearchViewModel : ViewModel() {

    private val _activePlatform = MutableStateFlow(HotPlatform.WEIBO)
    val activePlatform: StateFlow<HotPlatform> = _activePlatform.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _rawItems = MutableStateFlow<List<HotSearchItem>>(emptyList())
    private val _fetchedTime = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        fetchHotSearch(HotPlatform.WEIBO)
    }

    fun selectPlatform(platform: HotPlatform) {
        if (_activePlatform.value == platform && _uiState.value !is UiState.Error && _rawItems.value.isNotEmpty()) {
            return
        }
        _activePlatform.value = platform
        _searchQuery.value = "" // Reset search query on tab change
        fetchHotSearch(platform)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun refreshActivePlatform() {
        fetchHotSearch(_activePlatform.value)
    }

    private fun fetchHotSearch(platform: HotPlatform) {
        fetchJob?.cancel()
        _uiState.value = UiState.Loading
        _rawItems.value = emptyList()

        val endpoints = getFallbackEndpoints(platform)

        fetchJob = viewModelScope.launch {
            var lastErrorMsg = ""
            var success = false

            for (endpoint in endpoints) {
                try {
                    val response = RetrofitClient.apiService.getHotListWithUrl(endpoint)
                    if (response.isSuccessful && response.body() != null) {
                        val bodyString = response.body()!!.string()
                        val items = extractListFromJson(bodyString)
                        if (items.isNotEmpty()) {
                            _rawItems.value = items
                            
                            // Format update time or show local time
                            val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                
                            _fetchedTime.value = "$formattedTime | 源: ${getHostName(endpoint)}"
                            applyFilter()
                            success = true
                            break
                        } else {
                            lastErrorMsg += "\n节点(${getHostName(endpoint)})解析空"
                        }
                    } else {
                        lastErrorMsg += "\n节点(${getHostName(endpoint)})错误: ${response.code()}"
                    }
                } catch (e: Exception) {
                    lastErrorMsg += "\n节点(${getHostName(endpoint)})异常: ${e.localizedMessage ?: e.message ?: "未知"}"
                }
            }

            if (!success) {
                val finalError = lastErrorMsg.trim().ifBlank { "数据获取失败，所有节点均不可用。" }
                _uiState.value = UiState.Error(finalError, lastSuccessItems = null)
            }
        }
    }

    private fun getFallbackEndpoints(platform: HotPlatform): List<String> {
        val list = mutableListOf<String>()
        // General backup URLs
        val dailyHotMirrors = listOf(
            "https://dailyhotapi.3yu3.top",
            "https://dailyhot.api.lkwplus.com"
        )
        
        when (platform) {
            HotPlatform.WEIBO -> {
                list.add("https://cn.apihz.cn/api/xinwen/weibo.php?id=88888888&key=88888888")
                list.add("https://www.haotechs.cn/ljh-wx/api/weiboHot")
                list.add("https://api.xunjinlu.fun/api/rebang/weibo.php")
            }
            HotPlatform.ZHIHU -> {
                list.add("https://v.api.aa1.cn/api/zhihu-news/index.php?aa1=xiarou")
            }
            HotPlatform.BAIDU -> {
                list.add("https://api.xma.run/api/tools/bdhot/?type=game")
                list.add("https://v.api.aa1.cn/api/sougou-baidu/index.php?aa1=xiarou")
            }
            HotPlatform.NGABBS -> {
                list.add("https://cn.apihz.cn/api/xinwen/nga.php?id=88888888&key=88888888")
            }
            else -> {}
        }

        for (mirror in dailyHotMirrors) {
            list.add("$mirror/${platform.key}")
        }
        return list
    }

    private fun extractListFromJson(jsonString: String): List<HotSearchItem> {
        val items = mutableListOf<HotSearchItem>()
        try {
            val rootStr = jsonString.trim()
            if (rootStr.startsWith("[")) {
                val jsonArray = org.json.JSONArray(rootStr)
                items.addAll(parseJsonArray(jsonArray))
            } else if (rootStr.startsWith("{")) {
                val rootObj = org.json.JSONObject(rootStr)
                // first try "data" field directly if it's an array
                val array = findDeepestArray(rootObj)
                if (array != null) {
                    items.addAll(parseJsonArray(array))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } catch (e: StackOverflowError) {
            System.err.println("JSON too deeply nested, skipping")
        }
        return items
    }

    private fun findDeepestArray(obj: org.json.JSONObject, depth: Int = 0): org.json.JSONArray? {
        if (depth > 20) return null
        val knownKeys = listOf("data", "list", "result", "news", "hotList", "hot", "items", "routes")
        for (key in knownKeys) {
            if (obj.has(key)) {
                val v = obj.get(key)
                if (v is org.json.JSONArray && v.length() > 0) return v
            }
        }
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val v = obj.get(key)
            if (v is org.json.JSONArray && v.length() > 0) return v
            if (v is org.json.JSONObject) {
                val inner = findDeepestArray(v, depth + 1)
                if (inner != null) return inner
            }
        }
        return null
    }

    private fun parseJsonArray(array: org.json.JSONArray): List<HotSearchItem> {
        val list = mutableListOf<HotSearchItem>()
        for (i in 0 until array.length()) {
            val element = array.get(i)
            if (element is org.json.JSONObject) {
                val title = extractStringExt(element, listOf("title", "name", "keyword", "word", "hotword", "text", "content", "topic_name"))
                if (title.isNullOrBlank()) continue
                val url = extractStringExt(element, listOf("url", "link", "mobileUrl", "href", "short_url"))
                val desc = extractStringExt(element, listOf("desc", "description", "summary", "detail", "note"))
                val hotStr = extractStringExt(element, listOf("hot", "score", "index", "hotValue", "num", "hot_score", "search_volume"))
                val hotObj = if (hotStr != null) com.example.data.model.CoercedString(hotStr) else null
                list.add(HotSearchItem(title = title, url = url, desc = desc, hot = hotObj))
            } else if (element is String) {
                list.add(HotSearchItem(title = element, url = null))
            }
        }
        return list
    }

    private fun extractStringExt(obj: org.json.JSONObject, candidates: List<String>): String? {
        for (c in candidates) {
            if (obj.has(c) && !obj.isNull(c)) {
                return obj.get(c).toString()
            }
        }
        return null
    }

    private fun getHostName(url: String): String {
        return try {
            java.net.URL(url).host
        } catch (e: Exception) {
            url
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim()
        val currentRaw = _rawItems.value
        
        if (query.isEmpty()) {
            _uiState.value = UiState.Success(
                items = currentRaw,
                filteredCount = currentRaw.size,
                updateTime = _fetchedTime.value
            )
        } else {
            val filtered = currentRaw.filter { item ->
                (item.title?.contains(query, ignoreCase = true) ?: false) || 
                (item.desc?.contains(query, ignoreCase = true) ?: false) ||
                (item.author?.contains(query, ignoreCase = true) ?: false)
            }
            _uiState.value = UiState.Success(
                items = filtered,
                filteredCount = filtered.size,
                updateTime = _fetchedTime.value
            )
        }
    }

    private fun formatIsoTime(isoString: String): String {
        return try {
            // "2026-05-29T09:28:46.158Z" -> Simple "HH:mm" or "MM-dd HH:mm"
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = parser.parse(isoString) ?: return isoString
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            formatter.format(date)
        } catch (e: Exception) {
            isoString
        }
    }
}
