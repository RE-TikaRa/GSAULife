package com.tika.gsaulife.academic.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeTest {
    @Test
    fun `数字成绩按六十分判断`() {
        assertFalse(grade("59").passed)
        assertTrue(grade("60").passed)
    }

    @Test
    fun `文字成绩识别未通过状态`() {
        assertFalse(grade("不及格").passed)
        assertFalse(grade("不合格").passed)
        assertTrue(grade("合格").passed)
    }

    private fun grade(score: String) = Grade(
        term = "",
        courseName = "",
        courseId = "",
        credit = 0.0,
        score = score,
        gradePoint = null,
        courseType = "",
        examType = "",
        classId = "",
        studentId = "",
    )
}
