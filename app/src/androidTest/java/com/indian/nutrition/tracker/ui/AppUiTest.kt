package com.indian.nutrition.tracker.ui

import android.content.Context
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.indian.nutrition.tracker.MainActivity
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests. Test tags mirror the web app element ids
 * (e.g. `food-search-input`, `save-and-use-targets-btn`).
 * The device app database/data store is wiped once before the class so tests
 * start from web defaults (82 kg / 1950 kcal / 2750 ml).
 */
@RunWith(AndroidJUnit4::class)
class AppUiTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun wipeAppData() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase("inw.db")
            context.deleteDatabase("inw_settings.preferences_pb")
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String, timeout: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis = timeout) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String, timeout: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis = timeout) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun homeRendersAndWaterQuickAddWorks() {
        waitForTag("today-intake-card")
        waitForTag("home-weight-summary-card")

        composeRule.onNodeWithTag("water-add-250-btn").performScrollTo().performClick()
        composeRule.onNodeWithText("250 / ", substring = true).assertExists()

        composeRule.onNodeWithTag("toggle-water-history-btn").performClick()
        composeRule.onNodeWithText("250 ml", substring = true).assertExists()
    }

    @Test
    fun weightSheetLogsAndUpdatesSummary() {
        composeRule.onNodeWithTag("home-log-weight-btn").performScrollTo().performClick()
        waitForTag("log-weight-modal-dialog")
        composeRule.onNodeWithTag("weight-log-val-input").performTextReplacement("80")
        composeRule.onNodeWithTag("confirm-log-weight-btn").performClick()
        waitForText("80.0 kg")
    }

    @Test
    fun addFoodOpensSearchAndCanCreateCustomRecipe() {
        composeRule.onNodeWithTag("home-quick-add-food-btn").performScrollTo().performClick()
        waitForTag("food-search-input")
        composeRule.onNodeWithTag("tab-search-database").assertExists()
        composeRule.onNodeWithTag("tab-frequent-foods").assertExists()
        composeRule.onNodeWithTag("tab-custom-foods").performClick()

        composeRule.onNodeWithTag("open-create-custom-food-btn").performClick()
        waitForTag("custom-food-modal-dialog")
        composeRule.onNodeWithTag("custom-food-name-input").performScrollTo().performTextInput("Test Poha")
        composeRule.onNodeWithTag("custom-food-kcal-input").performScrollTo().performTextInput("260")
        composeRule.onNodeWithTag("custom-food-protein-input").performScrollTo().performTextInput("6")
        composeRule.onNodeWithTag("save-custom-food-submit-btn").performClick()
        waitForText("Test Poha")
    }

    @Test
    fun calculatorRendersProfileAndSavesTargets() {
        composeRule.onNodeWithText("Calculator").performClick()
        waitForTag("profile-current-weight")
        composeRule.onNodeWithTag("profile-target-weight").assertExists()
        composeRule.onNodeWithTag("unit-system-kg-btn").assertExists()
        composeRule.onNodeWithTag("target-mode-predefined-btn").performScrollTo()

        composeRule.onNodeWithTag("save-and-use-targets-btn").performScrollTo().performClick()
        waitForText("Saved & applied to dashboard!")
    }
}
