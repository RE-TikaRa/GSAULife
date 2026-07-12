package com.tika.gsaulife.card.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class PayCodeRepository {
    sealed class Result {
        data class Ok(
            val code: String,
            val name: String,
            val cardNo: String,
            val balance: String
        ) : Result()

        data object Invalid : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun fetch(openid: String, cardId: String): Result = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE?openid=$openid&displayflag=1&id=$cardId")
            .header("User-Agent", USER_AGENT)
            .build()
        try {
            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.Error("HTTP ${response.code}")
                parse(response.body.string())
            }
        } catch (exception: Exception) {
            Result.Error(exception.message ?: "网络错误")
        }
    }

    internal fun parse(html: String): Result {
        val code = CODE_REGEX.find(html)?.groupValues?.get(1)
        if (code.isNullOrBlank()) return Result.Invalid

        var name = ""
        var cardNo = ""
        var balance = ""
        ACCOUNT_LINE_REGEX.find(html)?.groupValues?.get(1)?.let { line ->
            NAME_CARD_REGEX.find(line)?.let {
                name = it.groupValues[1].trim()
                cardNo = it.groupValues[2].trim()
            }
            BALANCE_REGEX.find(line)?.let { balance = it.groupValues[1].trim() }
        }
        return Result.Ok(code, name, cardNo, balance)
    }

    companion object {
        private const val BASE = "https://yktapp.gsau.edu.cn/virtualcard/openVirtualcard"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Version/4.0 Chrome/108.0.0.0 Mobile Safari/537.36 MicroMessenger/8.0.30"

        private val CODE_REGEX = Regex("""id="code"\s+value="([0-9A-Fa-f]+)"""")
        private val ACCOUNT_LINE_REGEX = Regex("""<p class="bdb">([^<]*)</p>""")
        private val NAME_CARD_REGEX = Regex("""(.+?)[：:]\s*(\d+)""")
        private val BALANCE_REGEX = Regex("""余额[：:]\s*([0-9.]+元?)""")
    }
}
