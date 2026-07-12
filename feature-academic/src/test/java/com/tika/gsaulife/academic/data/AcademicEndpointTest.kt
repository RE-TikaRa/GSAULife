package com.tika.gsaulife.academic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.URI

class AcademicEndpointTest {
    @Test
    fun `教务系统使用 HTTPS`() {
        assertEquals("https", URI(SchoolSessionStore.JWGL_BASE).scheme)
    }

    @Test
    fun `学校数据请求不自动跟随重定向`() {
        assertFalse(AcademicHttp.client.followRedirects)
    }
}
