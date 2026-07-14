package com.tika.gsaulife.card.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.tika.gsaulife.card.R
import com.tika.gsaulife.card.LegalAgreementStore
import com.tika.gsaulife.card.RefreshMode
import com.tika.gsaulife.card.data.AccountStore
import com.tika.gsaulife.card.data.PayCodePolicy
import com.tika.gsaulife.card.qr.QrGenerator
import com.tika.gsaulife.card.ui.PayActivity
import com.tika.gsaulife.card.work.RefreshController
import com.tika.gsaulife.card.work.WidgetRefreshWorker
import com.tika.gsaulife.card.work.WidgetExpiry

class PayWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        if (!LegalAgreementStore.isAccepted(context)) {
            renderAll(context)
            return
        }
        RefreshController.setMode(context, RefreshMode.CONTINUOUS)
        renderAll(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { renderWidget(context, appWidgetManager, it) }
    }

    override fun onDisabled(context: Context) {
        WidgetExpiry.cancel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (!LegalAgreementStore.isAccepted(context)) {
            RefreshController.stop(context)
            renderAll(context)
            return
        }
        when (intent.action) {
            WidgetExpiry.ACTION -> renderAll(context)
            ACTION_RENDER -> renderAll(
                context,
                intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            )
            ACTION_SWITCH -> {
                AccountStore.get(context).switchToNext()
                refreshAll(context)
                if (RefreshController.getMode(context) == RefreshMode.CONTINUOUS) {
                    RefreshController.restore(context)
                }
                WidgetRefreshWorker.enqueue(context)
            }
            ACTION_REFRESH -> {
                if (RefreshController.getMode(context) == RefreshMode.CONTINUOUS) {
                    RefreshController.restore(context)
                }
                WidgetRefreshWorker.enqueue(context)
            }
        }
    }

    private fun renderAll(context: Context, ids: IntArray? = null) {
        val manager = AppWidgetManager.getInstance(context)
        (ids ?: widgetIds(context, manager)).forEach {
            renderWidget(context, manager, it)
        }
    }

    private fun renderWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.card_widget)
        if (!LegalAgreementStore.isAccepted(context)) {
            views.setTextViewText(R.id.card_widget_name, context.getString(R.string.card_widget_add))
            views.setContentDescription(
                R.id.card_widget_name,
                context.getString(R.string.card_widget_add)
            )
            views.setViewVisibility(R.id.card_widget_name, View.VISIBLE)
            views.setViewVisibility(R.id.card_widget_switch, View.GONE)
            views.setViewVisibility(R.id.card_widget_qr, View.GONE)
            views.setViewVisibility(R.id.card_widget_refresh, View.INVISIBLE)
            views.setViewVisibility(R.id.card_widget_hint, View.VISIBLE)
            views.setTextViewText(
                R.id.card_widget_hint,
                context.getString(R.string.card_widget_agreement)
            )
            views.setContentDescription(
                R.id.card_widget_body,
                context.getString(R.string.card_widget_agreement)
            )
            openAppIntent(context)?.let {
                views.setOnClickPendingIntent(R.id.card_widget_body, it)
            }
            runCatching { manager.updateAppWidget(widgetId, views) }
                .onFailure { Log.w(TAG, "Unable to update card widget", it) }
            return
        }
        val account = AccountStore.get(context).current()
        if (account == null) {
            views.setTextViewText(
                R.id.card_widget_name,
                context.getString(R.string.card_widget_add)
            )
            views.setContentDescription(
                R.id.card_widget_name,
                context.getString(R.string.card_widget_add)
            )
            views.setViewVisibility(R.id.card_widget_name, View.VISIBLE)
            views.setViewVisibility(R.id.card_widget_switch, View.GONE)
            views.setViewVisibility(R.id.card_widget_qr, View.GONE)
            views.setViewVisibility(R.id.card_widget_refresh, View.INVISIBLE)
            views.setViewVisibility(R.id.card_widget_hint, View.VISIBLE)
            views.setTextViewText(
                R.id.card_widget_hint,
                context.getString(R.string.card_widget_empty)
            )
            views.setContentDescription(
                R.id.card_widget_body,
                context.getString(R.string.card_widget_open_add)
            )
            openAppIntent(context)?.let {
                views.setOnClickPendingIntent(R.id.card_widget_body, it)
            }
        } else {
            val name = account.displayName()
            views.setTextViewText(R.id.card_widget_name, name)
            views.setTextViewText(R.id.card_widget_switch, name)
            views.setViewVisibility(R.id.card_widget_refresh, View.VISIBLE)
            if (AccountStore.get(context).list().size > 1) {
                views.setViewVisibility(R.id.card_widget_name, View.GONE)
                views.setViewVisibility(R.id.card_widget_switch, View.VISIBLE)
                views.setContentDescription(
                    R.id.card_widget_switch,
                    context.getString(R.string.card_widget_switch_card, name)
                )
                views.setOnClickPendingIntent(R.id.card_widget_switch, switchIntent(context))
            } else {
                views.setViewVisibility(R.id.card_widget_name, View.VISIBLE)
                views.setViewVisibility(R.id.card_widget_switch, View.GONE)
                views.setContentDescription(
                    R.id.card_widget_name,
                    context.getString(R.string.card_widget_current_card, name)
                )
            }
            views.setContentDescription(
                R.id.card_widget_body,
                context.getString(R.string.card_widget_open_pay, name)
            )
            if (account.hasFreshCode()) {
                views.setImageViewBitmap(
                    R.id.card_widget_qr,
                    QrGenerator.encode(account.cachedCode, QrGenerator.SIZE_WIDGET)
                )
                views.setViewVisibility(R.id.card_widget_qr, View.VISIBLE)
                views.setViewVisibility(R.id.card_widget_hint, View.GONE)
                WidgetExpiry.schedule(
                    context,
                    account.cachedAt + PayCodePolicy.VALIDITY_MS
                )
            } else {
                views.setViewVisibility(R.id.card_widget_qr, View.GONE)
                views.setViewVisibility(R.id.card_widget_hint, View.VISIBLE)
                views.setTextViewText(
                    R.id.card_widget_hint,
                    context.getString(R.string.card_widget_open_pay_hint)
                )
            }
            views.setOnClickPendingIntent(R.id.card_widget_body, openPayIntent(context))
            views.setOnClickPendingIntent(R.id.card_widget_refresh, refreshIntent(context))
        }
        runCatching { manager.updateAppWidget(widgetId, views) }
            .onFailure { Log.w(TAG, "Unable to update card widget", it) }
    }

    private fun openAppIntent(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return null
        return PendingIntent.getActivity(context, 10, intent, pendingIntentFlags())
    }

    private fun openPayIntent(context: Context): PendingIntent {
        val intent = Intent(context, PayActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 11, intent, pendingIntentFlags())
    }

    private fun switchIntent(context: Context): PendingIntent {
        val intent = Intent(context, PayWidgetProvider::class.java).setAction(ACTION_SWITCH)
        return PendingIntent.getBroadcast(context, 12, intent, pendingIntentFlags())
    }

    private fun refreshIntent(context: Context): PendingIntent {
        val intent = Intent(context, PayWidgetProvider::class.java).setAction(ACTION_REFRESH)
        return PendingIntent.getBroadcast(context, 13, intent, pendingIntentFlags())
    }

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    companion object {
        private const val TAG = "CardWidget"
        private const val ACTION_RENDER = "com.tika.gsaulife.card.WIDGET_RENDER"
        private const val ACTION_SWITCH = "com.tika.gsaulife.card.WIDGET_SWITCH"
        private const val ACTION_REFRESH = "com.tika.gsaulife.card.WIDGET_REFRESH"

        private fun widgetIds(context: Context, manager: AppWidgetManager): IntArray {
            val component = ComponentName(context, PayWidgetProvider::class.java)
            return manager.getAppWidgetIds(component)
        }

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = widgetIds(context, manager)
            if (ids.isEmpty()) return
            val intent = Intent(context, PayWidgetProvider::class.java).apply {
                action = ACTION_RENDER
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
