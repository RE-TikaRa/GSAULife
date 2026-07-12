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
            put("day", localEpochDay(date))
            put("week", week)
        }
        preferences.edit { putString(termKey(term), value.toString()) }
    }

    fun setTermStart(term: String, date: Long) {
        calibrateWeek(term, date, 1)
    }

    fun termStart(term: String): Long? {
        val day = termStartDay(term) ?: return null
        return dateFromEpochDay(day)
    }

    fun currentWeek(term: String, now: Long = System.currentTimeMillis()): Int? {
        val start = termStartDay(term) ?: return null
        return weekOfDays(start, localEpochDay(now))
    }

    fun clear() {
        preferences.edit { clear() }
    }

    private fun termKey(term: String): String = "week-day:$term"

    private fun termStartDay(term: String): Long? {
        val raw = preferences.getString(termKey(term), null) ?: return null
        val value = JSONObject(raw)
        return value.getLong("day") - (value.getInt("week") - 1) * 7L
    }

    companion object {
        fun weekOf(start: Long, day: Long): Int? {
            return weekOfDays(localEpochDay(start), localEpochDay(day))
        }

        private fun weekOfDays(start: Long, day: Long): Int? {
            val elapsedDays = day - start
            if (elapsedDays < 0) return null
            return (elapsedDays / 7 + 1).toInt()
        }

        fun termStartFromCalibration(date: Long, week: Int): Long =
            dateFromEpochDay(localEpochDay(date) - (week - 1) * 7L)

        internal fun localEpochDay(millis: Long): Long {
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

        internal fun dateFromEpochDay(day: Long): Long {
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = TimeUnit.DAYS.toMillis(day)
            }
            return Calendar.getInstance().run {
                clear()
                set(
                    utc.get(Calendar.YEAR),
                    utc.get(Calendar.MONTH),
                    utc.get(Calendar.DAY_OF_MONTH),
                )
                timeInMillis
            }
        }

        @Volatile
        private var instance: AcademicSettings? = null

        fun get(context: Context): AcademicSettings =
            instance ?: synchronized(this) {
                instance ?: AcademicSettings(context.applicationContext).also { instance = it }
            }
    }
}
