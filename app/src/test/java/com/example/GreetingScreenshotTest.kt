package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.HotPlatform
import com.example.ui.screens.HeaderSection
import com.example.ui.screens.PlatformsBar
import com.example.ui.screens.SearchSection
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
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Column(modifier = Modifier.fillMaxSize()) {
          HeaderSection(
            activePlatform = HotPlatform.WEIBO,
            onRefresh = {},
            rotationAngle = 0f
          )
          PlatformsBar(
            activePlatform = HotPlatform.WEIBO,
            onSelected = {}
          )
          SearchSection(
            query = "测试过滤",
            onQueryChanged = {},
            platform = HotPlatform.WEIBO
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
