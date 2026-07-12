package com.tika.gsaulife

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun `有更高版本时返回镜像化的发布页`() {
        val result = UpdateChecker.parse(release("v1.2.0"), "1.0.0")

        assertTrue(result is UpdateChecker.Result.NewVersion)
        result as UpdateChecker.Result.NewVersion
        assertEquals("1.2.0", result.version)
        assertEquals(
            "https://gh.re-tikara.fun/RE-TikaRa/GSAULife/releases/tag/v1.2.0",
            result.pageUrl
        )
    }

    @Test
    fun `版本相同时已是最新`() {
        assertTrue(
            UpdateChecker.parse(release("v1.0.0"), "1.0.0") is UpdateChecker.Result.UpToDate
        )
    }

    @Test
    fun `当前版本更高时已是最新`() {
        assertTrue(
            UpdateChecker.parse(release("v1.1.0"), "1.2.0") is UpdateChecker.Result.UpToDate
        )
    }

    @Test
    fun `多段版本号逐段比较`() {
        assertTrue(
            UpdateChecker.parse(release("v1.2.1"), "1.2") is UpdateChecker.Result.NewVersion
        )
        assertTrue(
            UpdateChecker.parse(release("v1.2"), "1.2.1") is UpdateChecker.Result.UpToDate
        )
    }

    @Test
    fun `稳定版高于同版本预发布版`() {
        assertTrue(
            UpdateChecker.parse(release("v1.0.0"), "1.0.0-rc1") is
                UpdateChecker.Result.NewVersion
        )
    }

    @Test
    fun `预发布版不高于同版本稳定版`() {
        assertTrue(
            UpdateChecker.parse(release("v1.0.0-rc1"), "1.0.0") is
                UpdateChecker.Result.UpToDate
        )
    }

    private fun release(tag: String) = """
        {
          "tag_name": "$tag",
          "html_url": "https://github.com/RE-TikaRa/GSAULife/releases/tag/$tag"
        }
    """.trimIndent()
}
