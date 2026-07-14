package com.tika.gsaulife.academic.data.auth

import com.tika.gsaulife.academic.data.SchoolSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request

internal class CasLoginService(private val auth: CasAuthClient) {
    private val client = auth.client
    private val noRedirect = client.newBuilder().followRedirects(false).build()
    private var execution: String = ""

    private fun get(url: String, referer: String? = null): String =
        request(url, referer).use { it.body?.string().orEmpty() }

    private fun bytes(url: String, referer: String? = null): ByteArray =
        request(url, referer).use { it.body?.bytes() ?: ByteArray(0) }

    private fun request(url: String, referer: String?) = client.newCall(
        Request.Builder().url(url)
            .header("User-Agent", auth.userAgent)
            .apply { referer?.let { header("Referer", it) } }
            .build(),
    ).execute()

    suspend fun wengineGate() = withContext(Dispatchers.IO) {
        val root = "$JWGL/"
        val page = get(root)
        val uid = Regex("id=\"uid\">([^<]+)<").find(page)?.groupValues?.get(1) ?: return@withContext
        get("$JWGL/wengine-auth/human-detect.js?token=$uid", referer = root)
        delay(6000)
        get(root)
    }

    suspend fun preparePassword(username: String): PasswordPrep = withContext(Dispatchers.IO) {
        val page = get(SVC)
        val salt = Regex("id=\"pwdEncryptSalt\"\\s+value=\"([^\"]*)\"").find(page)?.groupValues?.get(1).orEmpty()
        val div = page.indexOf("id=\"pwdLoginDiv\"").coerceAtLeast(0)
        execution = Regex("name=\"execution\"\\s+value=\"([^\"]*)\"").find(page, div)?.groupValues?.get(1).orEmpty()
        val need = get("$AUTH/authserver/checkNeedCaptcha.htl?username=$username&_=${now()}", referer = SVC)
            .contains("\"isNeed\":true")
        val captcha = if (need) bytes("$AUTH/authserver/getCaptcha.htl?_=${now()}", referer = SVC) else null
        PasswordPrep(salt, execution, captcha)
    }

    suspend fun submitPassword(username: String, encPwd: String, captcha: String): CasResult =
        withContext(Dispatchers.IO) {
            val form = FormBody.Builder()
                .add("username", username).add("password", encPwd).add("captcha", captcha)
                .add("_eventId", "submit").add("cllt", "userNameLogin").add("dllt", "generalLogin")
                .add("lt", "").add("execution", execution).build()
            client.newCall(
                Request.Builder().url(SVC).post(form)
                    .header("User-Agent", auth.userAgent).header("Referer", SVC).build(),
            ).execute().use { r ->
                val url = r.request.url.toString()
                val body = r.body?.string().orEmpty()
                if ("authserver" !in url && "pwdLoginDiv" !in body) CasResult.Success
                else CasResult.Failed(loginError(body))
            }
        }

    suspend fun qrToken(): String = withContext(Dispatchers.IO) {
        val page = get(SVC)
        execution = Regex("name=\"execution\"\\s+value=\"([^\"]*)\"").find(page)?.groupValues?.get(1).orEmpty()
        get("$AUTH/authserver/qrCode/getToken?ts=${now()}&uuid=").trim()
    }

    suspend fun qrImage(uuid: String): ByteArray =
        withContext(Dispatchers.IO) { bytes("$AUTH/authserver/qrCode/getCode?uuid=$uuid") }

    suspend fun qrStatus(uuid: String): QrStatus = withContext(Dispatchers.IO) {
        when (get("$AUTH/authserver/qrCode/getStatus.htl?ts=${now()}&uuid=$uuid").trim()) {
            "1" -> QrStatus.Confirmed
            "2" -> QrStatus.Scanned
            "3" -> QrStatus.Expired
            else -> QrStatus.Waiting
        }
    }

    suspend fun qrSubmit(uuid: String): CasResult = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("lt", "").add("uuid", uuid).add("cllt", "qrLogin").add("dllt", "generalLogin")
            .add("_eventId", "submit").add("execution", execution).build()
        val action = "$AUTH/authserver/login?display=qrLogin&service=http%3A%2F%2Fjwgl.gsau.edu.cn%2F"
        client.newCall(
            Request.Builder().url(action).post(form).header("User-Agent", auth.userAgent).build(),
        ).execute().use { r ->
            val url = r.request.url.toString()
            if ("authserver" !in url || "jwgl" in url) CasResult.Success else CasResult.Failed(null)
        }
    }

    private fun now() = System.currentTimeMillis()

    private fun loginError(body: String): String? {
        for (pat in listOf(
            "id=\"showErrorTip\"[^>]*>([^<]*)<",
            ">([^<]*(?:错误|有误|失败|禁止|锁定|验证码|不存在)[^<]*)<",
        )) {
            Regex(pat).find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return null
    }
}

internal data class PasswordPrep(val salt: String, val execution: String, val captchaPng: ByteArray?)

private const val AUTH = "https://authserver.gsau.edu.cn"
private val JWGL = SchoolSessionStore.JWGL_BASE
private val SVC = "$AUTH/authserver/login?service=http%3A%2F%2Fjwgl.gsau.edu.cn%2F"
