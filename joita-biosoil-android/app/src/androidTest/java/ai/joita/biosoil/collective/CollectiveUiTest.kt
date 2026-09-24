package ai.joita.biosoil.collective

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ai.joita.biosoil.MainActivity
import org.junit.Rule
import org.junit.Test

class CollectiveUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun registrationSavesAndCanBeReopenedAfterActivityRecreation() {
        compose.waitUntil(10000) { compose.onAllNodesWithText("Add farmer").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Add farmer").performClick()
        compose.onNodeWithText("Farmer name *").performTextInput("QA Registration")
        compose.onNodeWithText("Village *").performTextInput("QA Village")
        compose.onNodeWithText("SAVE").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Save or share farmer PDF").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Save or share farmer PDF").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Edit record").performClick()
        compose.onNodeWithText("Farmer name *").assertTextContains("QA Registration")
    }
}
