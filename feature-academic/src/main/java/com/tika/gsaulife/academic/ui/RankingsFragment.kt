package com.tika.gsaulife.academic.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.data.AcademicCache
import com.tika.gsaulife.academic.data.AcademicResult
import com.tika.gsaulife.academic.databinding.AcademicFragmentListBinding
import com.tika.gsaulife.academic.model.Ranking
import kotlinx.coroutines.launch

internal class RankingsFragment : Fragment(), AcademicPage {
    override val destination = AcademicDestination.RANKINGS

    private var _binding: AcademicFragmentListBinding? = null
    private val binding get() = _binding!!
    private val repository by lazy { xgfwRepository(requireContext()) }
    private val cache by lazy { AcademicCache.get(requireContext()) }

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
        binding.academicToolbar.setTitle(R.string.academic_feature_rankings)
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
            when (val result = repository.rankings()) {
                is AcademicResult.Ok -> render(result.data)
                AcademicResult.LoggedOut -> {
                    val cached = cache.loadRankings()
                    if (cached == null) showLoggedOut()
                    else render(cached.data, cached.fetchedAt, true)
                }
                AcademicResult.Stale -> Unit
                is AcademicResult.Error -> {
                    val cached = cache.loadRankings()
                    if (cached == null) showError(result.message)
                    else render(cached.data, cached.fetchedAt, false)
                }
            }
        }
    }

    private fun render(
        rankings: List<Ranking>,
        fetchedAt: Long? = null,
        needsLogin: Boolean = false,
    ) {
        if (fetchedAt == null) binding.academicCacheBanner.hide()
        else binding.academicCacheBanner.show(fetchedAt, needsLogin) { root().authenticate(destination) }
        if (rankings.isEmpty()) {
            binding.academicList.visibility = View.GONE
            binding.academicState.showEmpty(
                R.drawable.academic_ic_rankings,
                getString(R.string.academic_rankings_empty),
            )
            return
        }
        binding.academicState.hide()
        binding.academicList.visibility = View.VISIBLE
        binding.academicList.adapter = RankingAdapter(rankings)
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
