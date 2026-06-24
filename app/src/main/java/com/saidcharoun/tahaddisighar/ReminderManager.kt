package com.saidcharoun.tahaddisighar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * إشعار تذكير يومي يدعو الطفل لإكمال التحدّي اليومي والحفاظ على سلسلته.
 * يُجدول مرة واحدة (KEEP) ويتكرر كل 24 ساعة قرابة الساعة 7 مساءً.
 */
object ReminderManager {

    private const val CHANNEL_ID = "daily_reminder"
    private const val WORK_NAME = "daily_reminder_work"
    private const val REMINDER_HOUR = 19   // 7 مساءً

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "التذكير اليومي",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "تذكير بإكمال التحدّي اليومي" }
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(channel)
        }
    }

    fun scheduleDaily(context: Context) {
        ensureChannel(context)
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val initialDelay = next.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun showReminderNow(context: Context) {
        ensureChannel(context)
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("تحدّي المعلومات 🌟")
            .setContentText("تحدّيك اليومي بانتظارك! العب واكسب النقاط وحافظ على سلسلتك 🔥")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.notify(1001, notification)
        } catch (_: SecurityException) {
        }
    }
}

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        // لا نزعج المستخدم إن كان قد أكمل تحدّي اليوم
        val today = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        if (CoinManager.getDailyDate(applicationContext) != today) {
            ReminderManager.showReminderNow(applicationContext)
        }
        return Result.success()
    }
}
