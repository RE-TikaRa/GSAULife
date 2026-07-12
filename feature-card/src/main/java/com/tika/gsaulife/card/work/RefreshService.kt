package com.tika.gsaulife.card.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tika.gsaulife.card.R
import com.tika.gsaulife.card.data.AccountStore
import com.tika.gsaulife.card.data.PayCodeManager
import com.tika.gsaulife.card.data.PayCodePolicy
import com.tika.gsaulife.card.widget.PayWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RefreshService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (AccountStore.get(this).current() == null) {
            PayWidgetProvider.refreshAll(this)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (refreshJob?.isActive != true) {
            refreshJob = scope.launch {
                while (isActive) {
                    val startedAt = SystemClock.elapsedRealtime()
                    runCatching { PayCodeManager.refreshCurrent(applicationContext) }
                    PayWidgetProvider.refreshAll(applicationContext)
                    val elapsed = SystemClock.elapsedRealtime() - startedAt
                    delay((PayCodePolicy.REFRESH_INTERVAL_MS - elapsed).coerceAtLeast(0L))
                }
            }
        }
        return START_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        refreshJob?.cancel()
        AccountStore.get(this).clearCachedCodes()
        PayWidgetProvider.refreshAll(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.card_notification_channel),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.card_notification_channel_description)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.card_widget_label))
            .setContentText(getString(R.string.card_notification_content))
            .setSmallIcon(R.drawable.card_ic_refresh_light)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
        packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            builder.setContentIntent(PendingIntent.getActivity(this, 10, launchIntent, flags))
        }
        return builder.build()
    }

    companion object {
        private const val TAG = "CardRefreshService"
        private const val CHANNEL_ID = "gsaulife_card_refresh"
        private const val NOTIFICATION_ID = 1101

        fun start(context: Context) {
            start(context, Intent(context, RefreshService::class.java))
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, RefreshService::class.java)) }
                .onFailure { Log.w(TAG, "Unable to stop card refresh service", it) }
        }

        private fun start(context: Context, intent: Intent) = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }.onFailure { Log.w(TAG, "Unable to start card refresh service", it) }
    }
}
