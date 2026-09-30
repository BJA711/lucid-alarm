package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.data.local.AlarmDatabase
import com.example.data.repository.AlarmRepository

class AlarmApp : Application() {

    lateinit var database: AlarmDatabase
        private set

    lateinit var repository: AlarmRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AlarmDatabase.getInstance(this)
        repository = AlarmRepository(this, database.alarmDao())

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // High-priority full-screen alarm channel
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming alarm triggers and full-screen ringing alerts"
                setBypassDnd(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
                setSound(
                    Settings.System.DEFAULT_ALARM_ALERT_URI,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // General notification channel
            val infoChannel = NotificationChannel(
                INFO_CHANNEL_ID,
                "Upcoming Alarms & Info",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status and upcoming alarm notifications"
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(infoChannel)
        }
    }

    companion object {
        const val ALARM_CHANNEL_ID = "alarm_ringing_channel"
        const val INFO_CHANNEL_ID = "alarm_info_channel"

        lateinit var instance: AlarmApp
            private set
    }
}
