package com.example.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AlarmApp
import com.example.R
import com.example.ui.alarm.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRingingService : Service() {

    private lateinit var soundPlayer: AlarmSoundPlayer
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var currentAlarmId: Long = -1L
    private var currentLabel: String = "Wake Up"
    private var currentRingtoneUri: String = ""
    private var currentVibrate: Boolean = true
    private var currentSnoozeMinutes: Int = 10

    override fun onCreate() {
        super.onCreate()
        soundPlayer = AlarmSoundPlayer(this)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Acquire service-level wake lock to keep CPU active during ringing
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        try {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Alarms:AlarmRingingServiceWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes max ringing timeout
            }
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Failed to acquire service wake lock", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }

        val action = intent.action
        when (action) {
            ACTION_START_ALARM -> {
                currentAlarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                currentLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Wake Up"
                currentRingtoneUri = intent.getStringExtra(EXTRA_ALARM_RINGTONE_URI) ?: ""
                currentVibrate = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE, true)
                currentSnoozeMinutes = intent.getIntExtra(EXTRA_ALARM_SNOOZE_MINUTES, 10)

                startRinging()
            }
            ACTION_STOP_ALARM -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, currentAlarmId)
                stopRinging()
                CoroutineScope(Dispatchers.IO).launch {
                    AlarmApp.instance.repository.handleAlarmDismissed(alarmId)
                }
                stopSelf()
            }
            ACTION_SNOOZE_ALARM -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, currentAlarmId)
                val snoozeMin = intent.getIntExtra(EXTRA_ALARM_SNOOZE_MINUTES, currentSnoozeMinutes)
                stopRinging()
                AlarmApp.instance.repository.scheduleSnooze(
                    alarmId = alarmId,
                    snoozeMinutes = snoozeMin,
                    label = currentLabel,
                    ringtoneUri = currentRingtoneUri,
                    vibrate = currentVibrate
                )
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startRinging() {
        // Build Full Screen Intent pointing to AlarmActivity
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ALARM_ID, currentAlarmId)
            putExtra(EXTRA_ALARM_LABEL, currentLabel)
            putExtra(EXTRA_ALARM_RINGTONE_URI, currentRingtoneUri)
            putExtra(EXTRA_ALARM_VIBRATE, currentVibrate)
            putExtra(EXTRA_ALARM_SNOOZE_MINUTES, currentSnoozeMinutes)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, AlarmApp.ALARM_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentLabel)
            .setContentText("Alarm is ringing • Tap to solve")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)

        val notification = notificationBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start Audio
        soundPlayer.startPlaying(currentRingtoneUri)

        // Start Vibration
        if (currentVibrate) {
            startVibration()
        }

        // Launch full-screen activity directly
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.e("AlarmRingingService", "Could not startActivity from service directly", e)
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 800, 500, 800, 500, 800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, 0)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopRinging() {
        soundPlayer.stopPlaying()
        vibrator?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (e: Exception) {
                Log.e("AlarmRingingService", "Error releasing service wake lock", e)
            }
        }
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 2001
        const val ACTION_START_ALARM = "com.example.action.START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.action.STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.action.SNOOZE_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_ALARM_RINGTONE_URI = "extra_alarm_ringtone_uri"
        const val EXTRA_ALARM_VIBRATE = "extra_alarm_vibrate"
        const val EXTRA_ALARM_SNOOZE_MINUTES = "extra_alarm_snooze_minutes"
    }
}
