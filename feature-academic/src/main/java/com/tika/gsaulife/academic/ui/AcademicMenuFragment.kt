package com.tika.gsaulife.academic.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tika.gsaulife.academic.databinding.AcademicFragmentMenuBinding

internal class AcademicMenuFragment : Fragment() {
    private var _binding: AcademicFragmentMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AcademicFragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.academicCardGrades.setOnClickListener { open(AcademicDestination.GRADES) }
        binding.academicCardSchedule.setOnClickListener { open(AcademicDestination.SCHEDULE) }
        binding.academicCardExams.setOnClickListener { open(AcademicDestination.EXAMS) }
        binding.academicCardRankings.setOnClickListener { open(AcademicDestination.RANKINGS) }
    }

    private fun open(destination: AcademicDestination) {
        (parentFragment as AcademicRootFragment).open(destination)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
