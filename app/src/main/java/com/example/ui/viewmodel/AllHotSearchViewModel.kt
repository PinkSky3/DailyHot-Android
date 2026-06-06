package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.RetrofitClient
import com.example.data.model.AllHotNewsItem
import com.example.data.model.AllHotSource
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

sealed interface AllHotUiState {
    object Loading : AllHotUiState
    data class Success(
        val items: List<HotSearchItem>,
        val filteredCount: Int,
        val updateTime: String?,
        val sourceTitle: String,
        val sourceId: Int?,
        val dataType: String?,
        val totalCount: Int?,
        val apiChannel: String
    ) : AllHotUiState
    data class Error(val message: String, val lastSuccessItems: List<HotSearchItem>? = null) : AllHotUiState
}

class AllHotSearchViewModel : ViewModel() {

    private val _activePlatform = MutableStateFlow(HotPlatform.WEIBO)
    val activePlatform: StateFlow<HotPlatform> = _activePlatform.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _rawItems = MutableStateFlow<List<HotSearchItem>>(emptyList())
    private val _fetchedTime = MutableStateFlow<String?>(null)

    private var currentSourceTitle: String = ""
    private var currentSourceId: Int? = null
    private var currentDataType: String? = null
    private var currentTotalCount: Int? = null
    private var currentApiChannel: String = ""

    private val _uiState = MutableStateFlow<AllHotUiState>(AllHotUiState.Loading)
    val uiState: StateFlow<AllHotUiState> = _uiState.asStateFlow()

    private val sourceCache = mutableMapOf<HotPlatform, AllHotSource>()
    private var fetchJob: Job? = null
    private var hasLoadedOnce = false

    fun ensureLoaded() {
        if (!hasLoadedOnce) {
            fetchHotSearch(_activePlatform.value)
        }
    }

