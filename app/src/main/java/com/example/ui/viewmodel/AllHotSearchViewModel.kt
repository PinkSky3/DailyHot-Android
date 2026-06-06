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

private const val ALL_HOT_ALL_CATEGORY_KEY = "all"

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

data class AllHotSourceCategory(
    val key: String,
    val name: String,
    val count: Int
)

data class AllHotSourceOption(
    val id: Int,
    val title: String,
    val categoryKey: String,
    val categoryName: String,
    val rawType: String?
)

class AllHotSearchViewModel : ViewModel() {

    private val _activePlatform = MutableStateFlow(HotPlatform.WEIBO)
    val activePlatform: StateFlow<HotPlatform> = _activePlatform.asStateFlow()

    private val _sourceCategories = MutableStateFlow<List<AllHotSourceCategory>>(emptyList())
    val sourceCategories: StateFlow<List<AllHotSourceCategory>> = _sourceCategories.asStateFlow()

    private val _activeCategoryKey = MutableStateFlow(ALL_HOT_ALL_CATEGORY_KEY)
    val activeCategoryKey: StateFlow<String> = _activeCategoryKey.asStateFlow()

    private val _sourceOptions = MutableStateFlow<List<AllHotSourceOption>>(emptyList())
    val sourceOptions: StateFlow<List<AllHotSourceOption>> = _sourceOptions.asStateFlow()

    private val _activeSource = MutableStateFlow<AllHotSourceOption?>(null)
    val activeSource: StateFlow<AllHotSourceOption?> = _activeSource.asStateFlow()

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

    private var fetchJob: Job? = null
    private var hasLoadedOnce = false

    fun ensureLoaded() {
        if (!hasLoadedOnce) {
            loadSourcesAndSelectInitial()
        }
    }

    fun selectCategory(categoryKey: String) {
        if (_activeCategoryKey.value == categoryKey) return
        _activeCategoryKey.value = categoryKey

        val active = _activeSource.value
        if (active == null || !sourceMatchesCategory(active, categoryKey)) {
            sourcesForCategory(categoryKey).firstOrNull()?.let { selectSource(it) }
        }
    }

    fun selectSource(source: AllHotSourceOption) {
        if (_activeSource.value?.id == source.id && _uiState.value !is AllHotUiState.Error && _rawItems.value.isNotEmpty()) {
            return
        }
        setActiveSource(source)
        _searchQuery.value = ""
        fetchSourceData(source)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun refreshActivePlatform() {
        refreshActiveSource()
    }

    fun refreshActiveSource() {
        val source = _activeSource.value
        if (source == null) {
            loadSourcesAndSelectInitial(forceReload = true)
        } else {
            fetchSourceData(source)
        }
    }

    private fun loadSourcesAndSelectInitial(forceReload: Boolean = false) {
        if (!forceReload && _sourceOptions.value.isNotEmpty()) {
            (_activeSource.value ?: _sourceOptions.value.firstOrNull())?.let { source ->
                setActiveSource(source)
                fetchSourceData(source)
            }
            return
        }

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
            val channels = allHotChannels()
            if (channels.isEmpty()) {
                _uiState.value = AllHotUiState.Error("AllHot 公开代理未配置。公开安装版请设置 ALLHOT_PROXY_BASE_URL；本地私有构建可设置 ALLHOT_API_KEY，备用可设置 ALLHOT_BACKUP_API_KEY。")
                return@launch
            }

            var lastError = ""
            for (channel in channels) {
                try {
                    val sources = loadAllSources(channel)
                    val options = sources.mapNotNull { it.toSourceOption() }
                    if (options.isEmpty()) {
                        lastError = "AllHot ${channel.label}没有返回可用数据源。"
                        continue
                    }

                    updateSourceCollections(options)
                    val selected = chooseInitialSource(options)
                    setActiveSource(selected)
                    val success = fetchSourceDataWithFallback(selected, preferredChannel = channel)
                    if (success) return@launch
                    lastError = "AllHot ${channel.label}无法加载「${selected.title}」榜单。"
                } catch (e: Exception) {
                    lastError = "AllHot ${channel.label}接口异常: ${e.localizedMessage ?: e.message ?: "未知错误"}"
                }
            }

            _uiState.value = AllHotUiState.Error(lastError.ifBlank { "AllHot 公开代理和本地主备 key 均不可用，请检查代理服务或本地构建配置。" })
        }
    }

    private fun allHotChannels(): List<AllHotChannel> {
        val channels = mutableListOf<AllHotChannel>()
        normalizedProxyBaseUrl()?.let { channels += AllHotChannel.Proxy("公开代理", it) }

        listOf(
            AllHotChannel.Direct("主通道", BuildConfig.ALLHOT_OPEN_API_KEY),
            AllHotChannel.Direct("备用通道", BuildConfig.ALLHOT_BACKUP_OPEN_API_KEY)
        )
            .filter { it.apiKey.isNotBlank() }
            .distinctBy { it.apiKey }
            .forEach { channels += it }

        return channels
    }

