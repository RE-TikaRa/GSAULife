package com.tika.gsaulife.academic.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.databinding.AcademicItemRankingBinding
import com.tika.gsaulife.academic.model.Ranking

internal class RankingAdapter(private val rankings: List<Ranking>) :
    RecyclerView.Adapter<RankingAdapter.Holder>() {
    class Holder(val binding: AcademicItemRankingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        AcademicItemRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
        val ranking = rankings[position]
        academicRankingName.text = ranking.gradeLabel
        academicRankingYear.text = ranking.yearDisplay
        academicRankingMajor.text = root.context.getString(
            R.string.academic_ranking_major,
            ranking.majorRank,
            ranking.majorTotal,
        )
        academicRankingClass.text = root.context.getString(
            R.string.academic_ranking_class,
            ranking.classRank,
            ranking.classTotal,
        )
    }

    override fun getItemCount(): Int = rankings.size
}
