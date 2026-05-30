package com.example.data.api

import com.example.data.model.ChatCompletionRequest
import com.example.data.model.ChatCompletionResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AiChatApiService {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(
        @Body request: ChatCompletionRequest
    ): retrofit2.Response<ChatCompletionResponse>
}
