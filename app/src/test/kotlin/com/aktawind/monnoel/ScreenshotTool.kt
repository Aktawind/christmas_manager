package com.aktawind.monnoel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.aktawind.monnoel.core.common.AppResult
import com.aktawind.monnoel.core.domain.advent.BoxDraft
import com.aktawind.monnoel.core.domain.advent.CalendarDraft
import com.aktawind.monnoel.core.domain.advent.SupplyDraft
import com.aktawind.monnoel.core.domain.budget.ExpenseDraft
import com.aktawind.monnoel.core.domain.cooking.ProductionDraft
import com.aktawind.monnoel.core.domain.gifts.GiftDraft
import com.aktawind.monnoel.core.domain.gifts.IdeaDraft
import com.aktawind.monnoel.core.domain.gifts.WhisperDraft
import com.aktawind.monnoel.core.domain.meals.DishDraft
import com.aktawind.monnoel.core.domain.meals.MealDraft
import com.aktawind.monnoel.core.domain.tasks.TaskDraft
import com.aktawind.monnoel.core.domain.traditions.InspirationDraft
import com.aktawind.monnoel.core.domain.traditions.TraditionDraft
import com.aktawind.monnoel.core.model.BoxStatus
import com.aktawind.monnoel.core.model.CalendarKind
import com.aktawind.monnoel.core.model.CourseType
import com.aktawind.monnoel.core.model.ExpenseCategory
import com.aktawind.monnoel.core.model.GiftStatus
import com.aktawind.monnoel.core.model.IdeaRequestKind
import com.aktawind.monnoel.core.model.InspirationKind
import com.aktawind.monnoel.core.model.MealType
import com.aktawind.monnoel.core.model.ProductionStatus
import com.aktawind.monnoel.core.model.SurpriseType
import com.aktawind.monnoel.core.model.TaskModule
import com.aktawind.monnoel.core.model.TaskStatus
import com.aktawind.monnoel.core.model.TraditionCategory
import com.aktawind.monnoel.core.model.TraditionPeriod
import com.aktawind.monnoel.di.TestDataEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.time.LocalDate
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
        seedAdvent(data, edition.id, lea.id, tom.id)
        seedCooking(data, edition.id, setOf(lea.id, tom.id, mamie.id))
        data.setEditionBudget()(edition.id, 80_000)
        data.saveExpense()(null, edition.id, ExpenseDraft("Sapin Nordmann", 5_900, ExpenseCategory.TREE, paid = true))
        data.saveExpense()(null, edition.id, ExpenseDraft("Guirlandes lumineuses", 2_490, ExpenseCategory.DECORATION))
        data.saveExpense()(null, edition.id, ExpenseDraft("Papier cadeau", 1_200, ExpenseCategory.WRAPPING, paid = true))
        val past = (data.createEdition()(2025) as AppResult.Success).value
        data.saveGift()(null, past.id, GiftDraft(lea.id, "Puzzle licorne", priceCents = 22_000, status = GiftStatus.GIVEN))
        data.saveExpense()(null, past.id, ExpenseDraft("Sapin", 4_900, ExpenseCategory.TREE, paid = true))
        data.selectEdition()(edition.id)
        data.saveSpecialDate()(null, 12, 13, "Sainte-Lucie", "🕯️")
        data.saveSpecialDate()(null, 12, 20, "Arrivée de Mamie", "🚗")
        val whisper = data.saveWhisper()
        whisper(null, edition.id, WhisperDraft(lea.id, "Montre à paillettes", givenToPersonId = mamie.id, priceCents = 3_500))
        whisper(null, edition.id, WhisperDraft(lea.id, "Montre à paillettes", givenToPersonId = tom.id))
        whisper(null, edition.id, WhisperDraft(lea.id, "Atelier poterie", givenToName = "Tante Lucie", received = true))
        whisper(null, edition.id, WhisperDraft(tom.id, "Lampe dinosaure", givenToPersonId = mamie.id))
        data.createIdeaRequest()(edition.id, IdeaRequestKind.TO_SEND, lea.id, tom.id)
        data.createIdeaRequest()(edition.id, IdeaRequestKind.TO_RECEIVE, lea.id, mamie.id)
        suspend fun tradition(name: String, category: TraditionCategory, period: TraditionPeriod, favorite: Boolean = false) =
            (data.saveTradition()(null, TraditionDraft(name, category, period, favorite = favorite)) as AppResult.Success).value
        val lutin = tradition("Le lutin farceur", TraditionCategory.HOME, TraditionPeriod.ADVENT, favorite = true)
        val sapin = tradition("Décorer le sapin ensemble", TraditionCategory.HOME, TraditionPeriod.ADVENT)
        val marche = tradition("Marché de Noël de Strasbourg", TraditionCategory.OUTINGS, TraditionPeriod.ADVENT)
        tradition("Pyjamas assortis du réveillon", TraditionCategory.FAMILY, TraditionPeriod.EVE)
        listOf(lutin, sapin, marche).forEach { data.planTradition()(edition.id, it.id, true) }
        data.saveTraditionMemory()(edition.id, sapin.id, true, null, "Tom a mis l'étoile tout seul")
        val film = (data.saveInspiration()(null, InspirationDraft("Le Pôle Express", InspirationKind.FILM)) as AppResult.Success).value
        data.saveInspiration()(null, InspirationDraft("Le Grinch", InspirationKind.CARTOON))
        data.saveInspiration()(null, InspirationDraft("Patinoire de Noël", InspirationKind.OUTING))
        data.setInspirationDone()(edition.id, film.id, true)
        data.saveTask()(null, edition.id, TaskDraft("Acheter du papier cadeau", module = TaskModule.GIFTS))
        data.saveTask()(null, edition.id, TaskDraft("Sortir la crèche", module = TaskModule.HOME, status = TaskStatus.IN_PROGRESS))
        data.saveTask()(null, edition.id, TaskDraft("Commander le sapin", module = TaskModule.HOME, status = TaskStatus.DONE))
    }

    private suspend fun seedCooking(data: TestDataEntryPoint, editionId: String, family: Set<String>) {
        suspend fun recipe(name: String, category: String) = (data.saveRecipe()(null, name, null, category, "") as AppResult.Success).value
        val sables = recipe("Sablés à la cannelle", "rcat-sables")
        val buche = recipe("Bûche chocolat-marron", "rcat-buches")
        val truffes = recipe("Truffes au chocolat", "rcat-truffes")
        recipe("Mendiants aux fruits secs", "rcat-mendiants")
        recipe("Vin chaud aux épices", "rcat-boissons")
        val save = data.saveProduction()
        save(null, editionId, ProductionDraft(sables.id, sables.name, 3, "Voisins", ProductionStatus.DONE))
        save(null, editionId, ProductionDraft(truffes.id, truffes.name, 2, "À offrir", ProductionStatus.IN_PROGRESS))
        save(null, editionId, ProductionDraft(buche.id, buche.name, 1, "Repas de Noël"))
        save(null, editionId, ProductionDraft(null, "Biscuits pour l'école", 1, "École"))
        val eve = MealDraft(
            "Réveillon",
            MealType.CHRISTMAS_EVE,
            LocalDate.of(2026, 12, 24),
            "Chez nous",
            family,
            listOf("Oncle Paul", "Tata Jo"),
            15_000,
        )
        val meal = (data.saveMeal()(null, editionId, eve) as AppResult.Success).value
        val dish = data.saveDish()
        dish(null, meal.id, DishDraft(CourseType.APERITIF, null, "Toasts au saumon"))
        dish(null, meal.id, DishDraft(CourseType.STARTER, null, "Huîtres", toOrder = true))
        dish(null, meal.id, DishDraft(CourseType.MAIN, null, "Chapon aux marrons", broughtBy = "Mamie"))
        dish(null, meal.id, DishDraft(CourseType.DESSERT, buche.id, buche.name))
        dish(null, meal.id, DishDraft(CourseType.DRINKS, recipeId = null, name = "Champagne", toOrder = true, ordered = true))
        val lunch = MealDraft("Déjeuner de Noël", MealType.CHRISTMAS_DAY, LocalDate.of(2026, 12, 25), "Chez Mamie", family)
        data.saveMeal()(null, editionId, lunch)
        val snack = MealDraft("Goûter des cousins", MealType.SNACK, LocalDate.of(2026, 12, 26), "Chez nous")
        val snackMeal = (data.saveMeal()(null, editionId, snack) as AppResult.Success).value
        dish(null, snackMeal.id, DishDraft(CourseType.SWEET, sables.id, sables.name))
    }

    private suspend fun seedAdvent(data: TestDataEntryPoint, editionId: String, leaId: String, tomId: String) {
        val cal = (
            data.saveCalendar()(
                null,
                editionId,
                CalendarDraft("Calendrier de Tom", setOf(tomId), theme = "Dinosaures", budgetCents = 5_000),
            ) as AppResult.Success
            ).value
        data.saveCalendar()(null, editionId, CalendarDraft("Lutin farceur", setOf(leaId, tomId), theme = "Farces", boxCount = 25))
        val tisanes = CalendarDraft("Tisanes bio", setOf(leaId), kind = CalendarKind.READY_MADE, priceCents = 2_990, shop = "Herboristerie")
        data.saveCalendar()(null, editionId, tisanes)
        val boxes = data.adventRepository().getBoxes(cal.id)
        val contents = listOf(
            Triple(SurpriseType.TREAT, "Chocolat", BoxStatus.READY),
            Triple(SurpriseType.BOUGHT, "Petit dino", BoxStatus.READY),
            Triple(SurpriseType.ACTIVITY, "Biscuits ensemble", BoxStatus.TO_PREPARE),
            Triple(SurpriseType.MESSAGE, "Mot doux", BoxStatus.READY),
            Triple(SurpriseType.GAME, "Énigme", BoxStatus.TO_PREPARE),
            Triple(SurpriseType.EXPERIENCE, "Bon pour une luge", BoxStatus.TO_FIND),
        )
        contents.forEachIndexed { i, (type, title, status) ->
            val price = if (type == SurpriseType.BOUGHT) 450L else null
            data.saveBox()(boxes[i].id, BoxDraft(type = type, title = title, status = status, priceCents = price))
        }
        data.saveSupply()(cal.id, null, SupplyDraft("Sachets kraft", 24, 690, true), emptySet())
        val chocolates = SupplyDraft("Paquet de chocolats", 1, 1_200, true, type = SurpriseType.TREAT, boxTitle = "Chocolat")
        data.saveSupply()(cal.id, null, chocolates, boxes.drop(6).take(12).map { it.id }.toSet())
    }

    @Test
    fun shots() {
        Assume.assumeTrue(System.getenv("SHOT_DIR") != null)
        seed()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitText("avant Noël")
        shot("10-home")
        click("Cadeaux")
        waitText("Grouper")
        waitText("Coffret de feutres")
        shot("11-gifts-by-person")
        click("Grouper : par personne")
        click("par statut")
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
        waitText("Idées pour cette personne")
        shot("15-person")
        compose.onNodeWithText("+ Idée soufflée").performScrollTo()
        shot("25-whispers")
        compose.onNodeWithText("+ J’attends des idées de Léa").performScrollTo()
        shot("26-requests")
        compose.onAllNodesWithText("Coffret de feutres").onLast().performScrollTo().performClick()
        click("🤫 En faire une idée soufflée")
        waitText("De la part de")
        compose.onNode(hasSetTextAction() and hasText("Nom (ex.", substring = true)).performTextInput("Papa")
        compose.onAllNodesWithText("Enregistrer").onLast().performClick()
        waitText("de la part de Papa")
        compose.onNodeWithText("Livre de contes").performScrollTo()
        click("Livre de contes")
        waitText("Dupliquer pour")
        click("Dupliquer pour…")
        waitText("Une copie par personne")
        click("Tom")
        shot("24-duplicate")
        click("Dupliquer")
        waitText("Cadeau dupliqué")
        compose.onNodeWithContentDescription("Retour").performClick()
        compose.onNodeWithContentDescription("Retour").performClick()
        click("Listes")
        waitText("À envoyer")
        shot("21-lists")
        click("Mes Noëls")
        compose.onNodeWithContentDescription("Rechercher dans tous mes Noëls").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("choco")
        waitText("Cases")
        shot("22-search")
        click("Accueil")
        waitText("Le chemin vers Noël")
        shot("23-home")
        click("Réglages")
        waitText("Personnaliser")
        shot("16-settings")
        click("Avent")
        waitText("Calendrier de Tom")
        shot("18-advent-list")
        click("Tisanes bio")
        waitText("Boutique ou marque")
        shot("18b-ready-made")
        compose.onNodeWithContentDescription("Retour").performClick()
        waitText("Calendrier de Tom")
        click("Calendrier de Tom")
        waitText("Biscuits")
        shot("19-calendar")
        compose.onAllNodesWithContentDescription("Case 3 :", substring = true).onFirst().performTouchInput {
            down(center)
            advanceEventTime(1_500)
            up()
        }
        waitText("sélectionnée")
        click("Les vides")
        shot("20-selection")
        click("Accueil")
        waitText("Le chemin vers Noël")
        click("Cuisine")
        waitText("Déjeuner de Noël")
        shot("35-meals")
        compose.onAllNodesWithText("Réveillon", substring = true).onFirst().performClick()
        waitText("Chapon aux marrons")
        shot("36-meal")
        click("Chapon aux marrons")
        waitText("Qui apporte")
        shot("37-dish")
        compose.onNodeWithContentDescription("Retour").performClick()
        waitText("Chapon aux marrons")
        compose.onNodeWithContentDescription("Modifier le repas").performClick()
        waitText("Autre invité")
        shot("38-meal-edit")
        compose.onNodeWithContentDescription("Retour").performClick()
        waitText("Chapon aux marrons")
        compose.onNodeWithContentDescription("Retour").performClick()
        waitText("Goûter des cousins")
        compose.onAllNodesWithText("Goûter des cousins", substring = true).onFirst().performClick()
        waitText("Ajouter un moment")
        shot("39-snack")
        compose.onNodeWithContentDescription("Retour").performClick()
        waitText("Déjeuner de Noël")
        click("Mes recettes")
        waitText("Vin chaud")
        shot("31-recipes")
        click("Truffes au chocolat")
        waitText("Préparer pour Noël")
        shot("32-recipe")
        compose.onNodeWithContentDescription("Retour").performClick()
        click("Préparations")
        waitText("Biscuits pour l'école")
        shot("30-cooking")
        click("Biscuits pour l'école")
        waitText("Pour qui")
        shot("33-production")
        compose.onNodeWithContentDescription("Retour").performClick()
        click("Budget")
        waitText("Répartition du prévu")
        shot("40-budget")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Coût prévu de chaque Noël"))
        shot("41-budget-years")
        click("Traditions")
        waitText("Au programme de Noël")
        shot("42-program")
        click("Nos traditions")
        waitText("Piocher des idées")
        shot("42-traditions")
        click("Inspirations")
        waitText("Le Pôle Express")
        shot("43-inspirations")
        click("Nos traditions")
        waitText("Le lutin farceur")
        compose.onAllNodesWithText("Le lutin farceur").onLast().performTouchInput {
            click(androidx.compose.ui.geometry.Offset(width / 2f, 6f))
        }
        waitText("Période")
        shot("44-tradition-edit")
        compose.onNodeWithContentDescription("Retour").performClick()
        click("Réglages")
        waitText("Personnaliser")
        click("Catégories de recettes")
        waitText("Mes catégories")
        shot("34-recipe-categories")
        compose.onNodeWithContentDescription("Retour").performClick()
        click("Catégories de cadeaux")
        waitText("Mes catégories")
        shot("17-categories")
    }
}
