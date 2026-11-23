package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.FallbackFetchResult
import com.example.data.api.RetrofitClient
import com.example.data.api.fetchFirstParsed
import com.example.data.api.lastErrorSuffix
import com.example.data.model.OilPriceEntry
import com.example.data.model.OilPriceRootResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed interface OilPriceUiState {
    object Loading : OilPriceUiState
    data class Success(
        val province: String,
        val entries: List<OilPriceEntry>,
        val updateTime: String?,
        val nextUpdateTime: String?
    ) : OilPriceUiState
    data class Error(val message: String) : OilPriceUiState
}

/**
 * 油价数据源（均已在大陆网络实测可直连，按顺序兜底）：
 * 1. 60s API 官方实例：数据最新，但请求稍频繁就会返回 429 限流。
 * 2. api.nxvav.cn：与 60s 同格式的国内实例。
 * 3. 60s.crystelf.top：60s 公共镜像。
 * 4. api.qqsuu.cn：独立数据源，按省份返回 92/95/98/0 号价格。
 * 5. v2.xxapi.cn：一次返回 31 个省份，作为最后兜底。
 *
 * 已移除：api.yanyua.icu（SSL 握手失败）、60s.7se.cn（404）、
 * 60s.superjeason.qzz.io（所在 Deno Deploy Classic 平台已宣布停止服务）。
 */
private val SIXTY_S_FORMAT_ENDPOINTS = listOf(
    "https://60s.viki.moe/v2/fuel-price",
    "https://api.nxvav.cn/api/fuel-price/",
    "https://60s.crystelf.top/v2/fuel-price"
)
private const val QQSUU_OIL_URL = "https://api.qqsuu.cn/api/dm-oilprice"
private const val XXAPI_OIL_URL = "https://v2.xxapi.cn/api/oilPrice"

/** 合理油价区间（元/升），用于过滤异常或错位数据。 */
private const val MIN_VALID_PRICE = 3.0
private const val MAX_VALID_PRICE = 20.0

private const val LABEL_92 = "92#\u6C7D\u6CB9"
private const val LABEL_95 = "95#\u6C7D\u6CB9"
private const val LABEL_98 = "98#\u6C7D\u6CB9"
private const val LABEL_0 = "0#\u67F4\u6CB9"

class OilPriceViewModel : ViewModel() {

    private val responseAdapter = RetrofitClient.moshi.adapter(OilPriceRootResponse::class.java)

    private val _selectedProvince = MutableStateFlow("浙江")
    val selectedProvince: StateFlow<String> = _selectedProvince.asStateFlow()

