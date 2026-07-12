package com.tika.gsaulife

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object UpdateChecker {
    sealed class Result {
        data class NewVersion(val version: String, val pageUrl: String) : Result()
        data object UpToDate : Result()
        data class Error(val message: String) : Result()
    }

    private val client = OkHttpClient()

    suspend fun check(currentVersion: String): Result = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(LATEST)
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use Result.Error("HTTP ${response.code}")
                parse(response.body.string(), currentVersion)
            }
        }.getOrElse { Result.Error(it.message ?: "网络错误") }
    }

    internal fun parse(json: String, currentVersion: String): Result {
        val release = JSONObject(json)
        val version = release.getString("tag_name").removePrefix("v")
        if (compareVersions(version, currentVersion) <= 0) return Result.UpToDate
        val page = release.getString("html_url").replace("https://github.com", PROXY)
        return Result.NewVersion(version, page)
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = Version.parse(left)
        val b = Version.parse(right)
        repeat(maxOf(a.parts.size, b.parts.size)) { index ->
            val difference = (a.parts.getOrNull(index) ?: 0) -
                (b.parts.getOrNull(index) ?: 0)
            if (difference != 0) return difference
        }
        return when {
            a.preRelease == b.preRelease -> 0
            a.preRelease == null -> 1
            b.preRelease == null -> -1
            else -> a.preRelease.compareTo(b.preRelease)
        }
    }

    private data class Version(val parts: List<Int>, val preRelease: String?) {
        companion object {
            fun parse(value: String): Version {
                val version = value.substringBefore('+')
                return Version(
                    version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 },
                    version.substringAfter('-', "").ifEmpty { null }
                )
            }
        }
    }

    private const val PROXY = "https://gh.re-tikara.fun"
    private const val LATEST = "$PROXY/api/repos/RE-TikaRa/GSAULife/releases/latest"
}
