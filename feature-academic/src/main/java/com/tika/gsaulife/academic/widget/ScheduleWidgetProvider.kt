package com.tika.gsaulife.academic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.data.AcademicCache
import com.tika.gsaulife.academic.data.AcademicSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

abstract class ScheduleWidgetProvider : AppWidgetProvider() {
    protected abstract val titleRes: Int
    protected abstract val layoutRes: Int
    protected abstract val listIds: IntArray

    protected abstract fun bindContent(context: Context, views: RemoteViews, widgetId: Int)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_RENDER) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: manager.getAppWidgetIds(ComponentName(context, javaClass))
            ids.forEach { render(context, manager, it) }
            listIds.forEach { manager.notifyAppWidgetViewDataChanged(ids, it) }
        }
    }

    private fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, layoutRes)
        views.setTextViewText(R.id.academic_widget_title, context.getString(titleRes))
        views.setTextViewText(R.id.academic_widget_subtitle, subtitle(context))
        bindContent(context, views, widgetId)
        views.setOnClickPendingIntent(R.id.academic_widget_header, openAppIntent(context))

        runCatching { manager.updateAppWidget(widgetId, views) }
            .onFailure { Log.w(TAG, "Unable to update schedule widget", it) }
        listIds.forEach { manager.notifyAppWidgetViewDataChanged(widgetId, it) }
    }

    protected fun bindList(
        context: Context,
        views: RemoteViews,
        widgetId: Int,
        listId: Int,
        emptyId: Int,
        mode: String,
    ) {
        val serviceIntent = Intent(context, ScheduleWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(EXTRA_MODE, mode)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(listId, serviceIntent)
        views.setEmptyView(listId, emptyId)
        views.setPendingIntentTemplate(listId, openAppIntent(context))
    }

    private fun subtitle(context: Context): String {
        val cache = AcademicCache.get(context).loadSchedule()
        val date = DATE_FORMAT.format(Calendar.getInstance().time)
        if (cache == null) return date
        val week = AcademicSettings.get(context).currentWeek(cache.data.term)
            ?: return date
        return context.getString(R.string.academic_widget_subtitle, date, week)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            javaClass.name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    companion object {
        private const val TAG = "ScheduleWidget"
        const val ACTION_RENDER = "com.tika.gsaulife.academic.WIDGET_RENDER"
        private val DATE_FORMAT = SimpleDateFormat("M月d日 EEEE", Locale.CHINA)

        internal fun refresh(context: Context, provider: Class<out ScheduleWidgetProvider>) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, provider))
            if (ids.isEmpty()) return
            val intent = Intent(context, provider).apply {
                action = ACTION_RENDER
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

class TodayScheduleWidgetProvider : ScheduleWidgetProvider() {
    override val titleRes = R.string.academic_widget_today_title
    override val layoutRes = R.layout.academic_widget_schedule
    override val listIds = intArrayOf(R.id.academic_widget_list)

    override fun bindContent(context: Context, views: RemoteViews, widgetId: Int) {
        bindList(
            context, views, widgetId,
            R.id.academic_widget_list, R.id.academic_widget_empty, MODE_TODAY,
        )
    }

    companion object {
        fun refreshAll(context: Context) = refresh(context, TodayScheduleWidgetProvider::class.java)
    }
}

class UpcomingScheduleWidgetProvider : ScheduleWidgetProvider() {
    override val titleRes = R.string.academic_widget_upcoming_title
    override val layoutRes = R.layout.academic_widget_upcoming
    override val listIds = intArrayOf(
        R.id.academic_widget_today_list,
        R.id.academic_widget_tomorrow_list,
    )

    override fun bindContent(context: Context, views: RemoteViews, widgetId: Int) {
        bindList(
            context, views, widgetId,
            R.id.academic_widget_today_list, R.id.academic_widget_today_empty, MODE_TODAY,
        )
        bindList(
            context, views, widgetId,
            R.id.academic_widget_tomorrow_list, R.id.academic_widget_tomorrow_empty, MODE_TOMORROW,
        )
    }

    companion object {
        fun refreshAll(context: Context) =
            refresh(context, UpcomingScheduleWidgetProvider::class.java)
    }
}
