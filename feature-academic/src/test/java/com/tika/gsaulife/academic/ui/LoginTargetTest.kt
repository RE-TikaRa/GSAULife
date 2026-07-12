package com.tika.gsaulife.academic.ui

import com.tika.gsaulife.academic.SchoolSystem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginTargetTest {
    @Test
    fun `只接受教务系统目标地址`() {
        assertTrue(
            reachedSchoolTarget(
                SchoolSystem.ACADEMIC,
                "https://jwgl.gsau.edu.cn/jsxsd/framework/xsMain.jsp",
            )
        )
        assertFalse(
            reachedSchoolTarget(
                SchoolSystem.ACADEMIC,
                "https://jwgl.gsau.edu.cn.evil.example/jsxsd/framework/xsMain.jsp",
            )
        )
        assertFalse(
            reachedSchoolTarget(
                SchoolSystem.ACADEMIC,
                "https://evil.example/?next=https://jwgl.gsau.edu.cn/jsxsd/framework/",
            )
        )
        assertFalse(
            reachedSchoolTarget(
                SchoolSystem.ACADEMIC,
                "http://jwgl.gsau.edu.cn/jsxsd/framework/xsMain.jsp",
            )
        )
    }

    @Test
    fun `只接受学工系统目标地址`() {
        assertTrue(
            reachedSchoolTarget(
                SchoolSystem.STUDENT_AFFAIRS,
                "https://xgfw.gsau.edu.cn/xsfw/sys/jbxxapp/index.do",
            )
        )
        assertFalse(
            reachedSchoolTarget(
                SchoolSystem.STUDENT_AFFAIRS,
                "https://xgfw.gsau.edu.cn.evil.example/xsfw/",
            )
        )
    }
}
