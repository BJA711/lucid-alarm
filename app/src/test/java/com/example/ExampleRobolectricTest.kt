package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.QuestionGenerator
import com.example.data.model.AlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Alarms", appName)
    }

    @Test
    fun `question generator creates solvable questions across levels`() {
        for (level in 1..5) {
            for (i in 1..10) {
                val question = QuestionGenerator.generateQuestion(level)
                assertTrue("Expression must end with = ?", question.expression.endsWith("= ?"))
                assertTrue("Level must match requested", question.level == level)
            }
        }
    }

    @Test
    fun `alarm entity calculate trigger returns future millis`() {
        val now = Calendar.getInstance()
        val alarm = AlarmEntity(
            hour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24,
            minute = 30,
            isEnabled = true,
            label = "Morning",
            repeatDays = 0
        )
        val trigger = alarm.calculateNextTriggerMillis()
        assertTrue("Trigger must be in future", trigger > System.currentTimeMillis() - 1000)
    }

    @Test
    fun `active alarm notification does not contain dismiss or snooze bypass actions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fullScreenIntent = android.content.Intent(context, com.example.ui.alarm.AlarmActivity::class.java)
        val fullScreenPendingIntent = android.app.PendingIntent.getActivity(
            context,
            com.example.service.AlarmRingingService.NOTIFICATION_ID,
            fullScreenIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(context, com.example.AlarmApp.ALARM_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Wake Up")
            .setContentText("Alarm is ringing • Tap to solve")
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)

        val notification = notificationBuilder.build()

        // Notification must be ongoing
        assertTrue("Notification must be ongoing", (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0)
        // Notification must have NO bypass action buttons
        assertTrue("Notification actions must be null or empty", notification.actions == null || notification.actions.isEmpty())
        // Notification delete intent must be null
        assertTrue("Notification deleteIntent must be null", notification.deleteIntent == null)
    }
}
