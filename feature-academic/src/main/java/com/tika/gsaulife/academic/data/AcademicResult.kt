package com.tika.gsaulife.academic.data

internal sealed interface AcademicResult<out T> {
    data class Ok<T>(val data: T) : AcademicResult<T>
    data object LoggedOut : AcademicResult<Nothing>
    data object Stale : AcademicResult<Nothing>
    data class Error(val message: String) : AcademicResult<Nothing>
}
