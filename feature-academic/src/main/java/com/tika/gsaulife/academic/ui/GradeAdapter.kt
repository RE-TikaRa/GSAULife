package com.tika.gsaulife.academic.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.databinding.AcademicItemGradeBinding
import com.tika.gsaulife.academic.databinding.AcademicItemGradeOverallBinding
import com.tika.gsaulife.academic.databinding.AcademicItemGradeSectionBinding
import com.tika.gsaulife.academic.model.Grade
import com.tika.gsaulife.academic.model.GradeStats
import com.tika.gsaulife.academic.model.TermGrades
import com.tika.gsaulife.academic.model.groupByTerm
import java.util.Locale

internal class GradeAdapter(
    grades: List<Grade>,
    private val onGradeClick: (Grade) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private sealed interface Row {
        data class Overall(val stats: GradeStats) : Row
        data class Section(val term: TermGrades) : Row
        data class Item(val grade: Grade) : Row
    }

    private val rows = buildList {
        add(Row.Overall(GradeStats.of(grades)))
        for (term in groupByTerm(grades)) {
            add(Row.Section(term))
            term.grades.forEach { add(Row.Item(it)) }
        }
    }

    private class OverallHolder(val binding: AcademicItemGradeOverallBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class SectionHolder(val binding: AcademicItemGradeSectionBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class ItemHolder(val binding: AcademicItemGradeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Overall -> TYPE_OVERALL
        is Row.Section -> TYPE_SECTION
        is Row.Item -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_OVERALL -> OverallHolder(
                AcademicItemGradeOverallBinding.inflate(inflater, parent, false)
            )
            TYPE_SECTION -> SectionHolder(
                AcademicItemGradeSectionBinding.inflate(inflater, parent, false)
            )
            else -> ItemHolder(AcademicItemGradeBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Overall -> with((holder as OverallHolder).binding) {
                academicGradeGpa.text = format2(row.stats.avgGradePoint)
                academicGradeAverage.text = format1(row.stats.avgScore)
                academicGradeCredits.text = trim(row.stats.totalCredit)
                academicGradeCount.text = root.context.getString(
                    R.string.academic_number,
                    row.stats.courseCount,
                )
            }
            is Row.Section -> with((holder as SectionHolder).binding) {
                academicGradeTerm.text = row.term.term
                academicGradeSummary.text = root.context.getString(
                    R.string.academic_grades_term_summary,
                    format2(row.term.stats.avgGradePoint),
                    format1(row.term.stats.avgScore),
                    trim(row.term.stats.totalCredit),
                )
            }
            is Row.Item -> bindGrade((holder as ItemHolder).binding, row.grade)
        }
    }

    private fun bindGrade(binding: AcademicItemGradeBinding, grade: Grade) = with(binding) {
        academicGradeName.text = grade.courseName
        academicGradeDetail.text = buildString {
            append(grade.courseType)
            append(" · ")
            append(trim(grade.credit))
            append(root.context.getString(R.string.academic_credit_suffix))
            grade.gradePoint?.let {
                append(" · ")
                append(root.context.getString(R.string.academic_grade_point, it))
            }
        }
        academicGradeScore.text = grade.score
        academicGradeScore.setTextColor(
            ContextCompat.getColor(
                root.context,
                if (grade.passed) R.color.academic_score_pass else R.color.academic_score_fail,
            )
        )
        root.isClickable = grade.hasDetail
        root.setOnClickListener(if (grade.hasDetail) View.OnClickListener { onGradeClick(grade) } else null)
    }

    override fun getItemCount(): Int = rows.size

    private fun format1(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)
    private fun format2(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
    private fun trim(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    companion object {
        private const val TYPE_OVERALL = 0
        private const val TYPE_SECTION = 1
        private const val TYPE_ITEM = 2
    }
}
