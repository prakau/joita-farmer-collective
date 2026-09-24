package ai.joita.biosoil.collective

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ai.joita.biosoil.MainActivity
import org.junit.Rule
import org.junit.Test

class CollectiveUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun registrationSavesAndCanBeReopenedAfterActivityRecreation() {
        try {
        compose.waitUntil(30000) { compose.onAllNodesWithText("Add farmer").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Add farmer").performClick()
        compose.onNodeWithText("Farmer name *").performTextInput("QA Registration")
        compose.onNodeWithText("Village *").performTextInput("QA Village")
        compose.onNodeWithText("SAVE").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Edit record").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Edit record").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Edit record").performClick()
        compose.onNodeWithText("Farmer name *").assertTextContains("QA Registration")
        } finally {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            val qa = java.io.File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
            java.io.File(qa, "screen-semantics.txt").writeText(compose.onRoot(useUnmergedTree = true).printToString())
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
                java.io.File(qa, "registration-screen.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }
}
