package com.tika.gsaulife.academic.data.auth

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

internal class CasCookieJar : CookieJar {
    private val store = mutableMapOf<String, Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (c in cookies) store["${c.domain}|${c.name}"] = c
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        store.values.filter { it.matches(url) }

    @Synchronized
    fun cookies(): List<Cookie> = store.values.toList()

    @Synchronized
    fun cookiesForDomain(host: String): List<Cookie> = store.values.filter { it.domain == host }
}
