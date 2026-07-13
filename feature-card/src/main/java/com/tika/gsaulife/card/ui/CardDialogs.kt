package com.tika.gsaulife.card.ui

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tika.gsaulife.card.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal object CardDialogs {
    fun input(
        context: Context,
        scope: CoroutineScope,
        title: CharSequence,
        message: CharSequence,
        hint: CharSequence,
        positiveText: CharSequence,
        progressText: CharSequence = positiveText,
        initial: CharSequence = "",
        onPositive: suspend (String) -> CharSequence?
    ) {
        val input = EditText(context).apply {
            this.hint = hint
            setText(initial)
            setSelection(text.length)
            maxLines = 3
        }
        val density = context.resources.displayMetrics.density
        val errorView = TextView(context).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setPadding(0, (4 * density).toInt(), 0, 0)
            setTextColor(ContextCompat.getColor(context, R.color.card_danger))
            textSize = 12f
            visibility = View.GONE
        }
        val padding = (20 * density).toInt()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 0, padding, 0)
            addView(
                input,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                errorView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setView(container)
            .setNegativeButton(R.string.card_cancel, null)
            .setPositiveButton(positiveText, null)
            .create()
        var job: Job? = null
        dialog.setOnShowListener {
            val positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positive.setOnClickListener {
                errorView.visibility = View.GONE
                input.isEnabled = false
                positive.isEnabled = false
                positive.text = progressText
                job = scope.launch {
                    try {
                        val error = onPositive(input.text.toString())
                        if (!dialog.isShowing) return@launch
                        if (error == null) {
                            dialog.dismiss()
                        } else {
                            errorView.text = error
                            errorView.visibility = View.VISIBLE
                            input.requestFocus()
                        }
                    } catch (exception: CancellationException) {
                        if (dialog.isShowing) dialog.dismiss()
                        throw exception
                    } catch (_: Exception) {
                        if (dialog.isShowing) {
                            errorView.setText(R.string.card_action_failed)
                            errorView.visibility = View.VISIBLE
                        }
                    } finally {
                        if (dialog.isShowing) {
                            input.isEnabled = true
                            positive.isEnabled = true
                            positive.text = positiveText
                        }
                    }
                }
            }
        }
        dialog.setOnDismissListener { job?.cancel() }
        dialog.show()
    }

    fun menu(
        context: Context,
        title: CharSequence,
        labels: Array<CharSequence>,
        onSelected: (Int) -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setItems(labels) { _, index -> onSelected(index) }
            .show()
    }

    fun confirm(
        context: Context,
        title: CharSequence,
        message: CharSequence,
        positiveText: CharSequence,
        onPositive: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.card_cancel, null)
            .setPositiveButton(positiveText) { _, _ -> onPositive() }
            .show()
    }

    fun notice(anchor: View, message: CharSequence) {
        Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT).show()
    }
}
