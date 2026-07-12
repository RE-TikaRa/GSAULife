package com.tika.gsaulife.academic.data

import android.content.Context
import androidx.core.content.edit
import com.tika.gsaulife.academic.SchoolSystem
import org.json.JSONObject

internal class SchoolSessionStore private constructor(context: Context) {
    private val preferences =
        context.getSharedPreferences("academic_school_sessions", Context.MODE_PRIVATE)
    private val lock = Any()

    fun isLoggedIn(system: SchoolSystem): Boolean = cookies(system).isNotEmpty()

    fun save(system: SchoolSystem, cookies: Map<String, String>) = synchronized(lock) {
        if (cookies.isEmpty()) return
        preferences.edit {
            putString(system.key, JSONObject(cookies).toString())
            putLong(system.versionKey, version(system) + 1)
        }
    }

    fun snapshot(system: SchoolSystem): SessionSnapshot = synchronized(lock) {
        SessionSnapshot(
            version(system),
            cookies(system).entries.joinToString("; ") { "${it.key}=${it.value}" },
        )
    }

    fun isCurrent(system: SchoolSystem, version: Long): Boolean = synchronized(lock) {
        version(system) == version
    }

    fun runIfCurrent(system: SchoolSystem, version: Long, action: () -> Unit): Boolean =
        synchronized(lock) {
            if (version(system) != version) return@synchronized false
            action()
            true
        }

    fun clearIfCurrent(system: SchoolSystem, version: Long): Boolean = synchronized(lock) {
        if (version(system) != version) return@synchronized false
        preferences.edit {
            remove(system.key)
            putLong(system.versionKey, version + 1)
        }
        true
    }

    fun clearAll() = synchronized(lock) {
        val academicVersion = version(SchoolSystem.ACADEMIC) + 1
        val studentVersion = version(SchoolSystem.STUDENT_AFFAIRS) + 1
        preferences.edit {
            clear()
            putLong(SchoolSystem.ACADEMIC.versionKey, academicVersion)
            putLong(SchoolSystem.STUDENT_AFFAIRS.versionKey, studentVersion)
        }
    }

    private fun cookies(system: SchoolSystem): Map<String, String> {
        val raw = preferences.getString(system.key, null) ?: return emptyMap()
        val json = JSONObject(raw)
        return buildMap {
            for (name in json.keys()) put(name, json.getString(name))
        }
    }

    private fun version(system: SchoolSystem): Long =
        preferences.getLong(system.versionKey, 0)

    companion object {
        const val JWGL_BASE = "http://jwgl.gsau.edu.cn"
        const val XGFW_BASE = "https://xgfw.gsau.edu.cn"

        @Volatile
        private var instance: SchoolSessionStore? = null

        fun get(context: Context): SchoolSessionStore =
            instance ?: synchronized(this) {
                instance ?: SchoolSessionStore(context.applicationContext).also { instance = it }
            }
    }
}

internal data class SessionSnapshot(val version: Long, val cookieHeader: String)

private val SchoolSystem.key: String
    get() = when (this) {
        SchoolSystem.ACADEMIC -> "jwgl"
        SchoolSystem.STUDENT_AFFAIRS -> "xgfw"
    }

private val SchoolSystem.versionKey: String
    get() = "$key:version"
