package com.example.data.api

import com.example.data.model.HotSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface DailyHotApiService {
    @GET
    suspend fun getHotListWithUrl(
        @retrofit2.http.Url url: String
    ): HotSearchResponse

    @GET("{route}")
    suspend fun getHotList(
        @Path("route") route: String
    ): HotSearchResponse
}
