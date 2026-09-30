package com.example.data.repository

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.data.local.AlarmDao
import com.example.data.model.AlarmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmRepository(
    private val context: Context,
    private val alarmDao: AlarmDao
) {
    private val scheduler = AlarmScheduler(context)

    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExactAlarms()

    suspend fun getAlarmById(id: Long): AlarmEntity? = withContext(Dispatchers.IO) {
        alarmDao.getAlarmById(id)
    }

    suspend fun saveAlarm(alarm: AlarmEntity): Long = withContext(Dispatchers.IO) {
        val id = if (alarm.id == 0L) {
            alarmDao.insertAlarm(alarm)
        } else {
            alarmDao.updateAlarm(alarm)
            alarm.id
        }

        val savedAlarm = alarm.copy(id = id)
        if (savedAlarm.isEnabled) {
            scheduler.schedule(savedAlarm)
        } else {
            scheduler.cancel(savedAlarm.id)
        }
        id
    }

    suspend fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        val updated = alarm.copy(isEnabled = isEnabled)
        alarmDao.updateAlarm(updated)
        if (isEnabled) {
            scheduler.schedule(updated)
        } else {
            scheduler.cancel(updated.id)
        }
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) = withContext(Dispatchers.IO) {
        scheduler.cancel(alarm.id)
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun rescheduleAllEnabled() = withContext(Dispatchers.IO) {
        val enabledAlarms = alarmDao.getEnabledAlarms()
        for (alarm in enabledAlarms) {
            scheduler.schedule(alarm)
        }
    }

    suspend fun handleAlarmDismissed(alarmId: Long) = withContext(Dispatchers.IO) {
        val alarm = alarmDao.getAlarmById(alarmId) ?: return@withContext
        if (!alarm.isRepeating) {
            // One-time alarm completed, turn it off
            val updated = alarm.copy(isEnabled = false)
            alarmDao.updateAlarm(updated)
        } else {
            // Repeating alarm: schedule next occurrence
            scheduler.schedule(alarm)
        }
    }

    fun scheduleSnooze(
        alarmId: Long,
        snoozeMinutes: Int,
        label: String,
        ringtoneUri: String,
        vibrate: Boolean
    ) {
        scheduler.scheduleSnooze(alarmId, snoozeMinutes, label, ringtoneUri, vibrate)
    }
}