    fun selectPlatform(platform: HotPlatform) {
        if (_activePlatform.value == platform && _uiState.value !is AllHotUiState.Error && _rawItems.value.isNotEmpty()) {
            return
        }
        _activePlatform.value = platform
        _searchQuery.value = ""
        fetchHotSearch(platform)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun refreshActivePlatform() {
        sourceCache.remove(_activePlatform.value)
        fetchHotSearch(_activePlatform.value)
    }

    private fun fetchHotSearch(platform: HotPlatform) {
        hasLoadedOnce = true
        fetchJob?.cancel()
        _uiState.value = AllHotUiState.Loading
        _rawItems.value = emptyList()
        currentSourceTitle = ""
        currentSourceId = null
        currentDataType = null
        currentTotalCount = null
        currentApiChannel = ""

        fetchJob = viewModelScope.launch {
            val credentials = allHotCredentials()
            if (credentials.isEmpty()) {
                _uiState.value = AllHotUiState.Error("AllHot API key 未配置。请在本地 .env 或 GitHub Secrets 中设置 ALLHOT_API_KEY，备用可设置 ALLHOT_BACKUP_API_KEY。")
                return@launch
            }

            var lastError = ""
            for (credential in credentials) {
                try {
                    val source = resolveAllHotSource(platform, credential.apiKey)
                    val sourceId = source?.id
                    if (sourceId == null) {
                        lastError = "AllHot ${credential.label}没有匹配到「${platform.displayName}」数据源。"
                        continue
                    }

                    val response = RetrofitClient.allHotApi.getSourceData(apiKey = credential.apiKey, id = sourceId)
                    if (!response.isSuccessful) {
                        lastError = formatHttpError("AllHot ${credential.label}榜单数据请求失败", response.code())
                        continue
                    }

                    val body = response.body()
                    if (!isSuccessCode(body?.code)) {
                        lastError = body?.message ?: body?.msg ?: "AllHot ${credential.label}榜单数据返回异常。"
                        continue
                    }

                    val data = body?.data
                    val items = data?.list.orEmpty().mapNotNull { it.toHotSearchItem() }
                    if (items.isEmpty()) {
                        lastError = "AllHot ${credential.label}返回了空榜单：${source.displayTitle()}。"
                        continue
                    }

                    currentSourceTitle = source.displayTitle()
                    currentSourceId = sourceId
                    currentDataType = data?.dataType
                    currentTotalCount = data?.total
                    currentApiChannel = credential.label
                    _rawItems.value = items
                    _fetchedTime.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    applyFilter()
                    return@launch
                } catch (e: Exception) {
                    lastError = "AllHot ${credential.label}接口异常: ${e.localizedMessage ?: e.message ?: "未知错误"}"
                }
            }

            _uiState.value = AllHotUiState.Error(lastError.ifBlank { "AllHot 主备 key 均不可用，请检查 ALLHOT_API_KEY 和 ALLHOT_BACKUP_API_KEY。" })
        }
    }

    private fun allHotCredentials(): List<AllHotCredential> {
        return listOf(
            AllHotCredential("主通道", BuildConfig.ALLHOT_OPEN_API_KEY),
            AllHotCredential("备用通道", BuildConfig.ALLHOT_BACKUP_OPEN_API_KEY)
        ).filter { it.apiKey.isNotBlank() }.distinctBy { it.apiKey }
    }

    private suspend fun resolveAllHotSource(platform: HotPlatform, apiKey: String): AllHotSource? {
        sourceCache[platform]?.let { return it }

        for (keyword in sourceKeywords(platform)) {
            val response = RetrofitClient.allHotApi.searchSources(apiKey = apiKey, keyword = keyword)
            if (!response.isSuccessful || !isSuccessCode(response.body()?.code)) continue
            val source = chooseBestSource(response.body()?.data?.list.orEmpty(), platform, keyword)
            if (source?.id != null) {
                sourceCache[platform] = source
                return source
            }
        }

        for (page in 1..3) {
            val response = RetrofitClient.allHotApi.getSources(apiKey = apiKey, page = page)
            if (!response.isSuccessful || !isSuccessCode(response.body()?.code)) continue
            val source = chooseBestSource(response.body()?.data?.list.orEmpty(), platform, platform.displayName)
            if (source?.id != null) {
                sourceCache[platform] = source
                return source
            }
        }

        return null
    }

    private fun chooseBestSource(
        sources: List<AllHotSource>,
        platform: HotPlatform,
        keyword: String
    ): AllHotSource? {
        val candidates = sourceKeywords(platform) + keyword + platform.key
        return sources
            .filter { it.id != null && it.displayTitle().isNotBlank() }
            .maxByOrNull { source ->
                val title = source.displayTitle().normalize()
                candidates.sumOf { candidate ->
                    val normalized = candidate.normalize()
                    when {
                        normalized.isBlank() -> 0
                        title == normalized -> 40
                        title.contains(normalized) -> 20
                        normalized.contains(title) -> 10
                        else -> 0
                    }
                }
            }
            ?.takeIf { source ->
                val title = source.displayTitle().normalize()
                candidates.any { candidate ->
                    val normalized = candidate.normalize()
                    normalized.isNotBlank() && (title.contains(normalized) || normalized.contains(title))
                }
            }
    }

    private fun sourceKeywords(platform: HotPlatform): List<String> {
        return when (platform) {
            HotPlatform.WEIBO -> listOf("微博热搜", "微博")
            HotPlatform.ZHIHU -> listOf("知乎热榜", "知乎")
            HotPlatform.BAIDU -> listOf("百度热搜", "百度")
            HotPlatform.BILIBILI -> listOf("哔哩哔哩", "B站", "bilibili")
            HotPlatform.TOUTIAO -> listOf("今日头条", "头条")
            HotPlatform.TIEBA -> listOf("百度贴吧", "贴吧")
            HotPlatform.SSPAI -> listOf("少数派")
            HotPlatform.KR36 -> listOf("36氪", "36kr")
            HotPlatform.CSDN -> listOf("CSDN")
            HotPlatform.DOUYIN -> listOf("抖音")
            HotPlatform.DOUBAN_GROUP -> listOf("豆瓣小组", "豆瓣")
            HotPlatform.DOUBAN_MOVIE -> listOf("豆瓣电影")
            HotPlatform.HISTORY -> listOf("历史上的今天", "历史今日")
            HotPlatform.FIFTYONE_CTO -> listOf("51CTO")
            HotPlatform.ACFUN -> listOf("AcFun", "A站")
            HotPlatform.COOLAPK -> listOf("酷安")
            HotPlatform.EARTHQUAKE -> listOf("地震速报", "地震")
            HotPlatform.GENSHIN -> listOf("原神", "原神Tap")
            HotPlatform.HELLOGITHUB -> listOf("HelloGitHub", "GitHub")
            HotPlatform.HONKAI -> listOf("崩坏", "星铁Tap")
            HotPlatform.HUPU -> listOf("虎扑")
            HotPlatform.HUXIU -> listOf("虎嗅")
            HotPlatform.IFANR -> listOf("爱范儿", "ifanr")
            HotPlatform.ITHOME -> listOf("IT之家")
            HotPlatform.ITHOME_XIJIAYI -> listOf("IT之家限免", "限免")
            HotPlatform.JIANSHU -> listOf("简书")
            HotPlatform.JUEJIN -> listOf("掘金")
            HotPlatform.LOL -> listOf("英雄联盟", "LOL")
            HotPlatform.NETEASE_NEWS -> listOf("网易新闻", "网易")
            HotPlatform.NGABBS -> listOf("NGA")
            HotPlatform.NODESEEK -> listOf("NodeSeek")
            HotPlatform.QQ_NEWS -> listOf("腾讯新闻", "腾讯")
            HotPlatform.SINA_NEWS -> listOf("新浪新闻")
            HotPlatform.SINA -> listOf("新浪网", "新浪")
            HotPlatform.STARRAIL_MIYOUSHE -> listOf("星穹铁道", "米游社")
            HotPlatform.THEPAPER -> listOf("澎湃新闻", "澎湃")
            HotPlatform.V2EX -> listOf("V2EX")
            HotPlatform.WEATHERALARM -> listOf("气象预警", "天气预警")
            HotPlatform.WEREAD -> listOf("微信读书")
            HotPlatform.ZHIHU_DAILY -> listOf("知乎日报")
        }
    }

    private fun AllHotNewsItem.toHotSearchItem(): HotSearchItem? {
        val headline = title?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val descriptionText = firstNonBlank(desc, description, summary)
        val link = firstNonBlank(jumpUrl, url, mobileUrl)
        val image = firstNonBlank(imageUrl, cover, pic)
        val hotValue = hot ?: hotValue ?: index

        return HotSearchItem(
            title = headline,
            desc = descriptionText,
            cover = image,
            pic = image,
            hot = hotValue,
            url = link,
            mobileUrl = mobileUrl
        )
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim()
        val currentRaw = _rawItems.value
        val filtered = if (query.isEmpty()) {
            currentRaw
        } else {
            currentRaw.filter { item ->
                (item.title?.contains(query, ignoreCase = true) ?: false) ||
                    (item.desc?.contains(query, ignoreCase = true) ?: false) ||
                    (item.author?.contains(query, ignoreCase = true) ?: false)
            }
        }

        _uiState.value = AllHotUiState.Success(
            items = filtered,
            filteredCount = filtered.size,
            updateTime = _fetchedTime.value,
            sourceTitle = currentSourceTitle.ifBlank { _activePlatform.value.displayName },
            sourceId = currentSourceId,
            dataType = currentDataType,
            totalCount = currentTotalCount,
            apiChannel = currentApiChannel.ifBlank { "主通道" }
        )
    }

    private fun isSuccessCode(code: Int?): Boolean = code == null || code == 0 || code == 200

    private fun formatHttpError(prefix: String, code: Int): String {
        return when (code) {
            401, 403 -> "$prefix：鉴权失败，请检查 ALLHOT_API_KEY / ALLHOT_BACKUP_API_KEY。"
            else -> "$prefix：HTTP $code。"
        }
    }

    private fun AllHotSource.displayTitle(): String = firstNonBlank(title, name, type) ?: ""

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun String.normalize(): String {
        return lowercase(Locale.ROOT)
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")
            .replace("热搜", "")
            .replace("热榜", "")
            .replace("榜单", "")
    }

    private data class AllHotCredential(
        val label: String,
        val apiKey: String
    )
}
