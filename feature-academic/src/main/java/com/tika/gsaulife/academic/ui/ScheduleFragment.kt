package com.tika.gsaulife.academic.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.data.AcademicCache
import com.tika.gsaulife.academic.data.AcademicResult
import com.tika.gsaulife.academic.data.AcademicSettings
import com.tika.gsaulife.academic.databinding.AcademicFragmentScheduleBinding
import com.tika.gsaulife.academic.databinding.AcademicSheetCourseBinding
import com.tika.gsaulife.academic.model.Course
import com.tika.gsaulife.academic.model.SchedulePage
import kotlinx.coroutines.launch

internal class ScheduleFragment : Fragment(), AcademicPage {
    override val destination = AcademicDestination.SCHEDULE

    private var _binding: AcademicFragmentScheduleBinding? = null
    private val binding get() = _binding!!
    private val repository by lazy { jwxtRepository(requireContext()) }
    private val cache by lazy { AcademicCache.get(requireContext()) }
    private val settings by lazy { AcademicSettings.get(requireContext()) }

    private var term = ""
    private var courses: List<Course> = emptyList()
    private var selectedWeek = 1
    private var maxWeek = 20

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AcademicFragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.academicToolbar.setNavigationOnClickListener { root().resetToMenu() }
        binding.academicToolbar.inflateMenu(R.menu.academic_menu_schedule)
        binding.academicToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.academic_action_calibrate_week -> { showWeekCalibration(); true }
                R.id.academic_action_refresh -> { reload(); true }
                else -> false
            }
        }
        binding.academicScheduleGrid.onCourseClick = ::showCourseDetail
        binding.academicWeekList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        updateTitle()
        reload()
    }

    override fun reload() {
        if (_binding == null) return
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.schedule()) {
                is AcademicResult.Ok -> render(result.data)
                AcademicResult.LoggedOut -> {
                    val cached = cache.loadSchedule()
                    if (cached == null) showLoggedOut()
                    else render(cached.data, cached.fetchedAt, true)
                }
                AcademicResult.Stale -> Unit
                is AcademicResult.Error -> {
                    val cached = cache.loadSchedule()
                    if (cached == null) showError(result.message)
                    else render(cached.data, cached.fetchedAt, false)
                }
            }
        }
    }

    private fun render(page: SchedulePage, fetchedAt: Long? = null, needsLogin: Boolean = false) {
        term = page.term
        courses = page.courses
        if (fetchedAt == null) binding.academicCacheBanner.hide()
        else binding.academicCacheBanner.show(fetchedAt, needsLogin) { root().authenticate(destination) }
        if (courses.isEmpty()) {
            binding.academicScheduleScroll.visibility = View.GONE
            binding.academicWeekList.visibility = View.GONE
            binding.academicState.showEmpty(
                R.drawable.academic_ic_schedule,
                getString(R.string.academic_schedule_empty),
            )
            updateTitle()
            return
        }
        maxWeek = courses.flatMap { it.weeks }.maxOrNull()?.takeIf { it > 0 } ?: 20
        val calibrated = settings.currentWeek(term)
        selectedWeek = (calibrated ?: selectedWeek).coerceIn(1, maxWeek)
        binding.academicState.hide()
        binding.academicScheduleScroll.visibility = View.VISIBLE
        binding.academicWeekList.visibility = View.VISIBLE
        binding.academicWeekList.adapter = WeekAdapter(maxWeek, selectedWeek, ::selectWeek)
        binding.academicWeekList.scrollToPosition(selectedWeek - 1)
        renderWeek()
        updateTitle()
        if (calibrated == null) binding.root.post(::showWeekCalibration)
    }

    private fun selectWeek(week: Int) {
        if (week == selectedWeek) return
        selectedWeek = week
        (binding.academicWeekList.adapter as? WeekAdapter)?.select(week)
        renderWeek()
        updateTitle()
    }

    private fun renderWeek() {
        binding.academicScheduleGrid.submit(courses.filter { it.inWeek(selectedWeek) })
    }

    private fun updateTitle() {
        binding.academicToolbar.title = getString(R.string.academic_schedule_week, selectedWeek)
        binding.academicToolbar.subtitle = if (term.isEmpty()) null else term
    }

    private fun showWeekCalibration() {
        if (term.isEmpty() || courses.isEmpty()) return
        var week = selectedWeek
        val labels = Array(maxWeek) { getString(R.string.academic_week_label, it + 1) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.academic_schedule_calibrate_title)
            .setSingleChoiceItems(labels, selectedWeek - 1) { _, index -> week = index + 1 }
            .setPositiveButton(R.string.academic_action_confirm) { _, _ ->
                settings.calibrateWeek(term, System.currentTimeMillis(), week)
                selectWeek(week)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCourseDetail(course: Course) {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = AcademicSheetCourseBinding.inflate(layoutInflater)
        sheet.academicCourseName.text = course.name
        bindRow(sheet.academicCourseTeacher, course.teacher)
        bindRow(sheet.academicCourseRoom, course.room)
        sheet.academicCourseSections.text = getString(
            R.string.academic_schedule_sections,
            course.startSection,
            course.endSection,
        )
        bindRow(sheet.academicCourseWeeks, weeksLabel(course.weeks))
        dialog.setContentView(sheet.root)
        dialog.show()
    }

    private fun bindRow(view: TextView, value: String) {
        view.text = value
        view.visibility = if (value.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun weeksLabel(weeks: Set<Int>): String {
        if (weeks.isEmpty()) return ""
        val sorted = weeks.sorted()
        val ranges = mutableListOf<String>()
        var first = sorted.first()
        var previous = first
        for (index in 1 until sorted.size) {
            if (sorted[index] == previous + 1) {
                previous = sorted[index]
            } else {
                ranges.add(if (first == previous) "$first" else "$first-$previous")
                first = sorted[index]
                previous = first
            }
        }
        ranges.add(if (first == previous) "$first" else "$first-$previous")
        return getString(R.string.academic_schedule_weeks, ranges.joinToString(", "))
    }

    private fun showLoading() {
        binding.academicScheduleScroll.visibility = View.GONE
        binding.academicWeekList.visibility = View.GONE
        binding.academicCacheBanner.hide()
        binding.academicState.showLoading()
    }

    private fun showError(message: String) {
        binding.academicScheduleScroll.visibility = View.GONE
        binding.academicWeekList.visibility = View.GONE
        binding.academicState.showError(getString(R.string.academic_state_error, message), ::reload)
    }

    private fun showLoggedOut() {
        binding.academicScheduleScroll.visibility = View.GONE
        binding.academicWeekList.visibility = View.GONE
        binding.academicState.showLoggedOut(getString(R.string.academic_state_logged_out)) {
            root().authenticate(destination)
        }
    }

    private fun root(): AcademicRootFragment = parentFragment as AcademicRootFragment

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
