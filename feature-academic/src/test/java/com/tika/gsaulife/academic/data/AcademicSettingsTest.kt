package com.tika.gsaulife.academic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AcademicSettingsTest {
    private fun day(year: Int, month: Int, date: Int): Long = Calendar.getInstance().run {
        clear()
        set(year, month - 1, date)
        timeInMillis
    }

    @Test
    fun `校准日保持选择的教学周`() {
        val start = AcademicSettings.termStartFromCalibration(day(2025, 9, 1), 6)
        assertEquals(6, AcademicSettings.weekOf(start, day(2025, 9, 1)))
        assertEquals(6, AcademicSettings.weekOf(start, day(2025, 9, 7)))
    }

    @Test
    fun `校准后满七天进入下一教学周`() {
        val start = AcademicSettings.termStartFromCalibration(day(2025, 9, 1), 6)
        assertEquals(7, AcademicSettings.weekOf(start, day(2025, 9, 8)))
        assertEquals(8, AcademicSettings.weekOf(start, day(2025, 9, 15)))
    }

    @Test
    fun `校准日之前没有教学周`() {
        val start = day(2025, 9, 1)
        assertNull(AcademicSettings.weekOf(start, day(2025, 8, 31)))
    }

    @Test
    fun `三月二日开学时七月十三日是第二十周`() {
        assertEquals(20, AcademicSettings.weekOf(day(2026, 3, 2), day(2026, 7, 13)))
    }

    @Test
    fun `校准第二十周可换算为开学日期`() {
        assertEquals(
            day(2026, 3, 2),
            AcademicSettings.termStartFromCalibration(day(2026, 7, 13), 20),
        )
    }

    @Test
    fun `夏令时变化不影响教学周`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(20, AcademicSettings.weekOf(day(2026, 3, 2), day(2026, 7, 13)))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `切换时区后开学日期保持年月日`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
            val stored = AcademicSettings.localEpochDay(day(2026, 3, 2))

            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(day(2026, 3, 2), AcademicSettings.dateFromEpochDay(stored))
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
