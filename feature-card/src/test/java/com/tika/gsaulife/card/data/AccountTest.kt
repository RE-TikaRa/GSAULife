package com.tika.gsaulife.card.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountTest {
    @Test
    fun `displayName 优先用备注`() {
        val account = Account("abcd1234", "9", name = "郎振杰", cardNo = "107", alias = "饭卡")
        assertEquals("饭卡", account.displayName())
    }

    @Test
    fun `displayName 无备注回落姓名`() {
        val account = Account("abcd1234", "9", name = "郎振杰", cardNo = "107")
        assertEquals("郎振杰", account.displayName())
    }

    @Test
    fun `displayName 无备注无姓名回落卡号`() {
        val account = Account("abcd1234", "9", cardNo = "1073325020407")
        assertEquals("1073325020407", account.displayName())
    }

    @Test
    fun `displayName 全空回落 openid 尾四位`() {
        val account = Account("abcdef123456", "9")
        assertEquals("卡3456", account.displayName())
    }

    @Test
    fun `sameCard openid 与 cardId 都同才算同卡`() {
        val account = Account("abcd", "9")
        assertTrue(account.sameCard(Account("abcd", "9")))
    }

    @Test
    fun `sameCard 同 openid 不同卡号不算同卡`() {
        val account = Account("abcd", "9")
        assertFalse(account.sameCard(Account("abcd", "12")))
    }

    @Test
    fun `sameCard 不同 openid 不算同卡`() {
        val account = Account("abcd", "9")
        assertFalse(account.sameCard(Account("ffff", "9")))
    }

    @Test
    fun `sameCard 传入 null 返回 false`() {
        assertFalse(Account("abcd", "9").sameCard(null))
    }

    @Test
    fun `付款码在一分钟内有效`() {
        val account = Account("abcd", "9", cachedCode = "code", cachedAt = 1_000L)
        assertTrue(account.hasFreshCode(now = 60_999L))
    }

    @Test
    fun `付款码满一分钟失效`() {
        val account = Account("abcd", "9", cachedCode = "code", cachedAt = 1_000L)
        assertFalse(account.hasFreshCode(now = 61_000L))
    }

    @Test
    fun `空付款码始终无效`() {
        val account = Account("abcd", "9", cachedAt = 1_000L)
        assertFalse(account.hasFreshCode(now = 1_001L))
    }

    @Test
    fun `未记录获取时间的付款码无效`() {
        val account = Account("abcd", "9", cachedCode = "code")
        assertFalse(account.hasFreshCode(now = 1L))
    }

    @Test
    fun `获取时间晚于当前时间的付款码无效`() {
        val account = Account("abcd", "9", cachedCode = "code", cachedAt = 2_000L)
        assertFalse(account.hasFreshCode(now = 1_000L))
    }
}
