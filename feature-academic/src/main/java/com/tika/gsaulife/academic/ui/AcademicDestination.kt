package com.tika.gsaulife.academic.ui

import com.tika.gsaulife.academic.SchoolSystem

internal enum class AcademicDestination(val system: SchoolSystem) {
    GRADES(SchoolSystem.ACADEMIC),
    SCHEDULE(SchoolSystem.ACADEMIC),
    EXAMS(SchoolSystem.ACADEMIC),
    RANKINGS(SchoolSystem.STUDENT_AFFAIRS),
}

internal interface AcademicPage {
    val destination: AcademicDestination
    fun reload()
}
