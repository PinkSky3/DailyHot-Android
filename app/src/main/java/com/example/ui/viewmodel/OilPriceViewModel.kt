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
                val response = RetrofitClient.oilPriceApi.getOilPrice("get", province)
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    if (body != null) {
                        val parsed = parseResponse(body)
                        if (parsed != null) {
                            val entries = listOfNotNull(
                                OilPriceEntry("92\u53F7\u6C7D\u6CB9", parsed.gasoline_92 ?: return@launch),
                                OilPriceEntry("95\u53F7\u6C7D\u6CB9", parsed.gasoline_95 ?: return@launch),
                                OilPriceEntry("98\u53F7\u6C7D\u6CB9", parsed.gasoline_98 ?: return@launch),
                                OilPriceEntry("0\u53F7\u67F4\u6CB9", parsed.diesel_0 ?: return@launch)
                            ).filter { it.price.isNotBlank() }
                            if (entries.isNotEmpty()) {
                                _uiState.value = OilPriceUiState.Success(
                                    province = province,
                                    entries = entries,
                                    updateTime = parsed.updateTime,
                                    nextUpdateTime = parsed.nextUpdateTime
                                )
                                return@launch
                            }
                        }
                    }
                }
                _uiState.value = OilPriceUiState.Error(
                    "\u6CB9\u4EF7\u6570\u636E\u83B7\u53D6\u5931\u8D25\uFF0C\u8BF7\u68C0\u67E5\u7F51\u7EDC\u6216\u540E\u7AEF\u670D\u52A1"
                )
            } catch (e: Exception) {
                _uiState.value = OilPriceUiState.Error(
                    "\u6CB9\u4EF7\u63A5\u53E3\u5F02\u5E38: ${e.localizedMessage ?: e.message ?: "\u672A\u77E5\u9519\u8BEF"}"
                )
            }
        }
    }

    private data class ParsedOilPrice(
        val gasoline_92: String?,
        val gasoline_95: String?,
        val gasoline_98: String?,
        val diesel_0: String?,
        val updateTime: String?,
        val nextUpdateTime: String?
    )

    private fun parseResponse(body: String): ParsedOilPrice? {
        return try {
            val adapter = moshi.adapter(OilPriceRootResponse::class.java)
            val root = adapter.fromJson(body)
            if (root?.code != 200) return null
            val prov = root.data?.province ?: return null
            ParsedOilPrice(
                gasoline_92 = prov.gasoline_92,
                gasoline_95 = prov.gasoline_95,
                gasoline_98 = prov.gasoline_98,
                diesel_0 = prov.diesel_0,
                updateTime = root.data?.time,
                nextUpdateTime = root.data?.next_update_time
            )
        } catch (_: Exception) { null }
    }
}
