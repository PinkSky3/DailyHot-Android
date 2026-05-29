package com.example.data.api

import retrofit2.http.GET

interface News60sApiService {
    @GET("api/60s/")
    suspend fun getNews60s(): retrofit2.Response<okhttp3.ResponseBody>
}
