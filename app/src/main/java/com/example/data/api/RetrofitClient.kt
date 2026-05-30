package com.example.data.api

import com.example.BuildConfig
import com.example.data.model.CoercedStringAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://dailyhotapi.3yu3.top/"
    private const val PEAR_API_BASE_URL = "https://api.pearapi.ai/"

    private val moshi: Moshi = Moshi.Builder()
        .add(CoercedStringAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${BuildConfig.PEAR_AI_API_KEY}")
            .build()
        chain.proceed(request)
    }

    private val pearApiClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val pearRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(PEAR_API_BASE_URL)
        .client(pearApiClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: DailyHotApiService = retrofit.create(DailyHotApiService::class.java)
    val oilPriceApi: OilPriceApiService = pearRetrofit.create(OilPriceApiService::class.java)
    val news60sApi: News60sApiService = pearRetrofit.create(News60sApiService::class.java)
    val aiChatApi: AiChatApiService = pearRetrofit.create(AiChatApiService::class.java)
}
