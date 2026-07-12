package com.tika.gsaulife.academic.data

import com.tika.gsaulife.academic.model.Course
import com.tika.gsaulife.academic.model.Exam
import com.tika.gsaulife.academic.model.Grade
import com.tika.gsaulife.academic.model.SchedulePage
import com.tika.gsaulife.academic.model.ScoreComponent
import com.tika.gsaulife.academic.model.ScoreDetail
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

object JwxtParser {

    fun grades(html: String): List<Grade> {
        val result = mutableListOf<Grade>()
        for (row in dataTable(html).select("tr")) {
            val cells = row.select("td")
            if (cells.size < 12) continue
            val href = cells[4].selectFirst("a")?.attr("href").orEmpty()
            result.add(
                Grade(
                    term = text(cells[1]),
                    courseId = text(cells[2]),
                    courseName = text(cells[3]),
                    score = text(cells[4]),
                    credit = text(cells[6]).toDoubleOrNull() ?: 0.0,
                    gradePoint = text(cells[8]).toDoubleOrNull(),
                    examType = text(cells[9]),
                    courseType = text(cells[11]),
                    classId = param(href, "jx0404id"),
                    studentId = param(href, "xs0101id"),
                )
            )
        }
        return result
    }

    fun scoreDetail(html: String): ScoreDetail {
        val row = dataTable(html).select("tr")
            .map { it.select("td") }
            .firstOrNull { it.size >= 4 && it.size % 2 == 0 && text(it[0]) == "1" }
            ?: return ScoreDetail(emptyList(), "")
        val groups = (row.size - 2) / 2
        val components = mutableListOf<ScoreComponent>()
        for (index in 0 until minOf(groups, DETAIL_LABELS.size)) {
            val score = text(row[1 + index * 2])
            val ratio = text(row[2 + index * 2])
            if (ratio.removeSuffix("%").trim().toDoubleOrNull() == 0.0) continue
            components.add(ScoreComponent(DETAIL_LABELS[index], score, ratio))
        }
        return ScoreDetail(components, text(row[row.size - 1]))
    }

    fun exams(html: String): List<Exam> {
        val result = mutableListOf<Exam>()
        for (row in dataTable(html).select("tr")) {
            val cells = row.select("td")
            if (cells.size < 8) continue
            result.add(
                Exam(
                    courseName = text(cells[3]),
                    time = text(cells[4]),
                    location = text(cells[5]),
                    seat = text(cells[6]),
                )
            )
        }
        return result
    }

    fun schedulePage(html: String): SchedulePage {
        val term = currentTerm(html)
        require(term.isNotEmpty()) { "教务响应缺少当前学期" }
        return SchedulePage(term, schedule(html))
    }

    fun currentTerm(html: String): String =
        Jsoup.parse(html).selectFirst("#xnxq01id option[selected]")?.attr("value").orEmpty()

    fun schedule(html: String): List<Course> {
        val result = mutableListOf<Course>()
        var block = 0
        for (row in Jsoup.parse(html).select("tr")) {
            val cells = row.select("div.kbcontent")
            if (cells.isEmpty()) continue
            block++
            val start = block * 2 - 1
            val end = block * 2
            for (cell in cells) parseCourse(cell, start, end)?.let(result::add)
        }
        return result
    }

    private fun parseCourse(cell: Element, start: Int, end: Int): Course? {
        val parts = cell.id().split("-")
        if (parts.size < 2) return null
        val weekday = parts[parts.size - 2].toIntOrNull() ?: return null
        if (weekday !in 1..7) return null

        val name = firstText(cell)
        if (name.isEmpty()) return null
        var teacher = ""
        var room = ""
        var weeks = ""
        for (font in cell.select("font")) {
            val title = font.attr("title")
            val value = font.text().trim()
            when {
                "老师" in title || "教师" in title -> teacher = value
                "教室" in title -> room = value
                "周次" in title -> weeks = value
            }
        }
        return Course(name, teacher, room, weekday, start, end, parseWeeks(weeks))
    }

    private fun parseWeeks(value: String): Set<Int> {
        val result = sortedSetOf<Int>()
        for (segment in value.split(",")) {
            val range = RANGE_RE.find(segment)
            if (range == null) {
                SINGLE_RE.find(segment)?.let { result.add(it.groupValues[1].toInt()) }
                continue
            }
            val first = range.groupValues[1].toInt()
            val last = range.groupValues[2].toInt()
            val odd = "单" in segment
            val even = "双" in segment
            for (week in first..last) {
                if (odd && week % 2 == 0) continue
                if (even && week % 2 != 0) continue
                result.add(week)
            }
        }
        return result
    }

    private fun firstText(element: Element): String {
        for (node in element.childNodes()) {
            if (node is TextNode) {
                val value = node.text().trim()
                if (value.isNotEmpty()) return value
            }
            if (node is Element && node.tagName() == "br") break
        }
        return ""
    }

    private fun param(url: String, key: String): String =
        Regex("""$key=([^&'")]+)""").find(url)?.groupValues?.get(1).orEmpty()

    private fun text(element: Element): String = element.text().trim()

    private fun dataTable(html: String): Element =
        requireNotNull(Jsoup.parse(html).selectFirst("#dataList")) { "教务响应缺少数据表" }

    private val RANGE_RE = Regex("""(\d+)-(\d+)""")
    private val SINGLE_RE = Regex("""(\d+)""")
    private val DETAIL_LABELS = listOf("期末", "期中", "平时", "实验", "其他")
}
