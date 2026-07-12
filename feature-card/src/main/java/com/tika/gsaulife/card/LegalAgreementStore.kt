package com.tika.gsaulife.card

import android.content.Context
import androidx.core.content.edit

object LegalAgreementStore {
    private const val PREFS = "legal_agreement"
    private const val KEY_VERSION = "accepted_version"
    private const val VERSION = "1.0"

    fun isAccepted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_VERSION, null) == VERSION

    fun accept(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_VERSION, VERSION) }
    }
}
