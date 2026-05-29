package com.example.data.api

import com.example.data.model.HotSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface DailyHotApiService {
    @GET
    suspend fun getHotListWithUrl(
        @retrofit2.http.Url url: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    @GET("{route}")
    suspend fun getHotList(
        @Path("route") route: String
    ): com.example.data.model.HotSearchResponse
}
