package com.example.data.update

import com.example.data.api.EndpointFailure
import com.example.data.api.FallbackFetchResult
import com.example.data.model.UpdateManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {
    @Test
    fun `missing stable channel means current version is latest`() {
        val result = FallbackFetchResult.Failure(
            listOf(
                EndpointFailure("https://cdn.example/update.json", "HTTP 404"),
                EndpointFailure("https://raw.example/update.json", "HTTP 404")
            )
        ).toUpdateCheckResult()

        assertEquals(UpdateCheckResult.NoUpdate, result)
    }

    @Test
    fun `404 from any route means no release yet`() {
        val result = FallbackFetchResult.Failure(
            listOf(
                EndpointFailure("https://gh-proxy.example/update.json", "HTTP 404"),
                EndpointFailure("https://cdn.example/update.json", "timeout"),
                EndpointFailure("https://raw.example/update.json", "timeout")
            )
        ).toUpdateCheckResult()

        assertEquals(UpdateCheckResult.NoUpdate, result)
    }

    @Test
    fun `network failure on every route remains unavailable`() {
        val result = FallbackFetchResult.Failure(
            listOf(
                EndpointFailure("https://gh-proxy.example/update.json", "HTTP 502"),
                EndpointFailure("https://cdn.example/update.json", "timeout"),
                EndpointFailure("https://raw.example/update.json", "timeout")
            )
        ).toUpdateCheckResult()

        assertEquals(UpdateCheckResult.Unavailable, result)
    }

    @Test
    fun `available update offers mirror and github downloads`() {
        val update = (manifest(version = "1.4.0").toUpdateCheckResult("1.3.1") as UpdateCheckResult.Available).update
        val githubUrl = "https://github.com/PinkSky3/DailyHot-Android/releases/download/v1.4.0/JuHeZhiXun_ver1.4.0.apk"

        assertEquals(githubUrl, update.githubDownloadUrl)
        assertEquals(GH_PROXY_HOSTS.size, update.mirrorDownloadUrls.size)
        assertEquals("https://gh-proxy.org/$githubUrl", update.mirrorDownloadUrls.first())
        assertTrue(update.mirrorDownloadUrls.all { it.endsWith("/$githubUrl") })
    }

    @Test
    fun `newer confirmed stable manifest is available`() {
        val result = manifest(version = "1.4.0").toUpdateCheckResult("1.3.1")

        assertTrue(result is UpdateCheckResult.Available)
        assertEquals("1.4.0", (result as UpdateCheckResult.Available).update.version)
    }

    @Test
    fun `draft and prerelease channels are ignored`() {
        assertEquals(
            UpdateCheckResult.NoUpdate,
            manifest(version = "1.4.0", channel = "draft").toUpdateCheckResult("1.3.1")
        )
        assertEquals(
            UpdateCheckResult.NoUpdate,
            manifest(version = "1.4.0", channel = "prerelease").toUpdateCheckResult("1.3.1")
        )
    }

    @Test
    fun `action build links are ignored`() {
        val manifest = manifest(
            version = "1.4.0",
            downloadUrl = "https://github.com/PinkSky3/DailyHot-Android/actions/runs/123"
        )

        assertEquals(UpdateCheckResult.NoUpdate, manifest.toUpdateCheckResult("1.3.1"))
    }

    @Test
    fun `wrong release or apk path is ignored`() {
        assertEquals(
            UpdateCheckResult.NoUpdate,
            manifest(
                version = "1.4.0",
                releasePageUrl = "https://github.com/PinkSky3/DailyHot-Android/releases/tag/v1.4.1"
            ).toUpdateCheckResult("1.3.1")
        )
        assertEquals(
            UpdateCheckResult.NoUpdate,
            manifest(
                version = "1.4.0",
                downloadUrl = "https://cdn.jsdelivr.net/gh/PinkSky3/DailyHot-Android@release-channel/app-release.apk"
            ).toUpdateCheckResult("1.3.1")
        )
    }

    @Test
    fun `preview and older versions are ignored`() {
        assertEquals(
            UpdateCheckResult.NoUpdate,
            manifest(version = "1.4.0-beta").toUpdateCheckResult("1.3.1")
        )
        assertEquals(
            UpdateCheckResult.NoUpdate,
            manifest(version = "1.3.0").toUpdateCheckResult("1.3.1")
        )
    }

    private fun manifest(
        version: String,
        channel: String = "stable",
        downloadUrl: String = "https://cdn.jsdelivr.net/gh/PinkSky3/DailyHot-Android@release-channel/" +
            java.net.URLEncoder.encode("聚合智讯_ver$version.apk", Charsets.UTF_8.name()),
        releasePageUrl: String = "https://github.com/PinkSky3/DailyHot-Android/releases/tag/v$version"
    ) = UpdateManifest(
        channel = channel,
        version = version,
        title = "聚合智讯 $version",
        notes = "更新内容",
        downloadUrl = downloadUrl,
        releasePageUrl = releasePageUrl
    )
}
