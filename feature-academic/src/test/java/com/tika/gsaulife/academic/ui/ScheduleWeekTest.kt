package com.tika.gsaulife.academic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ScheduleWeekTest {
    @Test
    fun `课程只到十八周时仍可选择第二十周`() {
        assertEquals(20, scheduleMaxWeek(1..18, null))
    }

    @Test
    fun `当前周或课程周次超过二十时扩展范围`() {
        assertEquals(21, scheduleMaxWeek(1..18, 21))
        assertEquals(22, scheduleMaxWeek(listOf(1, 22), 20))
    }

    @Test
    fun `校准教学周允许一到九十九周`() {
        assertFalse(isValidCalibrationWeek(null))
        assertFalse(isValidCalibrationWeek(0))
        assertTrue(isValidCalibrationWeek(20))
        assertTrue(isValidCalibrationWeek(99))
        assertFalse(isValidCalibrationWeek(100))
    }

    @Test
    fun `负时区选择开学日期时日期不变`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
                clear()
                set(2026, Calendar.MARCH, 2)
                timeInMillis
            }
            val local = Calendar.getInstance().apply { timeInMillis = fromUtcDate(utc) }
            assertEquals(2026, local.get(Calendar.YEAR))
            assertEquals(Calendar.MARCH, local.get(Calendar.MONTH))
            assertEquals(2, local.get(Calendar.DAY_OF_MONTH))
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
