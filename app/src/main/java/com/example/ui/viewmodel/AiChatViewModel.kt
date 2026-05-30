package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.model.ChatCompletionRequest
import com.example.data.model.ChatMessage
import com.example.data.model.HotSearchItem
import com.example.data.model.ModelHealth
import com.example.data.model.OilPriceEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiMessage(
    val role: String,
    val content: String
)

data class AiContext(
    val hotItems: List<HotSearchItem> = emptyList(),
    val news60s: List<String> = emptyList(),
    val oilProvince: String? = null,
    val oilEntries: List<OilPriceEntry> = emptyList()
)

sealed interface AiChatState {
    object Idle : AiChatState
    object Loading : AiChatState
    data class Error(val message: String) : AiChatState
}

class AiChatViewModel : ViewModel() {

    private val _selectedModel = MutableStateFlow("")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _modelHealthList = MutableStateFlow<List<ModelHealth>>(emptyList())
    val modelHealthList: StateFlow<List<ModelHealth>> = _modelHealthList.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val messages: StateFlow<List<ChatUiMessage>> = _messages.asStateFlow()

    private val _chatState = MutableStateFlow<AiChatState>(AiChatState.Idle)
    val chatState: StateFlow<AiChatState> = _chatState.asStateFlow()

    private val _aiContext = MutableStateFlow(AiContext())
    val aiContext: StateFlow<AiContext> = _aiContext.asStateFlow()

    private val _dialogVisible = MutableStateFlow(false)
    val dialogVisible: StateFlow<Boolean> = _dialogVisible.asStateFlow()

    init {
        refreshModels()
        startPeriodicHealthCheck()
    }

    private fun refreshModels() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.aiChatApi.listModels()
                val models = if (response.isSuccessful) {
                    response.body()?.data?.filter { it.channel_type == "\u514D\u8D39" || it.channel_type == null }?.mapNotNull { info ->
                        val id = info.id ?: return@mapNotNull null
                        ModelHealth(id, info.model ?: id, false)
                    } ?: emptyList()
                } else emptyList()

                val list = if (models.isEmpty()) {
                    listOf(
                        ModelHealth("glm-4-flash-250414", "GLM-4-Flash", false),
                        ModelHealth("spark-lite", "Spark Lite", false)
                    )
                } else models

                _modelHealthList.value = list
                if (_selectedModel.value.isBlank() && list.isNotEmpty()) {
                    _selectedModel.value = list.first().id
                }

