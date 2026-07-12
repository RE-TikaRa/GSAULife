package com.tika.gsaulife.academic.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.SchoolSystem
import com.tika.gsaulife.academic.data.AcademicCache
import com.tika.gsaulife.academic.data.AcademicResult
import com.tika.gsaulife.academic.data.SchoolSessionStore
import com.tika.gsaulife.academic.databinding.AcademicFragmentListBinding
import com.tika.gsaulife.academic.databinding.AcademicItemScoreComponentBinding
import com.tika.gsaulife.academic.databinding.AcademicSheetScoreBinding
import com.tika.gsaulife.academic.model.Grade
import com.tika.gsaulife.academic.model.ScoreDetail
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

internal class GradesFragment : Fragment(), AcademicPage {
    override val destination = AcademicDestination.GRADES

    private var _binding: AcademicFragmentListBinding? = null
    private val binding get() = _binding!!
    private val repository by lazy { jwxtRepository(requireContext()) }
    private val cache by lazy { AcademicCache.get(requireContext()) }
    private val sessions by lazy { SchoolSessionStore.get(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AcademicFragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.academicToolbar.setTitle(R.string.academic_feature_grades)
        binding.academicToolbar.setNavigationOnClickListener { root().resetToMenu() }
        binding.academicToolbar.inflateMenu(R.menu.academic_menu_refresh)
        binding.academicToolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.academic_action_refresh) { reload(); true } else false
        }
        binding.academicList.layoutManager = LinearLayoutManager(requireContext())
        reload()
    }

    override fun reload() {
        if (_binding == null) return
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = repository.grades()) {
                is AcademicResult.Ok -> render(result.data)
                AcademicResult.LoggedOut -> {
                    val cached = cache.loadGrades()
                    if (cached == null) showLoggedOut()
                    else render(cached.data, cached.fetchedAt, true)
                }
                AcademicResult.Stale -> Unit
                is AcademicResult.Error -> {
                    val cached = cache.loadGrades()
                    if (cached == null) showError(result.message)
                    else render(cached.data, cached.fetchedAt, false)
                }
            }
        }
    }

    private fun render(grades: List<Grade>, fetchedAt: Long? = null, needsLogin: Boolean = false) {
        if (fetchedAt == null) binding.academicCacheBanner.hide()
        else binding.academicCacheBanner.show(fetchedAt, needsLogin) { root().authenticate(destination) }
        if (grades.isEmpty()) {
            binding.academicList.visibility = View.GONE
            binding.academicState.showEmpty(
                R.drawable.academic_ic_grades,
                getString(R.string.academic_grades_empty),
            )
            return
        }
        binding.academicState.hide()
        binding.academicList.visibility = View.VISIBLE
        binding.academicList.adapter = GradeAdapter(grades, ::showScoreDetail)
    }

    private fun showScoreDetail(grade: Grade) {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = AcademicSheetScoreBinding.inflate(layoutInflater)
        sheet.academicScoreName.text = grade.courseName
        sheet.academicScoreLoading.visibility = View.VISIBLE
        dialog.setContentView(sheet.root)
        dialog.show()
        viewLifecycleOwner.lifecycleScope.launch {
            val result = if (sessions.isLoggedIn(SchoolSystem.ACADEMIC)) {
                repository.scoreDetail(grade)
            } else {
                AcademicResult.LoggedOut
            }
            sheet.academicScoreLoading.visibility = View.GONE
            when (result) {
                is AcademicResult.Ok -> renderScoreDetail(sheet, result.data)
                AcademicResult.LoggedOut -> {
                    val cached = cache.loadScoreDetail(grade)
                    if (cached == null) showScoreMessage(sheet, getString(R.string.academic_state_logged_out))
                    else renderScoreDetail(sheet, cached.data, cached.fetchedAt)
                }
                AcademicResult.Stale -> dialog.dismiss()
                is AcademicResult.Error -> {
                    val cached = cache.loadScoreDetail(grade)
                    if (cached == null) showScoreMessage(sheet, result.message)
                    else renderScoreDetail(sheet, cached.data, cached.fetchedAt)
                }
            }
        }
    }

    private fun renderScoreDetail(
        sheet: AcademicSheetScoreBinding,
        detail: ScoreDetail,
        fetchedAt: Long? = null,
    ) {
        if (detail.components.isEmpty()) {
            showScoreMessage(sheet, getString(R.string.academic_grades_detail_empty))
            return
        }
        fetchedAt?.let {
            sheet.academicScoreTimestamp.visibility = View.VISIBLE
            sheet.academicScoreTimestamp.text = getString(
                R.string.academic_cached_at,
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)),
            )
        }
        detail.components.forEach { component ->
            val row = AcademicItemScoreComponentBinding.inflate(
                layoutInflater,
                sheet.academicScoreComponents,
                false,
            )
            row.academicScoreLabel.text = component.label
            row.academicScoreRatio.text = getString(R.string.academic_grades_detail_ratio, component.ratio)
            row.academicScoreValue.text = component.score
            sheet.academicScoreComponents.addView(row.root)
        }
        val total = AcademicItemScoreComponentBinding.inflate(
            layoutInflater,
            sheet.academicScoreComponents,
            false,
        )
        total.academicScoreLabel.setText(R.string.academic_grades_detail_total)
        total.academicScoreValue.text = detail.total
        sheet.academicScoreComponents.addView(total.root)
    }

    private fun showScoreMessage(sheet: AcademicSheetScoreBinding, message: String) {
        sheet.academicScoreMessage.visibility = View.VISIBLE
        sheet.academicScoreMessage.text = message
    }

    private fun showLoading() {
        binding.academicList.visibility = View.GONE
        binding.academicCacheBanner.hide()
        binding.academicState.showLoading()
    }

    private fun showError(message: String) {
        binding.academicList.visibility = View.GONE
        binding.academicState.showError(getString(R.string.academic_state_error, message), ::reload)
    }

    private fun showLoggedOut() {
        binding.academicList.visibility = View.GONE
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
