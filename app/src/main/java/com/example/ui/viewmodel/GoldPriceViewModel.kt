package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.FallbackFetchResult
import com.example.data.api.RetrofitClient
import com.example.data.api.fetchFirstParsed
import com.example.data.api.lastErrorSuffix
import com.example.data.model.GoldBankRecycleEntry
import com.example.data.model.GoldBrandEntry
import com.example.data.model.GoldMarketEntry
import com.example.data.model.GoldPriceSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed interface GoldPriceUiState {
    object Loading : GoldPriceUiState
    data class Success(val snapshot: GoldPriceSnapshot) : GoldPriceUiState
    data class Error(val message: String) : GoldPriceUiState
}

/**
 * 金价数据源（均已在大陆网络实测可直连）：
 * 1. 60s API 官方实例：国内/国际行情，数据最全。
 * 2. 东方财富行情接口：60s 不可用或数据异常时兜底，提供 Au99.99、Au(T+D)、伦敦金、COMEX 黄金。
 * 3. xxapi.cn：品牌金店报价、银行金条与回收价，作为补充数据独立请求。
 */
private const val SIXTY_S_GOLD_URL = "https://60s.viki.moe/v2/gold-price"
private const val EASTMONEY_GOLD_URL =
    "https://push2.eastmoney.com/api/qt/ulist.np/get?fltt=2&invt=2" +
        "&secids=118.AU9999,118.AUTD,122.XAU,101.GC00Y" +
        "&fields=f2,f3,f12,f14,f15,f16,f17,f124"
private const val XXAPI_GOLD_URL = "https://v2.xxapi.cn/api/goldprice"

private const val UNIT_CNY_PER_GRAM = "\u5143/\u514B"
private const val UNIT_USD_PER_OUNCE = "\u7F8E\u5143/\u76CE\u53F8"

class GoldPriceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<GoldPriceUiState>(GoldPriceUiState.Loading)
    val uiState: StateFlow<GoldPriceUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.value = GoldPriceUiState.Loading
            try {
                val (main, extra) = coroutineScope {
                    val mainDeferred = async { fetchMainGoldData() }
                    val extraDeferred = async { fetchSupplementaryGoldData() }
                    mainDeferred.await() to extraDeferred.await()
                }

                // 主行情和补充数据任意一方成功即展示，不再因主数据源失效而整页报错。
                if (main is MainGoldResult.Failed && !extra.hasData) {
                    _uiState.value = GoldPriceUiState.Error(
                        "\u91D1\u4EF7\u6570\u636E\u83B7\u53D6\u5931\u8D25\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5${main.detail}"
                    )
                    return@launch
                }

                val snapshot = when (main) {
                    is MainGoldResult.Loaded -> main.snapshot
                    is MainGoldResult.Failed -> GoldPriceSnapshot(
                        domesticMarkets = emptyList(),
                        internationalMarkets = emptyList(),
                        brands = emptyList(),
                        bankRecycle = emptyList(),
                        source = "",
                        updateTime = extra.updateTime ?: "\u672A\u77E5"
                    )
                }
                val source = listOf(snapshot.source, if (extra.hasData) "xxapi.cn" else "")
                    .filter { it.isNotBlank() }
                    .joinToString(" + ")

                _uiState.value = GoldPriceUiState.Success(
                    snapshot.copy(
                        brands = snapshot.brands + extra.brands,
                        bankRecycle = extra.bankRecycle,
                        source = source
                    )
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (e: Exception) {
                _uiState.value = GoldPriceUiState.Error(
                    "\u91D1\u4EF7\u63A5\u53E3\u5F02\u5E38: ${e.localizedMessage ?: e.message ?: "\u672A\u77E5\u9519\u8BEF"}"
                )
            }
        }
    }

    private suspend fun fetchMainGoldData(): MainGoldResult {
        val providers = linkedMapOf<String, (String) -> GoldPriceSnapshot?>(
            SIXTY_S_GOLD_URL to ::parseSixtySeconds,
            EASTMONEY_GOLD_URL to ::parseEastmoney
        )
        return when (val result = RetrofitClient.publicApi.fetchFirstParsed(
            urls = providers.keys,
            parse = { url, body -> runCatching { providers.getValue(url)(body) }.getOrNull() }
        )) {
            is FallbackFetchResult.Success -> MainGoldResult.Loaded(result.value)
            is FallbackFetchResult.Failure -> MainGoldResult.Failed(result.lastErrorSuffix())
        }
    }

    private suspend fun fetchSupplementaryGoldData(): SupplementaryGoldData {
        return when (val result = RetrofitClient.publicApi.fetchFirstParsed(
            urls = listOf(XXAPI_GOLD_URL),
            parse = { _, body -> runCatching { parseXxapi(body) }.getOrNull()?.takeIf { it.hasData } }
        )) {
            is FallbackFetchResult.Success -> result.value
            is FallbackFetchResult.Failure -> SupplementaryGoldData()
        }
    }

    /** 60s API：{ code, data: { date, metals: [...] } } */
    private fun parseSixtySeconds(body: String): GoldPriceSnapshot? {
        val root = JSONObject(body)
        if (root.optInt("code") != 200) return null
        val data = root.optJSONObject("data") ?: return null

        val rawMetals = data.optJSONArray("metals").orEmptyObjects()
        if (rawMetals.isEmpty()) return null

        // 部分镜像会返回错位数据（如最高价低于最低价），逐条校验，异常过多时整份丢弃并切换下一个数据源。
        val validMetals = rawMetals.filter { it.isPlausibleQuote() }
        if (validMetals.size * 2 < rawMetals.size) return null

        val metals = validMetals.map { it.toMarketEntry() }.distinctBy { it.name }
        val domestic = metals.filter { it.unit.trim() == UNIT_CNY_PER_GRAM }
        val international = metals.filter { it.unit.contains("\u7F8E\u5143") }
        if (domestic.isEmpty() && international.isEmpty()) return null

        val updated = validMetals.firstOrNull()?.clean("updated", "").orEmpty()
        return GoldPriceSnapshot(
            domesticMarkets = domestic,
            internationalMarkets = international,
            brands = emptyList(),
            bankRecycle = emptyList(),
            source = "60s API",
            updateTime = updated.ifBlank { data.clean("date", "\u672A\u77E5") }
        )
    }

    /** 东方财富：{ rc, data: { diff: [{ f2 最新价, f3 涨跌幅, f12 代码, f14 名称, f15 最高, f16 最低, f17 开盘, f124 时间戳 }] } } */
    private fun parseEastmoney(body: String): GoldPriceSnapshot? {
        val root = JSONObject(body)
        if (root.optInt("rc", -1) != 0) return null
        val items = root.optJSONObject("data")?.optJSONArray("diff").orEmptyObjects()

        val domestic = mutableListOf<GoldMarketEntry>()
        val international = mutableListOf<GoldMarketEntry>()
        var latestTimestamp = 0L

        for (item in items) {
            val price = item.optDouble("f2", Double.NaN)
            if (price.isNaN() || price <= 0.0) continue

            val code = item.optString("f12")
            val isDomestic = code == "AU9999" || code == "AUTD"
            val entry = GoldMarketEntry(
                name = item.clean("f14", code),
                sellPrice = formatPrice(price),
                openPrice = formatPrice(item.optDouble("f17", price)),
                highPrice = formatPrice(item.optDouble("f15", price)),
                lowPrice = formatPrice(item.optDouble("f16", price)),
                unit = if (isDomestic) UNIT_CNY_PER_GRAM else UNIT_USD_PER_OUNCE,
                changeRate = formatChangeRate(item.optDouble("f3", 0.0))
            )
            if (isDomestic) domestic += entry else international += entry
            latestTimestamp = maxOf(latestTimestamp, item.optLong("f124", 0L))
        }

        if (domestic.isEmpty() && international.isEmpty()) return null

        return GoldPriceSnapshot(
            domesticMarkets = domestic,
            internationalMarkets = international,
            brands = emptyList(),
            bankRecycle = emptyList(),
            source = "\u4E1C\u65B9\u8D22\u5BCC",
            updateTime = if (latestTimestamp > 0) formatTimestamp(latestTimestamp) else "\u672A\u77E5"
        )
    }

    private fun parseXxapi(body: String): SupplementaryGoldData {
        val root = JSONObject(body)
        if (root.optInt("code") != 200) return SupplementaryGoldData()
        val data = root.optJSONObject("data") ?: return SupplementaryGoldData()

        val brandObjects = data.optJSONArray("precious_metal_price").orEmptyObjects()
        val brands = brandObjects.map {
            GoldBrandEntry(
                brand = it.clean("brand", "\u672A\u77E5\u54C1\u724C"),
                goldPrice = it.clean("gold_price", "N/A"),
                bullionPrice = it.clean("bullion_price", "N/A"),
                platinumPrice = it.clean("platinum_price", "N/A"),
                updated = it.clean("updated_date", "\u672A\u77E5")
            )
        }

        val banks = data.optJSONArray("bank_gold_bar_price").orEmptyObjects().map {
            GoldBankRecycleEntry(
                name = it.clean("bank", "\u672A\u77E5"),
                price = it.clean("price", ""),
                type = "\u94F6\u884C\u91D1\u6761",
                updated = it.clean("updated_date", "")
            )
        }

        val recycle = data.optJSONArray("gold_recycle_price").orEmptyObjects().map {
            GoldBankRecycleEntry(
                name = it.clean("gold_type", "\u56DE\u6536\u4EF7"),
                price = it.clean("recycle_price", ""),
                type = it.clean("purity", "\u56DE\u6536"),
                updated = it.clean("updated_date", "")
            )
        }

        val updateTime = brandObjects.map { it.clean("updated_date", "") }
            .filter { it.isNotBlank() }
            .maxOrNull()

        return SupplementaryGoldData(
            brands = brands,
            bankRecycle = banks + recycle,
            updateTime = updateTime
        )
    }

    /** 校验单条行情：价格为正，最高价不低于最低价，当前价落在当日区间附近。 */
    private fun JSONObject.isPlausibleQuote(): Boolean {
        val sell = clean("sell_price", "").toDoubleOrNull() ?: return false
        val high = clean("high_price", "").toDoubleOrNull() ?: return false
        val low = clean("low_price", "").toDoubleOrNull() ?: return false
        if (sell <= 0.0 || high <= 0.0 || low <= 0.0) return false
        if (high < low) return false
        return sell >= low * 0.9 && sell <= high * 1.1
    }

    private fun JSONObject.toMarketEntry(): GoldMarketEntry {
        val sellPrice = clean("sell_price", "0")
        val openPrice = clean("today_price", "0")
        return GoldMarketEntry(
            name = clean("name", "\u672A\u77E5\u54C1\u79CD"),
            sellPrice = formatPrice(sellPrice),
            openPrice = formatPrice(openPrice),
            highPrice = formatPrice(clean("high_price", "0")),
            lowPrice = formatPrice(clean("low_price", "0")),
            unit = clean("unit", ""),
            changeRate = calculateChangeRate(sellPrice, openPrice)
        )
    }

    private fun calculateChangeRate(current: String, open: String): String {
        val currentValue = current.toDoubleOrNull() ?: return formatChangeRate(0.0)
        val openValue = open.toDoubleOrNull() ?: return formatChangeRate(0.0)
        if (openValue == 0.0) return formatChangeRate(0.0)
        return formatChangeRate((currentValue - openValue) / openValue * 100)
    }

    private fun formatChangeRate(rate: Double): String {
        val arrow = if (rate >= 0) "\u2191" else "\u2193"
        return "$arrow${String.format(Locale.getDefault(), "%.2f", kotlin.math.abs(rate))}%"
    }

    private fun formatPrice(raw: String): String {
        val value = raw.toDoubleOrNull() ?: return raw.ifBlank { "N/A" }
        return formatPrice(value)
    }

    private fun formatPrice(value: Double): String =
        String.format(Locale.getDefault(), "%.2f", value)

    private fun formatTimestamp(epochSeconds: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }.format(Date(epochSeconds * 1000))

    private fun JSONObject.clean(key: String, fallback: String): String {
        val value = optString(key, fallback)
        return if (value.isNullOrBlank() || value == "null") fallback else value
    }

    private fun JSONArray?.orEmptyObjects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index -> optJSONObject(index) }
    }

    private sealed interface MainGoldResult {
        data class Loaded(val snapshot: GoldPriceSnapshot) : MainGoldResult
        data class Failed(val detail: String) : MainGoldResult
    }

    private data class SupplementaryGoldData(
        val brands: List<GoldBrandEntry> = emptyList(),
        val bankRecycle: List<GoldBankRecycleEntry> = emptyList(),
        val updateTime: String? = null
    ) {
        val hasData: Boolean = brands.isNotEmpty() || bankRecycle.isNotEmpty()
    }
}
