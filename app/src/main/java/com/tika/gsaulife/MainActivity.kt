package com.tika.gsaulife

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tika.gsaulife.academic.AcademicFeature
import com.tika.gsaulife.card.CardFeature
import com.tika.gsaulife.card.LegalAgreementStore
import com.tika.gsaulife.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentTab = Tab.CARD

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyInsets()
        if (LegalAgreementStore.isAccepted(this)) {
            initialize(savedInstanceState)
        } else {
            showLegalAgreement(savedInstanceState)
        }
    }

    private fun initialize(savedInstanceState: Bundle?) {
        currentTab = savedInstanceState?.getString(STATE_TAB)?.let(Tab::valueOf) ?: Tab.CARD
        binding.navAcademic.setOnClickListener { select(Tab.ACADEMIC) }
        binding.navCard.setOnClickListener { select(Tab.CARD) }
        binding.navSettings.setOnClickListener { select(Tab.SETTINGS) }

        show(currentTab)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentTab == Tab.CARD) finish() else select(Tab.CARD)
            }
        })
    }

    private fun showLegalAgreement(savedInstanceState: Bundle?) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.first_launch_agreement_title)
            .setMessage(agreementMessage())
            .setCancelable(false)
            .setNegativeButton(R.string.first_launch_agreement_reject) { _, _ ->
                finishAffinity()
            }
            .setPositiveButton(R.string.first_launch_agreement_accept) { _, _ ->
                LegalAgreementStore.accept(this)
                CardFeature.refreshWidgets(this)
                initialize(savedInstanceState)
            }
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener {
            dialog.findViewById<TextView>(android.R.id.message)?.movementMethod =
                LinkMovementMethod.getInstance()
        }
        dialog.show()
    }

    private fun agreementMessage(): CharSequence = SpannableStringBuilder()
        .append(getString(R.string.first_launch_agreement_message))
        .append("\n\n")
        .appendLink(getString(R.string.privacy_title), getString(R.string.legal_privacy_url))
        .append("　")
        .appendLink(getString(R.string.agreement_title), getString(R.string.legal_terms_url))

    private fun SpannableStringBuilder.appendLink(label: String, uri: String) = apply {
        val start = length
        append(label)
        setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
                }
            },
            start,
            length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_TAB, currentTab.name)
        super.onSaveInstanceState(outState)
    }

    private fun select(tab: Tab) {
        val academic = supportFragmentManager.findFragmentByTag(Tab.ACADEMIC.tag)
        if ((currentTab == Tab.ACADEMIC || tab == Tab.ACADEMIC) && academic != null) {
            AcademicFeature.resetToMenu(academic)
        }
        currentTab = tab
        show(tab)
    }

    private fun show(tab: Tab) {
        val target = supportFragmentManager.findFragmentByTag(tab.tag) ?: create(tab)
        supportFragmentManager.beginTransaction().apply {
            setReorderingAllowed(true)
            Tab.entries.forEach { candidate ->
                supportFragmentManager.findFragmentByTag(candidate.tag)?.let {
                    hide(it)
                    setMaxLifecycle(it, Lifecycle.State.CREATED)
                }
            }
            if (target.isAdded) show(target) else add(R.id.content, target, tab.tag)
            setMaxLifecycle(target, Lifecycle.State.RESUMED)
            setPrimaryNavigationFragment(target)
        }.commit()
        renderNavigation(tab)
    }

    private fun create(tab: Tab): Fragment = when (tab) {
        Tab.ACADEMIC -> AcademicFeature.createFragment()
        Tab.CARD -> CardFeature.createFragment()
        Tab.SETTINGS -> SettingsFragment()
    }

    private fun renderNavigation(tab: Tab) {
        binding.navAcademic.isSelected = tab == Tab.ACADEMIC
        binding.navCard.isSelected = tab == Tab.CARD
        binding.navSettings.isSelected = tab == Tab.SETTINGS
        binding.navCard.alpha = if (tab == Tab.CARD) 1f else 0.78f
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.content.updatePadding(top = bars.top)
            binding.bottomNavigation.updatePadding(bottom = bars.bottom)
            val params = binding.navCard.layoutParams as ConstraintLayout.LayoutParams
            params.bottomMargin = bars.bottom + resources.getDimensionPixelSize(R.dimen.gsau_nav_card_margin)
            binding.navCard.layoutParams = params

            val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
            WindowInsetsControllerCompat(window, binding.root).apply {
                isAppearanceLightStatusBars = !night
                isAppearanceLightNavigationBars = !night
            }
            insets
        }
    }

    private enum class Tab(val tag: String) {
        ACADEMIC("academic"),
        CARD("card"),
        SETTINGS("settings")
    }

    companion object {
        private const val STATE_TAB = "selected_tab"
    }
}