    private val _uiState = MutableStateFlow<OilPriceUiState>(OilPriceUiState.Loading)
    val uiState: StateFlow<OilPriceUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        fetchOilPrice()
    }

    fun selectProvince(province: String) {
        _selectedProvince.value = province
        fetchOilPrice()
    }

    fun refresh() {
        fetchOilPrice()
    }

    private fun fetchOilPrice() {
        fetchJob?.cancel()
        _uiState.value = OilPriceUiState.Loading

        val province = _selectedProvince.value

        fetchJob = viewModelScope.launch {
            val providers = buildProviders(province)
            when (val result = RetrofitClient.publicApi.fetchFirstParsed(
                urls = providers.keys,
                parse = { url, body -> runCatching { providers.getValue(url)(body) }.getOrNull() }
            )) {
                is FallbackFetchResult.Success -> {
                    val parsed = result.value
                    _uiState.value = OilPriceUiState.Success(
                        province = parsed.province ?: province,
                        entries = parsed.entries,
                        updateTime = parsed.updateTime,
                        nextUpdateTime = parsed.nextUpdateTime
                    )
                }
                is FallbackFetchResult.Failure -> {
                    _uiState.value = OilPriceUiState.Error(
                        "\u6CB9\u4EF7\u6570\u636E\u83B7\u53D6\u5931\u8D25\uFF0C\u5DF2\u5C1D\u8BD5\u591A\u4E2A\u516C\u5171\u5B9E\u4F8B${result.lastErrorSuffix()}"
                    )
                }
            }
        }
    }

    /** 按优先级生成「请求地址 → 解析函数」，LinkedHashMap 保证请求顺序。 */
    private fun buildProviders(province: String): LinkedHashMap<String, (String) -> ParsedOilPrice?> {
        val encoded = URLEncoder.encode(province, StandardCharsets.UTF_8.name())
        val providers = linkedMapOf<String, (String) -> ParsedOilPrice?>()
        for (endpoint in SIXTY_S_FORMAT_ENDPOINTS) {
            providers["$endpoint?region=$encoded"] = { body -> parseSixtySeconds(body, province) }
        }
        providers["$QQSUU_OIL_URL?prov=$encoded"] = { body -> parseQqsuu(body, province) }
        providers[XXAPI_OIL_URL] = { body -> parseXxapi(body, province) }
        return providers
    }

    private data class ParsedOilPrice(
        val province: String?,
        val entries: List<OilPriceEntry>,
        val updateTime: String?,
        val nextUpdateTime: String?
    )

    /** 60s 格式：{ code, data: { region, items: [{ name, price, price_desc }], updated } } */
    private fun parseSixtySeconds(body: String, requestedProvince: String): ParsedOilPrice? {
        val root = responseAdapter.fromJson(body) ?: return null
        if (root.code != 200) return null
        val data = root.data ?: return null

        val region = data.region ?: data.province?.pri_name
        if (region != null && !isSameProvince(region, requestedProvince)) return null

        val itemEntries = data.items.orEmpty().mapNotNull { item ->
            val name = item.name?.trim().orEmpty()
            val value = item.price ?: extractNumber(item.price_desc)
            if (name.isBlank() || !isValidPrice(value)) null
            else OilPriceEntry(name, item.price_desc?.trim()?.takeIf { it.isNotBlank() } ?: formatPrice(value!!))
        }

        val entries = itemEntries.ifEmpty {
            val prov = data.province ?: return null
            listOfNotNull(
                buildEntry(LABEL_92, extractNumber(prov.gasoline_92)),
                buildEntry(LABEL_95, extractNumber(prov.gasoline_95)),
                buildEntry(LABEL_98, extractNumber(prov.gasoline_98)),
                buildEntry(LABEL_0, extractNumber(prov.diesel_0))
            )
        }
        if (entries.isEmpty()) return null

        return ParsedOilPrice(
            province = region,
            entries = entries,
            updateTime = data.updated_at?.let(::formatBeijingTime) ?: data.updated ?: data.time,
            nextUpdateTime = data.trend?.description ?: data.trend?.next_adjustment_date ?: data.next_update_time
        )
    }

    /** qqsuu：{ code, data: { prov, p92, p95, p98, p0, time } } */
    private fun parseQqsuu(body: String, requestedProvince: String): ParsedOilPrice? {
        val root = JSONObject(body)
        if (root.optInt("code") != 200) return null
        val data = root.optJSONObject("data") ?: return null

        val region = data.optString("prov").takeIf { it.isNotBlank() }
        if (region != null && !isSameProvince(region, requestedProvince)) return null

        val entries = listOfNotNull(
            buildEntry(LABEL_92, data.optString("p92").toDoubleOrNull()),
            buildEntry(LABEL_95, data.optString("p95").toDoubleOrNull()),
            buildEntry(LABEL_98, data.optString("p98").toDoubleOrNull()),
            buildEntry(LABEL_0, data.optString("p0").toDoubleOrNull())
        )
        if (entries.isEmpty()) return null

        return ParsedOilPrice(
            province = region,
            entries = entries,
            updateTime = data.optString("time").substringBefore('.').takeIf { it.isNotBlank() },
            nextUpdateTime = null
        )
    }

    /** xxapi：{ code, data: [{ regionName, n92, n95, n98, n0, date }] }，一次返回全部省份。 */
    private fun parseXxapi(body: String, requestedProvince: String): ParsedOilPrice? {
        val root = JSONObject(body)
        if (root.optInt("code") != 200) return null
        val list = root.optJSONArray("data") ?: return null

        val match = (0 until list.length())
            .mapNotNull { list.optJSONObject(it) }
            .firstOrNull { isSameProvince(it.optString("regionName"), requestedProvince) }
            ?: return null

        val entries = listOfNotNull(
            buildEntry(LABEL_92, match.optDouble("n92", Double.NaN)),
            buildEntry(LABEL_95, match.optDouble("n95", Double.NaN)),
            buildEntry(LABEL_98, match.optDouble("n98", Double.NaN)),
            buildEntry(LABEL_0, match.optDouble("n0", Double.NaN))
        )
        if (entries.isEmpty()) return null

        val adjustDate = match.optString("date").takeIf { it.isNotBlank() && it != "null" }
        return ParsedOilPrice(
            province = requestedProvince,
            entries = entries,
            updateTime = adjustDate?.let { "$it\uFF08\u8C03\u4EF7\u65E5\uFF09" },
            nextUpdateTime = null
        )
    }

    private fun buildEntry(label: String, value: Double?): OilPriceEntry? =
        if (isValidPrice(value)) OilPriceEntry(label, formatPrice(value!!)) else null

    private fun isValidPrice(value: Double?): Boolean =
        value != null && !value.isNaN() && value in MIN_VALID_PRICE..MAX_VALID_PRICE

    private fun extractNumber(raw: String?): Double? =
        raw?.let { Regex("\\d+(\\.\\d+)?").find(it)?.value?.toDoubleOrNull() }

    private fun formatPrice(value: Double): String =
        String.format(Locale.getDefault(), "%.2f \u5143/\u5347", value)

    /**
     * 各实例的 updated 字段使用各自服务器时区（如 nxvav 为 UTC，比北京时间慢 8 小时），
     * 因此优先用 updated_at 时间戳统一换算为北京时间。兼容秒和毫秒两种时间戳。
     */
    private fun formatBeijingTime(timestamp: Long): String? {
        if (timestamp <= 0L) return null
        val millis = if (timestamp < 100_000_000_000L) timestamp * 1000 else timestamp
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).apply {
            timeZone = BEIJING_TIME_ZONE
        }.format(Date(millis))
    }

    /** 兼容「浙江」「浙江省」「北京市」等写法。 */
    private fun isSameProvince(a: String, b: String): Boolean {
        val left = normalizeProvince(a)
        val right = normalizeProvince(b)
        if (left.isEmpty() || right.isEmpty()) return false
        return left == right || left.startsWith(right) || right.startsWith(left)
    }

    private fun normalizeProvince(name: String): String =
        name.trim().removeSuffix("\u7701").removeSuffix("\u5E02")
}

private val BEIJING_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")
