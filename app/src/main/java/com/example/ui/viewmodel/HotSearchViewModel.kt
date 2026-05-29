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
        val updateTime: String?,
        val categories: List<HotSearchCategory> = emptyList(),
        val activeCategory: HotSearchCategory = HotSearchCategory.ALL
    ) : UiState
    data class Error(val message: String, val lastSuccessItems: List<HotSearchItem>? = null) : UiState
}

enum class HotSearchCategory(val displayName: String, val keywords: List<String> = emptyList()) {
    ALL("全部"),
    ENTERTAINMENT("娱乐", listOf("明星", "演员", "歌手", "综艺", "电影", "电视剧", "票房", "演唱会",
        "舞台", "偶像", "粉丝", "音乐", "专辑", "影视", "娱乐", "网红", "直播", "抖音", "快手",
        "B站", "b站", "视频", "UP主", "up主", "主播", "选秀", "出道", "mv", "MV", "奥斯卡")),
    SOCIETY("社会", listOf("社会", "警方", "通报", "官方", "调查", "事故", "案件", "法治",
        "突发", "现场", "救援", "消防", "地震", "台风", "暴雨", "洪水", "疫情", "防控",
        "政策", "规定", "条例", "民生", "维权", "庭审", "判决", "逮捕", "失踪", "死亡")),
    TECH("科技", listOf("科技", "手机", "芯片", "AI", "人工智能", "大模型", "华为", "苹果",
        "小米", "特斯拉", "机器人", "算法", "数据", "5G", "6G", "互联网", "软件",
        "硬件", "数码", "电脑", "笔记本", "系统", "操作系统", "自动驾驶", "VR", "AR",
        "元宇宙", "区块链", "比特币", "SpaceX", "NASA")),
    SPORTS("体育", listOf("体育", "比赛", "冠军", "决赛", "奥运", "NBA", "CBA", "英超", "西甲",
        "足球", "篮球", "乒乓球", "羽毛球", "网球", "中国男足", "中国女足", "国足",
        "世界杯", "亚运", "金牌", "银牌", "铜牌", "运动员", "教练", "转会")),
    FINANCE("财经", listOf("财经", "股票", "基金", "股市", "A股", "港股", "美股", "比特币",
        "美元", "人民币", "利率", "通胀", "GDP", "经济", "市场", "投资", "理财",
        "保险", "房地产", "楼市", "房价", "降息", "加息", "破产", "上市", "融资")),
    EDUCATION("教育", listOf("教育", "高考", "中考", "考研", "留学", "大学", "中小学", "学校",
        "教师", "教授", "学生", "考试", "分数", "分数线", "录取", "毕业", "就业",
        "培训", "课程", "网课", "论文", "学术")),
    HEALTH("健康", listOf("健康", "医疗", "医院", "医生", "患者", "疾病", "癌症", "新冠",
        "疫苗", "药物", "药品", "医保", "养生", "运动", "减肥", "睡眠", "心理",
        "抑郁", "焦虑", "手术", "治疗")),
    WORLD("国际", listOf("国际", "美国", "日本", "韩国", "英国", "法国", "德国", "俄罗斯",
        "乌克兰", "以色列", "巴勒斯坦", "联合国", "外交", "使馆", "总统", "首相",
        "全球", "海外", "国外", "北约", "欧盟", "WTO", "制裁", "冲突", "战争")),
    GAMING("游戏", listOf("游戏", "原神", "王者荣耀", "吃鸡", "永劫无间", "LOL", "英雄联盟",
        "梦幻西游", "DNF", "魔兽", "Steam", "switch", "PS5", "Xbox", "任天堂",
        "米哈游", "腾讯游戏", "网易游戏", "电竞", "比赛", "战队", "选手", "补丁",
        "更新", "资料片", "手游", "端游"));

    companion object {
        val allWithAll = values().toList()
    }
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

    private val _activeCategory = MutableStateFlow(HotSearchCategory.ALL)
    val activeCategory: StateFlow<HotSearchCategory> = _activeCategory.asStateFlow()

    private var _categorizedItems = mutableMapOf<HotSearchCategory, List<HotSearchItem>>()

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

    fun selectCategory(category: HotSearchCategory) {
        _activeCategory.value = category
        applyFilter()
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
                            _categorizedItems = classifyItems(items)
                            
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

    private fun classifyItems(items: List<HotSearchItem>): MutableMap<HotSearchCategory, List<HotSearchItem>> {
        val map = mutableMapOf<HotSearchCategory, MutableList<HotSearchItem>>()
        for (cat in HotSearchCategory.allWithAll) {
            map[cat] = mutableListOf()
        }
        for (item in items) {
            val text = buildString {
                append(item.title ?: "")
                append(" ")
                append(item.desc ?: "")
            }.lowercase()
            var assigned = false
            for (cat in HotSearchCategory.allWithAll) {
                if (cat == HotSearchCategory.ALL) continue
                for (kw in cat.keywords) {
                    if (text.contains(kw.lowercase())) {
                        map[cat]!!.add(item)
                        assigned = true
                        break
                    }
                }
                if (assigned) break
            }
            if (!assigned) {
                map[HotSearchCategory.ALL]!!.add(item)
            }
        }
        return map.mapValues { it.value.toList() }.toMutableMap()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim()
        val currentRaw = _rawItems.value
        val category = _activeCategory.value
        
        val categoryFiltered = if (category == HotSearchCategory.ALL) {
            currentRaw
        } else {
            _categorizedItems[category] ?: emptyList()
        }
        
        if (query.isEmpty()) {
            val cats = HotSearchCategory.allWithAll.map { c ->
                c to (_categorizedItems[c]?.size ?: 0)
            }
            _uiState.value = UiState.Success(
                items = categoryFiltered,
                filteredCount = categoryFiltered.size,
                updateTime = _fetchedTime.value,
                categories = cats.filter { it.second > 0 || it.first == HotSearchCategory.ALL }.map { it.first },
                activeCategory = category
            )
        } else {
            val filtered = categoryFiltered.filter { item ->
                (item.title?.contains(query, ignoreCase = true) ?: false) || 
                (item.desc?.contains(query, ignoreCase = true) ?: false) ||
                (item.author?.contains(query, ignoreCase = true) ?: false)
            }
            _uiState.value = UiState.Success(
                items = filtered,
                filteredCount = filtered.size,
                updateTime = _fetchedTime.value,
                categories = emptyList(),
                activeCategory = HotSearchCategory.ALL
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
