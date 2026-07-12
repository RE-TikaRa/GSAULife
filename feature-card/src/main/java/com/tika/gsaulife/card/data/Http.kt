package com.tika.gsaulife.card.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(9, TimeUnit.SECONDS)
        .build()
}
