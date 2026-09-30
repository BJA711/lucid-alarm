package com.example.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AlarmApp
import com.example.alarm.AlarmCountdown
import com.example.alarm.AlarmLogic
import com.example.data.model.AlarmEntity
import com.example.data.repository.AlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlarmUiState(
    val alarms: List<AlarmEntity> = emptyList(),
    val nextAlarm: AlarmEntity? = null,
    val timeUntilNextFormatted: String? = null,
    val nextAlarmDetailed: String? = null,
    val canScheduleExactAlarms: Boolean = true
)

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository = (application as AlarmApp).repository
    private val _canScheduleExact = MutableStateFlow(repository.canScheduleExactAlarms())

    val uiState: StateFlow<AlarmUiState> = combine(
        repository.allAlarms,
        _canScheduleExact
    ) { alarms, canExact ->
        val enabledAlarms = alarms.filter { it.isEnabled }
        val next = enabledAlarms.minByOrNull { it.calculateNextTriggerMillis() }
        val countdown = next?.let {
            AlarmLogic.calculateCountdown(it.hour, it.minute, it.repeatDays)
        }

        AlarmUiState(
            alarms = alarms,
            nextAlarm = next,
            timeUntilNextFormatted = countdown?.let { "Upcoming ${it.formattedRemaining}" },
            nextAlarmDetailed = countdown?.formattedDetailed,
            canScheduleExactAlarms = canExact
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlarmUiState()
    )

    fun refreshPermissions() {
        _canScheduleExact.value = repository.canScheduleExactAlarms()
    }

    fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(alarm, isEnabled)
        }
    }

    fun saveAlarm(alarm: AlarmEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveAlarm(alarm)
            onComplete()
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    /**
     * Quickly schedules an alarm from the circular time picker dialog.
     */
    fun scheduleQuickAlarm(
        hour: Int,
        minute: Int,
        label: String = "Alarm",
        onComplete: (AlarmCountdown) -> Unit = {}
    ) {
        viewModelScope.launch {
            val countdown = AlarmLogic.calculateCountdown(hour, minute)
            val newAlarm = AlarmEntity(
                hour = hour,
                minute = minute,
                isEnabled = true,
                label = label.ifBlank { "Alarm" },
                repeatDays = 0,
                ringtoneName = "Gentle Chime",
                ringtoneUri = "builtin_gentle",
                isVibrate = true,
                snoozeDurationMinutes = 10
            )
            repository.saveAlarm(newAlarm)
            onComplete(countdown)
        }
    }

    /**
     * Quickly schedules a power nap alarm for the specified duration in minutes.
     */
    fun scheduleQuickNap(
        durationMinutes: Int,
        label: String = "Power Nap",
        onComplete: (AlarmCountdown) -> Unit = {}
    ) {
        viewModelScope.launch {
            val alarm = AlarmLogic.createQuickNapAlarm(durationMinutes, label)
            val countdown = AlarmLogic.calculateCountdown(alarm.hour, alarm.minute)
            repository.saveAlarm(alarm)
            onComplete(countdown)
        }
    }
}
