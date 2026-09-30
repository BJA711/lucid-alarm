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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.AlarmEntity
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun GlassDaySelector(
    repeatMask: Int,
    onRepeatMaskChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf(
        Pair("M", AlarmEntity.DAY_MON),
        Pair("T", AlarmEntity.DAY_TUE),
        Pair("W", AlarmEntity.DAY_WED),
        Pair("T", AlarmEntity.DAY_THU),
        Pair("F", AlarmEntity.DAY_FRI),
        Pair("S", AlarmEntity.DAY_SAT),
        Pair("S", AlarmEntity.DAY_SUN)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { (label, mask) ->
                val isSelected = (repeatMask and mask) != 0
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.06f),
                    label = "day_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF070B13) else TextSecondaryDark,
                    label = "day_text"
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = NeonCyan.copy(alpha = 0.3f)),
                            onClick = {
                                val newMask = if (isSelected) {
                                    repeatMask and mask.inv()
                                } else {
                                    repeatMask or mask
                                }
                                onRepeatMaskChanged(newMask)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetChip(
                text = "Once",
                isSelected = repeatMask == 0,
                onClick = { onRepeatMaskChanged(0) }
            )
            PresetChip(
                text = "Every day",
                isSelected = repeatMask == AlarmEntity.ALL_DAYS_MASK,
                onClick = { onRepeatMaskChanged(AlarmEntity.ALL_DAYS_MASK) }
            )
            PresetChip(
                text = "Weekdays",
                isSelected = repeatMask == AlarmEntity.WEEKDAYS_MASK,
                onClick = { onRepeatMaskChanged(AlarmEntity.WEEKDAYS_MASK) }
            )
            PresetChip(
                text = "Weekends",
                isSelected = repeatMask == AlarmEntity.WEEKENDS_MASK,
                onClick = { onRepeatMaskChanged(AlarmEntity.WEEKENDS_MASK) }
            )
        }
    }
}

@Composable
private fun PresetChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
        label = "preset_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.08f),
        label = "preset_border"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else TextSecondaryDark,
        label = "preset_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = NeonCyan.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
