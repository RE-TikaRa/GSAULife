package com.tika.gsaulife.academic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GradeStatsTest {
    private fun grade(
        term: String,
        name: String,
        credit: Double,
        score: String,
        gradePoint: Double?,
    ) = Grade(
        term = term,
        courseName = name,
        courseId = "",
        credit = credit,
        score = score,
        gradePoint = gradePoint,
        courseType = "必修",
        examType = "考试",
        classId = "",
        studentId = "",
    )

    @Test
    fun `加权平均分只统计数字成绩`() {
        val stats = GradeStats.of(
            listOf(
                grade("2025-2026-2", "课程一", 3.0, "90", 4.0),
                grade("2025-2026-2", "课程二", 2.0, "优", 5.0),
            )
        )
        assertEquals(90.0, stats.avgScore, 0.001)
        assertEquals(3.0, stats.scoredCredit, 0.001)
        assertEquals(5.0, stats.totalCredit, 0.001)
        assertEquals(2, stats.courseCount)
    }

    @Test
    fun `加权平均绩点包含等级成绩课程`() {
        val stats = GradeStats.of(
            listOf(
                grade("2025-2026-2", "课程一", 3.0, "90", 4.0),
                grade("2025-2026-2", "课程二", 2.0, "优", 5.0),
            )
        )
        assertEquals(4.4, stats.avgGradePoint, 0.001)
    }

    @Test
    fun `无绩点课程不参与绩点加权`() {
        val stats = GradeStats.of(
            listOf(
                grade("2025-2026-2", "课程一", 3.0, "90", 4.0),
                grade("2025-2026-2", "课程二", 1.0, "85", null),
            )
        )
        assertEquals(4.0, stats.avgGradePoint, 0.001)
        assertEquals(88.75, stats.avgScore, 0.001)
    }

    @Test
    fun `成绩按学期倒序分组`() {
        val terms = groupByTerm(
            listOf(
                grade("2024-2025-1", "A", 2.0, "80", 3.0),
                grade("2025-2026-2", "B", 3.0, "90", 4.0),
                grade("2024-2025-1", "C", 1.0, "70", 2.0),
            )
        )
        assertEquals(2, terms.size)
        assertEquals("2025-2026-2", terms.first().term)
        assertEquals("2024-2025-1", terms.last().term)
        assertEquals(2, terms.last().grades.size)
    }
}
