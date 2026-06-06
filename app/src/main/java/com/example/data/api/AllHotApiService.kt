package com.example.data.api

import com.example.data.model.AllHotSourceDataResponse
import com.example.data.model.AllHotSourcesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AllHotApiService {
    @GET("sources")
    suspend fun getSources(
        @Header("X-API-Key") apiKey: String,
        @Query("page") page: Int = 1
    ): Response<AllHotSourcesResponse>

    @GET("sources/search")
    suspend fun searchSources(
        @Header("X-API-Key") apiKey: String,
        @Query("keyword") keyword: String
    ): Response<AllHotSourcesResponse>

    @GET("sources/data")
    suspend fun getSourceData(
        @Header("X-API-Key") apiKey: String,
        @Query("id") id: Int,
        @Query("page") page: Int = 1
    ): Response<AllHotSourceDataResponse>
}
