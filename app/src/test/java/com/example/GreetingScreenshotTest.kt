package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.ui.MafiaScreen
import com.example.ui.MafiaViewModel
import com.example.ui.theme.MyApplicationTheme
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun mafia_app_screenshot() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = GameDatabase.getDatabase(context)
    val repository = GameRepository(database.matchDao())
    val viewModel = MafiaViewModel(repository)

    composeTestRule.setContent {
      MyApplicationTheme {
        MafiaScreen(viewModel = viewModel)
      }
    }

    // Verify app title exists
    composeTestRule.onNodeWithTag("app_title").assertExists()

    // Take screenshot
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
