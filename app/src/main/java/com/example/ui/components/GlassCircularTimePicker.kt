package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.alarm.AlarmLogic
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.TextTertiaryDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ClockPickerMode {
    HOURS,
    MINUTES
}

/**
 * Minimalist circular clock face interface matching the glassmorphic aesthetic.
 */
@Composable
fun MinimalistCircularClockFace(
    mode: ClockPickerMode,
    selectedHour: Int, // 1..12
    selectedMinute: Int, // 0..59
    onHourSelected: (Int) -> Unit,
    onMinuteSelected: (Int) -> Unit,
    onHourConfirmed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Target hand angle based on current mode and selection
    val targetAngleDeg = remember(mode, selectedHour, selectedMinute) {
        if (mode == ClockPickerMode.HOURS) {
            val h = if (selectedHour == 12) 0 else selectedHour
            (h * 30f)
        } else {
            (selectedMinute * 6f)
        }
    }

    val animatedAngleDeg by animateFloatAsState(
        targetValue = targetAngleDeg,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "hand_angle"
    )

    // Paints for Canvas native drawing
    val textPaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 15.sp.toPx() }
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    val selectedTextPaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 16.sp.toPx() }
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.WHITE
        }
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("circular_clock_face"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .pointerInput(mode) {
                    val center = Offset(size.width / 2f, size.height / 2f)

                    fun handleTouch(position: Offset, isRelease: Boolean = false) {
                        val dx = position.x - center.x
                        val dy = position.y - center.y
                        val rad = atan2(dy.toDouble(), dx.toDouble())
                        // Convert from -PI..PI (0 at right) to 0..360 (0 at top)
                        var deg = Math.toDegrees(rad).toFloat() + 90f
                        if (deg < 0) deg += 360f

                        if (mode == ClockPickerMode.HOURS) {
                            val rawH = (deg / 30f).roundToInt() % 12
                            val hourVal = if (rawH == 0) 12 else rawH
                            onHourSelected(hourVal)
                            if (isRelease) {
                                coroutineScope.launch {
                                    delay(180)
                                    onHourConfirmed()
                                }
                            }
                        } else {
                            val minVal = (deg / 6f).roundToInt() % 60
                            onMinuteSelected(minVal)
                        }
                    }

                    detectTapGestures(
                        onPress = { offset ->
                            handleTouch(offset, isRelease = true)
                        }
                    )
                }
                .pointerInput(mode) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val dx = change.position.x - center.x
                            val dy = change.position.y - center.y
                            var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                            if (deg < 0) deg += 360f

                            if (mode == ClockPickerMode.HOURS) {
                                val rawH = (deg / 30f).roundToInt() % 12
                                val hourVal = if (rawH == 0) 12 else rawH
                                onHourSelected(hourVal)
                            } else {
                                val minVal = (deg / 6f).roundToInt() % 60
                                onMinuteSelected(minVal)
                            }
                        },
                        onDragEnd = {
                            if (mode == ClockPickerMode.HOURS) {
                                coroutineScope.launch {
                                    delay(180)
                                    onHourConfirmed()
                                }
                            }
                        }
                    )
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f
            val numbersRadius = radius * 0.76f

            // 1. Frosted dial background gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonIndigo.copy(alpha = 0.15f),
                        NeonCyan.copy(alpha = 0.05f),
                        Color(0xFF0F172A).copy(alpha = 0.60f)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // 2. Outer frosted border
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Subtle tick marks around perimeter
            if (mode == ClockPickerMode.HOURS) {
                for (i in 0 until 12) {
                    val angleRad = (i * 30.0 - 90.0) * (PI / 180.0)
                    val tickOuter = radius - 4.dp.toPx()
                    val tickInner = radius - 10.dp.toPx()
                    val start = Offset(
                        (center.x + tickInner * cos(angleRad)).toFloat(),
                        (center.y + tickInner * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        (center.x + tickOuter * cos(angleRad)).toFloat(),
                        (center.y + tickOuter * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.20f),
                        start = start,
                        end = end,
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            } else {
                for (i in 0 until 60) {
                    val angleRad = (i * 6.0 - 90.0) * (PI / 180.0)
                    val isMajor = i % 5 == 0
                    val tickOuter = radius - 4.dp.toPx()
                    val tickInner = if (isMajor) radius - 10.dp.toPx() else radius - 7.dp.toPx()
                    val start = Offset(
                        (center.x + tickInner * cos(angleRad)).toFloat(),
                        (center.y + tickInner * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        (center.x + tickOuter * cos(angleRad)).toFloat(),
                        (center.y + tickOuter * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = if (isMajor) NeonCyan.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.12f),
                        start = start,
                        end = end,
                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }
            }

            // 4. Selector hand and thumb
            val angleRad = (animatedAngleDeg - 90.0) * (PI / 180.0)
            val thumbCenter = Offset(
                (center.x + numbersRadius * cos(angleRad)).toFloat(),
                (center.y + numbersRadius * sin(angleRad)).toFloat()
            )

            // Selector hand line
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(NeonIndigo.copy(alpha = 0.8f), NeonCyan),
                    start = center,
                    end = thumbCenter
                ),
                start = center,
                end = thumbCenter,
                strokeWidth = 2.5.dp.toPx()
            )

            // Center pivot dot
            drawCircle(
                color = NeonCyan,
                radius = 5.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = center
            )

            // Glowing thumb disc behind selected number
            val thumbRadius = 18.dp.toPx()
            // Ambient outer glow
            drawCircle(
                color = NeonCyan.copy(alpha = 0.25f),
                radius = thumbRadius + 4.dp.toPx(),
                center = thumbCenter
            )
            // Thumb surface
            drawCircle(
                color = NeonCyan,
                radius = thumbRadius,
                center = thumbCenter
            )

            // 5. Draw Clock Numbers
            val itemsCount = if (mode == ClockPickerMode.HOURS) 12 else 12
            val unselectedColor = TextSecondaryDark.toArgb()
            val textOffsetY = (textPaint.descent() + textPaint.ascent()) / 2f

            for (i in 1..itemsCount) {
                val value = if (mode == ClockPickerMode.HOURS) {
                    i
                } else {
                    (i * 5) % 60
                }
                val label = if (mode == ClockPickerMode.HOURS) {
                    value.toString()
                } else {
                    String.format(Locale.getDefault(), "%02d", value)
                }

                val itemAngleRad = if (mode == ClockPickerMode.HOURS) {
                    (i * 30.0 - 90.0) * (PI / 180.0)
                } else {
                    ((value / 5) * 30.0 - 90.0) * (PI / 180.0)
                }

                val itemPos = Offset(
                    (center.x + numbersRadius * cos(itemAngleRad)).toFloat(),
                    (center.y + numbersRadius * sin(itemAngleRad)).toFloat()
                )

                val isSelected = if (mode == ClockPickerMode.HOURS) {
                    value == selectedHour
                } else {
                    value == selectedMinute
                }

                if (isSelected) {
                    // Draw in dark high-contrast over cyan thumb
                    selectedTextPaint.color = android.graphics.Color.parseColor("#090D16")
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        itemPos.x,
                        itemPos.y - textOffsetY,
                        selectedTextPaint
                    )
                } else {
                    textPaint.color = unselectedColor
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        itemPos.x,
                        itemPos.y - textOffsetY,
                        textPaint
                    )
                }
            }

            // In minute mode, if a minute that is not a multiple of 5 is selected, draw its number in thumb
            if (mode == ClockPickerMode.MINUTES && selectedMinute % 5 != 0) {
                val minuteLabel = String.format(Locale.getDefault(), "%02d", selectedMinute)
                selectedTextPaint.color = android.graphics.Color.parseColor("#090D16")
                drawContext.canvas.nativeCanvas.drawText(
                    minuteLabel,
                    thumbCenter.x,
                    thumbCenter.y - textOffsetY,
                    selectedTextPaint
                )
            }
        }
    }
}

