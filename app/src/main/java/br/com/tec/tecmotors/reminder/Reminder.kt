package br.com.tec.tecmotors.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.tec.tecmotors.MainActivity
import br.com.tec.tecmotors.R
import java.time.LocalDate
import java.util.Calendar

object ReminderScheduler {
    private const val CHANNEL_ID = "tec_motors_reminders"
    private const val ALARM_REQUEST_CODE = 42010

    fun initialize(context: Context) {
        createChannel(context)
        scheduleDailyCheck(context)
    }

    fun scheduleDailyCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = reminderPendingIntent(context)

        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }.timeInMillis

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun publishMonthlyReminderIfNeeded(context: Context, now: LocalDate = LocalDate.now()) {
        val isStartOfMonth = now.dayOfMonth == 1
        val isEndOfMonth = now.dayOfMonth == now.lengthOfMonth()
        if (!isStartOfMonth && !isEndOfMonth) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        createChannel(context)

        val title = if (isStartOfMonth) {
            context.getString(R.string.notif_title_month_start)
        } else {
            context.getString(R.string.notif_title_month_end)
        }

        val text = if (isStartOfMonth) {
            context.getString(R.string.notif_text_month_start)
        } else {
            context.getString(R.string.notif_text_month_end)
        }

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(now.toEpochDay().toInt(), notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun reminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ReminderScheduler.publishMonthlyReminderIfNeeded(context)
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.initialize(context)
        }
    }
}
