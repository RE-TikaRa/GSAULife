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
            Ranking(
                year = item.optString("XN"),
                yearDisplay = item.optString("XN_DISPLAY"),
                term = item.optString("XQ"),
                termDisplay = item.optString("XQ_DISPLAY"),
                gradeLabel = item.optString("CJNJ"),
                majorRank = item.optInt("ZYPM"),
                majorTotal = item.optInt("ZYZRS"),
                classRank = item.optInt("BJPM"),
                classTotal = item.optInt("BJRS"),
            )
        }
    }

    private val SUCCESS_CODES = setOf("#E000000000000", "0")
}
