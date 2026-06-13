package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.model.OilPriceRootResponse
import com.example.data.model.OilPriceEntry
import com.example.data.model.PROVINCES
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

class OilPriceViewModel : ViewModel() {

    private val apiBases = listOf(
        "https://60s.viki.moe",
        "https://api.yanyua.icu",
        "https://60s.7se.cn",
        "https://60s.crystelf.top"
    )

    private val _selectedProvince = MutableStateFlow(PROVINCES[10])
    val selectedProvince: StateFlow<String> = _selectedProvince.asStateFlow()

    private val _uiState = MutableStateFlow<OilPriceUiState>(OilPriceUiState.Loading)
    val uiState: StateFlow<OilPriceUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

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
            var lastError: String? = null
            try {
                val encodedProvince = URLEncoder.encode(province, StandardCharsets.UTF_8.name())
                for (base in apiBases) {
                    try {
                        val response = RetrofitClient.oilPriceApi.fetch("$base/v2/fuel-price?region=$encodedProvince")
                        if (response.isSuccessful) {
                            val body = response.body()?.string()
                            if (body != null) {
                                val parsed = parseResponse(body)
                                if (parsed != null && parsed.entries.isNotEmpty()) {
                                    _uiState.value = OilPriceUiState.Success(
                                        province = parsed.province ?: province,
                                        entries = parsed.entries,
                                        updateTime = parsed.updateTime,
                                        nextUpdateTime = parsed.nextUpdateTime
                                    )
                                    return@launch
                                }
                                lastError = "\u63A5\u53E3\u8FD4\u56DE\u7A7A\u6CB9\u4EF7: $base"
                            }
                        } else {
                            lastError = "HTTP ${response.code()}: $base"
                        }
                    } catch (e: Exception) {
                        lastError = "$base ${e.localizedMessage ?: e.message ?: "\u672A\u77E5\u9519\u8BEF"}"
                    }
                }
                _uiState.value = OilPriceUiState.Error(
                    "\u6CB9\u4EF7\u6570\u636E\u83B7\u53D6\u5931\u8D25\uFF0C\u5DF2\u5C1D\u8BD5\u591A\u4E2A\u516C\u5171\u5B9E\u4F8B${lastError?.let { "\uFF1A$it" } ?: ""}"
                )
            } catch (e: Exception) {
                _uiState.value = OilPriceUiState.Error(
                    "\u6CB9\u4EF7\u63A5\u53E3\u5F02\u5E38: ${e.localizedMessage ?: e.message ?: "\u672A\u77E5\u9519\u8BEF"}"
                )
            }
        }
    }

    private data class ParsedOilPrice(
        val province: String?,
        val entries: List<OilPriceEntry>,
        val updateTime: String?,
        val nextUpdateTime: String?
    )

    private fun parseResponse(body: String): ParsedOilPrice? {
        return try {
            val adapter = moshi.adapter(OilPriceRootResponse::class.java)
            val root = adapter.fromJson(body)
            if (root?.code != 200) return null
            val data = root.data ?: return null
            val entries = data.items
                ?.mapNotNull { item ->
                    val name = item.name?.trim().orEmpty()
                    val price = item.price_desc?.trim()
                        ?: item.price?.let { String.format(java.util.Locale.getDefault(), "%.2f \u5143/\u5347", it) }
                    if (name.isBlank() || price.isNullOrBlank()) null
                    else OilPriceEntry(name, price)
                }
                ?.filter { it.price.isNotBlank() }
                .orEmpty()
                .ifEmpty {
                    val prov = data.province ?: return null
                    listOfNotNull(
                        prov.gasoline_92?.takeIf { it.isNotBlank() }?.let { OilPriceEntry("92\u53F7\u6C7D\u6CB9", normalizePriceText(it)) },
                        prov.gasoline_95?.takeIf { it.isNotBlank() }?.let { OilPriceEntry("95\u53F7\u6C7D\u6CB9", normalizePriceText(it)) },
                        prov.gasoline_98?.takeIf { it.isNotBlank() }?.let { OilPriceEntry("98\u53F7\u6C7D\u6CB9", normalizePriceText(it)) },
                        prov.diesel_0?.takeIf { it.isNotBlank() }?.let { OilPriceEntry("0\u53F7\u67F4\u6CB9", normalizePriceText(it)) }
                    )
                }
            if (entries.isEmpty()) return null
            ParsedOilPrice(
                province = data.region ?: data.province?.pri_name,
                entries = entries,
                updateTime = data.updated ?: data.time,
                nextUpdateTime = data.trend?.description ?: data.trend?.next_adjustment_date ?: data.next_update_time
            )
        } catch (_: Exception) { null }
    }

    private fun normalizePriceText(raw: String): String {
        val price = raw.trim()
        return if (price.contains("\u5143") || price.contains("/") || price.contains("L", ignoreCase = true)) {
            price
        } else {
            "$price \u5143/\u5347"
        }
    }
}
