package com.tika.gsaulife.card.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.tika.gsaulife.card.widget.PayWidgetProvider

internal object WidgetExpiry {
    const val ACTION = "com.tika.gsaulife.card.WIDGET_EXPIRE"

    fun schedule(context: Context, expiresAt: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val delay = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val triggerAt = SystemClock.elapsedRealtime() + delay
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                pendingIntent(context)
            )
        } else {
            manager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                pendingIntent(context)
            )
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, PayWidgetProvider::class.java).setAction(ACTION)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, 14, intent, flags)
    }
}
