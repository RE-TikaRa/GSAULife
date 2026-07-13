package com.tika.gsaulife.card.work

import android.content.Context
import android.provider.Settings
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tika.gsaulife.card.LegalAgreementStore
import com.tika.gsaulife.card.data.AccountStore
import com.tika.gsaulife.card.data.PayCodeManager
import com.tika.gsaulife.card.widget.PayWidgetProvider

internal class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (inputData.getInt(KEY_BOOT_COUNT, -1) != bootCount(applicationContext)) {
            return Result.success()
        }
        if (LegalAgreementStore.isAccepted(applicationContext)) {
            AccountStore.get(applicationContext).current()?.let {
                PayCodeManager.refresh(applicationContext, it)
            }
        }
        PayWidgetProvider.refreshAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val KEY_BOOT_COUNT = "boot_count"
        private const val UNIQUE_WORK_NAME = "card_widget_refresh"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInputData(workDataOf(KEY_BOOT_COUNT to bootCount(context)))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun bootCount(context: Context): Int = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.BOOT_COUNT,
            -1
        )
    }
}
