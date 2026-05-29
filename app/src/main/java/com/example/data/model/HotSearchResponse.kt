package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HotSearchResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "name") val name: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "link") val link: String?,
    @Json(name = "updateTime") val updateTime: String?,
    @Json(name = "total") val total: Int?,
    @Json(name = "data") val data: List<HotSearchItem>?
)

@JsonClass(generateAdapter = true)
data class HotSearchItem(
    @Json(name = "id") val id: CoercedString? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "desc") val desc: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "pic") val pic: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "hot") val hot: CoercedString? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "mobileUrl") val mobileUrl: String? = null
)
