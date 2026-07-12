package com.tika.gsaulife.academic.data

import android.content.Context
import androidx.core.content.edit
import com.tika.gsaulife.academic.SchoolSystem
import com.tika.gsaulife.academic.model.Course
import com.tika.gsaulife.academic.model.Exam
import com.tika.gsaulife.academic.model.ExamPage
import com.tika.gsaulife.academic.model.Grade
import com.tika.gsaulife.academic.model.Ranking
import com.tika.gsaulife.academic.model.SchedulePage
import com.tika.gsaulife.academic.model.ScoreDetail
import com.tika.gsaulife.academic.model.mapObjects
import com.tika.gsaulife.academic.model.toJsonArray
import org.json.JSONObject

internal data class CachedValue<T>(val data: T, val fetchedAt: Long)

internal class AcademicCache private constructor(context: Context) {
    private val preferences =
        context.getSharedPreferences("academic_cache", Context.MODE_PRIVATE)

    fun saveGrades(grades: List<Grade>) = save(KEY_GRADES, grades.toJsonArray { it.toJson() })

    fun loadGrades(): CachedValue<List<Grade>>? =
        load(KEY_GRADES) { it.getJSONArray("data").mapObjects(Grade::fromJson) }

    fun saveScoreDetail(grade: Grade, detail: ScoreDetail) {
        val root = JSONObject(preferences.getString(KEY_SCORE_DETAILS, "{}") ?: "{}")
        root.put(detailKey(grade), payload(detail.toJson()))
        preferences.edit { putString(KEY_SCORE_DETAILS, root.toString()) }
    }

    fun loadScoreDetail(grade: Grade): CachedValue<ScoreDetail>? {
        val root = JSONObject(preferences.getString(KEY_SCORE_DETAILS, "{}") ?: "{}")
        val item = root.optJSONObject(detailKey(grade)) ?: return null
        return CachedValue(ScoreDetail.fromJson(item.getJSONObject("data")), item.getLong("ts"))
    }

    fun saveSchedule(page: SchedulePage) = save(
        KEY_SCHEDULE,
        JSONObject().apply {
            put("term", page.term)
            put("courses", page.courses.toJsonArray { it.toJson() })
        }
    )

    fun loadSchedule(): CachedValue<SchedulePage>? = load(KEY_SCHEDULE) {
        val data = it.getJSONObject("data")
        SchedulePage(
            term = data.getString("term"),
            courses = data.getJSONArray("courses").mapObjects(Course::fromJson),
        )
    }

    fun saveExams(page: ExamPage) = save(
        KEY_EXAMS,
        JSONObject().apply {
            put("term", page.term)
            put("exams", page.exams.toJsonArray { it.toJson() })
        }
    )

    fun loadExams(): CachedValue<ExamPage>? = load(KEY_EXAMS) {
        val data = it.getJSONObject("data")
        ExamPage(
            term = data.getString("term"),
            exams = data.getJSONArray("exams").mapObjects(Exam::fromJson),
        )
    }

    fun saveRankings(rankings: List<Ranking>) =
        save(KEY_RANKINGS, rankings.toJsonArray { it.toJson() })

    fun loadRankings(): CachedValue<List<Ranking>>? =
        load(KEY_RANKINGS) { it.getJSONArray("data").mapObjects(Ranking::fromJson) }

    fun clear() {
        preferences.edit { clear() }
    }

    fun clear(system: SchoolSystem) {
        preferences.edit {
            when (system) {
                SchoolSystem.ACADEMIC -> {
                    remove(KEY_GRADES)
                    remove(KEY_SCORE_DETAILS)
                    remove(KEY_SCHEDULE)
                    remove(KEY_EXAMS)
                }
                SchoolSystem.STUDENT_AFFAIRS -> remove(KEY_RANKINGS)
            }
        }
    }

    private fun save(key: String, value: Any) {
        preferences.edit { putString(key, payload(value).toString()) }
    }

    private fun payload(value: Any): JSONObject = JSONObject().apply {
        put("ts", System.currentTimeMillis())
        put("data", value)
    }

    private fun <T> load(key: String, transform: (JSONObject) -> T): CachedValue<T>? {
        val raw = preferences.getString(key, null) ?: return null
        val payload = JSONObject(raw)
        return CachedValue(transform(payload), payload.getLong("ts"))
    }

    private fun detailKey(grade: Grade): String =
        "${grade.studentId}:${grade.classId}:${grade.score}"

    companion object {
        private const val KEY_GRADES = "grades"
        private const val KEY_SCORE_DETAILS = "score_details"
        private const val KEY_SCHEDULE = "schedule"
        private const val KEY_EXAMS = "exams"
        private const val KEY_RANKINGS = "rankings"

        @Volatile
        private var instance: AcademicCache? = null

        fun get(context: Context): AcademicCache =
            instance ?: synchronized(this) {
                instance ?: AcademicCache(context.applicationContext).also { instance = it }
            }
    }
}
