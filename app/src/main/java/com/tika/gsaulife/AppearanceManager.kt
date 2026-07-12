package com.tika.gsaulife

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

enum class ThemeMode { SYSTEM, LIGHT, DARK }

object AppearanceManager {
    private const val PREFS = "appearance"
    private const val KEY_THEME = "theme_mode"

    fun getMode(context: Context): ThemeMode {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, ThemeMode.SYSTEM.name)
        return ThemeMode.valueOf(value ?: ThemeMode.SYSTEM.name)
    }

    fun setMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_THEME, mode.name) }
        apply(mode)
    }

    fun apply(mode: ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }
}
