package com.tika.gsaulife.card.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountJsonTest {
    @Test
    fun `toJson fromJson 往返保持字段一致`() {
        val account = Account(
            openid = "abcd1234",
            cardId = "12",
            name = "郎振杰",
            cardNo = "1073325020407",
            balance = "12.50",
            cachedCode = "deadbeef",
            cachedAt = 1_700_000_000_000L,
            alias = "饭卡"
        )
        assertEquals(account, Account.fromJson(account.toJson()))
    }

    @Test
    fun `fromJson 缺 openid 丢弃`() {
        val json = JSONObject().put("cardId", "9").put("name", "无凭证")
        assertNull(Account.fromJson(json))
    }

    @Test
    fun `fromJson 空 openid 丢弃`() {
        val json = JSONObject().put("openid", "").put("cardId", "9")
        assertNull(Account.fromJson(json))
    }

    @Test
    fun `fromJson 缺 cardId 回落默认卡`() {
        val json = JSONObject().put("openid", "abcd1234")
        assertEquals(Account.DEFAULT_CARD_ID, Account.fromJson(json)?.cardId)
    }

    @Test
    fun `fromJson 缺可选字段回落空值`() {
        val account = Account.fromJson(
            JSONObject().put("openid", "abcd1234").put("cardId", "9")
        )!!
        assertEquals("", account.name)
        assertEquals("", account.cardNo)
        assertEquals("", account.balance)
        assertEquals("", account.cachedCode)
        assertEquals(0L, account.cachedAt)
        assertEquals("", account.alias)
    }
}
