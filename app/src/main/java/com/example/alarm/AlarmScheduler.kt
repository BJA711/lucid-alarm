package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.model.AlarmEntity
import com.example.receiver.AlarmReceiver

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancel(alarm.id)
            return
        }

        val triggerMillis = alarm.calculateNextTriggerMillis()
        scheduleExact(alarm.id, triggerMillis, alarm.label, alarm.ringtoneUri, alarm.isVibrate, alarm.snoozeDurationMinutes)
    }

    fun scheduleSnooze(
        alarmId: Long,
        snoozeMinutes: Int,
        label: String,
        ringtoneUri: String,
        vibrate: Boolean
    ) {
        val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        scheduleExact(alarmId, triggerMillis, "$label (Snoozed)", ringtoneUri, vibrate, snoozeMinutes)
    }

    private fun scheduleExact(
        alarmId: Long,
        triggerMillis: Long,
        label: String,
        ringtoneUri: String,
        vibrate: Boolean,
        snoozeMinutes: Int
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_ALARM_RINGTONE_URI, ringtoneUri)
            putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE, vibrate)
            putExtra(AlarmReceiver.EXTRA_ALARM_SNOOZE_MINUTES, snoozeMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show Intent for when tapping the alarm clock icon in lockscreen / status bar
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            (alarmId + 100000).toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
            Log.d("AlarmScheduler", "Alarm $alarmId scheduled for $triggerMillis ($label)")
        } catch (e: SecurityException) {
            Log.w("AlarmScheduler", "Exact alarm permission missing, falling back to setAndAllowWhileIdle", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } catch (fallbackError: Exception) {
                Log.e("AlarmScheduler", "Failed to schedule alarm $alarmId", fallbackError)
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error scheduling alarm $alarmId", e)
        }
    }

    fun cancel(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Alarm $alarmId cancelled")
        }
    }
}
