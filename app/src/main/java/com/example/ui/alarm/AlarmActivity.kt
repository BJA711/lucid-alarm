package com.example.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.MathQuestion
import com.example.alarm.QuestionGenerator
import com.example.service.AlarmRingingService
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassNumberPad
import com.example.ui.components.GlassScaffoldBackground
import com.example.ui.theme.AlarmClockTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class AlarmActivity : ComponentActivity() {

    private var alarmId: Long = -1L
    private var alarmLabel: String = "Wake Up"
    private var snoozeMinutes: Int = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn screen on and show over keyguard
        setupWindowFlags()

        alarmId = intent.getLongExtra(AlarmRingingService.EXTRA_ALARM_ID, -1L)
        alarmLabel = intent.getStringExtra(AlarmRingingService.EXTRA_ALARM_LABEL) ?: "Wake Up"
        snoozeMinutes = intent.getIntExtra(AlarmRingingService.EXTRA_ALARM_SNOOZE_MINUTES, 10)

        // Prevent back button from silently escaping while alarm is ringing
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Keep active - answer required
            }
        })

        setContent {
            AlarmClockTheme {
                AlarmRingingContent(
                    label = alarmLabel,
                    snoozeMinutes = snoozeMinutes,
                    onDismissSuccess = {
                        stopAlarmService()
                        finish()
                    },
                    onSnoozeSuccess = {
                        snoozeAlarmService()
                        finish()
                    }
                )
            }
        }
    }

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun stopAlarmService() {
        val stopIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_STOP_ALARM
            putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
        }
        startService(stopIntent)
    }

    private fun snoozeAlarmService() {
        val snoozeIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_SNOOZE_ALARM
            putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingingService.EXTRA_ALARM_SNOOZE_MINUTES, snoozeMinutes)
        }
        startService(snoozeIntent)
    }
}

@Composable
fun AlarmRingingContent(
    label: String,
    snoozeMinutes: Int,
    onDismissSuccess: () -> Unit,
    onSnoozeSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Live clock string
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
            delay(1000L)
        }
    }

    // Hidden adaptive session difficulty level (1..5+)
    var sessionDifficultyLevel by remember { mutableIntStateOf(1) }
    var currentQuestion by remember {
        mutableStateOf(QuestionGenerator.generateQuestion(sessionDifficultyLevel))
    }

    var userAnswerText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSolvedSuccess by remember { mutableStateOf(false) }

    // Shake animation for incorrect answers
    val shakeOffset = remember { Animatable(0f) }

    // Subtle pulsing animation on the ringing icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    fun triggerShake() {
        coroutineScope.launch {
            errorMessage = "Incorrect, check your calculation"
            repeat(3) {
                shakeOffset.animateTo(-16f, tween(50))
                shakeOffset.animateTo(16f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
            userAnswerText = ""
        }
    }

    fun requestNewQuestion() {
        // Silently escalate difficulty level upon user requesting another question
        sessionDifficultyLevel = (sessionDifficultyLevel + 1).coerceAtMost(5)
        currentQuestion = QuestionGenerator.generateQuestion(sessionDifficultyLevel)
        userAnswerText = ""
        errorMessage = null
    }

    fun verifyAndExecute(onSuccess: () -> Unit) {
        val parsedAnswer = userAnswerText.toIntOrNull()
        if (parsedAnswer != null && parsedAnswer == currentQuestion.answer) {
            isSolvedSuccess = true
            coroutineScope.launch {
                delay(400L)
                onSuccess()
            }
        } else {
            // Silently advance session difficulty after failure
            sessionDifficultyLevel = (sessionDifficultyLevel + 1).coerceAtMost(5)
            triggerShake()
            // Generate next question at higher difficulty to prevent brute forcing
            currentQuestion = QuestionGenerator.generateQuestion(sessionDifficultyLevel)
        }
    }

    GlassScaffoldBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Pulsing Alarm Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.15f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Time Display
            Text(
                text = currentTimeString.ifBlank { "7:00" },
                fontSize = 62.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimaryDark,
                letterSpacing = (-1.0).sp
            )

            // Alarm Label
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondaryDark,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Glass Question Panel
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                shape = RoundedCornerShape(28.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f),
                borderColor = if (errorMessage != null) NeonRose.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.20f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Arithmetic expression with clear spacing: e.g. "27 + 48 × 3 = ?"
                    Text(
                        text = currentQuestion.expression,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("question_expression")
                    )

                    // User Answer Display Field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(
                                1.5.dp,
                                if (userAnswerText.isNotBlank()) NeonCyan else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userAnswerText.isEmpty()) "Enter answer" else userAnswerText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (userAnswerText.isEmpty()) TextSecondaryDark.copy(alpha = 0.5f) else TextPrimaryDark,
                            letterSpacing = 1.sp,
                            modifier = Modifier.testTag("user_answer_display")
                        )
                    }

                    // Subtle error message feedback
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = NeonRose,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Glass Keypad for entering digits
                    GlassNumberPad(
                        onDigitClick = { digit ->
                            if (userAnswerText.length < 8) {
                                userAnswerText += digit
                                errorMessage = null
                            }
                        },
                        onBackspaceClick = {
                            if (userAnswerText.isNotEmpty()) {
                                userAnswerText = userAnswerText.dropLast(1)
                                errorMessage = null
                            }
                        },
                        onClearClick = {
                            userAnswerText = ""
                            errorMessage = null
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Button
                    GlassButton(
                        text = if (isSolvedSuccess) "Verified" else "Submit",
                        onClick = { verifyAndExecute(onDismissSuccess) },
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = true,
                        icon = if (isSolvedSuccess) Icons.Default.Check else null,
                        testTag = "submit_answer_button"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary actions: "New Question" & "Reset"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Reset input action
                        Text(
                            text = "Reset",
                            color = TextSecondaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    userAnswerText = ""
                                    errorMessage = null
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("reset_question_action")
                        )

                        // New question action
                        Text(
                            text = "New Question",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { requestNewQuestion() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("new_question_action")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Snooze Action (Requires solving the question as instructed)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable {
                        // Prompt requirements:
                        // "When the alarm is ringing, the snooze action should also require successfully answering the current arithmetic question."
                        if (userAnswerText.isBlank()) {
                            errorMessage = "Answer question to snooze"
                            triggerShake()
                        } else {
                            verifyAndExecute(onSnoozeSuccess)
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Snooze,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Snooze ($snoozeMinutes min)",
                    color = TextSecondaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
