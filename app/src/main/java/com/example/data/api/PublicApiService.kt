package com.example.data.api

import com.example.data.model.WeatherAlertResponse
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url

interface PublicApiService {
    @GET
    suspend fun fetchRaw(@Url url: String): Response<ResponseBody>

    @GET
    suspend fun fetchWeather(@Url url: String): Response<WeatherAlertResponse>

    /** 探测下载线路是否可用：配合 Range 头只取少量字节，Streaming 避免把响应体读进内存。 */
    @Streaming
    @GET
    suspend fun probe(@Url url: String, @Header("Range") range: String): Response<ResponseBody>
}

internal data class EndpointFailure(
    val url: String,
    val reason: String
)

internal sealed interface FallbackFetchResult<out T> {
    data class Success<T>(val url: String, val value: T) : FallbackFetchResult<T>
    data class Failure(val errors: List<EndpointFailure>) : FallbackFetchResult<Nothing>
}

internal fun FallbackFetchResult.Failure.lastErrorSuffix(): String =
    errors.lastOrNull()?.let { "\uFF1A${it.url} ${it.reason}" }.orEmpty()

internal suspend fun <T> PublicApiService.fetchFirstParsed(
    urls: Iterable<String>,
    parse: (String, String) -> T?
): FallbackFetchResult<T> {
    val errors = mutableListOf<EndpointFailure>()
    for (url in urls) {
        try {
            val response = fetchRaw(url)
            if (!response.isSuccessful) {
                errors += EndpointFailure(url, "HTTP ${response.code()}")
                continue
            }
            val body = response.body()?.string()
            val value = body?.let { parse(url, it) }
            if (value != null) return FallbackFetchResult.Success(url, value)
            errors += EndpointFailure(url, if (body == null) "\u7A7A\u54CD\u5E94" else "\u89E3\u6790\u4E3A\u7A7A")
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            errors += EndpointFailure(url, exception.localizedMessage ?: exception.message ?: "\u672A\u77E5\u9519\u8BEF")
        }
    }
    return FallbackFetchResult.Failure(errors)
}
