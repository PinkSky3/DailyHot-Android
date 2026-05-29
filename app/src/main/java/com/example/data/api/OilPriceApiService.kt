package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OilPriceApiService {
    @GET("api/dm-oilprice")
    suspend fun getOilPrice(
        @Query("prov") province: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    @GET("api/api-yj/index.php")
    suspend fun getOilPriceBackup(
        @Query("shengfen") province: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    @GET("resource/v2/oilprice")
    suspend fun getOilPriceIster(
        @Query("token") token: String,
        @Query("keyword") keyword: String
    ): retrofit2.Response<okhttp3.ResponseBody>
}
