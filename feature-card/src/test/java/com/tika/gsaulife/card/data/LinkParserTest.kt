package com.tika.gsaulife.card.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkParserTest {
    @Test
    fun `从完整链接提取 openid 与 id`() {
        val link = "https://yktapp.gsau.edu.cn/virtualcard/openVirtualcard?openid=aBcD1234ef56&displayflag=1&id=9"
        val parsed = LinkParser.parse(link)
        assertEquals("aBcD1234ef56", parsed?.openid)
        assertEquals("9", parsed?.cardId)
    }

    @Test
    fun `缺少 id 时回落默认卡号 9`() {
        val parsed = LinkParser.parse("openid=aBcD1234ef56")
        assertEquals("aBcD1234ef56", parsed?.openid)
        assertEquals("9", parsed?.cardId)
    }

    @Test
    fun `夹带多余文字也能识别 openid`() {
        val parsed = LinkParser.parse("看这个 openid=deadbeef01 &id=12 谢谢")
        assertEquals("deadbeef01", parsed?.openid)
        assertEquals("12", parsed?.cardId)
    }

    @Test
    fun `备用路径识别含字母下划线连字符的 openid`() {
        val parsed = LinkParser.parse("openid=oMgHu6Ab-cD_xyz12&id=9")
        assertEquals("oMgHu6Ab-cD_xyz12", parsed?.openid)
        assertEquals("9", parsed?.cardId)
    }

    @Test
    fun `没有 openid 返回 null`() {
        assertNull(LinkParser.parse("https://example.com/nothing-here"))
        assertNull(LinkParser.parse(""))
    }
}