                if (list.isNotEmpty()) testModels(list.map { it.id })
            } catch (_: Exception) {
                val fallback = listOf(
                    ModelHealth("glm-4-flash-250414", "GLM-4-Flash", false),
                    ModelHealth("spark-lite", "Spark Lite", false)
                )
                _modelHealthList.value = fallback
                if (_selectedModel.value.isBlank()) _selectedModel.value = fallback.first().id
            }
        }
    }

    private suspend fun testModels(modelIds: List<String>) {
        for (id in modelIds) {
            try {
                val request = ChatCompletionRequest(
                    model = id,
                    messages = listOf(ChatMessage("user", "hi")),
                    temperature = 0.1
                )
                val response = RetrofitClient.aiChatApi.chatCompletions(request)
                updateModelHealth(id, response.isSuccessful)
            } catch (_: Exception) {
                updateModelHealth(id, false)
            }
        }
    }

    private fun updateModelHealth(id: String, isOnline: Boolean) {
        val current = _modelHealthList.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx < 0) return
        current[idx] = current[idx].copy(isOnline = isOnline)
        _modelHealthList.value = current

        val sel = _selectedModel.value
        val currentOnline = current.any { it.id == sel && it.isOnline }
        if (sel.isBlank() || !currentOnline) {
            val firstOnline = current.firstOrNull { it.isOnline }
            if (firstOnline != null) _selectedModel.value = firstOnline.id
        }
    }

    private fun startPeriodicHealthCheck() {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                val ids = _modelHealthList.value.map { it.id }
                if (ids.isNotEmpty()) testModels(ids)
            }
        }
    }

    fun selectModel(id: String) {
        _selectedModel.value = id
    }

    fun toggleDialog() {
        _dialogVisible.value = !_dialogVisible.value
    }

    fun showDialog() {
        _dialogVisible.value = true
    }

    fun hideDialog() {
        _dialogVisible.value = false
    }

    fun updateContext(context: AiContext) {
        _aiContext.value = context
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val modelId = _selectedModel.value
        if (modelId.isBlank()) {
            _chatState.value = AiChatState.Error("\u6CA1\u6709\u53EF\u7528\u6A21\u578B\uFF0C\u8BF7\u7A0D\u540E\u518D\u8BD5")
            return
        }

        val userMessage = ChatUiMessage(role = "user", content = text)
        _messages.value = _messages.value + userMessage
        _chatState.value = AiChatState.Loading

        val ctx = _aiContext.value
        val systemPrompt = buildSystemPrompt(ctx)
        val history = _messages.value.map { ChatMessage(it.role, it.content) }

        viewModelScope.launch {
            try {
                val request = ChatCompletionRequest(
                    model = modelId,
                    messages = listOfNotNull(
                        ChatMessage("system", systemPrompt),
                        *history.dropLast(1).toTypedArray(),
                        ChatMessage("user", text)
                    )
                )
                val response = RetrofitClient.aiChatApi.chatCompletions(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    val reply = body?.choices?.firstOrNull()?.message?.content
                    if (reply != null) {
                        _messages.value = _messages.value + ChatUiMessage("assistant", reply)
                        _chatState.value = AiChatState.Idle
                    } else {
                        _chatState.value = AiChatState.Error("\u6A21\u578B\u56DE\u7B54\u4E3A\u7A7A\uFF0C\u8BF7\u6362\u4E00\u4E2A\u6A21\u578B\u8BD5\u8BD5")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = if (errorBody != null) "\u8BF7\u6C42\u5931\u8D25(${response.code()}): $errorBody" else "\u8BF7\u6C42\u5931\u8D25: ${response.code()} ${response.message()}"
                    _chatState.value = AiChatState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _chatState.value = AiChatState.Error("\u7F51\u7EDC\u5F02\u5E38: ${e.localizedMessage ?: e.message ?: "\u672A\u77E5\u9519\u8BEF"}")
            }
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
        _chatState.value = AiChatState.Idle
    }

    private fun buildSystemPrompt(ctx: AiContext): String {
        val sb = StringBuilder()
        sb.appendLine("\u4F60\u662F\u4E00\u4E2A\u667A\u80FD\u52A9\u624B\uFF0C\u5F53\u524D\u4E3A\u7528\u6237\u63D0\u4F9B\u4EE5\u4E0B\u5B9E\u65F6\u6570\u636E\u53C2\u8003\uFF1A")
        sb.appendLine()

        if (ctx.hotItems.isNotEmpty()) {
            sb.appendLine("\u3010\u70ED\u641C\u6570\u636E\u3011")
            ctx.hotItems.take(15).forEachIndexed { i, item ->
                sb.appendLine("${i + 1}. ${item.title ?: "\u65E0\u6807\u9898"} - ${item.hot ?: "\u70ED\u5EA6\u672A\u77E5"}")
            }
            sb.appendLine()
        }

        if (ctx.news60s.isNotEmpty()) {
            sb.appendLine("\u301060\u79D2\u65B0\u95FB\u3011")
            ctx.news60s.forEachIndexed { i, news ->
                sb.appendLine("${i + 1}. $news")
            }
            sb.appendLine()
        }

        if (ctx.oilEntries.isNotEmpty()) {
            sb.appendLine("\u3010\u5F53\u524D\u6CB9\u4EF7\uFF08${ctx.oilProvince ?: "\u672A\u77E5\u7701\u4EFD"}\uFF09\u3011")
            ctx.oilEntries.forEach { entry ->
                sb.appendLine("${entry.label}: ${entry.price}\u5143/\u5347")
            }
            sb.appendLine()
        }

        sb.appendLine("\u8BF7\u6839\u636E\u4EE5\u4E0A\u6570\u636E\u56DE\u7B54\u7528\u6237\u7684\u95EE\u9898\uFF0C\u5982\u679C\u95EE\u9898\u4E0E\u6570\u636E\u65E0\u5173\uFF0C\u7528\u4F60\u81EA\u5DF1\u7684\u77E5\u8BC6\u56DE\u7B54\u5373\u53EF\u3002")
        return sb.toString()
    }
}
