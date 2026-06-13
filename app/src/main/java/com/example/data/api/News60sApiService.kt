package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Url

interface News60sApiService {
    @GET
    suspend fun fetch(@Url url: String): retrofit2.Response<okhttp3.ResponseBody>
}
