package com.tika.gsaulife.academic.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.tika.gsaulife.academic.SchoolSystem
import com.tika.gsaulife.academic.data.AcademicCache
import com.tika.gsaulife.academic.data.AcademicHttp
import com.tika.gsaulife.academic.data.SchoolSessionStore
import com.tika.gsaulife.academic.databinding.AcademicActivityLoginBinding
import org.json.JSONObject
import java.net.URI

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: AcademicActivityLoginBinding
    private val sessions by lazy { SchoolSessionStore.get(this) }
    private var step = Step.ACADEMIC
    private var academicCookies: Map<String, String> = emptyMap()

    private enum class Step { ACADEMIC, STUDENT_AFFAIRS, DONE }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = AcademicActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(binding.academicWebView, true)
        with(binding.academicWebView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = AcademicHttp.USER_AGENT
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
        }
        binding.academicWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                binding.academicLoginProgress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                binding.academicLoginProgress.visibility = View.GONE
                capture(url)
            }
        }
        if (savedInstanceState == null || binding.academicWebView.restoreState(savedInstanceState) == null) {
            if (intent.getBooleanExtra(EXTRA_FORCE_REAUTH, false)) {
                cookies.removeAllCookies {
                    cookies.flush()
                    binding.academicWebView.loadUrl(SchoolSessionStore.JWGL_BASE)
                }
            } else {
                binding.academicWebView.loadUrl(SchoolSessionStore.JWGL_BASE)
            }
        } else {
            step = Step.entries[savedInstanceState.getInt(STATE_STEP)]
            savedInstanceState.getString(STATE_ACADEMIC)?.let {
                val json = JSONObject(it)
                academicCookies = buildMap {
                    for (name in json.keys()) put(name, json.getString(name))
                }
            }
        }
    }

    private fun capture(url: String?) {
        if (step == Step.DONE || url == null) return
        val system = when (step) {
            Step.ACADEMIC -> SchoolSystem.ACADEMIC
            Step.STUDENT_AFFAIRS -> SchoolSystem.STUDENT_AFFAIRS
            Step.DONE -> return
        }
        if (!reachedSchoolTarget(system, url)) return
        val cookieUrl = when (system) {
            SchoolSystem.ACADEMIC ->
                "${SchoolSessionStore.JWGL_BASE}/jsxsd/framework/xsMain.jsp"
            SchoolSystem.STUDENT_AFFAIRS -> "${SchoolSessionStore.XGFW_BASE}/xsfw/"
        }
        val cookies = parseCookies(CookieManager.getInstance().getCookie(cookieUrl))
        if (cookies.isEmpty()) return
        when (step) {
            Step.ACADEMIC -> {
                academicCookies = cookies
                step = Step.STUDENT_AFFAIRS
                binding.academicWebView.loadUrl(XGFW_URL)
            }
            Step.STUDENT_AFFAIRS -> {
                step = Step.DONE
                sessions.save(SchoolSystem.ACADEMIC, academicCookies)
                sessions.save(SchoolSystem.STUDENT_AFFAIRS, cookies)
                val cache = AcademicCache.get(this)
                cache.clear(SchoolSystem.ACADEMIC)
                cache.clear(SchoolSystem.STUDENT_AFFAIRS)
                CookieManager.getInstance().flush()
                setResult(RESULT_OK)
                finish()
            }
            Step.DONE -> Unit
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.academicWebView.saveState(outState)
        outState.putInt(STATE_STEP, step.ordinal)
        if (academicCookies.isNotEmpty()) {
            outState.putString(STATE_ACADEMIC, JSONObject(academicCookies).toString())
        }
        super.onSaveInstanceState(outState)
    }

    private fun parseCookies(raw: String?): Map<String, String> {
        raw ?: return emptyMap()
        return buildMap {
            for (pair in raw.split(";")) {
                val separator = pair.indexOf('=')
                if (separator <= 0) continue
                val name = pair.substring(0, separator).trim()
                if (name !in this) put(name, pair.substring(separator + 1).trim())
            }
        }
    }

    override fun onDestroy() {
        binding.academicWebView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_FORCE_REAUTH = "academic.force_reauth"
        private const val STATE_STEP = "academic.login_step"
        private const val STATE_ACADEMIC = "academic.login_academic_cookies"
        private const val XGFW_URL =
            "${SchoolSessionStore.XGFW_BASE}/xsfw/sys/jbxxapp/*default/index.do#/wdxx"

        fun intent(context: Context, forceReauth: Boolean = false): Intent =
            Intent(context, LoginActivity::class.java)
                .putExtra(EXTRA_FORCE_REAUTH, forceReauth)
    }
}

internal fun reachedSchoolTarget(system: SchoolSystem, url: String): Boolean = runCatching {
    val uri = URI(url)
    if (!uri.scheme.equals("https", ignoreCase = true)) return@runCatching false
    val path = uri.path.orEmpty()
    when (system) {
        SchoolSystem.ACADEMIC ->
            uri.host.equals("jwgl.gsau.edu.cn", ignoreCase = true) &&
                path.startsWith("/jsxsd/framework/")
        SchoolSystem.STUDENT_AFFAIRS ->
            uri.host.equals("xgfw.gsau.edu.cn", ignoreCase = true) &&
                path.startsWith("/xsfw/")
    }
}.getOrDefault(false)
