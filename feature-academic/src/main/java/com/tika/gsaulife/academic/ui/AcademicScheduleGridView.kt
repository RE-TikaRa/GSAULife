package com.tika.gsaulife.academic.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.model.Course
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

class AcademicScheduleGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var onCourseClick: ((Course) -> Unit)? = null
    var onSwipeWeek: ((Int) -> Unit)? = null

    private var courses: List<Course> = emptyList()
    private var pressedCourse = ExploreByTouchHelper.INVALID_ID
    private var swiped = false
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private val density = resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val swipeDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                val start = e1 ?: return false
                if (abs(velocityX) < abs(velocityY) || abs(velocityX) < minFlingVelocity) {
                    return false
                }
                if (abs(e2.x - start.x) < abs(e2.y - start.y)) return false
                onSwipeWeek?.invoke(if (velocityX < 0) 1 else -1) ?: return false
                swiped = true
                return true
            }
        },
    )
    private val minFlingVelocity = 220f * density

    private fun themeColor(attribute: Int): Int = MaterialColors.getColor(this, attribute)
    private fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        value,
        resources.displayMetrics,
    )

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
        textSize = sp(11f)
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(MaterialR.attr.colorOnSurface)
        textAlign = Paint.Align.CENTER
        textSize = sp(13f)
        isFakeBoldText = true
    }
    private val coursePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val courseTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(11f)
    }
    private val roomTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(10f)
    }
    private val sectionWidth = max(24f * density, sectionPaint.measureText("10") + 8f * density)
    private val headerHeight = max(34f * density, textHeight(headerPaint) + 12f * density)
    private val minDayWidth = max(
        48f * density,
        max(
            headerPaint.measureText(context.getString(R.string.academic_schedule_weekday, "日")) +
                12f * density,
            courseTextPaint.measureText("机械") + 24f * density,
        ),
    )
    private val rowHeight = max(
        58f * density,
        textHeight(courseTextPaint) * 2 + textHeight(roomTextPaint) + 12f * density,
    )
    private val palette = listOf(
        MaterialR.attr.colorPrimaryContainer to MaterialR.attr.colorOnPrimaryContainer,
        MaterialR.attr.colorSecondaryContainer to MaterialR.attr.colorOnSecondaryContainer,
        MaterialR.attr.colorTertiaryContainer to MaterialR.attr.colorOnTertiaryContainer,
    ).map { (background, foreground) -> themeColor(background) to themeColor(foreground) }
    private val accessibilityHelper = object : ExploreByTouchHelper(this) {
        override fun getVirtualViewAt(x: Float, y: Float): Int = courseAt(x, y)

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            courses.indices
                .sortedWith(
                    compareBy<Int> { courses[it].startSection }
                        .thenBy { courses[it].weekday }
                        .thenBy { courses[it].name }
                )
                .forEach(virtualViewIds::add)
        }

        override fun onPopulateNodeForVirtualView(
            virtualViewId: Int,
            node: AccessibilityNodeInfoCompat,
        ) {
            val course = courses[virtualViewId]
            val bounds = Rect()
            courseBounds(course).roundOut(bounds)
            node.className = Button::class.java.name
            node.contentDescription = courseDescription(course)
            node.isClickable = true
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
            setBoundsInScreenFromBoundsInParent(node, bounds)
        }

        override fun onPerformActionForVirtualView(
            virtualViewId: Int,
            action: Int,
            arguments: Bundle?,
        ): Boolean = action == AccessibilityNodeInfoCompat.ACTION_CLICK &&
            activateCourse(virtualViewId)
    }

    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        ViewCompat.setAccessibilityDelegate(this, accessibilityHelper)
    }

    fun submit(items: List<Course>) {
        courses = items
        accessibilityHelper.invalidateRoot()
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = ceil(sectionWidth + minDayWidth * WEEKDAYS.size).toInt()
        val desiredHeight = ceil(headerHeight + rowHeight * SECTION_COUNT).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val dayWidth = dayWidth()
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerBackgroundPaint)
        val headerBaseline = headerHeight / 2 - (headerPaint.descent() + headerPaint.ascent()) / 2
        for (day in WEEKDAYS.indices) {
            val center = sectionWidth + dayWidth * (day + 0.5f)
            canvas.drawText(
                context.getString(R.string.academic_schedule_weekday, WEEKDAYS[day]),
                center,
                headerBaseline,
                headerPaint,
            )
        }
        for (section in 0 until SECTION_COUNT) {
            val top = headerHeight + rowHeight * section
            canvas.drawLine(0f, top, width.toFloat(), top, gridPaint)
            val baseline = top + rowHeight / 2 - (sectionPaint.descent() + sectionPaint.ascent()) / 2
            canvas.drawText("${section + 1}", sectionWidth / 2, baseline, sectionPaint)
        }
        for (day in 0..WEEKDAYS.size) {
            val x = sectionWidth + dayWidth * day
            canvas.drawLine(x, headerHeight, x, height.toFloat(), gridPaint)
        }
        courses.forEach { drawCourse(canvas, it) }
    }

    private fun drawCourse(canvas: Canvas, course: Course) {
        val bounds = courseBounds(course)
        val (background, foreground) = colorOf(course.name)
        coursePaint.color = background
        courseTextPaint.color = foreground
        roomTextPaint.color = foreground
        canvas.drawRoundRect(bounds, 6f * density, 6f * density, coursePaint)
        drawText(canvas, course, bounds)
    }

    private fun courseBounds(course: Course): RectF {
        val dayWidth = dayWidth()
        val left = sectionWidth + dayWidth * (course.weekday - 1) + density
        val right = sectionWidth + dayWidth * course.weekday - density
        val top = headerHeight + rowHeight * (course.startSection - 1) + density
        val span = max(1, course.endSection - course.startSection + 1)
        val bottom = headerHeight + rowHeight * (course.startSection - 1 + span) - density
        return RectF(left, top, right, bottom)
    }

    private fun drawText(canvas: Canvas, course: Course, bounds: RectF) {
        val padding = 4f * density
        val maxWidth = bounds.width() - padding * 2
        var y = bounds.top + padding - courseTextPaint.ascent()
        for (line in wrap(course.name, courseTextPaint, maxWidth)) {
            if (y > bounds.bottom - padding) break
            canvas.drawText(line, bounds.left + padding, y, courseTextPaint)
            y += textHeight(courseTextPaint)
        }
        if (course.room.isNotEmpty() && y <= bounds.bottom - padding) {
            y += 2f * density
            for (line in wrap(course.room, roomTextPaint, maxWidth)) {
                if (y > bounds.bottom - padding) break
                canvas.drawText(line, bounds.left + padding, y, roomTextPaint)
                y += textHeight(roomTextPaint)
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
        swipeDetector.onTouchEvent(event)
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                swiped = false
                dragging = false
                downX = event.x
                downY = event.y
                pressedCourse = courseAt(event.x, event.y)
                true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging) {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                        dragging = true
                        pressedCourse = ExploreByTouchHelper.INVALID_ID
                        parent?.requestDisallowInterceptTouchEvent(true)
                    } else if (abs(dy) > touchSlop) {
                        parent?.requestDisallowInterceptTouchEvent(false)
                        pressedCourse = ExploreByTouchHelper.INVALID_ID
                    }
                }
                true
            }

            MotionEvent.ACTION_UP -> {
                val virtualViewId = pressedCourse
                pressedCourse = ExploreByTouchHelper.INVALID_ID
                if (
                    !swiped &&
                    !dragging &&
                    virtualViewId != ExploreByTouchHelper.INVALID_ID &&
                    courseAt(event.x, event.y) == virtualViewId &&
                    activateCourse(virtualViewId)
                ) {
                    performClick()
                }
                true
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedCourse = ExploreByTouchHelper.INVALID_ID
                true
            }

            else -> true
        }
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean =
        accessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun courseAt(x: Float, y: Float): Int = courses.indices.reversed()
        .firstOrNull { courseBounds(courses[it]).contains(x, y) }
        ?: ExploreByTouchHelper.INVALID_ID

    private fun activateCourse(index: Int): Boolean {
        val course = courses.getOrNull(index) ?: return false
        val listener = onCourseClick ?: return false
        listener(course)
        accessibilityHelper.sendEventForVirtualView(
            index,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
        )
        return true
    }

    private fun courseDescription(course: Course): String = buildList {
        add(course.name)
        add(context.getString(R.string.academic_schedule_weekday, WEEKDAYS[course.weekday - 1]))
        add(
            context.getString(
                R.string.academic_schedule_sections,
                course.startSection,
                course.endSection,
            )
        )
        if (course.teacher.isNotEmpty()) {
            add(context.getString(R.string.academic_schedule_teacher, course.teacher))
        }
        if (course.room.isNotEmpty()) {
            add(context.getString(R.string.academic_schedule_room, course.room))
        }
    }.joinToString("，")

    private fun dayWidth(): Float = (width - sectionWidth) / WEEKDAYS.size

    private fun textHeight(paint: Paint): Float = paint.descent() - paint.ascent()

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
