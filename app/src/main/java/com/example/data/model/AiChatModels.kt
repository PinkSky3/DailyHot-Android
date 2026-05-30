package com.example.data.model

enum class AiModel(val apiName: String, val displayName: String) {
    GEMMA("gemma-4-26b-a4b-it", "Gemma 4"),
    STEP("step-3.5-flash", "Step 3.5")
}

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
