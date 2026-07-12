package com.tika.gsaulife.academic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

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
}
