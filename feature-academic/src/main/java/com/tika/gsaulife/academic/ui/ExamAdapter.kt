package com.tika.gsaulife.academic.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.databinding.AcademicItemExamBinding
import com.tika.gsaulife.academic.model.Exam

internal class ExamAdapter(private val exams: List<Exam>) :
    RecyclerView.Adapter<ExamAdapter.Holder>() {
    class Holder(val binding: AcademicItemExamBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        AcademicItemExamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
        val exam = exams[position]
        academicExamName.text = exam.courseName
        academicExamIndex.text = root.context.getString(R.string.academic_exam_index, position + 1)
        academicExamTime.text = exam.time
        academicExamLocation.text = exam.location
        academicExamLocation.visibility = if (exam.location.isEmpty()) View.GONE else View.VISIBLE
        academicExamSeat.text = exam.seat
        academicExamSeat.visibility = if (exam.seat.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = exams.size
}
