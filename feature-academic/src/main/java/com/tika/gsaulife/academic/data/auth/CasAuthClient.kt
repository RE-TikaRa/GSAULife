package com.tika.gsaulife.academic.data.auth

import com.tika.gsaulife.academic.data.AcademicHttp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal class CasAuthClient {
    val jar = CasCookieJar()
    val userAgent = AcademicHttp.USER_AGENT
    val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .cookieJar(jar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
