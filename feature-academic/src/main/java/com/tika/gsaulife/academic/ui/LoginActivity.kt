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
import java.net.URI

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: AcademicActivityLoginBinding
    private lateinit var system: SchoolSystem
    private val sessions by lazy { SchoolSessionStore.get(this) }
    private var finishingLogin = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        system = SchoolSystem.valueOf(intent.getStringExtra(EXTRA_SYSTEM)!!)
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
                    binding.academicWebView.loadUrl(startUrl(system))
                }
            } else {
                binding.academicWebView.loadUrl(startUrl(system))
            }
        }
    }

    private fun capture(url: String?) {
        if (finishingLogin || url == null || !reachedTarget(url)) return
        val cookieUrl = when (system) {
            SchoolSystem.ACADEMIC ->
                "${SchoolSessionStore.JWGL_BASE}/jsxsd/framework/xsMain.jsp"
            SchoolSystem.STUDENT_AFFAIRS -> "${SchoolSessionStore.XGFW_BASE}/xsfw/"
        }
        val cookies = parseCookies(CookieManager.getInstance().getCookie(cookieUrl))
        if (cookies.isEmpty()) return
        finishingLogin = true
        sessions.save(system, cookies)
        AcademicCache.get(this).clear(system)
        CookieManager.getInstance().flush()
        setResult(RESULT_OK)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.academicWebView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    private fun reachedTarget(url: String): Boolean = reachedSchoolTarget(system, url)

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
        private const val EXTRA_SYSTEM = "academic.school_system"
        private const val EXTRA_FORCE_REAUTH = "academic.force_reauth"
        private const val XGFW_URL =
            "${SchoolSessionStore.XGFW_BASE}/xsfw/sys/jbxxapp/*default/index.do#/wdxx"

        fun intent(context: Context, system: SchoolSystem, forceReauth: Boolean = false): Intent =
            Intent(context, LoginActivity::class.java)
                .putExtra(EXTRA_SYSTEM, system.name)
                .putExtra(EXTRA_FORCE_REAUTH, forceReauth)

        private fun startUrl(system: SchoolSystem): String = when (system) {
            SchoolSystem.ACADEMIC -> SchoolSessionStore.JWGL_BASE
            SchoolSystem.STUDENT_AFFAIRS -> XGFW_URL
        }
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
