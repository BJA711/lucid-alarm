package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.service.AlarmRingingService
import com.example.ui.alarm.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        Log.d("AlarmReceiver", "Alarm fired with action: ${intent.action}")

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Wake Up"
        val ringtoneUri = intent.getStringExtra(EXTRA_ALARM_RINGTONE_URI) ?: ""
        val vibrate = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE, true)
        val snoozeMinutes = intent.getIntExtra(EXTRA_ALARM_SNOOZE_MINUTES, 10)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        // 1. Acquire partial wake lock for 15s to keep CPU running through service startup
        try {
            val cpuWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Alarms:AlarmReceiverCpuWakeLock"
            )
            cpuWakeLock.acquire(15000L)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to acquire CPU wake lock", e)
        }

        // 2. Acquire screen-on wake lock to illuminate display when phone screen is off
        try {
            @Suppress("DEPRECATION")
            val screenWakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "Alarms:AlarmReceiverScreenWakeLock"
            )
            screenWakeLock.acquire(10000L)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to acquire screen wake lock", e)
        }

        // 3. Start foreground ringing service
        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_START_ALARM
            putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingingService.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmRingingService.EXTRA_ALARM_RINGTONE_URI, ringtoneUri)
            putExtra(AlarmRingingService.EXTRA_ALARM_VIBRATE, vibrate)
            putExtra(AlarmRingingService.EXTRA_ALARM_SNOOZE_MINUTES, snoozeMinutes)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to start AlarmRingingService", e)
        }

        // 4. Directly launch AlarmActivity from BroadcastReceiver context
        try {
            val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmRingingService.EXTRA_ALARM_LABEL, label)
                putExtra(AlarmRingingService.EXTRA_ALARM_RINGTONE_URI, ringtoneUri)
                putExtra(AlarmRingingService.EXTRA_ALARM_VIBRATE, vibrate)
                putExtra(AlarmRingingService.EXTRA_ALARM_SNOOZE_MINUTES, snoozeMinutes)
            }
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Could not start AlarmActivity directly from receiver", e)
        }
    }

    companion object {
        const val ACTION_FIRE_ALARM = "com.example.action.FIRE_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_ALARM_RINGTONE_URI = "extra_alarm_ringtone_uri"
        const val EXTRA_ALARM_VIBRATE = "extra_alarm_vibrate"
        const val EXTRA_ALARM_SNOOZE_MINUTES = "extra_alarm_snooze_minutes"
    }
}
