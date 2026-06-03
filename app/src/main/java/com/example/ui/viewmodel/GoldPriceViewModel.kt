package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.model.GoldBankRecycleEntry
import com.example.data.model.GoldBrandEntry
import com.example.data.model.GoldMarketEntry
import com.example.data.model.GoldPriceSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

sealed interface GoldPriceUiState {
    object Loading : GoldPriceUiState
    data class Success(val snapshot: GoldPriceSnapshot) : GoldPriceUiState
    data class Error(val message: String) : GoldPriceUiState
}

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
                val main = fetchMainGoldData()
                if (main == null) {
                    _uiState.value = GoldPriceUiState.Error("\u91D1\u4EF7\u6570\u636E\u83B7\u53D6\u5931\u8D25\uFF0C\u8BF7\u7A0D\u540E\u91CD\u8BD5")
                    return@launch
                }

                val extra = fetchSupplementaryGoldData()
                val brands = main.brands + extra.brands
                val source = if (extra.hasData) "${main.source} + xxapi.cn" else main.source

                _uiState.value = GoldPriceUiState.Success(
                    main.copy(
                        brands = brands,
                        bankRecycle = extra.bankRecycle,
                        source = source
                    )
                )
            } catch (e: Exception) {
                _uiState.value = GoldPriceUiState.Error(
                    "\u91D1\u4EF7\u63A5\u53E3\u5F02\u5E38: ${e.localizedMessage ?: e.message ?: "\u672A\u77E5\u9519\u8BEF"}"
                )
            }
        }
    }

    private suspend fun fetchMainGoldData(): GoldPriceSnapshot? {
        val providers = listOf(
            "https://tmini.net/api/gold-price?type=json" to ::parseTmini,
            "https://api.freejk.com/shuju/jinjia/" to ::parseFreejk
        )
        for ((url, parser) in providers) {
            try {
                val body = RetrofitClient.goldPriceApi.fetch(url).body()?.string() ?: continue
                parser(body)?.let { return it }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private suspend fun fetchSupplementaryGoldData(): SupplementaryGoldData {
        return try {
            val body = RetrofitClient.goldPriceApi.fetch("https://v2.xxapi.cn/api/goldprice").body()?.string()
                ?: return SupplementaryGoldData()
            parseXxapi(body)
        } catch (_: Exception) {
            SupplementaryGoldData()
        }
    }

    private fun parseTmini(body: String): GoldPriceSnapshot? {
        val root = JSONObject(body)
        if (!root.has("metals")) return null

        val metals = root.optJSONArray("metals").orEmptyObjects().map {
            it.toMarketEntry().let { entry ->
                if (entry.name.contains("\u7EBD\u7EA6\u9EC4\u91D1")) entry.copy(unit = "\u7F8E\u5143/\u76CE\u53F8")
                else entry
            }
        }
        val brands = root.optJSONArray("stores").orEmptyObjects().map {
            GoldBrandEntry(
                brand = it.clean("brand", "\u672A\u77E5\u54C1\u724C"),
                goldPrice = it.clean("price", "N/A"),
                bullionPrice = "-",
                platinumPrice = "-",
                updated = it.clean("updated", "\u672A\u77E5")
            )
        }

        return GoldPriceSnapshot(
            domesticMarkets = metals.filter { it.unit.trim() == "\u5143/\u514B" },
            internationalMarkets = metals.filter { it.unit.contains("\u7F8E\u5143") },
            brands = brands,
            bankRecycle = emptyList(),
            source = "tmini.net \u8D35\u91D1\u5C5E\u884C\u60C5",
            updateTime = root.clean("date", "\u672A\u77E5")
        )
    }

    private fun parseFreejk(body: String): GoldPriceSnapshot? {
        val root = JSONObject(body)
        if (root.optString("status") != "success" || !root.has("data")) return null
        val data = root.optJSONObject("data") ?: return null

        val domesticPrice = data.clean("price", "0")
        val internationalPrice = data.clean("international_price", "0")
        val entries = listOf(
            flatMarketEntry("\u4E0A\u6D77\u9EC4\u91D1\u73B0\u8D27", domesticPrice, "\u5143/\u514B"),
            flatMarketEntry("\u4F26\u6566\u91D1\u73B0\u8D27\u9EC4\u91D1", internationalPrice, "\u7F8E\u5143/\u76CE\u53F8")
        )

        return GoldPriceSnapshot(
            domesticMarkets = entries.filter { it.unit == "\u5143/\u514B" },
            internationalMarkets = entries.filter { it.unit.contains("\u7F8E\u5143") },
            brands = emptyList(),
            bankRecycle = emptyList(),
            source = "freejk.com \u514D\u8D39API",
            updateTime = data.clean("update_time", "\u672A\u77E5")
        )
    }

    private fun parseXxapi(body: String): SupplementaryGoldData {
        val root = JSONObject(body)
        if (root.optInt("code") != 200) return SupplementaryGoldData()
        val data = root.optJSONObject("data") ?: return SupplementaryGoldData()

        val brands = data.optJSONArray("precious_metal_price").orEmptyObjects().map {
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

        return SupplementaryGoldData(brands = brands, bankRecycle = banks + recycle)
    }

    private fun JSONObject.toMarketEntry(): GoldMarketEntry {
        val sellPrice = clean("sell_price", "0")
        val openPrice = clean("today_price", "0")
        val highPrice = clean("high_price", "0")
        val lowPrice = clean("low_price", "0")
        val changeRate = calculateChangeRate(sellPrice, openPrice)
        return GoldMarketEntry(
            name = clean("name", "\u672A\u77E5\u54C1\u79CD"),
            sellPrice = formatPrice(sellPrice),
            openPrice = formatPrice(openPrice),
            highPrice = formatPrice(highPrice),
            lowPrice = formatPrice(lowPrice),
            unit = clean("unit", ""),
            changeRate = changeRate
        )
    }

    private fun flatMarketEntry(name: String, price: String, unit: String): GoldMarketEntry {
        val formatted = formatPrice(price)
        return GoldMarketEntry(
            name = name,
            sellPrice = formatted,
            openPrice = formatted,
            highPrice = formatted,
            lowPrice = formatted,
            unit = unit,
            changeRate = "\u21910.00%"
        )
    }

    private fun calculateChangeRate(current: String, open: String): String {
        val currentValue = current.toDoubleOrNull() ?: return "\u21910.00%"
        val openValue = open.toDoubleOrNull() ?: return "\u21910.00%"
        if (openValue == 0.0) return "\u21910.00%"
        val rate = (currentValue - openValue) / openValue * 100
        val arrow = if (rate >= 0) "\u2191" else "\u2193"
        return "$arrow${String.format(Locale.getDefault(), "%.2f", kotlin.math.abs(rate))}%"
    }

    private fun formatPrice(raw: String): String {
        val value = raw.toDoubleOrNull() ?: return raw.ifBlank { "N/A" }
        return String.format(Locale.getDefault(), "%.2f", value)
    }

    private fun JSONObject.clean(key: String, fallback: String): String {
        val value = optString(key, fallback)
        return if (value.isNullOrBlank() || value == "null") fallback else value
    }

    private fun JSONArray?.orEmptyObjects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index -> optJSONObject(index) }
    }

    private data class SupplementaryGoldData(
        val brands: List<GoldBrandEntry> = emptyList(),
        val bankRecycle: List<GoldBankRecycleEntry> = emptyList()
    ) {
        val hasData: Boolean = brands.isNotEmpty() || bankRecycle.isNotEmpty()
    }
}
