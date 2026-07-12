package com.tika.gsaulife.academic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.json.JSONException
import org.json.JSONObject

class XgfwParserTest {
    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResource("/fixtures/$name")).readText()

    @Test
    fun `排名列表字段对齐`() {
        val rankings = XgfwParser.rankings(fixture("academic_rankings.json"))
        assertEquals(4, rankings.size)
        val first = rankings.first()
        assertEquals("2024", first.year)
        assertEquals("2024-2025学年", first.yearDisplay)
        assertEquals("1", first.term)
        assertEquals("第一学期", first.termDisplay)
        assertEquals("大一上", first.gradeLabel)
        assertEquals(12, first.majorRank)
        assertEquals(120, first.majorTotal)
        assertEquals(5, first.classRank)
        assertEquals(40, first.classTotal)
        assertEquals("大二下", rankings.last().gradeLabel)
    }

    @Test
    fun `排名真实成功码可解析`() {
        val json = JSONObject(fixture("academic_rankings.json"))
            .put("returnCode", "#E000000000000")
            .toString()
        assertEquals(4, XgfwParser.rankings(json).size)
    }

    @Test
    fun `排名业务错误不解析为空列表`() {
        assertThrows(JSONException::class.java) {
            XgfwParser.rankings(
                """{"returnCode":"#E001","description":"失败","data":[]}"""
            )
        }
    }
}
