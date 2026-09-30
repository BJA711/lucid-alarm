package com.example.alarm

import com.example.data.model.AlarmEntity
import java.util.Calendar
import java.util.Locale

data class AlarmCountdown(
    val hours: Long,
    val minutes: Long,
    val isTomorrow: Boolean,
    val dayName: String?,
    val formattedRemaining: String,
    val formattedDetailed: String,
    val triggerMillis: Long,
    val remainingMillis: Long = (hours * 3600_000L) + (minutes * 60_000L)
)

object AlarmLogic {

    /**
     * Converts an angle in degrees (0..360, where 0 is at 12 o'clock) to an hour (1..12).
     */
    fun calculateHourFromAngle(angleDegrees: Float): Int {
        val normalized = (angleDegrees % 360f + 360f) % 360f
        val hour = Math.round(normalized / 30f) % 12
        return if (hour == 0) 12 else hour
    }

    /**
     * Converts an angle in degrees (0..360, where 0 is at 12 o'clock) to a minute (0..59).
     */
    fun calculateMinuteFromAngle(angleDegrees: Float): Int {
        val normalized = (angleDegrees % 360f + 360f) % 360f
        return Math.round(normalized / 6f) % 60
    }

    /**
     * Converts a 12-hour value (1..12) and AM/PM flag to 24-hour (0..23).
     */
    fun to24Hour(displayHour: Int, isPm: Boolean): Int {
        val normalized = if (displayHour == 12) 0 else displayHour
        return if (isPm) normalized + 12 else normalized
    }

    /**
     * Converts a 24-hour value (0..23) to 12-hour display value (1..12) and isPm boolean.
     */
    fun to12Hour(hour24: Int): Pair<Int, Boolean> {
        val isPm = hour24 >= 12
        val displayHour = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return Pair(displayHour, isPm)
    }

    /**
     * Computes the exact countdown time remaining until an alarm set for [hour]:[minute] rings.
     */
    fun calculateCountdown(
        hour: Int,
        minute: Int,
        repeatDays: Int = 0,
        nowMillis: Long = System.currentTimeMillis()
    ): AlarmCountdown {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var dayName: String? = null
        var isTomorrow = false

        val triggerMillis = if (repeatDays == 0) {
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
                isTomorrow = true
            }
            target.timeInMillis
        } else {
            // Find next matching day in repeat days mask
            var foundMillis = target.timeInMillis
            var found = false
            for (i in 0..7) {
                val candidate = (target.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, i)
                }
                val mask = when (candidate.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> AlarmEntity.DAY_MON
                    Calendar.TUESDAY -> AlarmEntity.DAY_TUE
                    Calendar.WEDNESDAY -> AlarmEntity.DAY_WED
                    Calendar.THURSDAY -> AlarmEntity.DAY_THU
                    Calendar.FRIDAY -> AlarmEntity.DAY_FRI
                    Calendar.SATURDAY -> AlarmEntity.DAY_SAT
                    Calendar.SUNDAY -> AlarmEntity.DAY_SUN
                    else -> 0
                }

                if ((repeatDays and mask) != 0 && candidate.timeInMillis > now.timeInMillis) {
                    foundMillis = candidate.timeInMillis
                    isTomorrow = i == 1
                    if (i > 0) {
                        dayName = candidate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                    }
                    found = true
                    break
                }
            }
            if (!found) {
                target.add(Calendar.DAY_OF_YEAR, 1)
                isTomorrow = true
                target.timeInMillis
            } else {
                foundMillis
            }
        }

        val diffMs = maxOf(0L, triggerMillis - nowMillis)
        // Convert to minutes, rounded up so 0m 45s is treated as 1m
        val totalMinutes = (diffMs + 59_999L) / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val formattedRemaining = when {
            hours > 0 && minutes > 0 -> "in ${hours}h ${minutes}m"
            hours > 0 -> "in ${hours}h"
            minutes > 0 -> "in ${minutes}m"
            else -> "in less than 1m"
        }

        val relativeDay = when {
            dayName != null -> "on $dayName"
            isTomorrow -> "Tomorrow"
            else -> "Today"
        }

        val detailedText = when {
            hours > 0 && minutes > 0 -> "Rings in $hours hr $minutes min ($relativeDay)"
            hours > 0 -> "Rings in $hours hr ($relativeDay)"
            minutes > 0 -> "Rings in $minutes min ($relativeDay)"
            else -> "Rings in less than a minute ($relativeDay)"
        }

        return AlarmCountdown(
            hours = hours,
            minutes = minutes,
            isTomorrow = isTomorrow,
            dayName = dayName,
            formattedRemaining = formattedRemaining,
            formattedDetailed = detailedText,
            triggerMillis = triggerMillis
        )
    }

    /**
     * Creates a quick nap alarm for [durationMinutes] into the future.
     */
    fun createQuickNapAlarm(durationMinutes: Int, label: String = "Power Nap"): AlarmEntity {
        val now = Calendar.getInstance().apply {
            add(Calendar.MINUTE, durationMinutes)
        }
        return AlarmEntity(
            hour = now.get(Calendar.HOUR_OF_DAY),
            minute = now.get(Calendar.MINUTE),
            isEnabled = true,
            label = label,
            repeatDays = 0,
            ringtoneName = "Gentle Chime",
            ringtoneUri = "builtin_gentle",
            isVibrate = true,
            snoozeDurationMinutes = 5
        )
    }
}
