package br.com.tec.tecmotors

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class AppSmokeInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun canNavigateSaveData_andKeepAfterRecreate() {
        composeRule.waitUntil(timeoutMillis = 8000) {
            composeRule.onAllNodesWithText("Abastecimentos").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Veiculos").performClick()

        composeRule.onAllNodesWithText("Data (DD/MM/AAAA)").onFirst().performTextInput("25/02/2026")
        composeRule.onAllNodesWithText("Odometro (km)").onFirst().performTextInput("22222")
        composeRule.onNodeWithText("Salvar odometro").performClick()

        composeRule.onNodeWithText("Odometro registrado").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Veiculos").performClick()
        composeRule.onNodeWithText("Ultimo odometro:", substring = true).assertIsDisplayed()
    }
}
