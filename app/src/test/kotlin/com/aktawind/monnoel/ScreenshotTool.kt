package com.aktawind.monnoel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.gifts.GiftDraft
import com.aktawind.monnoel.core.domain.gifts.IdeaDraft
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.di.TestDataEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import kotlinx.coroutines.runBlocking
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
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun shot(name: String) {
        compose.waitForIdle()
        scenario.onActivity { activity ->
            val view = activity.window.decorView
            val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bmp))
            File(System.getenv("SHOT_DIR"), "$name.png").outputStream().use {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun waitText(t: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun click(t: String) = compose.onAllNodesWithText(t).onFirst().performClick()

    private fun seed() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val data = EntryPointAccessors.fromApplication(app, TestDataEntryPoint::class.java)
        val edition = (data.createEdition()(2026) as AppResult.Success).value
        val lea = (
            data.savePerson()(
                null,
                "Léa",
                "Martin",
                null,
                "Dessin, licornes, livres",
                "",
            ) as AppResult.Success
            ).value
        val tom = (data.savePerson()(null, "Tom", "", null, "Dinosaures", "") as AppResult.Success).value
        val mamie = (data.savePerson()(null, "Mamie", "Jo", null, "Jardinage, thé", "") as AppResult.Success).value
        data.setBudget()(edition.id, lea.id, 6_000)
        data.setBudget()(edition.id, tom.id, 4_000)
        val idea = (
            data.saveIdea()(
                null,
                IdeaDraft(
                    "Coffret de feutres",
                    estimatedPriceCents = 2_490,
                    personIds = setOf(lea.id),
                    categoryId = "cat-leisure",
                ),
            ) as AppResult.Success
            ).value
        data.saveIdea()(
            null,
            IdeaDraft(
                "Théière en fonte",
                estimatedPriceCents = 3_500,
                personIds = setOf(mamie.id),
                categoryId = "cat-home",
            ),
        )
        data.saveIdea()(null, IdeaDraft("Puzzle 1000 pièces", estimatedPriceCents = 1_990))
        data.offerIdea()(idea.id, lea.id, edition.id)
        val g1 = (
            data.saveGift()(
                null,
                edition.id,
                GiftDraft(lea.id, "Livre de contes", priceCents = 1_800, categoryId = "cat-books"),
            ) as AppResult.Success
            ).value
        data.changeStatus()(g1.id, GiftStatus.WRAPPED)
        val g2 = (
            data.saveGift()(
                null,
                edition.id,
                GiftDraft(
                    tom.id,
                    "Dinosaure télécommandé",
                    priceCents = 4_590,
                    shop = "Jouets & Cie",
                    categoryId = "cat-toys",
                ),
            ) as AppResult.Success
            ).value
        data.changeStatus()(g2.id, GiftStatus.ORDERED)
        data.saveGift()(
            null,
            edition.id,
            GiftDraft(mamie.id, "Plaid tout doux", priceCents = 3_900, status = GiftStatus.TO_WRAP),
        )
    }

    @Test
    fun shots() {
        Assume.assumeTrue(System.getenv("SHOT_DIR") != null)
        seed()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitText("avant Noël")
        shot("10-home")
        click("Cadeaux")
        waitText("Par personne")
        shot("11-gifts-by-person")
        click("Par statut")
        shot("12-gifts-by-status")
        click("Personnes")
        waitText("Léa Martin")
        shot("13-persons")
        click("Idées")
        waitText("Théière")
        shot("14-ideas")
        click("Personnes")
        waitText("Léa Martin")
        click("Léa Martin")
        waitText("Liste de souhaits")
        shot("15-person")
        click("Réglages")
        waitText("Personnaliser")
        shot("16-settings")
        click("Catégories de cadeaux")
        waitText("Mes catégories")
        shot("17-categories")
    }
}
