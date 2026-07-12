package com.tika.gsaulife.academic.data

import com.tika.gsaulife.academic.model.Ranking
import org.json.JSONException
import org.json.JSONObject

object XgfwParser {
    fun rankings(json: String): List<Ranking> {
        val root = JSONObject(json)
        if (root.optString("returnCode") !in SUCCESS_CODES) {
            throw JSONException(root.optString("description").ifBlank { "排名请求失败" })
        }
        val array = root.optJSONArray("data")
            ?: throw JSONException(
                root.optString("description").ifBlank { "排名响应缺少 data" }
        )
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val majorRank = item.getInt("ZYPM")
            val majorTotal = item.getInt("ZYZRS")
            val classRank = item.getInt("BJPM")
            val classTotal = item.getInt("BJRS")
            if (majorRank !in 1..majorTotal || classRank !in 1..classTotal) {
                throw JSONException("排名条目数值无效")
            }
            Ranking(
                year = item.requiredString("XN"),
                yearDisplay = item.requiredString("XN_DISPLAY"),
                term = item.requiredString("XQ"),
                termDisplay = item.requiredString("XQ_DISPLAY"),
                gradeLabel = item.requiredString("CJNJ"),
                majorRank = majorRank,
                majorTotal = majorTotal,
                classRank = classRank,
                classTotal = classTotal,
            )
        }
    }

    private fun JSONObject.requiredString(name: String): String =
        getString(name).ifBlank { throw JSONException("排名条目缺少 $name") }

    private val SUCCESS_CODES = setOf("#E000000000000", "0")
}
