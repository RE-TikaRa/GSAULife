package com.tika.gsaulife.academic.data

import com.tika.gsaulife.academic.SchoolSystem
import com.tika.gsaulife.academic.model.Ranking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONException
import java.io.IOException

internal class XgfwRepository(
    private val sessions: SchoolSessionStore,
    private val cache: AcademicCache,
) {
    private val client = AcademicHttp.client

    suspend fun rankings(): AcademicResult<List<Ranking>> = withContext(Dispatchers.IO) {
        val session = sessions.snapshot(SchoolSystem.STUDENT_AFFAIRS)
        val request = Request.Builder()
            .url("$BASE/xsfw/sys/jbxxapp/modules/infoStudent/getStuAcademicRankings.do")
            .header("User-Agent", AcademicHttp.USER_AGENT)
            .header("Cookie", session.cookieHeader)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "$BASE/xsfw/sys/jbxxapp/*default/index.do")
            .post(FormBody.Builder().add("requestParamStr", "{}").build())
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 401 || response.code == 403) {
                    return@withContext sessionExpired(session.version)
                }
                if (response.code in 300..399) {
                    return@withContext sessionExpired(session.version)
                }
                if (!response.isSuccessful) {
                    return@withContext error(session.version, "HTTP ${response.code}")
                }
                val text = response.body.string()
                if (isLoginPage(text)) {
                    sessionExpired(session.version)
                } else {
                    val rankings = XgfwParser.rankings(text)
                    if (
                        sessions.runIfCurrent(SchoolSystem.STUDENT_AFFAIRS, session.version) {
                            cache.saveRankings(rankings)
                        }
                    ) {
                        AcademicResult.Ok(rankings)
                    } else {
                        AcademicResult.Stale
                    }
                }
            }
        } catch (exception: IOException) {
            error(session.version, exception.message ?: "网络错误")
        } catch (exception: JSONException) {
            error(session.version, exception.message ?: "排名数据格式错误")
        }
    }

    private fun isLoginPage(text: String): Boolean =
        "authserver" in text || "用户登录" in text || "wengine" in text

    private fun sessionExpired(version: Long): AcademicResult<Nothing> =
        if (sessions.clearIfCurrent(SchoolSystem.STUDENT_AFFAIRS, version)) {
            AcademicResult.LoggedOut
        } else {
            AcademicResult.Stale
        }

    private fun error(version: Long, message: String): AcademicResult<Nothing> =
        if (sessions.isCurrent(SchoolSystem.STUDENT_AFFAIRS, version)) {
            AcademicResult.Error(message)
        } else {
            AcademicResult.Stale
        }

    companion object {
        private const val BASE = SchoolSessionStore.XGFW_BASE
    }
}
