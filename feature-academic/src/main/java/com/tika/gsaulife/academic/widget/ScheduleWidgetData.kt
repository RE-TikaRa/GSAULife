package com.tika.gsaulife.academic.widget

import android.content.Context
import com.tika.gsaulife.academic.data.AcademicCache
import com.tika.gsaulife.academic.data.AcademicSettings
import com.tika.gsaulife.academic.model.Course
import com.tika.gsaulife.academic.model.SectionTime
import java.util.Calendar

internal data class ScheduleWidgetItem(
    val name: String,
    val time: String,
    val room: String,
)

internal data class ScheduleWidgetDay(
    val weekday: Int,
    val items: List<ScheduleWidgetItem>,
)

internal object ScheduleWidgetData {
    fun today(context: Context, now: Long = System.currentTimeMillis()): ScheduleWidgetDay {
        val weekday = weekdayOf(now)
        return ScheduleWidgetDay(weekday, dayItems(context, weekday, now, hidePassed = true))
    }

    fun tomorrow(context: Context, now: Long = System.currentTimeMillis()): ScheduleWidgetDay {
        val today = weekdayOf(now)
        val tomorrow = if (today == 7) 1 else today + 1
        return ScheduleWidgetDay(tomorrow, dayItems(context, tomorrow, now + DAY_MS, hidePassed = false))
    }

    private fun dayItems(
        context: Context,
        weekday: Int,
        millis: Long,
        hidePassed: Boolean,
    ): List<ScheduleWidgetItem> {
        val cache = AcademicCache.get(context).loadSchedule() ?: return emptyList()
        val settings = AcademicSettings.get(context)
        val week = settings.currentWeek(cache.data.term, millis) ?: return emptyList()
        val sections = settings.sectionTimes()
        val nowMinute = if (hidePassed) minuteOfDay(millis) else null
        return cache.data.courses
            .asSequence()
            .filter { it.weekday == weekday && it.inWeek(week) }
            .filter { course -> nowMinute == null || !hasEnded(course, sections, nowMinute) }
            .sortedBy { it.startSection }
            .map { ScheduleWidgetItem(it.name, timeLabel(it, sections), it.room) }
            .toList()
    }

    private fun hasEnded(course: Course, sections: List<SectionTime>, nowMinute: Int): Boolean {
        val end = sections.getOrNull(course.endSection - 1) ?: return false
        return nowMinute >= end.endMinute
    }

    private fun timeLabel(course: Course, sections: List<SectionTime>): String {
        val start = sections.getOrNull(course.startSection - 1)
        val end = sections.getOrNull(course.endSection - 1)
        return if (start != null && end != null) {
            "${start.formatStart()}-${end.formatEnd()}"
        } else {
            "第${course.startSection}-${course.endSection}节"
        }
    }

    private fun weekdayOf(millis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }
    }

    private fun minuteOfDay(millis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private const val DAY_MS = 24L * 60 * 60 * 1000
}
