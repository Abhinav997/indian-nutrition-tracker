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
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.indian.nutrition.tracker.MainActivity
import org.junit.Before
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

/**
 * Compose UI tests on the real MainActivity.
 * Tests are ordered alphabetically for deterministic CI execution.
 *
 * The CI emulator uses a 320×640 screen so many LazyColumn items are
 * initially off-screen. We use performTouchInput { swipeUp() } to
 * scroll the list and bring items into the composition window.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AppUiTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun wipeAppData() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase("inw.db")
            val dsFile = File(context.filesDir, "datastore/inw_settings.preferences_pb")
            if (dsFile.exists()) dsFile.delete()
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun ensureHomeScreen() {
        waitForTag("bottom-navigation-bar", timeout = 20_000)
        waitForTag("today-intake-card", timeout = 20_000)
    }

    private fun waitForTag(tag: String, timeout: Long = 15_000) {
        composeRule.waitUntil(timeoutMillis = timeout) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String, timeout: Long = 20_000) {
        composeRule.waitUntil(timeoutMillis = timeout) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTagGone(tag: String, timeout: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis = timeout) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun test01_homeRendersAndWaterQuickAddWorks() {
        // Core home-screen cards are in the semantic tree
        composeRule.onNodeWithTag("today-intake-card").assertExists()
        composeRule.onNodeWithTag("home-weight-summary-card").assertExists()

        // The WaterCard sits below the fold on the CI's 320×640 emulator.
        // Swipe up on the intake card to scroll the LazyColumn and bring
        // the WaterCard into the composition window.
        composeRule.onNodeWithTag("today-intake-card").performTouchInput {
            swipeUp()
        }

        // Wait for the water add button to be composed
        waitForTag("water-add-250-btn")
        composeRule.onNodeWithTag("water-add-250-btn").performClick()

        // Water total updates to 250
        waitForText("250 / ")

        // Open water history
        composeRule.onNodeWithTag("toggle-water-history-btn").performClick()
        waitForText("250 ml")
    }

    @Test
    fun test02_weightSheetLogsAndUpdatesSummary() {
        composeRule.onNodeWithTag("home-log-weight-btn").performScrollTo().performClick()
        waitForTag("log-weight-modal-dialog")

        composeRule.onNodeWithTag("weight-log-val-input").performTextReplacement("80")
        composeRule.onNodeWithTag("confirm-log-weight-btn").performClick()

        waitForTagGone("log-weight-modal-dialog")
        waitForText("80.0 kg")
    }

    @Test
    fun test03_addFoodOpensSearchAndCanCreateCustomRecipe() {
        composeRule.onNodeWithTag("home-quick-add-food-btn").performScrollTo().performClick()
        waitForTag("food-search-input")

        composeRule.onNodeWithTag("tab-search-database").assertExists()

        // Switch to Custom tab
        composeRule.onNodeWithTag("tab-custom-foods").performClick()

        // Open custom food dialog
        waitForTag("open-create-custom-food-btn")
        composeRule.onNodeWithTag("open-create-custom-food-btn").performClick()
        waitForTag("custom-food-modal-dialog")

        // Fill form — do NOT use performScrollTo inside AlertDialog
        // (AlertDialog content has no Scroll SemanticsAction parent).
        composeRule.onNodeWithTag("custom-food-name-input").performTextInput("Test Poha")
        composeRule.onNodeWithTag("custom-food-kcal-input").performTextInput("260")
        composeRule.onNodeWithTag("custom-food-protein-input").performTextInput("6")

        // Save custom food
        composeRule.onNodeWithTag("save-custom-food-submit-btn").performClick()

        waitForText("Test Poha")
    }

    @Test
    fun test04_calculatorRendersProfileAndSavesTargets() {
        // Navigate to Calculator tab
        composeRule.onNodeWithText("Calculator").performClick()

        // Wait for the profile section to compose
        waitForTag("profile-current-weight", timeout = 20_000)
        composeRule.onNodeWithTag("profile-target-weight").assertExists()

        // The save button is below the fold on the CI's small emulator.
        // Swipe up on the profile section to scroll the LazyColumn and
        // bring the ResultsCard (with save button) into composition.
        composeRule.onNodeWithTag("profile-current-weight").performTouchInput {
            swipeUp()
        }

        // Wait for save button to be composed
        waitForTag("save-and-use-targets-btn", timeout = 20_000)
        composeRule.onNodeWithTag("save-and-use-targets-btn").performClick()

        waitForText("Saved & applied to dashboard!")
    }
}
