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
            try {
                val response = RetrofitClient.news60sApi.getNews60s()
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    if (body != null) {
                        val parsed = parseResponse(body)
                        if (parsed != null && parsed.isNotEmpty()) {
                            _uiState.value = News60sUiState.Success(newsList = parsed)
                            return@launch
                        }
                    }
                }
                _uiState.value = News60sUiState.Error(
                    "60S\u65B0\u95FB\u83B7\u53D6\u5931\u8D25\uFF0C\u8BF7\u68C0\u67E5\u7F51\u7EDC\u6216\u540E\u7AEF\u670D\u52A1"
                )
            } catch (e: Exception) {
                _uiState.value = News60sUiState.Error(
                    "60S\u63A5\u53E3\u5F02\u5E38: ${e.localizedMessage ?: e.message ?: "\u672A\u77E5\u9519\u8BEF"}"
                )
            }
        }
    }

    private fun parseResponse(body: String): List<String>? {
        return try {
            val adapter = moshi.adapter(News60sRootResponse::class.java)
            val root = adapter.fromJson(body)
            if (root?.code != 200) null
            else root.data?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) { null }
    }
}
