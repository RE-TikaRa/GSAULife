package com.tika.gsaulife.card

import android.content.Context
import androidx.fragment.app.Fragment
import com.tika.gsaulife.card.ui.CardFragment
import com.tika.gsaulife.card.widget.PayWidgetProvider
import com.tika.gsaulife.card.work.RefreshController

object CardFeature {
    fun createFragment(): Fragment = CardFragment()

    fun getRefreshMode(context: Context): RefreshMode =
        RefreshController.getMode(context)

    fun setRefreshMode(context: Context, mode: RefreshMode) {
        RefreshController.setMode(context, mode)
    }

    fun restoreContinuousRefresh(context: Context) {
        RefreshController.restore(context)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        RefreshController.isIgnoringBatteryOptimizations(context)

    fun requestIgnoreBatteryOptimizations(context: Context) {
        RefreshController.requestIgnoreBatteryOptimizations(context)
    }

    fun openAutoStartSettings(context: Context) {
        RefreshController.openAutoStartSettings(context)
    }

    fun refreshWidgets(context: Context) {
        PayWidgetProvider.refreshAll(context)
    }
}
