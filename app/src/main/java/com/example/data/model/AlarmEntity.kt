package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Locale

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val label: String = "Wake Up",
    val repeatDays: Int = 0, // Bitmask: 1=Mon, 2=Tue, 4=Wed, 8=Thu, 16=Fri, 32=Sat, 64=Sun. 0 = Once
    val ringtoneName: String = "System Default",
    val ringtoneUri: String = "",
    val isVibrate: Boolean = true,
    val snoozeDurationMinutes: Int = 10
) {
    companion object {
        const val DAY_MON = 1 shl 0
        const val DAY_TUE = 1 shl 1
        const val DAY_WED = 1 shl 2
        const val DAY_THU = 1 shl 3
        const val DAY_FRI = 1 shl 4
        const val DAY_SAT = 1 shl 5
        const val DAY_SUN = 1 shl 6

        val ALL_DAYS_MASK = DAY_MON or DAY_TUE or DAY_WED or DAY_THU or DAY_FRI or DAY_SAT or DAY_SUN
        val WEEKDAYS_MASK = DAY_MON or DAY_TUE or DAY_WED or DAY_THU or DAY_FRI
        val WEEKENDS_MASK = DAY_SAT or DAY_SUN
    }

    val isRepeating: Boolean
        get() = repeatDays > 0

    fun isDaySelected(mask: Int): Boolean {
        return (repeatDays and mask) != 0
    }

    fun getFormattedTime(is24Hour: Boolean = false): String {
        return if (is24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        } else {
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format(Locale.getDefault(), "%d:%02d", displayHour, minute)
        }
    }

    fun getAmPm(): String {
        return if (hour >= 12) "PM" else "AM"
    }

    fun getRepeatSummary(): String {
        return when (repeatDays) {
            0 -> "Once"
            ALL_DAYS_MASK -> "Every day"
            WEEKDAYS_MASK -> "Weekdays"
            WEEKENDS_MASK -> "Weekends"
            else -> {
                val days = mutableListOf<String>()
                if (isDaySelected(DAY_MON)) days.add("Mon")
                if (isDaySelected(DAY_TUE)) days.add("Tue")
                if (isDaySelected(DAY_WED)) days.add("Wed")
                if (isDaySelected(DAY_THU)) days.add("Thu")
                if (isDaySelected(DAY_FRI)) days.add("Fri")
                if (isDaySelected(DAY_SAT)) days.add("Sat")
                if (isDaySelected(DAY_SUN)) days.add("Sun")
                days.joinToString(", ")
            }
        }
    }

    /**
     * Calculates the exact next epoch millis timestamp when this alarm should trigger.
     */
    fun calculateNextTriggerMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!isRepeating) {
            // One-time alarm
            if (target.timeInMillis <= now.timeInMillis) {
                // Time has passed for today, set for tomorrow
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Repeating alarm: find the next day that is active
        for (i in 0..7) {
            val checkCalendar = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            val checkMillis = checkCalendar.timeInMillis

            // Calendar.DAY_OF_WEEK: SUNDAY=1, MONDAY=2, ... SATURDAY=7
            val mask = when (checkCalendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> DAY_MON
                Calendar.TUESDAY -> DAY_TUE
                Calendar.WEDNESDAY -> DAY_WED
                Calendar.THURSDAY -> DAY_THU
                Calendar.FRIDAY -> DAY_FRI
                Calendar.SATURDAY -> DAY_SAT
                Calendar.SUNDAY -> DAY_SUN
                else -> 0
            }

            if (isDaySelected(mask) && checkMillis > now.timeInMillis) {
                return checkMillis
            }
        }

        // Fallback
        return target.timeInMillis + 24 * 3600 * 1000
    }
}
