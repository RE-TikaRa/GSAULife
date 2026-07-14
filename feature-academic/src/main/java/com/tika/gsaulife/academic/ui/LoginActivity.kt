package com.tika.gsaulife.academic.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.SchoolSystem
import com.tika.gsaulife.academic.data.AcademicCache
import com.tika.gsaulife.academic.data.SchoolSessionStore
import com.tika.gsaulife.academic.data.auth.CasAuthClient
import com.tika.gsaulife.academic.data.auth.CasLoginService
import com.tika.gsaulife.academic.data.auth.CasResult
import com.tika.gsaulife.academic.data.auth.PasswordCipher
import com.tika.gsaulife.academic.data.auth.QrStatus
import com.tika.gsaulife.academic.databinding.AcademicActivityLoginBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URI

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: AcademicActivityLoginBinding
    private val sessions by lazy { SchoolSessionStore.get(this) }
    private val casAuth = CasAuthClient()
    private val service by lazy { CasLoginService(casAuth) }

    private var gated = false
    private var scanJob: Job? = null
    private var scanUuid: String? = null
    private var pendingPrep: com.tika.gsaulife.academic.data.auth.PasswordPrep? = null
    private var building = false

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
        binding.academicLoginToolbar.setNavigationOnClickListener { finish() }
        setupWebView()
        setupTabs()
        setupPasswordPanel()
        if (intent.getBooleanExtra(EXTRA_FORCE_REAUTH, false)) {
            CookieManager.getInstance().removeAllCookies { CookieManager.getInstance().flush() }
        }
        showScan()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(binding.academicWebView, true)
        with(binding.academicWebView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = casAuth.userAgent
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        binding.academicWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                if (building) captureXgfw(url)
            }
        }
    }

    private fun setupTabs() {
        val tabs = binding.academicLoginTabs
        tabs.addTab(tabs.newTab().setText(R.string.academic_login_tab_scan))
        tabs.addTab(tabs.newTab().setText(R.string.academic_login_tab_password))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) showScan() else showPassword()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        binding.academicLoginQr.setOnClickListener { startScan() }
    }

    private fun showScan() {
        binding.academicLoginScanPanel.visibility = View.VISIBLE
        binding.academicLoginPwdPanel.visibility = View.GONE
        startScan()
    }

    private fun showPassword() {
        scanJob?.cancel()
        binding.academicLoginScanPanel.visibility = View.GONE
        binding.academicLoginPwdPanel.visibility = View.VISIBLE
    }

    private suspend fun ensureGate() {
        if (gated) return
        service.wengineGate()
        gated = true
    }

    private fun startScan() {
        if (building) return
        scanJob?.cancel()
        scanUuid = null
        binding.academicLoginQr.setImageDrawable(null)
        binding.academicLoginScanSpinner.visibility = View.VISIBLE
        status(R.string.academic_login_preparing)
        scanJob = lifecycleScope.launch {
            ensureGate()
            val uuid = service.qrToken()
            scanUuid = uuid
            showQr(service.qrImage(uuid))
            pollWecom(uuid)
        }
    }

    private fun resumeScan() {
        if (building) return
        val uuid = scanUuid ?: return
        if (scanJob?.isActive == true) return
        if (binding.academicLoginScanPanel.visibility != View.VISIBLE) return
        scanJob = lifecycleScope.launch { pollWecom(uuid) }
    }

    private suspend fun pollWecom(uuid: String) {
        while (true) {
            when (service.qrStatus(uuid)) {
                QrStatus.Confirmed -> {
                    if (service.qrSubmit(uuid) is CasResult.Success) onCasSuccess()
                    else status(R.string.academic_login_failed)
                    return
                }
                QrStatus.Scanned -> status(R.string.academic_login_scan_scanned)
                QrStatus.Expired -> { scanUuid = null; status(R.string.academic_login_scan_expired); return }
                QrStatus.Waiting -> Unit
            }
            delay(1500)
        }
    }

    private fun showQr(png: ByteArray) {
        binding.academicLoginScanSpinner.visibility = View.GONE
        binding.academicLoginScanStatus.text = ""
        BitmapFactory.decodeByteArray(png, 0, png.size)?.let { binding.academicLoginQr.setImageBitmap(it) }
    }

    private fun status(resId: Int) {
        binding.academicLoginScanStatus.text = getString(resId)
    }

    private fun setupPasswordPanel() {
        binding.academicLoginSubmit.setOnClickListener { submitPassword() }
        binding.academicLoginCaptchaImg.setOnClickListener {
            val user = binding.academicLoginUsername.text?.toString().orEmpty()
            if (user.isNotEmpty()) preparePassword(user)
        }
    }

    private fun submitPassword() {
        if (building) return
        val user = binding.academicLoginUsername.text?.toString().orEmpty().trim()
        val pass = binding.academicLoginPassword.text?.toString().orEmpty()
        if (user.isEmpty() || pass.isEmpty()) { status(R.string.academic_login_empty_fields); return }
        val prep = pendingPrep
        if (prep == null) { preparePassword(user); return }
        val captcha = binding.academicLoginCaptcha.text?.toString().orEmpty().trim()
        lifecycleScope.launch {
            val enc = PasswordCipher.encrypt(pass, prep.salt)
            val result = service.submitPassword(user, enc, captcha)
            if (result is CasResult.Success) onCasSuccess()
            else {
                pendingPrep = null
                status(R.string.academic_login_failed)
            }
        }
    }

    private fun preparePassword(user: String) {
        lifecycleScope.launch {
            ensureGate()
            val prep = service.preparePassword(user)
            pendingPrep = prep
            val png = prep.captchaPng
            if (png != null) {
                binding.academicLoginCaptchaRow.visibility = View.VISIBLE
                BitmapFactory.decodeByteArray(png, 0, png.size)?.let {
                    binding.academicLoginCaptchaImg.setImageBitmap(it)
                }
            } else {
                binding.academicLoginCaptchaRow.visibility = View.GONE
                submitPassword()
            }
        }
    }

    private fun onCasSuccess() {
        scanJob?.cancel()
        building = true
        binding.academicLoginContent.visibility = View.GONE
        binding.academicLoginBuilding.visibility = View.VISIBLE
        val manager = CookieManager.getInstance()
        for (c in casAuth.jar.cookies()) {
            manager.setCookie("https://${c.domain}", "${c.name}=${c.value}")
        }
        manager.flush()
        val jwgl = casAuth.jar.cookiesForDomain("jwgl.gsau.edu.cn")
            .associate { it.name to it.value }
        sessions.save(SchoolSystem.ACADEMIC, jwgl)
        binding.academicWebView.loadUrl(XGFW_URL)
    }

    private fun captureXgfw(url: String?) {
        url ?: return
        if (!reachedSchoolTarget(SchoolSystem.STUDENT_AFFAIRS, url)) return
        val raw = CookieManager.getInstance().getCookie("${SchoolSessionStore.XGFW_BASE}/xsfw/")
        raw ?: return
        val xgfw = buildMap {
            for (pair in raw.split(";")) {
                val sep = pair.indexOf('=')
                if (sep <= 0) continue
                val name = pair.substring(0, sep).trim()
                if (name !in this) put(name, pair.substring(sep + 1).trim())
            }
        }
        if (xgfw.isEmpty()) return
        building = false
        sessions.save(SchoolSystem.STUDENT_AFFAIRS, xgfw)
        val cache = AcademicCache.get(this)
        cache.clear(SchoolSystem.ACADEMIC)
        cache.clear(SchoolSystem.STUDENT_AFFAIRS)
        CookieManager.getInstance().flush()
        setResult(RESULT_OK)
        finish()
    }

    override fun onStart() {
        super.onStart()
        resumeScan()
    }

    override fun onStop() {
        scanJob?.cancel()
        super.onStop()
    }

    override fun onDestroy() {
        binding.academicWebView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_FORCE_REAUTH = "academic.force_reauth"
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
