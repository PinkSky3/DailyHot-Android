package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OilPriceApiService {
    @GET("api/oilprice")
    suspend fun getOilPrice(
        @Query("type") type: String,
        @Query("province") province: String
    ): retrofit2.Response<okhttp3.ResponseBody>
}
