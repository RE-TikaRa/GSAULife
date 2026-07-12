package com.tika.gsaulife

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tika.gsaulife.academic.AcademicFeature
import com.tika.gsaulife.academic.SchoolSystem
import com.tika.gsaulife.card.CardFeature
import com.tika.gsaulife.card.RefreshMode
import com.tika.gsaulife.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var updatingControls = false

    private val academicLogin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { renderIdentity() }

    private val studentLogin = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { renderIdentity() }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.academicLogin.setOnClickListener {
            academicLogin.launch(AcademicFeature.loginIntent(requireContext(), SchoolSystem.ACADEMIC))
        }
        binding.studentLogin.setOnClickListener {
            studentLogin.launch(
                AcademicFeature.loginIntent(requireContext(), SchoolSystem.STUDENT_AFFAIRS)
            )
        }
        binding.logoutAcademic.setOnClickListener { confirmLogout() }

        binding.themeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || updatingControls) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                binding.themeLight.id -> ThemeMode.LIGHT
                binding.themeDark.id -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            if (mode != AppearanceManager.getMode(requireContext())) {
                AppearanceManager.setMode(requireContext(), mode)
            }
        }

        binding.refreshGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || updatingControls) return@addOnButtonCheckedListener
            val mode = if (checkedId == binding.refreshContinuous.id) {
                RefreshMode.CONTINUOUS
            } else {
                RefreshMode.ON_DEMAND
            }
            CardFeature.setRefreshMode(requireContext(), mode)
            if (mode == RefreshMode.CONTINUOUS) requestNotificationPermission()
            renderRefreshMode()
        }
        binding.batteryButton.setOnClickListener {
            CardFeature.requestIgnoreBatteryOptimizations(requireContext())
        }
        binding.autoStartButton.setOnClickListener {
            CardFeature.openAutoStartSettings(requireContext())
        }

        binding.versionText.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
        binding.updateButton.setOnClickListener { checkUpdate() }
        binding.emailButton.setOnClickListener { openUri("mailto:163mail@re-tikara.fun") }
        binding.githubButton.setOnClickListener { openUri("https://github.com/RE-TikaRa") }
        binding.bilibiliButton.setOnClickListener {
            openUri("https://m.bilibili.com/space/374412219")
        }
        binding.wechatButton.setOnClickListener { copyWechat() }
        binding.licenseButton.setOnClickListener {
            showText(R.string.license_title, R.string.license_content)
        }
        binding.privacyButton.setOnClickListener {
            showText(R.string.privacy_title, R.string.privacy_content)
        }
        binding.agreementButton.setOnClickListener {
            showText(R.string.agreement_title, R.string.agreement_content)
        }
    }

    override fun onResume() {
        super.onResume()
        renderIdentity()
        renderTheme()
        renderRefreshMode()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun renderIdentity() {
        if (_binding == null) return
        val academic = AcademicFeature.isLoggedIn(requireContext(), SchoolSystem.ACADEMIC)
        val student = AcademicFeature.isLoggedIn(requireContext(), SchoolSystem.STUDENT_AFFAIRS)
        binding.academicStatus.setText(
            if (academic) R.string.settings_logged_in else R.string.settings_logged_out
        )
        binding.studentStatus.setText(
            if (student) R.string.settings_logged_in else R.string.settings_logged_out
        )
        binding.academicLogin.setText(
            if (academic) R.string.settings_relogin else R.string.settings_login
        )
        binding.studentLogin.setText(
            if (student) R.string.settings_relogin else R.string.settings_login
        )
        binding.logoutAcademic.isEnabled = academic || student
    }

    private fun renderTheme() {
        updatingControls = true
        binding.themeGroup.check(
            when (AppearanceManager.getMode(requireContext())) {
                ThemeMode.SYSTEM -> binding.themeSystem.id
                ThemeMode.LIGHT -> binding.themeLight.id
                ThemeMode.DARK -> binding.themeDark.id
            }
        )
        updatingControls = false
    }

    private fun renderRefreshMode() {
        if (_binding == null) return
        val mode = CardFeature.getRefreshMode(requireContext())
        updatingControls = true
        binding.refreshGroup.check(
            if (mode == RefreshMode.CONTINUOUS) {
                binding.refreshContinuous.id
            } else {
                binding.refreshOnDemand.id
            }
        )
        updatingControls = false
        binding.continuousSettings.visibility =
            if (mode == RefreshMode.CONTINUOUS) View.VISIBLE else View.GONE
        if (mode == RefreshMode.CONTINUOUS) {
            binding.batteryStatus.setText(
                if (CardFeature.isIgnoringBatteryOptimizations(requireContext())) {
                    R.string.settings_battery_ignored
                } else {
                    R.string.settings_battery_active
                }
            )
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_logout_title)
            .setMessage(R.string.settings_logout_message)
            .setNegativeButton(R.string.settings_cancel, null)
            .setPositiveButton(R.string.settings_logout) { _, _ ->
                AcademicFeature.logoutAll(requireContext())
                renderIdentity()
            }
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkUpdate() {
        binding.updateButton.isEnabled = false
        binding.updateButton.setText(R.string.settings_checking_update)
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = UpdateChecker.check(BuildConfig.VERSION_NAME)) {
                is UpdateChecker.Result.NewVersion -> MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.settings_new_version, result.version))
                    .setMessage(R.string.settings_new_version_message)
                    .setNegativeButton(R.string.settings_cancel, null)
                    .setPositiveButton(R.string.settings_open_release) { _, _ ->
                        openUri(result.pageUrl)
                    }
                    .show()
                is UpdateChecker.Result.UpToDate -> Snackbar.make(
                    binding.root,
                    R.string.settings_up_to_date,
                    Snackbar.LENGTH_SHORT
                ).show()
                is UpdateChecker.Result.Error -> Snackbar.make(
                    binding.root,
                    getString(R.string.settings_update_failed, result.message),
                    Snackbar.LENGTH_LONG
                ).show()
            }
            binding.updateButton.isEnabled = true
            binding.updateButton.setText(R.string.settings_check_update)
        }
    }

    private fun copyWechat() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("微信公众号", "EmotionStudio"))
        Snackbar.make(binding.root, R.string.settings_wechat_copied, Snackbar.LENGTH_SHORT).show()
    }

    private fun showText(title: Int, message: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.settings_close, null)
            .show()
    }

    private fun openUri(uri: String) {
        startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
    }
}
