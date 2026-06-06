package com.example.data.api

import com.example.data.model.AllHotSourceDataResponse
import com.example.data.model.AllHotSourcesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AllHotApiService {
    @GET("sources")
    suspend fun getSources(
        @Query("page") page: Int = 1
    ): Response<AllHotSourcesResponse>

    @GET("sources/search")
    suspend fun searchSources(
        @Query("keyword") keyword: String
    ): Response<AllHotSourcesResponse>

    @GET("sources/data")
    suspend fun getSourceData(
        @Query("id") id: Int,
        @Query("page") page: Int = 1
    ): Response<AllHotSourceDataResponse>
}
