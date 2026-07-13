package com.tika.gsaulife.academic.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.tika.gsaulife.academic.model.Course
import kotlin.math.max

class AcademicScheduleGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var onCourseClick: ((Course) -> Unit)? = null

    private var courses: List<Course> = emptyList()
    private val hitAreas = mutableListOf<Pair<RectF, Course>>()
    private val density = resources.displayMetrics.density
    private val sectionWidth = 26f * density
    private val headerHeight = 34f * density
    private val rowHeight = 58f * density

    private fun themeColor(attribute: Int): Int = MaterialColors.getColor(this, attribute)

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(MaterialR.attr.colorOutlineVariant)
        strokeWidth = density
    }
    private val headerBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(MaterialR.attr.colorSurfaceVariant)
    }
    private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(MaterialR.attr.colorOnSurfaceVariant)
        textAlign = Paint.Align.CENTER
        textSize = 11f * density
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(MaterialR.attr.colorOnSurface)
        textAlign = Paint.Align.CENTER
        textSize = 13f * density
        isFakeBoldText = true
    }
    private val coursePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val courseTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f * density }
    private val roomTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f * density }
    private val palette = listOf(
        MaterialR.attr.colorPrimaryContainer to MaterialR.attr.colorOnPrimaryContainer,
        MaterialR.attr.colorSecondaryContainer to MaterialR.attr.colorOnSecondaryContainer,
        MaterialR.attr.colorTertiaryContainer to MaterialR.attr.colorOnTertiaryContainer,
    ).map { (background, foreground) -> themeColor(background) to themeColor(foreground) }

    init {
        isClickable = true
    }

    fun submit(items: List<Course>) {
        courses = items
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            (headerHeight + rowHeight * SECTION_COUNT).toInt(),
        )
    }

    override fun onDraw(canvas: Canvas) {
        hitAreas.clear()
        val dayWidth = (width - sectionWidth) / 7f
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerBackgroundPaint)
        val headerBaseline = headerHeight / 2 - (headerPaint.descent() + headerPaint.ascent()) / 2
        for (day in WEEKDAYS.indices) {
            val center = sectionWidth + dayWidth * (day + 0.5f)
            canvas.drawText("周${WEEKDAYS[day]}", center, headerBaseline, headerPaint)
        }
        for (section in 0 until SECTION_COUNT) {
            val top = headerHeight + rowHeight * section
            canvas.drawLine(0f, top, width.toFloat(), top, gridPaint)
            val baseline = top + rowHeight / 2 - (sectionPaint.descent() + sectionPaint.ascent()) / 2
            canvas.drawText("${section + 1}", sectionWidth / 2, baseline, sectionPaint)
        }
        for (day in 0..7) {
            val x = sectionWidth + dayWidth * day
            canvas.drawLine(x, headerHeight, x, height.toFloat(), gridPaint)
        }
        courses.forEach { drawCourse(canvas, it, dayWidth) }
    }

    private fun drawCourse(canvas: Canvas, course: Course, dayWidth: Float) {
        val left = sectionWidth + dayWidth * (course.weekday - 1) + density
        val right = sectionWidth + dayWidth * course.weekday - density
        val top = headerHeight + rowHeight * (course.startSection - 1) + density
        val span = max(1, course.endSection - course.startSection + 1)
        val bottom = headerHeight + rowHeight * (course.startSection - 1 + span) - density
        val bounds = RectF(left, top, right, bottom)
        val (background, foreground) = colorOf(course.name)
        coursePaint.color = background
        courseTextPaint.color = foreground
        roomTextPaint.color = foreground
        canvas.drawRoundRect(bounds, 6f * density, 6f * density, coursePaint)
        drawText(canvas, course, bounds)
        hitAreas.add(bounds to course)
    }

    private fun drawText(canvas: Canvas, course: Course, bounds: RectF) {
        val padding = 4f * density
        val maxWidth = bounds.width() - padding * 2
        var y = bounds.top + padding - courseTextPaint.ascent()
        for (line in wrap(course.name, courseTextPaint, maxWidth)) {
            if (y > bounds.bottom - padding) break
            canvas.drawText(line, bounds.left + padding, y, courseTextPaint)
            y += courseTextPaint.descent() - courseTextPaint.ascent()
        }
        if (course.room.isNotEmpty() && y <= bounds.bottom - padding) {
            y += 2f * density
            for (line in wrap(course.room, roomTextPaint, maxWidth)) {
                if (y > bounds.bottom - padding) break
                canvas.drawText(line, bounds.left + padding, y, roomTextPaint)
                y += roomTextPaint.descent() - roomTextPaint.ascent()
            }
        }
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val current = StringBuilder()
        for (character in text) {
            if (paint.measureText(current.toString() + character) > maxWidth && current.isNotEmpty()) {
                lines.add(current.toString())
                current.setLength(0)
            }
            current.append(character)
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            hitAreas.firstOrNull { it.first.contains(event.x, event.y) }?.second?.let {
                onCourseClick?.invoke(it)
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun colorOf(name: String): Pair<Int, Int> {
        var hash = 0
        for (character in name) hash = (hash * 31 + character.code) and Int.MAX_VALUE
        return palette[hash % palette.size]
    }

    companion object {
        private const val SECTION_COUNT = 10
        private val WEEKDAYS = arrayOf("一", "二", "三", "四", "五", "六", "日")
    }
}
