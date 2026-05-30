package com.example.data.model

data class ModelHealth(
    val id: String,
    val displayName: String,
    val isOnline: Boolean
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7
)

data class ChatCompletionResponse(
    val choices: List<Choice>? = null
)

data class Choice(
    val message: ChatMessage? = null
)

data class ModelsListResponse(
    val `object`: String? = null,
    val data: List<ModelInfo>? = null
)

data class ModelInfo(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = null,
    val owned_by: String? = null,
    val model: String? = null,
    val model_type: String? = null,
    val channel_type: String? = null
)
