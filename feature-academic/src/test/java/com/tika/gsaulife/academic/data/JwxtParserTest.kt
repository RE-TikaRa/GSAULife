package com.tika.gsaulife.academic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JwxtParserTest {
    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResource("/fixtures/$name")).readText()

    @Test
    fun `成绩列表字段对齐`() {
        val grades = JwxtParser.grades(fixture("academic_grades.html"))
        assertEquals(2, grades.size)
        assertEquals("大学英语IV", grades.first().courseName)
        assertEquals("COURSE-001", grades.first().courseId)
        assertEquals("60", grades.first().score)
        assertEquals(3.0, grades.first().credit, 0.001)
        assertEquals(1.5, grades.first().gradePoint!!, 0.001)
        assertEquals("公共必修课", grades.first().courseType)
        assertEquals(86.0, grades[1].numericScore!!, 0.001)
        assertEquals(4.1, grades[1].gradePoint!!, 0.001)
    }

    @Test
    fun `成绩列表提取明细参数`() {
        val grade = JwxtParser.grades(fixture("academic_grades.html")).first()
        assertEquals("STUDENT-001", grade.studentId)
        assertEquals("CLASS-001", grade.classId)
        assertTrue(grade.hasDetail)
    }

    @Test
    fun `成绩明细忽略零占比分项`() {
        val detail = JwxtParser.scoreDetail(fixture("academic_score_detail.html"))
        assertEquals("60", detail.total)
        assertEquals(2, detail.components.size)
        assertEquals("期末", detail.components[0].label)
        assertEquals("64", detail.components[0].score)
        assertEquals("40%", detail.components[0].ratio)
        assertEquals("平时", detail.components[1].label)
        assertEquals("57", detail.components[1].score)
        assertEquals("60%", detail.components[1].ratio)
    }

    @Test
    fun `成绩明细适配四组结构`() {
        val detail = JwxtParser.scoreDetail(fixture("academic_score_detail_short.html"))
        assertEquals("86", detail.total)
        assertEquals(2, detail.components.size)
        assertEquals("期末", detail.components[0].label)
        assertEquals("82", detail.components[0].score)
        assertEquals("平时", detail.components[1].label)
        assertEquals("88", detail.components[1].score)
    }

    @Test
    fun `考试列表字段对齐`() {
        val exams = JwxtParser.exams(fixture("academic_exams.html"))
        assertEquals(8, exams.size)
        assertEquals("课程一", exams.first().courseName)
        assertEquals("2026-07-01 09:00~11:00", exams.first().time)
        assertEquals("教学楼A101", exams.first().location)
        assertEquals("课程八", exams.last().courseName)
    }

    @Test
    fun `课表解析课程与当前学期`() {
        val page = JwxtParser.schedulePage(fixture("academic_schedule.html"))
        assertEquals("2025-2026-2", page.term)
        val course = page.courses.first { it.name == "大学英语IV" }
        assertEquals("教师甲", course.teacher)
        assertEquals("教学楼A101", course.room)
        assertTrue(course.weekday in 1..7)
        assertTrue(1 in course.weeks)
        assertTrue(16 in course.weeks)
    }

    @Test
    fun `课表解析连续周次`() {
        val courses = JwxtParser.schedule(fixture("academic_schedule.html"))
        val course = courses.first { it.name == "工程材料" }
        assertTrue(course.weeks.containsAll((1..8).toList()))
    }
}
