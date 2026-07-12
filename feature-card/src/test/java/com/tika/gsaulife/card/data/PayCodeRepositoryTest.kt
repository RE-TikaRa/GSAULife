package com.tika.gsaulife.card.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PayCodeRepositoryTest {
    private val repository = PayCodeRepository()

    private fun html(
        code: String = HEX_CODE,
        accountLine: String = "郎振杰：1073325020407 余额：16.43元"
    ) = """
        <html><body>
        <input type="hidden" id="code" value="$code" />
        <p class="bdb">$accountLine</p>
        </body></html>
    """.trimIndent()

    @Test
    fun `完整页面解析出付款码与账户信息`() {
        val result = repository.parse(html())
        assertTrue(result is PayCodeRepository.Result.Ok)
        result as PayCodeRepository.Result.Ok
        assertEquals(HEX_CODE, result.code)
        assertEquals("郎振杰", result.name)
        assertEquals("1073325020407", result.cardNo)
        assertEquals("16.43元", result.balance)
    }

    @Test
    fun `缺少 code 字段判为凭证失效`() {
        val html = "<html><body><p class=\"bdb\">郎振杰：1073325020407 余额：16.43元</p></body></html>"
        assertTrue(repository.parse(html) is PayCodeRepository.Result.Invalid)
    }

    @Test
    fun `没有账户信息时仍返回付款码`() {
        val html = "<html><body><input id=\"code\" value=\"$HEX_CODE\" /></body></html>"
        val result = repository.parse(html)
        assertTrue(result is PayCodeRepository.Result.Ok)
        result as PayCodeRepository.Result.Ok
        assertEquals(HEX_CODE, result.code)
        assertEquals("", result.name)
        assertEquals("", result.balance)
    }

    @Test
    fun `英文冒号与缺余额也能提取姓名卡号`() {
        val result = repository.parse(html(accountLine = "张三:1073325020408"))
        assertTrue(result is PayCodeRepository.Result.Ok)
        result as PayCodeRepository.Result.Ok
        assertEquals("张三", result.name)
        assertEquals("1073325020408", result.cardNo)
        assertEquals("", result.balance)
    }

    companion object {
        private const val HEX_CODE = "0a1b2c3d4e5f60718293a4b5c6d7e8f9"
    }
}