    private fun normalizedProxyBaseUrl(): String? {
        val raw = BuildConfig.ALLHOT_PROXY_BASE_URL.trim()
        if (raw.isBlank()) return null
        return raw.trimEnd('/') + "/"
    }

    private suspend fun loadAllSources(channel: AllHotChannel): List<AllHotSource> {
        val sources = mutableListOf<AllHotSource>()
        var expectedTotal: Int? = null

        for (page in 1..10) {
            val response = getSources(channel, page)
            if (!response.isSuccessful || !isSuccessCode(response.body()?.code)) break

            val data = response.body()?.data ?: break
            val pageSources = data.list.orEmpty()
            if (pageSources.isEmpty()) break

            sources += pageSources
            expectedTotal = data.total ?: expectedTotal
            if (expectedTotal != null && sources.size >= expectedTotal) break
        }

        return sources.distinctBy { it.id }
    }

    private suspend fun searchSources(channel: AllHotChannel, keyword: String) = when (channel) {
        is AllHotChannel.Proxy -> RetrofitClient.allHotApi.searchSourcesViaProxy(
            url = channel.url("sources/search"),
            keyword = keyword
        )
        is AllHotChannel.Direct -> RetrofitClient.allHotApi.searchSources(
            apiKey = channel.apiKey,
            keyword = keyword
        )
    }

    private suspend fun getSources(channel: AllHotChannel, page: Int) = when (channel) {
        is AllHotChannel.Proxy -> RetrofitClient.allHotApi.getSourcesViaProxy(
            url = channel.url("sources"),
            page = page
        )
        is AllHotChannel.Direct -> RetrofitClient.allHotApi.getSources(
            apiKey = channel.apiKey,
            page = page
        )
    }

    private suspend fun getSourceData(channel: AllHotChannel, sourceId: Int) = when (channel) {
        is AllHotChannel.Proxy -> RetrofitClient.allHotApi.getSourceDataViaProxy(
            url = channel.url("sources/data"),
            id = sourceId
        )
        is AllHotChannel.Direct -> RetrofitClient.allHotApi.getSourceData(
            apiKey = channel.apiKey,
            id = sourceId
        )
    }

    private fun updateSourceCollections(options: List<AllHotSourceOption>) {
        _sourceOptions.value = options
        _sourceCategories.value = buildSourceCategories(options)
        if (_activeCategoryKey.value !in _sourceCategories.value.map { it.key }) {
            _activeCategoryKey.value = ALL_HOT_ALL_CATEGORY_KEY
        }
    }

    private fun buildSourceCategories(options: List<AllHotSourceOption>): List<AllHotSourceCategory> {
        val grouped = options
            .groupBy { it.categoryKey to it.categoryName }
            .map { (category, items) ->
                AllHotSourceCategory(
                    key = category.first,
                    name = category.second,
                    count = items.size
                )
            }
            .sortedWith(compareByDescending<AllHotSourceCategory> { it.count }.thenBy { it.name })

        return listOf(AllHotSourceCategory(ALL_HOT_ALL_CATEGORY_KEY, "全部", options.size)) + grouped
    }

    private fun chooseInitialSource(options: List<AllHotSourceOption>): AllHotSourceOption {
        val current = _activeSource.value
        if (current != null) {
            options.firstOrNull { it.id == current.id }?.let { return it }
        }
        return options.first()
    }

    private fun setActiveSource(source: AllHotSourceOption) {
        _activeSource.value = source
        _activePlatform.value = inferVisualPlatform(source.title)
    }

    private fun sourcesForCategory(categoryKey: String): List<AllHotSourceOption> {
        return _sourceOptions.value.filter { sourceMatchesCategory(it, categoryKey) }
    }

    private fun sourceMatchesCategory(source: AllHotSourceOption, categoryKey: String): Boolean {
        return categoryKey == ALL_HOT_ALL_CATEGORY_KEY || source.categoryKey == categoryKey
    }

    private fun AllHotSource.toSourceOption(): AllHotSourceOption? {
        val sourceId = id ?: return null
        val sourceTitle = displayTitle().takeIf { it.isNotBlank() } ?: return null
        val category = inferCategory(this, sourceTitle)
        return AllHotSourceOption(
            id = sourceId,
            title = sourceTitle,
            categoryKey = category.key,
            categoryName = category.name,
            rawType = type
        )
    }

