package com.tika.gsaulife.card.data

import androidx.core.net.toUri

object LinkParser {
    data class Parsed(val openid: String, val cardId: String)

    fun parse(text: String): Parsed? {
        val trimmed = text.trim()
        runCatching {
            val uri = trimmed.toUri()
            val openid = uri.getQueryParameter("openid")
            if (!openid.isNullOrBlank()) {
                val id = uri.getQueryParameter("id") ?: Account.DEFAULT_CARD_ID
                return Parsed(openid, id)
            }
        }
        val openid = Regex("openid=([A-Za-z0-9_-]+)").find(trimmed)?.groupValues?.get(1)
        if (!openid.isNullOrBlank()) {
            val id = Regex("[?&]id=(\\d+)").find(trimmed)?.groupValues?.get(1)
                ?: Account.DEFAULT_CARD_ID
            return Parsed(openid, id)
        }
        return null
    }
}
