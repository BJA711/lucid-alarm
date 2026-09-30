package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.util.Locale

@Composable
fun GlassTimePicker(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    onTimeChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    onOpenCircularPicker: (() -> Unit)? = null
) {
    val displayHour = if (is24Hour) {
        hour
    } else {
        when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
    }
    val isPm = hour >= 12

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        backgroundColor = Color.White.copy(alpha = 0.06f),
        borderColor = Color.White.copy(alpha = 0.18f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hour column
                TimeDigitColumn(
                    value = displayHour,
                    onIncrement = {
                        val newHour = if (is24Hour) {
                            (hour + 1) % 24
                        } else {
                            if (isPm) {
                                val h = (displayHour % 12) + 1
                                if (h == 12) 12 else h + 12
                            } else {
                                val h = (displayHour % 12) + 1
                                if (h == 12) 0 else h
                            }
                        }
                        onTimeChanged(newHour, minute)
                    },
                    onDecrement = {
                        val newHour = if (is24Hour) {
                            if (hour == 0) 23 else hour - 1
                        } else {
                            if (isPm) {
                                val h = if (displayHour == 1) 12 else displayHour - 1
                                if (h == 12) 12 else h + 12
                            } else {
                                val h = if (displayHour == 1) 12 else displayHour - 1
                                if (h == 12) 0 else h
                            }
                        }
                        onTimeChanged(newHour, minute)
                    },
                    onBoxClick = onOpenCircularPicker,
                    testTag = "hour_column"
                )

                // Colon separator
                Text(
                    text = ":",
                    color = NeonCyan,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )

                // Minute column
                TimeDigitColumn(
                    value = minute,
                    onIncrement = {
                        val newMin = (minute + 1) % 60
                        onTimeChanged(hour, newMin)
                    },
                    onDecrement = {
                        val newMin = if (minute == 0) 59 else minute - 1
                        onTimeChanged(hour, newMin)
                    },
                    onBoxClick = onOpenCircularPicker,
                    testTag = "minute_column"
                )

                // AM / PM Toggle if 12-hour
                if (!is24Hour) {
                    Spacer(modifier = Modifier.width(16.dp))
                    AmPmToggle(
                        isPm = isPm,
                        onToggle = { pm ->
                            val newHour = if (pm) {
                                if (hour < 12) hour + 12 else hour
                            } else {
                                if (hour >= 12) hour - 12 else hour
                            }
                            onTimeChanged(newHour, minute)
                        }
                    )
                }
            }

            if (onOpenCircularPicker != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.12f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = NeonCyan.copy(alpha = 0.3f)),
                            onClick = onOpenCircularPicker
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("open_circular_picker_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Circular Clock Face",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeDigitColumn(
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onBoxClick: (() -> Unit)? = null,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(testTag)
    ) {
        IconButton(
            onClick = onIncrement,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Increase",
                tint = NeonCyan,
                modifier = Modifier.size(32.dp)
            )
        }

        val boxModifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(18.dp)
            )
            .then(
                if (onBoxClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = NeonCyan.copy(alpha = 0.3f)),
                        onClick = onBoxClick
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)

        Box(
            modifier = boxModifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%02d", value),
                color = TextPrimaryDark,
                fontSize = 52.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.5).sp
            )
        }

        IconButton(
            onClick = onDecrement,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrease",
                tint = NeonCyan,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun AmPmToggle(
    isPm: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(16.dp)
            )
            .padding(4.dp)
    ) {
        AmPmPill(
            text = "AM",
            selected = !isPm,
            onClick = { onToggle(false) }
        )
        Spacer(modifier = Modifier.height(4.dp))
        AmPmPill(
            text = "PM",
            selected = isPm,
            onClick = { onToggle(true) }
        )
    }
}

@Composable
private fun AmPmPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) NeonCyan else Color.Transparent,
        label = "pill_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF090D16) else TextSecondaryDark,
        label = "pill_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = NeonCyan.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
