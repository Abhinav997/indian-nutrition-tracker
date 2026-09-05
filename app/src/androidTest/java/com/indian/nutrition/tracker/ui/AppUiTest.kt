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
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

/**
 * Compose UI tests. Test tags mirror the web app element ids.
 * Tests are ordered alphabetically for deterministic execution on CI.
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

    private fun waitForText(text: String, timeout: Long = 15_000) {
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
        // Core home-screen cards are present (intake is in initial viewport)
        composeRule.onNodeWithTag("today-intake-card").assertExists()
        composeRule.onNodeWithTag("home-weight-summary-card").assertExists()

        // Scroll to the WaterCard and add 250 ml
        waitForTag("water-add-250-btn")
        composeRule.onNodeWithTag("water-add-250-btn").performScrollTo().performClick()
        waitForText("250 / ")

        // Open water history
        composeRule.onNodeWithTag("toggle-water-history-btn").performScrollTo().performClick()
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
        composeRule.onNodeWithTag("tab-frequent-foods").assertExists()

        // Switch to Custom tab
        composeRule.onNodeWithTag("tab-custom-foods").performClick()

        // Open custom food creation dialog (button is inside a Card, not a
        // scrollable container — skip performScrollTo)
        waitForTag("open-create-custom-food-btn")
        composeRule.onNodeWithTag("open-create-custom-food-btn").performClick()
        waitForTag("custom-food-modal-dialog")

        // Fill in name and required macros
        composeRule.onNodeWithTag("custom-food-name-input").performScrollTo()
            .performTextInput("Test Poha")
        composeRule.onNodeWithTag("custom-food-kcal-input").performScrollTo()
            .performTextInput("260")
        composeRule.onNodeWithTag("custom-food-protein-input").performScrollTo()
            .performTextInput("6")

        composeRule.onNodeWithTag("save-custom-food-submit-btn").performScrollTo().performClick()

        waitForText("Test Poha")
    }

    @Test
    fun test04_calculatorRendersProfileAndSavesTargets() {
        // Navigate to Calculator tab
        composeRule.onNodeWithText("Calculator").performClick()

        // Wait for profile fields to appear
        waitForTag("profile-current-weight", timeout = 20_000)
        composeRule.onNodeWithTag("profile-target-weight").assertExists()
        composeRule.onNodeWithTag("unit-system-kg-btn").assertExists()

        // Scroll within the LazyColumn to the save button
        waitForTag("save-and-use-targets-btn")
        composeRule.onNodeWithTag("save-and-use-targets-btn").performScrollTo().performClick()

        waitForText("Saved & applied to dashboard!")
    }
}
