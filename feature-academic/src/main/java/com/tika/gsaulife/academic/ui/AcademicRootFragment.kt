package com.tika.gsaulife.academic.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.data.SchoolSessionStore

class AcademicRootFragment : Fragment(R.layout.academic_fragment_root) {
    private val sessions by lazy { SchoolSessionStore.get(requireContext()) }
    private var pendingDestination: AcademicDestination? = null
    private var backCallback: OnBackPressedCallback? = null
    private val backStackListener =
        FragmentManager.OnBackStackChangedListener {
            backCallback?.isEnabled = childFragmentManager.backStackEntryCount > 0
        }

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val destination = pendingDestination
        pendingDestination = null
        if (result.resultCode != android.app.Activity.RESULT_OK || destination == null) return@registerForActivityResult
        val current = childFragmentManager.findFragmentById(R.id.academic_content)
        if (current is AcademicPage && current.destination == destination) current.reload()
        else showPage(destination)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDestination = savedInstanceState?.getString(STATE_PENDING)
            ?.let(AcademicDestination::valueOf)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (childFragmentManager.fragments.isEmpty()) showMenu()
        backCallback = object : OnBackPressedCallback(
            childFragmentManager.backStackEntryCount > 0
        ) {
            override fun handleOnBackPressed() {
                resetToMenu()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backCallback!!,
        )
        childFragmentManager.addOnBackStackChangedListener(backStackListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingDestination?.let { outState.putString(STATE_PENDING, it.name) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        childFragmentManager.removeOnBackStackChangedListener(backStackListener)
        backCallback = null
        super.onDestroyView()
    }

    fun resetToMenu() {
        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        } else if (childFragmentManager.findFragmentById(R.id.academic_content) !is AcademicMenuFragment) {
            showMenu()
        }
    }

    internal fun open(destination: AcademicDestination) {
        if (sessions.isLoggedIn(destination.system)) showPage(destination)
        else authenticate(destination)
    }

    internal fun authenticate(destination: AcademicDestination) {
        pendingDestination = destination
        loginLauncher.launch(LoginActivity.intent(requireContext(), destination.system))
    }

    private fun showMenu() {
        childFragmentManager.commit {
            replace(R.id.academic_content, AcademicMenuFragment())
        }
    }

    private fun showPage(destination: AcademicDestination) {
        val fragment = when (destination) {
            AcademicDestination.GRADES -> GradesFragment()
            AcademicDestination.SCHEDULE -> ScheduleFragment()
            AcademicDestination.EXAMS -> ExamsFragment()
            AcademicDestination.RANKINGS -> RankingsFragment()
        }
        childFragmentManager.commit {
            replace(R.id.academic_content, fragment)
            addToBackStack(destination.name)
        }
    }

    companion object {
        private const val STATE_PENDING = "academic.pending_destination"
    }
}
