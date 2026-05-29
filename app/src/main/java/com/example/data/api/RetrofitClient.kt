package com.example.data.api

import com.example.data.model.CoercedStringAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://dailyhotapi.3yu3.top/"
    private const val OIL_PRICE_BASE_URL = "https://api.qqsuu.cn/"
    private const val OIL_PRICE_BACKUP_BASE_URL = "https://v.api.aa1.cn/"

    private val moshi: Moshi = Moshi.Builder()
        .add(CoercedStringAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val oilPriceRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(OIL_PRICE_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val oilPriceBackupRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(OIL_PRICE_BACKUP_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: DailyHotApiService = retrofit.create(DailyHotApiService::class.java)
    val oilPriceApi: OilPriceApiService = oilPriceRetrofit.create(OilPriceApiService::class.java)
    val oilPriceBackupApi: OilPriceApiService = oilPriceBackupRetrofit.create(OilPriceApiService::class.java)
}
