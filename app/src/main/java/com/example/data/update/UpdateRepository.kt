package com.example.data.update

import com.example.data.api.FallbackFetchResult
import com.example.data.api.PublicApiService
import com.example.data.api.RetrofitClient
import com.example.data.api.fetchFirstParsed
import com.example.data.model.AppUpdate
import com.example.data.model.UpdateManifest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

sealed interface UpdateCheckResult {
    data class Available(val update: AppUpdate) : UpdateCheckResult
    data object NoUpdate : UpdateCheckResult
    data object Unavailable : UpdateCheckResult
}

private const val GITHUB_REPO = "PinkSky3/DailyHot-Android"
private const val RAW_MANIFEST_URL =
    "https://raw.githubusercontent.com/$GITHUB_REPO/release-channel/update.json"
private const val JSDELIVR_MANIFEST_URL =
    "https://cdn.jsdelivr.net/gh/$GITHUB_REPO@release-channel/update.json"

/** 单条国内线路的探测时间上限，超时就换下一条。 */
private const val MIRROR_PROBE_TIMEOUT_MS = 6_000L

/**
 * gh-proxy 加速节点，用法是把完整的 GitHub 地址拼在节点后面。
 * 均已在大陆网络实测可直连，按实测下载速度从快到慢排列。
 */
internal val GH_PROXY_HOSTS = listOf(
    "https://gh-proxy.org",
    "https://gh-proxy.com",
    "https://cdn.gh-proxy.org",
    "https://edgeone.gh-proxy.org"
)

internal fun ghProxyUrl(host: String, githubUrl: String): String = "$host/$githubUrl"

class UpdateRepository(
    private val api: PublicApiService = RetrofitClient.publicApi
) {
    private val manifestAdapter = RetrofitClient.moshi.adapter(UpdateManifest::class.java)

    suspend fun check(currentVersion: String): UpdateCheckResult {
        // 先走国内加速线路，再试 jsDelivr 和 GitHub 原站。
        // 参数每分钟变化一次，避免加速节点返回缓存的旧清单。
        val cacheBuster = "?ts=${System.currentTimeMillis() / 60_000}"
        val manifestUrls = GH_PROXY_HOSTS.map { ghProxyUrl(it, RAW_MANIFEST_URL) + cacheBuster } +
            listOf(JSDELIVR_MANIFEST_URL, RAW_MANIFEST_URL)

        return when (val result = api.fetchFirstParsed(
            urls = manifestUrls,
            parse = { _, body -> runCatching { manifestAdapter.fromJson(body) }.getOrNull() }
        )) {
            is FallbackFetchResult.Success -> result.value.toUpdateCheckResult(currentVersion)
            is FallbackFetchResult.Failure -> result.toUpdateCheckResult()
        }
    }

    /** 依次探测国内直链线路，返回第一条能连通的下载地址；全部不通时返回 null。 */
    suspend fun resolveMirrorDownload(update: AppUpdate): String? {
        for (url in update.mirrorDownloadUrls) {
            val reachable = withTimeoutOrNull(MIRROR_PROBE_TIMEOUT_MS) {
                try {
                    // 只请求第 1 个字节，确认线路可用，不下载整个安装包
                    val response = api.probe(url, "bytes=0-0")
                    response.body()?.close()
                    response.errorBody()?.close()
                    response.code() == 200 || response.code() == 206
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    false
                }
            } ?: false
            if (reachable) return url
        }
        return null
    }
}

/**
 * 清单获取失败时：只要有一条线路明确返回 404，说明清单文件确实不存在（还没有正式发布），
 * 按「已是最新」处理；全部是网络错误才算检查失败。
 */
internal fun FallbackFetchResult.Failure.toUpdateCheckResult(): UpdateCheckResult =
    if (errors.any { it.reason == "HTTP 404" }) {
        UpdateCheckResult.NoUpdate
    } else {
        UpdateCheckResult.Unavailable
    }

internal fun UpdateManifest.toUpdateCheckResult(currentVersion: String): UpdateCheckResult {
    if (channel != "stable") return UpdateCheckResult.NoUpdate

    val releaseVersion = SemanticVersion.parse(version) ?: return UpdateCheckResult.NoUpdate
    val installedVersion = SemanticVersion.parse(currentVersion) ?: return UpdateCheckResult.Unavailable
    if (releaseVersion <= installedVersion) return UpdateCheckResult.NoUpdate

    val normalizedVersion = releaseVersion.toString()
    val expectedReleasePage =
        "https://github.com/$GITHUB_REPO/releases/tag/v$normalizedVersion"
    val encodedApkName = java.net.URLEncoder.encode(
        "聚合智讯_ver$normalizedVersion.apk",
        Charsets.UTF_8.name()
    )
    val expectedDownloadUrl =
        "https://cdn.jsdelivr.net/gh/$GITHUB_REPO@release-channel/$encodedApkName"
    // 清单格式保持不变，旧版本 App 仍按原规则校验
    if (releasePageUrl != expectedReleasePage) return UpdateCheckResult.NoUpdate
    if (downloadUrl != expectedDownloadUrl) return UpdateCheckResult.NoUpdate

    // GitHub Release 中的安装包文件名，与发布脚本保持一致
    val githubDownloadUrl =
        "https://github.com/$GITHUB_REPO/releases/download/v$normalizedVersion/JuHeZhiXun_ver$normalizedVersion.apk"

    return UpdateCheckResult.Available(
        AppUpdate(
            version = normalizedVersion,
            title = title.ifBlank { "聚合智讯 $normalizedVersion" },
            notes = notes.trim(),
            downloadUrl = downloadUrl,
            releasePageUrl = releasePageUrl,
            githubDownloadUrl = githubDownloadUrl,
            mirrorDownloadUrls = GH_PROXY_HOSTS.map { ghProxyUrl(it, githubDownloadUrl) }
        )
    )
}

internal data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val stableVersionPattern = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)$")

        fun parse(value: String): SemanticVersion? {
            val match = stableVersionPattern.matchEntire(value.trim()) ?: return null
            return SemanticVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].toIntOrNull() ?: return null
            )
        }
    }
}
