package com.tika.gsaulife.card.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tika.gsaulife.card.LegalAgreementStore
import com.tika.gsaulife.card.data.AccountStore
import com.tika.gsaulife.card.widget.PayWidgetProvider

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!LegalAgreementStore.isAccepted(context)) {
            RefreshController.stop(context)
            PayWidgetProvider.refreshAll(context)
            return
        }
        AccountStore.get(context).clearCachedCodes()
        PayWidgetProvider.refreshAll(context)
    }
}
