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

private val PROVINCE_TO_CITY = mapOf(
    "北京" to "北京", "上海" to "上海", "天津" to "天津", "重庆" to "重庆",
    "河北" to "石家庄", "山西" to "太原", "辽宁" to "沈阳", "吉林" to "长春",
    "黑龙江" to "哈尔滨", "江苏" to "南京", "浙江" to "杭州", "安徽" to "合肥",
    "福建" to "福州", "江西" to "南昌", "山东" to "济南", "河南" to "郑州",
    "湖北" to "武汉", "湖南" to "长沙", "广东" to "广州", "广西" to "南宁",
    "海南" to "海口", "四川" to "成都", "贵州" to "贵阳", "云南" to "昆明",
    "西藏" to "拉萨", "陕西" to "西安", "甘肃" to "兰州", "青海" to "西宁",
    "宁夏" to "银川", "新疆" to "乌鲁木齐", "内蒙古" to "呼和浩特"
)

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
            if (tryPrimary(province)) return@launch
            if (tryBackup(province)) return@launch
            if (tryIster(province)) return@launch
            _uiState.value = OilPriceUiState.Error("油价数据获取失败，所有接口均不可用")
        }
    }

    private suspend fun tryPrimary(province: String): Boolean {
        return try {
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
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) { false }
    }

    private suspend fun tryBackup(province: String): Boolean {
        return try {
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
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) { false }
    }

    private suspend fun tryIster(province: String): Boolean {
        val city = PROVINCE_TO_CITY[province] ?: province
        return try {
            val response = RetrofitClient.oilPriceIsterApi.getOilPriceIster(
                token = "CDAgWQhHJGzBiDLCsfdivemXqArPUNdL",
                keyword = city
            )
            if (response.isSuccessful) {
                val body = response.body()?.string()
                if (body != null) {
                    val parsed = parseIsterResponse(body, province)
                    if (parsed != null) {
                        _uiState.value = OilPriceUiState.Success(
                            province = province,
                            entries = parsed,
                            source = "api.istero.com"
                        )
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) { false }
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

    private fun parseIsterResponse(body: String, province: String): List<OilPriceEntry>? {
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
            if (items.isEmpty()) {
                val code = root.optString("code")
                val data = root.optJSONObject("data") ?: root.optJSONArray("data")
                if (data is org.json.JSONObject) {
                    val dataKeys = data.keys()
                    while (dataKeys.hasNext()) {
                        val k = dataKeys.next()
                        val v = data.optString(k)
                        if (k.contains("号") || k.contains("柴油") || k.contains("汽油") || k == "92" || k == "95" || k == "98" || k == "0") {
                            items.add(OilPriceEntry(label = k, price = v))
                        }
                    }
                } else if (data is org.json.JSONArray) {
                    for (i in 0 until data.length()) {
                        val obj = data.optJSONObject(i)
                        if (obj != null) {
                            val label = obj.optString("name", obj.optString("label", obj.optString("oil_name", "")))
                            val price = obj.optString("price", obj.optString("value", obj.optString("oil_price", "")))
                            if (label.isNotBlank() && price.isNotBlank()) {
                                items.add(OilPriceEntry(label = label, price = price))
                            }
                        }
                    }
                }
            }
            if (items.isNotEmpty()) items else null
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
