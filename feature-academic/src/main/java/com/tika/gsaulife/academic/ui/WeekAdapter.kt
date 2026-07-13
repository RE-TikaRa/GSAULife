package com.tika.gsaulife.academic.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.databinding.AcademicItemWeekBinding

internal class WeekAdapter(
    private val maxWeek: Int,
    private var selectedWeek: Int,
    private val onWeekSelected: (Int) -> Unit,
) : RecyclerView.Adapter<WeekAdapter.Holder>() {
    class Holder(val binding: AcademicItemWeekBinding) : RecyclerView.ViewHolder(binding.root)

    fun select(week: Int) {
        val previous = selectedWeek
        selectedWeek = week
        notifyItemChanged(previous - 1)
        notifyItemChanged(week - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        AcademicItemWeekBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
        val week = position + 1
        academicWeekLabel.text = root.context.getString(R.string.academic_number, week)
        academicWeekLabel.contentDescription = root.context.getString(
            R.string.academic_schedule_week_accessibility,
            week,
        )
        academicWeekLabel.isSelected = week == selectedWeek
        root.setOnClickListener { onWeekSelected(week) }
    }

    override fun getItemCount(): Int = maxWeek
}