/**
 * Full custom circular time-picking dialog matching the app's glassmorphism aesthetic.
 */
@Composable
fun GlassCircularTimePickerDialog(
    initialHour: Int, // 0..23
    initialMinute: Int, // 0..59
    onDismissRequest: () -> Unit,
    onTimeConfirmed: (hour24: Int, minute: Int) -> Unit
) {
    val (startDisplayHour, startIsPm) = remember(initialHour) {
        AlarmLogic.to12Hour(initialHour)
    }

    var displayHour by remember { mutableIntStateOf(startDisplayHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    var isPm by remember { mutableStateOf(startIsPm) }
    var currentMode by remember { mutableStateOf(ClockPickerMode.HOURS) }

    // Real-time alarm countdown logic
    val current24Hour = remember(displayHour, isPm) {
        AlarmLogic.to24Hour(displayHour, isPm)
    }
    val countdown = remember(current24Hour, minute) {
        AlarmLogic.calculateCountdown(current24Hour, minute)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xE60D1322))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.24f),
                            NeonCyan.copy(alpha = 0.30f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(24.dp)
                .testTag("circular_time_picker_dialog"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header with title and icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set Alarm Time",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time Display & Mode Selector Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hour Box
                    TimeDigitBox(
                        value = String.format(Locale.getDefault(), "%02d", displayHour),
                        isSelected = currentMode == ClockPickerMode.HOURS,
                        onClick = { currentMode = ClockPickerMode.HOURS },
                        testTag = "dialog_hour_box"
                    )

                    Text(
                        text = ":",
                        color = NeonCyan,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    // Minute Box
                    TimeDigitBox(
                        value = String.format(Locale.getDefault(), "%02d", minute),
                        isSelected = currentMode == ClockPickerMode.MINUTES,
                        onClick = { currentMode = ClockPickerMode.MINUTES },
                        testTag = "dialog_minute_box"
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // AM / PM Segmented Selector
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(3.dp)
                    ) {
                        AmPmPillSmall(
                            label = "AM",
                            isSelected = !isPm,
                            onClick = { isPm = false },
                            testTag = "dialog_am_button"
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        AmPmPillSmall(
                            label = "PM",
                            isSelected = isPm,
                            onClick = { isPm = true },
                            testTag = "dialog_pm_button"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live dynamic countdown chip showing real-time alarm logic
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.10f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = countdown.formattedDetailed,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Minimalist Circular Clock Face
                MinimalistCircularClockFace(
                    mode = currentMode,
                    selectedHour = displayHour,
                    selectedMinute = minute,
                    onHourSelected = { displayHour = it },
                    onMinuteSelected = { minute = it },
                    onHourConfirmed = {
                        currentMode = ClockPickerMode.MINUTES
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick preset adjustment chips (+15m, +30m, +1h)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    QuickAdjustChip(text = "+15m") {
                        val newTotal = minute + 15
                        minute = newTotal % 60
                        if (newTotal >= 60) {
                            displayHour = (displayHour % 12) + 1
                        }
                    }
                    QuickAdjustChip(text = "+30m") {
                        val newTotal = minute + 30
                        minute = newTotal % 60
                        if (newTotal >= 60) {
                            displayHour = (displayHour % 12) + 1
                        }
                    }
                    QuickAdjustChip(text = "+1h") {
                        displayHour = (displayHour % 12) + 1
                    }
                    QuickAdjustChip(text = "7:00 AM") {
                        displayHour = 7
                        minute = 0
                        isPm = false
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = onDismissRequest,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        testTag = "dialog_cancel_button"
                    )

                    GlassButton(
                        text = "Set Time",
                        onClick = {
                            val computed24 = AlarmLogic.to24Hour(displayHour, isPm)
                            onTimeConfirmed(computed24, minute)
                        },
                        isPrimary = true,
                        modifier = Modifier.weight(1.2f),
                        testTag = "dialog_confirm_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeDigitBox(
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.14f),
        label = "box_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        label = "box_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else TextPrimaryDark,
        label = "box_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = NeonCyan.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            color = textColor,
            fontSize = 42.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
private fun AmPmPillSmall(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else Color.Transparent,
        label = "pill_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF090D16) else TextSecondaryDark,
        label = "pill_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = NeonCyan.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun QuickAdjustChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = NeonCyan.copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
