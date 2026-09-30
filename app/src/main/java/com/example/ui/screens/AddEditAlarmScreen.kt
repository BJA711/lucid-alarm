package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmEntity
import com.example.alarm.AlarmLogic
import com.example.service.AlarmSoundPlayer
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassCircularTimePickerDialog
import com.example.ui.components.GlassDaySelector
import com.example.ui.components.GlassScaffoldBackground
import com.example.ui.components.GlassTimePicker
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.util.Calendar

@Composable
fun AddEditAlarmScreen(
    initialAlarm: AlarmEntity?,
    onSave: (AlarmEntity) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val now = Calendar.getInstance()
    var hour by remember { mutableIntStateOf(initialAlarm?.hour ?: now.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: now.get(Calendar.MINUTE)) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "Wake Up") }
    var repeatDays by remember { mutableIntStateOf(initialAlarm?.repeatDays ?: 0) }
    var ringtoneName by remember { mutableStateOf(initialAlarm?.ringtoneName ?: "Gentle Chime") }
    var ringtoneUri by remember { mutableStateOf(initialAlarm?.ringtoneUri ?: "builtin_gentle") }
    var isVibrate by remember { mutableStateOf(initialAlarm?.isVibrate ?: true) }
    var snoozeMinutes by remember { mutableIntStateOf(initialAlarm?.snoozeDurationMinutes ?: 10) }
    var showCircularPicker by remember { mutableStateOf(false) }

    // Sound preview player
    val soundPlayer = remember { AlarmSoundPlayer(context) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.stopPlaying()
        }
    }

    val availableRingtones = listOf(
        Pair("Gentle Chime", "builtin_gentle"),
        Pair("Digital Pulse", "builtin_digital"),
        Pair("Classic Bell", "builtin_bell"),
        Pair("System Default", "")
    )

    val snoozeOptions = listOf(5, 10, 15, 20)

    GlassScaffoldBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top navigation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        soundPlayer.stopPlaying()
                        onCancel()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimaryDark
                    )
                }

                Text(
                    text = if (initialAlarm == null) "New Alarm" else "Edit Alarm",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                )

                // Save button
                GlassButton(
                    text = "Save",
                    onClick = {
                        soundPlayer.stopPlaying()
                        val updated = AlarmEntity(
                            id = initialAlarm?.id ?: 0L,
                            hour = hour,
                            minute = minute,
                            isEnabled = true,
                            label = label.ifBlank { "Alarm" },
                            repeatDays = repeatDays,
                            ringtoneName = ringtoneName,
                            ringtoneUri = ringtoneUri,
                            isVibrate = isVibrate,
                            snoozeDurationMinutes = snoozeMinutes
                        )
                        onSave(updated)
                    },
                    isPrimary = true,
                    modifier = Modifier.height(40.dp),
                    testTag = "save_alarm_button"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time Picker
                GlassTimePicker(
                    hour = hour,
                    minute = minute,
                    is24Hour = false,
                    onTimeChanged = { h, m ->
                        hour = h
                        minute = m
                    },
                    onOpenCircularPicker = {
                        showCircularPicker = true
                    }
                )

                // Live Alarm Countdown Card
                val liveCountdown = remember(hour, minute, repeatDays) {
                    AlarmLogic.calculateCountdown(hour, minute, repeatDays)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NeonCyan.copy(alpha = 0.08f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = liveCountdown.formattedDetailed,
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Label Input Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Alarm Label",
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            placeholder = { Text("Alarm label (e.g. Wake Up)", color = TextSecondaryDark) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                cursorColor = NeonCyan
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("alarm_label_input")
                        )
                    }
                }

                // Repeat Days Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Repeat",
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GlassDaySelector(
                            repeatMask = repeatDays,
                            onRepeatMaskChanged = { repeatDays = it }
                        )
                    }
                }

                // Sound Selection Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alarm Sound",
                                color = TextSecondaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // Preview sound toggle button
                            IconButton(
                                onClick = {
                                    if (isPlayingPreview) {
                                        soundPlayer.stopPlaying()
                                        isPlayingPreview = false
                                    } else {
                                        soundPlayer.startPlaying(ringtoneUri)
                                        isPlayingPreview = true
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Preview Sound",
                                    tint = NeonCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Ringtone list
                        availableRingtones.forEach { (name, uri) ->
                            val isSelected = ringtoneName == name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent
                                    )
                                    .clickable {
                                        ringtoneName = name
                                        ringtoneUri = uri
                                        if (isPlayingPreview) {
                                            soundPlayer.startPlaying(uri)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isSelected) NeonCyan else TextSecondaryDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = name,
                                        color = if (isSelected) TextPrimaryDark else TextSecondaryDark,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Selected",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Vibration Toggle Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Vibration",
                                    color = TextPrimaryDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Vibrate when alarm triggers",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = isVibrate,
                            onCheckedChange = { isVibrate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF080D1A),
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextSecondaryDark,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }

                // Snooze Duration Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Snooze Duration",
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            snoozeOptions.forEach { min ->
                                val isSelected = snoozeMinutes == min
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) NeonCyan else Color.White.copy(alpha = 0.06f)
                                        )
                                        .clickable { snoozeMinutes = min }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${min}m",
                                        color = if (isSelected) Color(0xFF080D1A) else TextSecondaryDark,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        if (showCircularPicker) {
            GlassCircularTimePickerDialog(
                initialHour = hour,
                initialMinute = minute,
                onDismissRequest = { showCircularPicker = false },
                onTimeConfirmed = { newHour, newMin ->
                    hour = newHour
                    minute = newMin
                    showCircularPicker = false
                }
            )
        }
    }
}
