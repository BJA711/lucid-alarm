package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.service.AlarmRingingService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        Log.d("AlarmReceiver", "Alarm fired with action: ${intent.action}")

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Wake Up"
        val ringtoneUri = intent.getStringExtra(EXTRA_ALARM_RINGTONE_URI) ?: ""
        val vibrate = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE, true)
        val snoozeMinutes = intent.getIntExtra(EXTRA_ALARM_SNOOZE_MINUTES, 10)

        // Acquire a temporary wake lock to ensure the CPU remains active while starting the service
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Alarms:AlarmReceiverWakeLock"
        )
        wakeLock.acquire(10000L) // 10 seconds max

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
        } finally {
            if (wakeLock.isHeld) {
                try {
                    wakeLock.release()
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error releasing wakelock", e)
                }
            }
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
