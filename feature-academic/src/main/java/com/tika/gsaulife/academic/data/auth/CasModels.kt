package com.tika.gsaulife.academic.data.auth

internal sealed interface CasResult {
    object Success : CasResult
    data class Failed(val message: String?) : CasResult
}

internal enum class QrStatus { Waiting, Scanned, Confirmed, Expired }
