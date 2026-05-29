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
        val updateTime: String?
    ) : UiState
    data class Error(val message: String, val lastSuccessItems: List<HotSearchItem>? = null) : UiState
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

    private fun fetchHotSearch(platform: HotPlatform) {
        fetchJob?.cancel()
        _uiState.value = UiState.Loading
        _rawItems.value = emptyList()

        val baseUrlList = listOf(
            "https://dailyhotapi.3yu3.top/",
            "https://dailyhot.api.lkwplus.com/"
        )

        fetchJob = viewModelScope.launch {
            var lastErrorMsg = ""
            var success = false

            for (baseUrl in baseUrlList) {
                try {
                    val fullUrl = "${baseUrl.trimEnd('/')}/${platform.key}"
                    val response = RetrofitClient.apiService.getHotListWithUrl(fullUrl)
                    if (response.code == 200 && response.data != null) {
                        _rawItems.value = response.data
                        
                        // Format update time or show local time
                        val formattedTime = response.updateTime?.let { formatIsoTime(it) } 
                            ?: SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            
                        _fetchedTime.value = formattedTime
                        applyFilter()
                        success = true
                        break
                    } else {
                        lastErrorMsg = "节点(${getHostName(baseUrl)})返回错误: ${response.code}"
                    }
                } catch (e: Exception) {
                    lastErrorMsg = "节点(${getHostName(baseUrl)})异常: ${e.localizedMessage ?: e.message ?: "未知网度错误"}"
                }
            }

            if (!success) {
                _uiState.value = UiState.Error(lastErrorMsg.ifBlank { "数据获取失败，所有节点均不可用。" }, lastSuccessItems = null)
            }
        }
    }

    private fun getHostName(url: String): String {
        return try {
            java.net.URL(url).host
        } catch (e: Exception) {
            url
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim()
        val currentRaw = _rawItems.value
        
        if (query.isEmpty()) {
            _uiState.value = UiState.Success(
                items = currentRaw,
                filteredCount = currentRaw.size,
                updateTime = _fetchedTime.value
            )
        } else {
            val filtered = currentRaw.filter { item ->
                (item.title?.contains(query, ignoreCase = true) ?: false) || 
                (item.desc?.contains(query, ignoreCase = true) ?: false) ||
                (item.author?.contains(query, ignoreCase = true) ?: false)
            }
            _uiState.value = UiState.Success(
                items = filtered,
                filteredCount = filtered.size,
                updateTime = _fetchedTime.value
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
