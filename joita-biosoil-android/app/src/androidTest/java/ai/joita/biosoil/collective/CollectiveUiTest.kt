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
        compose.waitUntil(30000) { compose.onAllNodesWithTag("add-farmer").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("add-farmer").performClick()
        compose.onNodeWithText("किसान का नाम *").performTextInput("QA Registration")
        compose.onNodeWithText("गाँव *").performTextInput("QA Village")
        compose.onNodeWithText("सहेजें").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("रिकॉर्ड संपादित करें").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("रिकॉर्ड संपादित करें").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("रिकॉर्ड संपादित करें").performClick()
        compose.onNodeWithText("किसान का नाम *").assertTextContains("QA Registration")
        compose.onNodeWithContentDescription("बंद करें").performClick()
        compose.onNodeWithTag("add-impact").performScrollTo().performClick()
        compose.onNodeWithText("फील्ड कर्मी का नाम *").performScrollTo().performTextInput("QA Officer")
        compose.onNodeWithTag("impact-stage-बेसलाइन").performScrollTo().performClick()
        // Leaving and reopening restores the automatically saved draft.
        compose.onNodeWithContentDescription("बंद करें").performClick()
        compose.onNodeWithTag("add-impact").performClick()
        compose.onNodeWithText("फील्ड कर्मी का नाम *").assertTextContains("QA Officer")
        compose.onNodeWithTag("impact-stage-बेसलाइन").performScrollTo().assertIsOn()
        compose.onNodeWithText("परियोजना किसान ID", substring = true).performScrollTo().performTextInput("CCF-QA-1")
        compose.onNodeWithText("FarmTrace / खेत / Plot ID", substring = true).performScrollTo().performTextInput("P-QA-1")
        compose.onNodeWithText("परियोजना क्षेत्र (एकड़)", substring = true).performScrollTo().performTextInput("1.5")
        compose.onNodeWithText("फसल / अवस्था", substring = true).performScrollTo().performTextInput("गेहूँ / बढ़वार")
        capture("impact-screen.png")
        compose.onNodeWithText("सहेजें").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("सहेजें").fetchSemanticsNodes().isEmpty() }
        val appContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        CollectiveRepository(appContext).use { repo ->
            org.junit.Assert.assertEquals("QA Officer", repo.impacts().first().officer)
            org.junit.Assert.assertEquals(0L, repo.impacts().first().consentedAt)
        }
        } finally {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            val qa = java.io.File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
            java.io.File(qa, "screen-semantics.txt").writeText(compose.onAllNodes(isRoot(), useUnmergedTree = true).onFirst().printToString())
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
                java.io.File(qa, "registration-screen.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }

    private fun capture(name: String) {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val qa = java.io.File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(qa, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
