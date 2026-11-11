package com.aktawind.monnoel

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.io.File
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Outil (pas un test) : génère des captures d'écran tablette pour vérifier le rendu.
 * Ne s'exécute que si la variable d'environnement SHOT_DIR est définie.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-land-mdpi")
class ScreenshotTool {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
        File(System.getenv("SHOT_DIR"), "$name.png").outputStream().use {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun waitText(t: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun shots() {
        Assume.assumeTrue(System.getenv("SHOT_DIR") != null)
        waitText("Bienvenue")
        shot("1-welcome")
        compose.onNodeWithText("Commencer mon Noël", substring = true).performClick()
        waitText("avant Noël")
        shot("2-home")
        compose.onNodeWithText("Mes Noëls").performClick()
        waitText("Affiché")
        shot("3-years")
        compose.onNodeWithText("Réglages").performClick()
        waitText("Apparence")
        shot("4-settings")
        compose.onNodeWithText("Sombre (nuit de Noël)").performClick()
        compose.onNodeWithText("Accueil").performClick()
        waitText("avant Noël")
        shot("5-home-dark")
    }
}
