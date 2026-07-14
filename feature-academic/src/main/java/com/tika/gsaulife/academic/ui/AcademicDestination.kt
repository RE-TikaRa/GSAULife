package com.tika.gsaulife.academic.ui

internal enum class AcademicDestination {
    GRADES,
    SCHEDULE,
    EXAMS,
    RANKINGS,
}

internal interface AcademicPage {
    val destination: AcademicDestination
    fun reload()
}
