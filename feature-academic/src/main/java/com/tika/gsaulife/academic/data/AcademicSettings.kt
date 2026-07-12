package com.tika.gsaulife.academic.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

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

    fun currentWeek(term: String, now: Long = System.currentTimeMillis()): Int? {
        val raw = preferences.getString(termKey(term), null) ?: return null
        val value = JSONObject(raw)
        return weekFromCalibration(value.getLong("date"), value.getInt("week"), now)
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
            val elapsedDays = TimeUnit.MILLISECONDS.toDays(atStartOfDay(day) - atStartOfDay(start))
            if (elapsedDays < 0) return null
            return (elapsedDays / 7 + 1).toInt()
        }

        fun weekFromCalibration(date: Long, week: Int, day: Long): Int {
            val elapsedDays = TimeUnit.MILLISECONDS.toDays(atStartOfDay(day) - atStartOfDay(date))
            return week + Math.floorDiv(elapsedDays, 7).toInt()
        }

        @Volatile
        private var instance: AcademicSettings? = null

        fun get(context: Context): AcademicSettings =
            instance ?: synchronized(this) {
                instance ?: AcademicSettings(context.applicationContext).also { instance = it }
            }
    }
}