    private fun inferCategory(source: AllHotSource, sourceTitle: String): AllHotSourceCategory {
        val rawType = source.type?.trim()
        if (!rawType.isNullOrBlank() && rawType != sourceTitle) {
            return AllHotSourceCategory(rawType.categoryKey(), rawType, 0)
        }

        val normalized = sourceTitle.normalize()
        val categoryName = when {
            listOf("微博", "知乎", "贴吧", "v2ex", "nodeseek", "豆瓣小组").any { normalized.contains(it.normalize()) } -> "社区"
            listOf("头条", "新闻", "澎湃", "网易", "腾讯", "新浪", "虎嗅", "爱范儿", "少数派").any { normalized.contains(it.normalize()) } -> "资讯"
            listOf("b站", "哔哩", "acfun", "抖音", "电影", "虎扑", "lol", "原神", "崩坏", "星穹").any { normalized.contains(it.normalize()) } -> "文娱"
            listOf("github", "csdn", "掘金", "51cto", "ithome", "it之家").any { normalized.contains(it.normalize()) } -> "科技"
            listOf("地震", "天气", "气象", "历史").any { normalized.contains(it.normalize()) } -> "生活"
            else -> "其他"
        }
        return AllHotSourceCategory(categoryName.categoryKey(), categoryName, 0)
    }

    private fun inferVisualPlatform(sourceTitle: String): HotPlatform {
        val title = sourceTitle.normalize()
        return HotPlatform.values().firstOrNull { platform ->
            sourceKeywords(platform).any { keyword ->
                val normalizedKeyword = keyword.normalize()
                normalizedKeyword.isNotBlank() && title.contains(normalizedKeyword)
            }
        } ?: HotPlatform.WEIBO
    }

    private fun fetchSourceData(source: AllHotSourceOption) {
        fetchJob?.cancel()
        _uiState.value = AllHotUiState.Loading
        _rawItems.value = emptyList()

        fetchJob = viewModelScope.launch {
            fetchSourceDataWithFallback(source)
        }
    }

    private suspend fun fetchSourceDataWithFallback(
        source: AllHotSourceOption,
        preferredChannel: AllHotChannel? = null
    ): Boolean {
        resetCurrentSourceMetadata()
        val channels = orderedChannels(preferredChannel)
        if (channels.isEmpty()) {
            _uiState.value = AllHotUiState.Error("AllHot 公开代理未配置。公开安装版请设置 ALLHOT_PROXY_BASE_URL；本地私有构建可设置 ALLHOT_API_KEY，备用可设置 ALLHOT_BACKUP_API_KEY。")
            return false
        }

        var lastError = ""
        for (channel in channels) {
            try {
                val response = getSourceData(channel, source.id)
                if (!response.isSuccessful) {
                    lastError = formatHttpError("AllHot ${channel.label}榜单数据请求失败", response.code(), channel)
                    continue
                }

                val body = response.body()
                if (!isSuccessCode(body?.code)) {
                    lastError = body?.message ?: body?.msg ?: "AllHot ${channel.label}榜单数据返回异常。"
                    continue
                }

                val data = body?.data
                val items = data?.list.orEmpty().mapNotNull { it.toHotSearchItem() }
                if (items.isEmpty()) {
                    lastError = "AllHot ${channel.label}返回了空榜单：${source.title}。"
                    continue
                }

                currentSourceTitle = source.title
                currentSourceId = source.id
                currentDataType = data?.dataType ?: source.rawType
                currentTotalCount = data?.total
                currentApiChannel = channel.label
                _rawItems.value = items
                _fetchedTime.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                applyFilter()
                return true
            } catch (e: Exception) {
                lastError = "AllHot ${channel.label}接口异常: ${e.localizedMessage ?: e.message ?: "未知错误"}"
            }
        }

        _uiState.value = AllHotUiState.Error(lastError.ifBlank { "AllHot 数据源不可用：${source.title}" })
        return false
    }

    private fun orderedChannels(preferredChannel: AllHotChannel?): List<AllHotChannel> {
        val channels = allHotChannels()
        if (preferredChannel == null) return channels
        return listOf(preferredChannel) + channels.filterNot { it.channelKey() == preferredChannel.channelKey() }
    }

    private fun resetCurrentSourceMetadata() {
        currentSourceTitle = ""
        currentSourceId = null
        currentDataType = null
        currentTotalCount = null
        currentApiChannel = ""
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

    private fun formatHttpError(prefix: String, code: Int, channel: AllHotChannel): String {
        return when (code) {
            401, 403 -> {
                val hint = when (channel) {
                    is AllHotChannel.Proxy -> "请检查 AllHot 代理服务的环境变量。"
                    is AllHotChannel.Direct -> "请检查 ALLHOT_API_KEY / ALLHOT_BACKUP_API_KEY。"
                }
                "$prefix：鉴权失败，$hint"
            }
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

    private fun String.categoryKey(): String {
        return normalize()
            .replace(Regex("[^a-z0-9\\u4e00-\\u9fa5]+"), "")
            .ifBlank { "other" }
    }

    private fun AllHotChannel.channelKey(): String {
        return when (this) {
            is AllHotChannel.Proxy -> "proxy:$baseUrl"
            is AllHotChannel.Direct -> "direct:$apiKey"
        }
    }

    private sealed class AllHotChannel(open val label: String) {
        data class Proxy(
            override val label: String,
            val baseUrl: String
        ) : AllHotChannel(label) {
            fun url(path: String): String = baseUrl + path
        }

        data class Direct(
            override val label: String,
            val apiKey: String
        ) : AllHotChannel(label)
    }
}
