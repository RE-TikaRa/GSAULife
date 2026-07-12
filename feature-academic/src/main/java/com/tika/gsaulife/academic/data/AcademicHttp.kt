package com.tika.gsaulife.academic.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal object AcademicHttp {
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
}
