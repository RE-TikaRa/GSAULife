package com.tika.gsaulife.academic.ui

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
