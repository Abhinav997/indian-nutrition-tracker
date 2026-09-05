package com.indian.nutrition.tracker.ui

import android.content.Context
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
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Compose UI tests. Test tags mirror the web app element ids
 * (e.g. `food-search-input`, `save-and-use-targets-btn`).
 *
 * The app database and DataStore are wiped before each test run so tests
 * start from web defaults (82 kg / 1950 kcal / 2750 ml).
 */
@RunWith(AndroidJUnit4::class)
class AppUiTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun wipeAppData() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            // Delete Room database
            context.deleteDatabase("inw.db")
            // Delete DataStore preferences file (not a SQLite database!)
            val dsFile = File(context.filesDir, "datastore/inw_settings.preferences_pb")
            if (dsFile.exists()) dsFile.delete()
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun ensureHomeScreen() {
        // Wait for the home screen to fully render before each test.
        // Each test gets a fresh Activity, so we must wait for initial load.
        waitForTag("bottom-navigation-bar")
        waitForTag("today-intake-card", timeout = 15_000)
    }

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

    /**
     * Wait for the given text to disappear (useful after sheet dismiss animations).
     */
    private fun waitForTagGone(tag: String, timeout: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis = timeout) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun homeRendersAndWaterQuickAddWorks() {
        // Verify core home-screen cards are present
        composeRule.onNodeWithTag("today-intake-card").assertExists()
        composeRule.onNodeWithTag("home-weight-summary-card").assertExists()
        composeRule.onNodeWithTag("water-tracker-widget").assertExists()

        // Add 250 ml water via quick-add button
        composeRule.onNodeWithTag("water-add-250-btn").performScrollTo().performClick()

        // Wait for the water total to update (async Room insert → Flow → recompose)
        waitForText("250 / ")

        // Open water history
        composeRule.onNodeWithTag("toggle-water-history-btn").performScrollTo().performClick()
        waitForText("250 ml")
    }

    @Test
    fun weightSheetLogsAndUpdatesSummary() {
        // Open the weight logging bottom sheet
        composeRule.onNodeWithTag("home-log-weight-btn").performScrollTo().performClick()
        waitForTag("log-weight-modal-dialog")

        // Enter 80 kg and save
        composeRule.onNodeWithTag("weight-log-val-input").performTextReplacement("80")
        composeRule.onNodeWithTag("confirm-log-weight-btn").performClick()

        // Wait for the sheet to dismiss
        waitForTagGone("log-weight-modal-dialog")

        // Wait for the WeightSummaryCard to reflect the new weight
        waitForText("80.0 kg")
    }

    @Test
    fun addFoodOpensSearchAndCanCreateCustomRecipe() {
        // Navigate to food search via the Add Food button
        composeRule.onNodeWithTag("home-quick-add-food-btn").performScrollTo().performClick()
        waitForTag("food-search-input")

        // Verify tabs exist
        composeRule.onNodeWithTag("tab-search-database").assertExists()
        composeRule.onNodeWithTag("tab-frequent-foods").assertExists()

        // Switch to Custom tab
        composeRule.onNodeWithTag("tab-custom-foods").performClick()

        // Open custom food creation dialog
        composeRule.onNodeWithTag("open-create-custom-food-btn").performClick()
        waitForTag("custom-food-modal-dialog")

        // Fill in name and required macros
        composeRule.onNodeWithTag("custom-food-name-input").performScrollTo()
            .performTextInput("Test Poha")
        composeRule.onNodeWithTag("custom-food-kcal-input").performScrollTo()
            .performTextInput("260")
        composeRule.onNodeWithTag("custom-food-protein-input").performScrollTo()
            .performTextInput("6")

        // Save
        composeRule.onNodeWithTag("save-custom-food-submit-btn").performScrollTo().performClick()

        // Wait for the custom food to appear in the list
        waitForText("Test Poha")
    }

    @Test
    fun calculatorRendersProfileAndSavesTargets() {
        // Navigate to Calculator tab via bottom navigation
        composeRule.onNodeWithText("Calculator").performClick()

        // Wait for the profile section to render
        waitForTag("profile-current-weight")
        composeRule.onNodeWithTag("profile-target-weight").assertExists()
        composeRule.onNodeWithTag("unit-system-kg-btn").assertExists()

        // Scroll to the target mode section and verify
        composeRule.onNodeWithTag("target-mode-predefined-btn").performScrollTo()

        // Scroll to and click Save
        composeRule.onNodeWithTag("save-and-use-targets-btn").performScrollTo().performClick()

        // Wait for the snackbar confirmation
        waitForText("Saved & applied to dashboard!")
    }
}
