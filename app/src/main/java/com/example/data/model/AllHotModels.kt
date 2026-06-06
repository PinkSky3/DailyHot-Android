package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AllHotSourcesResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "data") val data: AllHotSourcesData? = null
)

@JsonClass(generateAdapter = true)
data class AllHotSourcesData(
    @Json(name = "list") val list: List<AllHotSource>? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "page") val page: Int? = null,
    @Json(name = "page_size") val pageSize: Int? = null
)

@JsonClass(generateAdapter = true)
data class AllHotSource(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class AllHotSourceDataResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "data") val data: AllHotSourceData? = null
)

@JsonClass(generateAdapter = true)
data class AllHotSourceData(
    @Json(name = "list") val list: List<AllHotNewsItem>? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "data_type") val dataType: String? = null
)

@JsonClass(generateAdapter = true)
data class AllHotNewsItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "desc") val desc: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "pic") val pic: String? = null,
    @Json(name = "jump_url") val jumpUrl: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "mobileUrl") val mobileUrl: String? = null,
    @Json(name = "hot") val hot: CoercedString? = null,
    @Json(name = "hot_value") val hotValue: CoercedString? = null,
    @Json(name = "index") val index: CoercedString? = null
)
