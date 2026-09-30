package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.model.AlarmEntity
import com.example.ui.screens.AlarmItemCard
import com.example.ui.theme.AlarmClockTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun alarm_card_screenshot() {
        val testAlarm = AlarmEntity(
            id = 1L,
            hour = 7,
            minute = 0,
            isEnabled = true,
            label = "Wake Up",
            repeatDays = AlarmEntity.WEEKDAYS_MASK
        )

        composeTestRule.setContent {
            AlarmClockTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    AlarmItemCard(
                        alarm = testAlarm,
                        onToggle = {},
                        onClick = {},
                        onDelete = {}
                    )
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/alarm_card.png")
    }
}
