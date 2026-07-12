package com.tika.gsaulife.academic.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import com.tika.gsaulife.academic.R
import com.tika.gsaulife.academic.databinding.AcademicViewStateBinding

internal class AcademicStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val binding = AcademicViewStateBinding.inflate(LayoutInflater.from(context), this)

    fun showLoading() {
        visibility = View.VISIBLE
        binding.academicStateProgress.visibility = View.VISIBLE
        binding.academicStateContent.visibility = View.GONE
    }

    fun showEmpty(@DrawableRes icon: Int, message: String) {
        bind(icon, message)
        binding.academicStateAction.visibility = View.GONE
    }

    fun showError(message: String, retry: () -> Unit) {
        bind(R.drawable.academic_ic_error, message)
        binding.academicStateAction.visibility = View.VISIBLE
        binding.academicStateAction.setText(R.string.academic_action_retry)
        binding.academicStateAction.setIconResource(R.drawable.academic_ic_refresh)
        binding.academicStateAction.setOnClickListener { retry() }
    }

    fun showLoggedOut(message: String, login: () -> Unit) {
        bind(R.drawable.academic_ic_error, message)
        binding.academicStateAction.visibility = View.VISIBLE
        binding.academicStateAction.setText(R.string.academic_action_login)
        binding.academicStateAction.setIconResource(R.drawable.academic_ic_login)
        binding.academicStateAction.setOnClickListener { login() }
    }

    fun hide() {
        visibility = View.GONE
    }

    private fun bind(@DrawableRes icon: Int, message: String) {
        visibility = View.VISIBLE
        binding.academicStateProgress.visibility = View.GONE
        binding.academicStateContent.visibility = View.VISIBLE
        binding.academicStateIcon.setImageResource(icon)
        binding.academicStateMessage.text = message
    }
}
