package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.model.OilPriceData
import com.example.data.model.OilPriceEntry
import com.example.data.model.OilPriceResponse
import com.example.data.model.PROVINCES
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OilPriceUiState {
    object Loading : OilPriceUiState
    data class Success(
        val province: String,
        val entries: List<OilPriceEntry>,
        val source: String
    ) : OilPriceUiState
    data class Error(val message: String) : OilPriceUiState
}

class OilPriceViewModel : ViewModel() {

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
            try {
                val response = RetrofitClient.oilPriceApi.getOilPrice(province)
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    if (body != null) {
                        val parsed = parsePrimaryResponse(body, province)
                        if (parsed != null) {
                            _uiState.value = OilPriceUiState.Success(
                                province = province,
                                entries = parsed,
                                source = "api.qqsuu.cn"
                            )
                            return@launch
                        }
                    }
                }
            } catch (_: Exception) {}

            try {
                val response = RetrofitClient.oilPriceBackupApi.getOilPriceBackup(province)
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    if (body != null) {
                        val parsed = parseBackupResponse(body, province)
                        if (parsed != null) {
                            _uiState.value = OilPriceUiState.Success(
                                province = province,
                                entries = parsed,
                                source = "v.api.aa1.cn"
                            )
                            return@launch
                        }
                    }
                }
                _uiState.value = OilPriceUiState.Error("油价数据获取失败")
            } catch (e: Exception) {
                _uiState.value = OilPriceUiState.Error("油价接口异常: ${e.localizedMessage ?: e.message ?: "未知错误"}")
            }
        }
    }

    private fun parsePrimaryResponse(body: String, province: String): List<OilPriceEntry>? {
        return try {
            val adapter = moshi.adapter(OilPriceResponse::class.java)
            val response = adapter.fromJson(body)
            val data = response?.data ?: return null
            dataToEntries(data)
        } catch (_: Exception) {
            parseFallbackJson(body, province)
        }
    }

    private fun parseBackupResponse(body: String, province: String): List<OilPriceEntry>? {
        return try {
            val root = org.json.JSONObject(body)
            val data = root.optJSONObject("data") ?: root.optJSONObject("result") ?: return try {
                val items = mutableListOf<OilPriceEntry>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = root.optString(k)
                    if (k.contains("号") || k.contains("柴油") || k.contains("汽油")) {
                        items.add(OilPriceEntry(label = k, price = v))
                    }
                }
                if (items.isEmpty()) null else items
            } catch (_: Exception) { null }

            if (data != null) {
                val items = mutableListOf<OilPriceEntry>()
                val keys = data.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = data.optString(k)
                    if (k.contains("号") || k.contains("柴油") || k.contains("汽油") || k == "92" || k == "95" || k == "98" || k == "0") {
                        items.add(OilPriceEntry(label = k, price = v))
                    }
                }
                if (items.isNotEmpty()) items else null
            } else null
        } catch (_: Exception) { null }
    }

    private fun parseFallbackJson(body: String, province: String): List<OilPriceEntry>? {
        return try {
            val root = org.json.JSONObject(body)
            val items = mutableListOf<OilPriceEntry>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = root.optString(k)
                if (k.contains("号") || k.contains("柴油") || k.contains("汽油") || k == "92" || k == "95" || k == "98" || k == "0") {
                    items.add(OilPriceEntry(label = k, price = v))
                }
            }
            if (items.isNotEmpty()) items else null
        } catch (_: Exception) { null }
    }

    private fun dataToEntries(data: OilPriceData): List<OilPriceEntry> {
        val map = linkedMapOf(
            "92号汽油" to data.`92`,
            "92号乙醇" to data.`92h`,
            "95号汽油" to data.`95`,
            "95号乙醇" to data.`95h`,
            "98号汽油" to data.`98`,
            "98号乙醇" to data.`98h`,
            "0号柴油" to data.`0`,
            "0号乙醇" to data.`0h`,
            "-10号柴油" to data.`-10`,
            "-10号乙醇" to data.`-10h`,
            "-20号柴油" to data.`-20`,
            "-20号乙醇" to data.`-20h`
        )
        return map.filterValues { it != null && it.isNotBlank() }
            .map { (label, price) -> OilPriceEntry(label = label, price = price!!) }
    }
}
