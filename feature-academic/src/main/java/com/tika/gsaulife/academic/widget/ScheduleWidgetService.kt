package com.tika.gsaulife.academic.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.tika.gsaulife.academic.R

const val EXTRA_MODE = "academic.widget.mode"
const val MODE_TODAY = "today"
const val MODE_TOMORROW = "tomorrow"
const val MODE_EXAMS = "exams"

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_TODAY
        return ScheduleRemoteViewsFactory(applicationContext, mode)
    }
}

private class ScheduleRemoteViewsFactory(
    private val context: Context,
    private val mode: String,
) : RemoteViewsService.RemoteViewsFactory {
    private var items: List<ScheduleWidgetItem> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        items = when (mode) {
            MODE_EXAMS -> ScheduleWidgetData.exams(context)
            MODE_TOMORROW -> ScheduleWidgetData.tomorrow(context).items
            else -> ScheduleWidgetData.today(context).items
        }
    }

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val layout = if (mode == MODE_EXAMS) {
            R.layout.academic_widget_exam_row
        } else {
            R.layout.academic_widget_row
        }
        return RemoteViews(context.packageName, layout).apply {
            setTextViewText(R.id.academic_widget_row_name, item.name)
            setTextViewText(R.id.academic_widget_row_time, item.time)
            if (item.room.isEmpty()) {
                setViewVisibility(R.id.academic_widget_row_room, View.GONE)
            } else {
                setViewVisibility(R.id.academic_widget_row_room, View.VISIBLE)
                setTextViewText(R.id.academic_widget_row_room, item.room)
            }
            setOnClickFillInIntent(R.id.academic_widget_row_root, Intent())
        }
    }

    override fun getCount(): Int = items.size

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getLoadingView(): RemoteViews? = null

    override fun onDestroy() {
        items = emptyList()
    }
}
