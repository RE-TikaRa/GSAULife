package com.tika.gsaulife.academic.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

data class Grade(
    val term: String,
    val courseName: String,
    val courseId: String,
    val credit: Double,
    val score: String,
    val gradePoint: Double?,
    val courseType: String,
    val examType: String,
    val classId: String,
    val studentId: String,
) {
    val numericScore: Double? get() = score.toDoubleOrNull()
    val hasDetail: Boolean get() = classId.isNotEmpty()
    val passed: Boolean
        get() {
            numericScore?.let { return it >= 60 }
            gradePoint?.let { return it > 0 }
            return score !in FAILED_SCORES
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("term", term)
        put("courseName", courseName)
        put("courseId", courseId)
        put("credit", credit)
        put("score", score)
        put("gradePoint", gradePoint ?: JSONObject.NULL)
        put("courseType", courseType)
        put("examType", examType)
        put("classId", classId)
        put("studentId", studentId)
    }

    companion object {
        private val FAILED_SCORES = setOf("不及格", "不合格", "未通过", "缺考", "作弊")

        fun fromJson(json: JSONObject): Grade = Grade(
            term = json.getString("term"),
            courseName = json.getString("courseName"),
            courseId = json.getString("courseId"),
            credit = json.getDouble("credit"),
            score = json.getString("score"),
            gradePoint = if (json.isNull("gradePoint")) null else json.getDouble("gradePoint"),
            courseType = json.getString("courseType"),
            examType = json.getString("examType"),
            classId = json.optString("classId"),
            studentId = json.optString("studentId"),
        )
    }
}

data class ScoreComponent(
    val label: String,
    val score: String,
    val ratio: String,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("label", label)
        put("score", score)
        put("ratio", ratio)
    }

    companion object {
        fun fromJson(json: JSONObject): ScoreComponent = ScoreComponent(
            label = json.getString("label"),
            score = json.getString("score"),
            ratio = json.getString("ratio"),
        )
    }
}

data class ScoreDetail(
    val components: List<ScoreComponent>,
    val total: String,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("components", components.toJsonArray { it.toJson() })
        put("total", total)
    }

    companion object {
        fun fromJson(json: JSONObject): ScoreDetail = ScoreDetail(
            components = json.getJSONArray("components").mapObjects(ScoreComponent::fromJson),
            total = json.getString("total"),
        )
    }
}

data class Course(
    val name: String,
    val teacher: String,
    val room: String,
    val weekday: Int,
    val startSection: Int,
    val endSection: Int,
    val weeks: Set<Int>,
) {
    fun inWeek(week: Int): Boolean = week in weeks

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("teacher", teacher)
        put("room", room)
        put("weekday", weekday)
        put("startSection", startSection)
        put("endSection", endSection)
        put("weeks", JSONArray(weeks.toList()))
    }

    companion object {
        fun fromJson(json: JSONObject): Course = Course(
            name = json.getString("name"),
            teacher = json.getString("teacher"),
            room = json.getString("room"),
            weekday = json.getInt("weekday"),
            startSection = json.getInt("startSection"),
            endSection = json.getInt("endSection"),
            weeks = json.getJSONArray("weeks").mapInts().toSet(),
        )
    }
}

data class SchedulePage(
    val term: String,
    val courses: List<Course>,
)

data class SectionTime(
    val startMinute: Int,
    val durationMinute: Int,
) {
    val endMinute: Int get() = startMinute + durationMinute

    fun formatStart(): String = formatMinute(startMinute)

    fun formatEnd(): String = formatMinute(endMinute)

    fun toJson(): JSONObject = JSONObject().apply {
        put("startMinute", startMinute)
        put("durationMinute", durationMinute)
    }

    companion object {
        const val COUNT = 10

        val DEFAULT: List<SectionTime> = listOf(
            SectionTime(8 * 60, 50),
            SectionTime(9 * 60, 50),
            SectionTime(10 * 60 + 20, 50),
            SectionTime(11 * 60 + 20, 50),
            SectionTime(14 * 60, 50),
            SectionTime(15 * 60, 50),
            SectionTime(16 * 60 + 20, 50),
            SectionTime(17 * 60 + 20, 50),
            SectionTime(19 * 60 + 30, 50),
            SectionTime(20 * 60 + 30, 50),
        )

        fun fromJson(json: JSONObject): SectionTime = SectionTime(
            startMinute = json.getInt("startMinute"),
            durationMinute = json.getInt("durationMinute"),
        )

        private fun formatMinute(minute: Int): String {
            val clamped = ((minute % 1440) + 1440) % 1440
            return "%02d:%02d".format(clamped / 60, clamped % 60)
        }
    }
}

data class Exam(
    val courseName: String,
    val time: String,
    val location: String,
    val seat: String,
) {
    fun endTime(): Long? {
        val match = TIME_RE.find(time) ?: return null
        val (date, end) = match.destructured
        return runCatching {
            TIME_FORMAT.parse("$date $end")?.time
        }.getOrNull()
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("courseName", courseName)
        put("time", time)
        put("location", location)
        put("seat", seat)
    }

    companion object {
        private val TIME_RE = Regex("""(\d{4}-\d{2}-\d{2})\s+\d{1,2}:\d{2}\s*[~-]\s*(\d{1,2}:\d{2})""")
        private val TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd H:mm", Locale.CHINA)

        fun fromJson(json: JSONObject): Exam = Exam(
            courseName = json.getString("courseName"),
            time = json.getString("time"),
            location = json.getString("location"),
            seat = json.getString("seat"),
        )
    }
}

data class ExamPage(
    val term: String,
    val exams: List<Exam>,
)

data class Ranking(
    val year: String,
    val yearDisplay: String,
    val term: String,
    val termDisplay: String,
    val gradeLabel: String,
    val majorRank: Int,
    val majorTotal: Int,
    val classRank: Int,
    val classTotal: Int,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("year", year)
        put("yearDisplay", yearDisplay)
        put("term", term)
        put("termDisplay", termDisplay)
        put("gradeLabel", gradeLabel)
        put("majorRank", majorRank)
        put("majorTotal", majorTotal)
        put("classRank", classRank)
        put("classTotal", classTotal)
    }

    companion object {
        fun fromJson(json: JSONObject): Ranking = Ranking(
            year = json.getString("year"),
            yearDisplay = json.getString("yearDisplay"),
            term = json.getString("term"),
            termDisplay = json.getString("termDisplay"),
            gradeLabel = json.getString("gradeLabel"),
            majorRank = json.getInt("majorRank"),
            majorTotal = json.getInt("majorTotal"),
            classRank = json.getInt("classRank"),
            classTotal = json.getInt("classTotal"),
        )
    }
}

internal fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray =
    JSONArray().also { array -> forEach { array.put(transform(it)) } }

internal fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

private fun JSONArray.mapInts(): List<Int> = (0 until length()).map(::getInt)
