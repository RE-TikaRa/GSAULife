package com.tika.gsaulife.card.work

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.edit
import androidx.core.net.toUri
import com.tika.gsaulife.card.RefreshMode
import com.tika.gsaulife.card.data.AccountStore
import com.tika.gsaulife.card.widget.PayWidgetProvider

internal object RefreshController {
    private const val PREFS = "gsaulife_card_refresh"
    private const val KEY_MODE = "mode"

    fun getMode(context: Context): RefreshMode {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, RefreshMode.ON_DEMAND.name)
        return runCatching { RefreshMode.valueOf(stored!!) }
            .getOrDefault(RefreshMode.ON_DEMAND)
    }

    fun setMode(context: Context, mode: RefreshMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_MODE, mode.name) }
        if (mode == RefreshMode.CONTINUOUS) {
            restore(context)
        } else {
            RefreshService.stop(context)
            PayWidgetProvider.refreshAll(context)
        }
    }

    fun restore(context: Context) {
        PayWidgetProvider.refreshAll(context)
        if (
            getMode(context) == RefreshMode.CONTINUOUS &&
            AccountStore.get(context).current() != null
        ) {
            RefreshService.start(context)
        } else {
            RefreshService.stop(context)
        }
    }

    fun enterPaymentScreen(context: Context) {
        RefreshService.stop(context)
    }

    fun leavePaymentScreen(context: Context) {
        restore(context)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(request) }
            .onFailure {
                val settings = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(settings) }
            }
    }

    fun openAutoStartSettings(context: Context) {
        val candidates = listOf(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ),
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            ),
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.startupapp.StartupAppListActivity"
            ),
            ComponentName(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartupAppListActivity"
            ),
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            ),
            ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )
        )
        candidates.forEach { component ->
            val intent = Intent().apply {
                this.component = component
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                runCatching { context.startActivity(intent) }
                return
            }
        }
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(details) }
    }
}
