package com.tika.gsaulife.academic.data

import com.tika.gsaulife.academic.SchoolSystem
import com.tika.gsaulife.academic.model.ExamPage
import com.tika.gsaulife.academic.model.Grade
import com.tika.gsaulife.academic.model.SchedulePage
import com.tika.gsaulife.academic.model.ScoreDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException

internal class JwxtRepository(
    private val sessions: SchoolSessionStore,
    private val cache: AcademicCache,
) {
    private val client = AcademicHttp.client

    suspend fun grades(): AcademicResult<List<Grade>> = withContext(Dispatchers.IO) {
        val session = sessions.snapshot(SchoolSystem.ACADEMIC)
        request(
            get("/jsxsd/kscj/cjcx_list", "$BASE/jsxsd/kscj/cjcx_frm", session.cookieHeader),
            session.version,
            JwxtParser::grades,
        ).cacheIfCurrent(session.version, cache::saveGrades)
    }

    suspend fun scoreDetail(grade: Grade): AcademicResult<ScoreDetail> =
        withContext(Dispatchers.IO) {
            val session = sessions.snapshot(SchoolSystem.ACADEMIC)
            val path = "/jsxsd/kscj/pscj_list.do?xs0101id=${grade.studentId}" +
                "&jx0404id=${grade.classId}&zcj=${grade.score}"
            request(
                get(path, "$BASE/jsxsd/kscj/cjcx_list", session.cookieHeader),
                session.version,
                JwxtParser::scoreDetail,
            ).cacheIfCurrent(session.version) { cache.saveScoreDetail(grade, it) }
        }

    suspend fun schedule(): AcademicResult<SchedulePage> = withContext(Dispatchers.IO) {
        val session = sessions.snapshot(SchoolSystem.ACADEMIC)
        request(
            get(SCHEDULE_PATH, null, session.cookieHeader),
            session.version,
            JwxtParser::schedulePage,
        ).cacheIfCurrent(session.version, cache::saveSchedule)
    }

    suspend fun exams(): AcademicResult<ExamPage> = withContext(Dispatchers.IO) {
        val session = sessions.snapshot(SchoolSystem.ACADEMIC)
        when (
            val schedule = request(
                get(SCHEDULE_PATH, null, session.cookieHeader),
                session.version,
                JwxtParser::schedulePage,
            )
        ) {
            is AcademicResult.Ok -> {
                if (!sessions.isCurrent(SchoolSystem.ACADEMIC, session.version)) {
                    return@withContext AcademicResult.Stale
                }
                val term = schedule.data.term
                if (term.isEmpty()) return@withContext AcademicResult.Error("未读取到当前学期")
                val body = FormBody.Builder()
                    .add("xqlbmc", "")
                    .add("xnxqid", term)
                    .add("xqlb", "")
                    .build()
                request(
                    post(
                        "/jsxsd/xsks/xsksap_list",
                        body,
                        "$BASE/jsxsd/xsks/xsksap_query",
                        session.cookieHeader,
                    ),
                    session.version,
                ) { html ->
                    ExamPage(term, JwxtParser.exams(html))
                }.cacheIfCurrent(session.version, cache::saveExams)
            }
            AcademicResult.LoggedOut -> AcademicResult.LoggedOut
            AcademicResult.Stale -> AcademicResult.Stale
            is AcademicResult.Error -> schedule
        }
    }

    private fun get(path: String, referer: String?, cookieHeader: String): Request =
        requestBuilder(referer, cookieHeader).url("$BASE$path").get().build()

    private fun post(path: String, body: FormBody, referer: String, cookieHeader: String): Request =
        requestBuilder(referer, cookieHeader).url("$BASE$path").post(body).build()

    private fun requestBuilder(referer: String?, cookieHeader: String): Request.Builder = Request.Builder()
        .header("User-Agent", AcademicHttp.USER_AGENT)
        .header("Cookie", cookieHeader)
        .also { if (referer != null) it.header("Referer", referer) }

    private fun <T> AcademicResult<T>.cacheIfCurrent(
        version: Long,
        save: (T) -> Unit,
    ): AcademicResult<T> = when (this) {
        is AcademicResult.Ok -> {
            if (sessions.runIfCurrent(SchoolSystem.ACADEMIC, version) { save(data) }) {
                this
            } else {
                AcademicResult.Stale
            }
        }
        else -> this
    }

    private fun <T> request(
        request: Request,
        version: Long,
        parse: (String) -> T,
    ): AcademicResult<T> = try {
        client.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) return sessionExpired(version)
            if (response.code in 300..399) return sessionExpired(version)
            if (!response.isSuccessful) return error(version, "HTTP ${response.code}")
            val text = response.body.string()
            if (isLoginPage(text)) sessionExpired(version) else AcademicResult.Ok(parse(text))
        }
    } catch (exception: IOException) {
        error(version, exception.message ?: "网络错误")
    }

    private fun sessionExpired(version: Long): AcademicResult<Nothing> =
        if (sessions.clearIfCurrent(SchoolSystem.ACADEMIC, version)) {
            AcademicResult.LoggedOut
        } else {
            AcademicResult.Stale
        }

    private fun error(version: Long, message: String): AcademicResult<Nothing> =
        if (sessions.isCurrent(SchoolSystem.ACADEMIC, version)) {
            AcademicResult.Error(message)
        } else {
            AcademicResult.Stale
        }

    private fun isLoginPage(text: String): Boolean =
        "authserver" in text || "用户登录" in text || "wengine" in text

    companion object {
        private const val BASE = SchoolSessionStore.JWGL_BASE
        private const val SCHEDULE_PATH = "/jsxsd/xskb/xskb_list.do"
    }
}
