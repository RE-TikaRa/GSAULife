package com.tika.gsaulife.academic.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class AcademicEndpointTest {
    @Test
    fun `教务系统使用 HTTPS`() {
        assertEquals("https", URI(SchoolSessionStore.JWGL_BASE).scheme)
    }
}
