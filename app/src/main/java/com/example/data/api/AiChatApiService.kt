package com.example.data.api

import com.example.data.model.ChatCompletionRequest
import com.example.data.model.ChatCompletionResponse
import com.example.data.model.ModelsListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AiChatApiService {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(
        @Body request: ChatCompletionRequest
    ): retrofit2.Response<ChatCompletionResponse>

    @GET("v1/models")
    suspend fun listModels(): retrofit2.Response<ModelsListResponse>
}
