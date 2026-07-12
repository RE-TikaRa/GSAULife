package com.tika.gsaulife.card.ui

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tika.gsaulife.card.R

internal object CardDialogs {
    fun input(
        context: Context,
        title: CharSequence,
        message: CharSequence,
        hint: CharSequence,
        positiveText: CharSequence,
        initial: CharSequence = "",
        onPositive: (String) -> Unit
    ) {
        val input = EditText(context).apply {
            this.hint = hint
            setText(initial)
            setSelection(text.length)
            maxLines = 3
        }
        val padding = (20 * context.resources.displayMetrics.density).toInt()
        val container = FrameLayout(context).apply {
            setPadding(padding, 0, padding, 0)
            addView(
                input,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setView(container)
            .setNegativeButton(R.string.card_cancel, null)
            .setPositiveButton(positiveText) { _, _ -> onPositive(input.text.toString()) }
            .show()
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
