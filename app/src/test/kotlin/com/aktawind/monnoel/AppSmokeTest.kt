package com.aktawind.monnoel

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Démarre la vraie application (Hilt, Room, DataStore) pour vérifier qu'elle se lance
 * et que le parcours de premier lancement fonctionne.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-land")
class AppSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun premierLancementPuisAccueil() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Bienvenue dans Mon Noël").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Commencer mon Noël", substring = true).performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Mes Noëls").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Réglages").performClick()
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Exporter mes données").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Mes Noëls").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription("Actions pour Noël", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
