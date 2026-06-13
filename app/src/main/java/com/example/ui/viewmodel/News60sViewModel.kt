package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.model.News60sRootResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface News60sUiState {
    object Loading : News60sUiState
    data class Success(
        val newsList: List<String>,
        val updateTime: String? = null
    ) : News60sUiState
    data class Error(val message: String) : News60sUiState
}

class News60sViewModel : ViewModel() {

    private val apiBases = listOf(
        "https://60s.viki.moe",
        "https://api.yanyua.icu",
        "https://60s.7se.cn",
        "https://60s.crystelf.top"
    )

    private val _uiState = MutableStateFlow<News60sUiState>(News60sUiState.Loading)
    val uiState: StateFlow<News60sUiState> = _uiState.asStateFlow()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    init {
        fetchNews()
    }

    fun refresh() {
        fetchNews()
    }

    private fun fetchNews() {
        _uiState.value = News60sUiState.Loading
        viewModelScope.launch {
            var lastError: String? = null
            try {
                for (base in apiBases) {
                    try {
                        val response = RetrofitClient.news60sApi.fetch("$base/v2/60s")
                        if (response.isSuccessful) {
                            val body = response.body()?.string()
                            if (body != null) {
                                val parsed = parseResponse(body)
                                if (parsed != null && parsed.newsList.isNotEmpty()) {
                                    _uiState.value = News60sUiState.Success(
                                        newsList = parsed.newsList,
                                        updateTime = parsed.updateTime
                                    )
                                    return@launch
                                }
                                lastError = "\u63A5\u53E3\u8FD4\u56DE\u7A7A\u65B0\u95FB: $base"
                            }
                        } else {
                            lastError = "HTTP ${response.code()}: $base"
                        }
                    } catch (e: Exception) {
                        lastError = "$base ${e.localizedMessage ?: e.message ?: "\u672A\u77E5\u9519\u8BEF"}"
                    }
                }
                _uiState.value = News60sUiState.Error(
                    "60S\u65B0\u95FB\u83B7\u53D6\u5931\u8D25\uFF0C\u5DF2\u5C1D\u8BD5\u591A\u4E2A\u516C\u5171\u5B9E\u4F8B${lastError?.let { "\uFF1A$it" } ?: ""}"
                )
            } catch (e: Exception) {
                _uiState.value = News60sUiState.Error(
                    "60S\u63A5\u53E3\u5F02\u5E38: ${e.localizedMessage ?: e.message ?: "\u672A\u77E5\u9519\u8BEF"}"
                )
            }
        }
    }

    private data class ParsedNews60s(
        val newsList: List<String>,
        val updateTime: String?
    )

    private fun parseResponse(body: String): ParsedNews60s? {
        return try {
            val adapter = moshi.adapter(News60sRootResponse::class.java)
            val root = adapter.fromJson(body)
            if (root?.code != 200) return null
            val data = root.data ?: return null
            val news = data.news
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            if (news.isEmpty()) null
            else ParsedNews60s(
                newsList = news,
                updateTime = data.updated ?: data.api_updated ?: data.date
            )
        } catch (_: Exception) {
            null
        }
    }
}
