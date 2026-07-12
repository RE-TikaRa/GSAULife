package com.tika.gsaulife.academic.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.TimeZone

internal class AcademicSettings private constructor(context: Context) {
    private val preferences =
        context.getSharedPreferences("academic_settings", Context.MODE_PRIVATE)

    fun calibrateWeek(term: String, date: Long, week: Int) {
        val value = JSONObject().apply {
            put("date", atStartOfDay(date))
            put("week", week)
        }
        preferences.edit { putString(termKey(term), value.toString()) }
    }

    fun setTermStart(term: String, date: Long) {
        calibrateWeek(term, date, 1)
    }

    fun termStart(term: String): Long? {
        val raw = preferences.getString(termKey(term), null) ?: return null
        val value = JSONObject(raw)
        return termStartFromCalibration(value.getLong("date"), value.getInt("week"))
    }

    fun currentWeek(term: String, now: Long = System.currentTimeMillis()): Int? {
        val start = termStart(term) ?: return null
        return weekOf(start, now)
    }

    fun clear() {
        preferences.edit { clear() }
    }

    private fun termKey(term: String): String = "week:$term"

    companion object {
        fun atStartOfDay(millis: Long): Long = Calendar.getInstance().run {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }

        fun weekOf(start: Long, day: Long): Int? {
            val elapsedDays = localEpochDay(day) - localEpochDay(start)
            if (elapsedDays < 0) return null
            return (elapsedDays / 7 + 1).toInt()
        }

        fun termStartFromCalibration(date: Long, week: Int): Long = Calendar.getInstance().run {
            timeInMillis = atStartOfDay(date)
            add(Calendar.DATE, -(week - 1) * 7)
            timeInMillis
        }

        private fun localEpochDay(millis: Long): Long {
            val local = Calendar.getInstance().apply { timeInMillis = millis }
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    local.get(Calendar.YEAR),
                    local.get(Calendar.MONTH),
                    local.get(Calendar.DAY_OF_MONTH),
                )
            }
            return TimeUnit.MILLISECONDS.toDays(utc.timeInMillis)
        }

        @Volatile
        private var instance: AcademicSettings? = null

        fun get(context: Context): AcademicSettings =
            instance ?: synchronized(this) {
                instance ?: AcademicSettings(context.applicationContext).also { instance = it }
            }
    }
}
