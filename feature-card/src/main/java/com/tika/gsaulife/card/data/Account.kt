package com.tika.gsaulife.card.data

import org.json.JSONObject

data class Account(
    val openid: String,
    val cardId: String,
    var name: String = "",
    var cardNo: String = "",
    var balance: String = "",
    var cachedCode: String = "",
    var cachedAt: Long = 0L,
    var alias: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("openid", openid)
        put("cardId", cardId)
        put("name", name)
        put("cardNo", cardNo)
        put("balance", balance)
        put("cachedCode", cachedCode)
        put("cachedAt", cachedAt)
        put("alias", alias)
    }

    fun displayName(): String = when {
        alias.isNotBlank() -> alias
        name.isNotBlank() -> name
        cardNo.isNotBlank() -> cardNo
        else -> "卡" + openid.takeLast(4)
    }

    fun sameCard(other: Account?): Boolean =
        other != null && openid == other.openid && cardId == other.cardId

    fun hasFreshCode(now: Long = System.currentTimeMillis()): Boolean {
        val age = now - cachedAt
        return cachedCode.isNotBlank() && cachedAt > 0L && age in 0 until PayCodePolicy.VALIDITY_MS
    }

    companion object {
        const val DEFAULT_CARD_ID = "9"

        fun fromJson(json: JSONObject): Account? {
            val openid = json.optString("openid", "")
            if (openid.isBlank()) return null
            return Account(
                openid = openid,
                cardId = json.optString("cardId", DEFAULT_CARD_ID),
                name = json.optString("name", ""),
                cardNo = json.optString("cardNo", ""),
                balance = json.optString("balance", ""),
                cachedCode = json.optString("cachedCode", ""),
                cachedAt = json.optLong("cachedAt", 0L),
                alias = json.optString("alias", "")
            )
        }
    }
}
