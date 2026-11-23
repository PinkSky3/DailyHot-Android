package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateManifest(
    val channel: String,
    val version: String,
    val title: String,
    val notes: String,
    val downloadUrl: String,
    val releasePageUrl: String
)

data class AppUpdate(
    val version: String,
    val title: String,
    val notes: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    /** GitHub Release 原站下载地址，不经过加速线路。 */
    val githubDownloadUrl: String = "",
    /** 国内直链（gh-proxy 加速）候选地址，按优先级排列。 */
    val mirrorDownloadUrls: List<String> = emptyList()
)
