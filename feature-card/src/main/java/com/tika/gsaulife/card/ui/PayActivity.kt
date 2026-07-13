package com.tika.gsaulife.card.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.tika.gsaulife.card.LegalAgreementStore
import com.tika.gsaulife.card.R
import com.tika.gsaulife.card.data.Account
import com.tika.gsaulife.card.data.AccountStore
import com.tika.gsaulife.card.data.PayCodeManager
import com.tika.gsaulife.card.data.PayCodePolicy
import com.tika.gsaulife.card.data.PayCodeRepository
import com.tika.gsaulife.card.databinding.CardActivityPayBinding
import com.tika.gsaulife.card.qr.QrGenerator
import com.tika.gsaulife.card.widget.PayWidgetProvider
import com.tika.gsaulife.card.work.RefreshController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PayActivity : AppCompatActivity() {
    private lateinit var binding: CardActivityPayBinding
    private var agreementAccepted = false
    private var refreshJob: Job? = null
    private var expiryJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        agreementAccepted = LegalAgreementStore.isAccepted(this)
        if (!agreementAccepted) {
            packageManager.getLaunchIntentForPackage(packageName)?.let(::startActivity)
            finish()
            return
        }
        binding = CardActivityPayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets()
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.cardPayBack.setOnClickListener { finish() }
        binding.cardPayQr.setOnClickListener { refreshNow() }
        binding.cardPayRefresh.setOnClickListener { refreshNow() }
    }

    private fun applyInsets() {
        val initialLeft = binding.root.paddingLeft
        val initialTop = binding.root.paddingTop
        val initialRight = binding.root.paddingRight
        val initialBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = initialLeft + bars.left,
                top = initialTop + bars.top,
                right = initialRight + bars.right,
                bottom = initialBottom + bars.bottom,
            )
            val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = !night
                isAppearanceLightNavigationBars = !night
            }
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        if (!agreementAccepted) return
        RefreshController.enterPaymentScreen(this)
        showCached()
        startRefreshLoop()
    }

    override fun onPause() {
        if (agreementAccepted) {
            refreshJob?.cancel()
            expiryJob?.cancel()
            RefreshController.leavePaymentScreen(this)
        }
        super.onPause()
    }

    private fun showCached() {
        val account = AccountStore.get(this).current()
        if (account == null) {
            expiryJob?.cancel()
            binding.cardPayName.setText(R.string.card_no_card)
            binding.cardPayBalance.text = ""
            binding.cardPayQrContainer.visibility = View.GONE
            hideQr(loading = false)
            binding.cardPayHint.setText(R.string.card_pay_no_card)
            binding.cardPayRefresh.visibility = View.GONE
            return
        }
        binding.cardPayQrContainer.visibility = View.VISIBLE
        binding.cardPayRefresh.visibility = View.VISIBLE
        binding.cardPayName.text = account.displayName()
        binding.cardPayBalance.text = if (account.balance.isBlank()) {
            ""
        } else {
            getString(R.string.card_balance, account.balance)
        }
        if (account.hasFreshCode()) {
            showQr(account)
            binding.cardPayHint.setText(R.string.card_pay_auto_refresh)
        } else {
            expiryJob?.cancel()
            hideQr(loading = true)
            binding.cardPayHint.setText(R.string.card_pay_refreshing)
        }
    }

    private fun startRefreshLoop() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                refresh(manual = false)
                delay(PayCodePolicy.REFRESH_INTERVAL_MS)
            }
        }
    }

    private fun refreshNow() {
        lifecycleScope.launch { refresh(manual = true) }
    }

    private suspend fun refresh(manual: Boolean) {
        val account = AccountStore.get(this).current() ?: return
        if (!account.hasFreshCode()) hideQr(loading = true)
        if (manual || !account.hasFreshCode()) {
            binding.cardPayHint.setText(R.string.card_pay_refreshing)
        }
        val result = PayCodeManager.refresh(this, account)
        if (!account.sameCard(AccountStore.get(this).current())) {
            showCached()
            refresh(manual = false)
            return
        }
        when (result) {
            is PayCodeRepository.Result.Ok -> {
                showQr(account)
                binding.cardPayName.text = account.displayName()
                binding.cardPayBalance.text = if (account.balance.isBlank()) {
                    ""
                } else {
                    getString(R.string.card_balance, account.balance)
                }
                if (binding.cardPayHint.text != getString(R.string.card_pay_auto_refresh)) {
                    binding.cardPayHint.setText(R.string.card_pay_auto_refresh)
                }
            }
            PayCodeRepository.Result.Invalid -> {
                showCached()
                if (!account.hasFreshCode()) hideQr(loading = false)
                binding.cardPayHint.setText(R.string.card_pay_invalid)
            }
            is PayCodeRepository.Result.Error -> {
                showCached()
                if (!account.hasFreshCode()) hideQr(loading = false)
                binding.cardPayHint.text = getString(R.string.card_fetch_failed, result.message)
            }
        }
        PayWidgetProvider.refreshAll(this)
    }

    private fun scheduleExpiry(account: Account) {
        expiryJob?.cancel()
        val remaining = account.cachedAt + PayCodePolicy.VALIDITY_MS - System.currentTimeMillis()
        expiryJob = lifecycleScope.launch {
            delay(remaining.coerceAtLeast(0L))
            val current = AccountStore.get(this@PayActivity).current() ?: return@launch
            if (!account.sameCard(current)) return@launch
            if (current.hasFreshCode()) {
                showCached()
            } else {
                hideQr(loading = false)
                binding.cardPayHint.setText(R.string.card_expired)
            }
        }
    }

    private fun showQr(account: Account) {
        binding.cardPayQr.setImageBitmap(
            QrGenerator.encode(account.cachedCode, QrGenerator.SIZE_FULLSCREEN)
        )
        binding.cardPayQr.visibility = View.VISIBLE
        binding.cardPayQrProgress.visibility = View.GONE
        binding.cardPayQrError.visibility = View.GONE
        scheduleExpiry(account)
    }

    private fun hideQr(loading: Boolean) {
        binding.cardPayQr.setImageDrawable(null)
        binding.cardPayQr.visibility = View.GONE
        binding.cardPayQrProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.cardPayQrError.visibility = if (loading) View.GONE else View.VISIBLE
    }
}
