package com.aktawind.monnoel.core.backup.data

import androidx.room.withTransaction
import com.aktawind.monnoel.core.backup.format.AdventBoxDto
import com.aktawind.monnoel.core.backup.format.AdventCalendarDto
import com.aktawind.monnoel.core.backup.format.AdventCalendarPersonDto
import com.aktawind.monnoel.core.backup.format.AdventSupplyDto
import com.aktawind.monnoel.core.backup.format.AdventTemplateBoxDto
import com.aktawind.monnoel.core.backup.format.AdventTemplateDto
import com.aktawind.monnoel.core.backup.format.BackupData
import com.aktawind.monnoel.core.backup.format.CraftDto
import com.aktawind.monnoel.core.backup.format.CraftSupplyBoughtDto
import com.aktawind.monnoel.core.backup.format.CraftSupplyDto
import com.aktawind.monnoel.core.backup.format.CraftYearDto
import com.aktawind.monnoel.core.backup.format.DecorIdeaDto
import com.aktawind.monnoel.core.backup.format.DismissedSuggestionDto
import com.aktawind.monnoel.core.backup.format.EditionDto
import com.aktawind.monnoel.core.backup.format.ExpenseDto
import com.aktawind.monnoel.core.backup.format.GiftCategoryDto
import com.aktawind.monnoel.core.backup.format.GiftIdeaDto
import com.aktawind.monnoel.core.backup.format.IdeaPersonDto
import com.aktawind.monnoel.core.backup.format.IdeaRequestDto
import com.aktawind.monnoel.core.backup.format.InspirationDoneDto
import com.aktawind.monnoel.core.backup.format.InspirationDto
import com.aktawind.monnoel.core.backup.format.MealDishDto
import com.aktawind.monnoel.core.backup.format.MealDto
import com.aktawind.monnoel.core.backup.format.MealPersonDto
import com.aktawind.monnoel.core.backup.format.PersonBudgetDto
import com.aktawind.monnoel.core.backup.format.PersonDto
import com.aktawind.monnoel.core.backup.format.PlannedGiftDto
import com.aktawind.monnoel.core.backup.format.ProductionDto
import com.aktawind.monnoel.core.backup.format.RecipeCategoryDto
import com.aktawind.monnoel.core.backup.format.RecipeDto
import com.aktawind.monnoel.core.backup.format.SpecialDateDto
import com.aktawind.monnoel.core.backup.format.TaskDto
import com.aktawind.monnoel.core.backup.format.TraditionDto
import com.aktawind.monnoel.core.backup.format.TraditionPersonDto
import com.aktawind.monnoel.core.backup.format.TraditionYearDto
import com.aktawind.monnoel.core.backup.format.WhisperDto
import com.aktawind.monnoel.core.database.MonNoelDatabase
import com.aktawind.monnoel.core.database.entity.AdventBoxEntity
import com.aktawind.monnoel.core.database.entity.AdventCalendarEntity
import com.aktawind.monnoel.core.database.entity.AdventCalendarPersonEntity
import com.aktawind.monnoel.core.database.entity.AdventSupplyEntity
import com.aktawind.monnoel.core.database.entity.AdventTemplateBoxEntity
import com.aktawind.monnoel.core.database.entity.AdventTemplateEntity
import com.aktawind.monnoel.core.database.entity.CraftEntity
import com.aktawind.monnoel.core.database.entity.CraftSupplyBoughtEntity
import com.aktawind.monnoel.core.database.entity.CraftSupplyEntity
import com.aktawind.monnoel.core.database.entity.CraftYearEntity
import com.aktawind.monnoel.core.database.entity.DecorIdeaEntity
import com.aktawind.monnoel.core.database.entity.DismissedSuggestionEntity
import com.aktawind.monnoel.core.database.entity.EditionEntity
import com.aktawind.monnoel.core.database.entity.ExpenseEntity
import com.aktawind.monnoel.core.database.entity.GiftCategoryEntity
import com.aktawind.monnoel.core.database.entity.GiftIdeaEntity
import com.aktawind.monnoel.core.database.entity.IdeaPersonEntity
import com.aktawind.monnoel.core.database.entity.IdeaRequestEntity
import com.aktawind.monnoel.core.database.entity.InspirationDoneEntity
import com.aktawind.monnoel.core.database.entity.InspirationEntity
import com.aktawind.monnoel.core.database.entity.MealDishEntity
import com.aktawind.monnoel.core.database.entity.MealEntity
import com.aktawind.monnoel.core.database.entity.MealPersonEntity
import com.aktawind.monnoel.core.database.entity.PersonBudgetEntity
import com.aktawind.monnoel.core.database.entity.PersonEntity
import com.aktawind.monnoel.core.database.entity.PlannedGiftEntity
import com.aktawind.monnoel.core.database.entity.ProductionEntity
import com.aktawind.monnoel.core.database.entity.RecipeCategoryEntity
import com.aktawind.monnoel.core.database.entity.RecipeEntity
import com.aktawind.monnoel.core.database.entity.SpecialDateEntity
import com.aktawind.monnoel.core.database.entity.TaskEntity
import com.aktawind.monnoel.core.database.entity.TraditionEntity
import com.aktawind.monnoel.core.database.entity.TraditionPersonEntity
import com.aktawind.monnoel.core.database.entity.TraditionYearEntity
import com.aktawind.monnoel.core.database.entity.WhisperEntity
import javax.inject.Inject

/** Lit toute la base pour l'export, et la remplace intégralement lors d'un import. */
class BackupDataSource @Inject constructor(private val db: MonNoelDatabase) {
    suspend fun snapshot(): BackupData = db.withTransaction {
        BackupData(
            editions = db.editionDao().getAll().map { it.toDto() },
            giftCategories = db.giftCategoryDao().getAll().map { GiftCategoryDto(it.id, it.name, it.sortOrder) },
            persons = db.personDao().getAll().map { it.toDto() },
            giftIdeas = db.giftIdeaDao().getAll().map { it.toDto() },
            ideaPersons = db.giftIdeaDao().getAllLinks().map { IdeaPersonDto(it.ideaId, it.personId) },
            plannedGifts = db.plannedGiftDao().getAll().map { it.toDto() },
            personBudgets = db.plannedGiftDao().getAllBudgets().map {
                PersonBudgetDto(it.editionId, it.personId, it.amountCents)
            },
            adventCalendars = db.adventDao().getAllCalendars().map {
                AdventCalendarDto(
                    it.id, it.editionId, it.name, it.description, it.coverPhotoPath, it.theme, it.budgetCents, it.notes,
                    it.boxCount, it.createdAt, it.updatedAt, it.kind, it.priceCents, it.shop, it.bought,
                )
            },
            adventCalendarPersons = db.adventDao().getAllCalendarPersons().map {
                AdventCalendarPersonDto(it.calendarId, it.personId)
            },
            adventSupplies = db.adventDao().getAllSupplies().map {
                AdventSupplyDto(
                    it.id, it.calendarId, it.name, it.quantity, it.priceCents, it.bought, it.sortOrder, it.photoPath, it.type,
                    it.boxTitle, it.supplier,
                )
            },
            adventBoxes = db.adventDao().getAllBoxes().map {
                AdventBoxDto(
                    it.id, it.calendarId, it.number, it.type, it.title, it.photoPath, it.priceCents, it.supplier, it.url,
                    it.status, it.notes, it.supplyId,
                )
            },
            tasks = db.taskDao().getAll().map {
                TaskDto(
                    it.id, it.editionId, it.title, it.description, it.status, it.module, it.notes, it.linkType, it.linkId,
                    it.suggestionKey, it.createdAt, it.updatedAt,
                )
            },
            recipeCategories = db.cookingDao().getAllCategories().map { RecipeCategoryDto(it.id, it.name, it.sortOrder) },
            recipes = db.cookingDao().getAllRecipes().map {
                RecipeDto(it.id, it.name, it.photoPath, it.categoryId, it.url, it.createdAt, it.updatedAt)
            },
            productions = db.cookingDao().getAllProductions().map {
                ProductionDto(
                    it.id, it.editionId, it.recipeId, it.recipeName, it.recipePhotoPath, it.quantity, it.destination, it.status,
                    it.photoPath, it.notes, it.createdAt, it.updatedAt,
                )
            },
            meals = db.mealDao().getAllMeals().map {
                MealDto(
                    it.id, it.editionId, it.name, it.type, it.date, it.place, it.guests, it.estimatedCostCents, it.notes,
                    it.createdAt, it.updatedAt, it.courses,
                )
            },
            mealPersons = db.mealDao().getAllMealPersons().map { MealPersonDto(it.mealId, it.personId) },
            mealDishes = db.mealDao().getAllDishes().map {
                MealDishDto(
                    it.id, it.mealId, it.course, it.recipeId, it.name, it.photoPath, it.broughtBy, it.toOrder, it.ordered,
                    it.sortOrder, it.createdAt,
                )
            },
            expenses = db.expenseDao().getAll().map {
                ExpenseDto(it.id, it.editionId, it.title, it.amountCents, it.category, it.paid, it.notes, it.createdAt, it.updatedAt)
            },
            whispers = db.whisperDao().getAll().map {
                WhisperDto(
                    it.id, it.editionId, it.recipientId, it.title, it.priceCents, it.url, it.givenToPersonId, it.givenToName, it.received,
                    it.notes, it.createdAt, it.updatedAt, it.photoPath,
                )
            },
            ideaRequests = db.ideaRequestDao().getAll().map {
                IdeaRequestDto(it.id, it.editionId, it.kind, it.personId, it.otherPersonId, it.otherName, it.taskId, it.createdAt)
            },
            traditions = db.traditionDao().getAllTraditions().map {
                TraditionDto(it.id, it.name, it.photoPath, it.category, it.period, it.notes, it.favorite, it.createdAt, it.updatedAt)
            },
            traditionPersons = db.traditionDao().getAllTraditionPersons().map { TraditionPersonDto(it.traditionId, it.personId) },
            traditionYears = db.traditionDao().getAllYears().map {
                TraditionYearDto(it.id, it.editionId, it.traditionId, it.done, it.memoryPhotoPath, it.memoryNote, it.createdAt)
            },
            inspirations = db.traditionDao().getAllInspirations().map {
                InspirationDto(it.id, it.kind, it.title, it.photoPath, it.url, it.notes, it.createdAt, it.updatedAt)
            },
            inspirationsDone = db.traditionDao().getAllDone().map { InspirationDoneDto(it.editionId, it.inspirationId) },
            specialDates = db.specialDateDao().getAll().map { SpecialDateDto(it.id, it.month, it.day, it.label, it.emoji) },
            crafts = db.craftDao().getAllCrafts().map {
                CraftDto(it.id, it.title, it.photoPath, it.category, it.tutorialUrl, it.notes, it.createdAt, it.updatedAt)
            },
            craftSupplies = db.craftDao().getAllSupplies().map { CraftSupplyDto(it.id, it.craftId, it.name, it.priceCents, it.sortOrder) },
            craftYears = db.craftDao().getAllYears().map {
                CraftYearDto(it.id, it.editionId, it.craftId, it.status, it.resultPhotoPath, it.createdAt)
            },
            craftSuppliesBought = db.craftDao().getAllBought().map { CraftSupplyBoughtDto(it.editionId, it.supplyId) },
            decorIdeas = db.craftDao().getAllDecor().map {
                DecorIdeaDto(it.id, it.title, it.photoPath, it.url, it.comment, it.place, it.favorite, it.createdAt, it.updatedAt)
            },
            dismissedSuggestions = db.taskDao().getAllDismissed().map { DismissedSuggestionDto(it.editionId, it.suggestionKey) },
            adventTemplates = db.adventDao().getAllTemplates().map {
                AdventTemplateDto(it.id, it.name, it.theme, it.description, it.boxCount, it.createdAt)
            },
            adventTemplateBoxes = db.adventDao().getAllTemplateBoxes().map {
                AdventTemplateBoxDto(it.id, it.templateId, it.number, it.type, it.title, it.priceCents, it.supplier, it.url, it.notes)
            },
        )
    }

    /** Remplacement complet dans une seule transaction : tout ou rien. */
    suspend fun replaceAll(data: BackupData) {
        db.withTransaction {
            // Suppression des enfants vers les parents.
            db.expenseDao().deleteAll()
            db.specialDateDao().deleteAll()
            db.craftDao().deleteAllDecor()
            db.craftDao().deleteAllCrafts()
            db.traditionDao().deleteAllDone()
            db.traditionDao().deleteAllInspirations()
            db.traditionDao().deleteAllYears()
            db.traditionDao().deleteAllTraditions()
            db.whisperDao().deleteAll()
            db.ideaRequestDao().deleteAll()
            db.mealDao().deleteAllDishes()
            db.mealDao().deleteAllMeals()
            db.cookingDao().deleteAllProductions()
            db.cookingDao().deleteAllRecipes()
            db.cookingDao().deleteAllCategories()
            db.taskDao().deleteAll()
            db.taskDao().deleteAllDismissed()
            db.adventDao().deleteAllCalendars()
            db.adventDao().deleteAllTemplates()
            db.plannedGiftDao().deleteAll()
            db.giftIdeaDao().deleteAll()
            db.personDao().deleteAll()
            db.giftCategoryDao().deleteAll()
            db.editionDao().deleteAll()
            // Insertion des parents vers les enfants.
            db.editionDao().insertAll(data.editions.map { it.toEntity() })
            db.giftCategoryDao().insertAll(data.giftCategories.map { GiftCategoryEntity(it.id, it.name, it.sortOrder) })
            db.personDao().insertAll(data.persons.map { it.toEntity() })
            db.giftIdeaDao().insertAll(data.giftIdeas.map { it.toEntity() })
            db.giftIdeaDao().insertLinks(data.ideaPersons.map { IdeaPersonEntity(it.ideaId, it.personId) })
            db.plannedGiftDao().insertAll(data.plannedGifts.map { it.toEntity() })
            db.plannedGiftDao().insertBudgets(
                data.personBudgets.map { PersonBudgetEntity(it.editionId, it.personId, it.amountCents) },
            )
            db.cookingDao().insertCategories(data.recipeCategories.map { RecipeCategoryEntity(it.id, it.name, it.sortOrder) })
            db.cookingDao().insertRecipes(
                data.recipes.map { RecipeEntity(it.id, it.name, it.photoPath, it.categoryId, it.url, it.createdAt, it.updatedAt) },
            )
            db.cookingDao().insertProductions(
                data.productions.map {
                    ProductionEntity(
                        it.id, it.editionId, it.recipeId, it.recipeName, it.recipePhotoPath, it.quantity, it.destination, it.status,
                        it.photoPath, it.notes, it.createdAt, it.updatedAt,
                    )
                },
            )
            db.traditionDao().insertTraditions(
                data.traditions.map {
                    TraditionEntity(it.id, it.name, it.photoPath, it.category, it.period, it.notes, it.favorite, it.createdAt, it.updatedAt)
                },
            )
            db.traditionDao().insertTraditionPersons(data.traditionPersons.map { TraditionPersonEntity(it.traditionId, it.personId) })
            db.traditionDao().insertYears(
                data.traditionYears.map {
                    TraditionYearEntity(it.id, it.editionId, it.traditionId, it.done, it.memoryPhotoPath, it.memoryNote, it.createdAt)
                },
            )
            db.traditionDao().insertInspirations(
                data.inspirations.map {
                    InspirationEntity(it.id, it.kind, it.title, it.photoPath, it.url, it.notes, it.createdAt, it.updatedAt)
                },
            )
            db.traditionDao().insertDone(data.inspirationsDone.map { InspirationDoneEntity(it.editionId, it.inspirationId) })
            db.specialDateDao().insertAll(data.specialDates.map { SpecialDateEntity(it.id, it.month, it.day, it.label, it.emoji) })
            db.craftDao().insertCrafts(
                data.crafts.map {
                    CraftEntity(it.id, it.title, it.photoPath, it.category, it.tutorialUrl, it.notes, it.createdAt, it.updatedAt)
                },
            )
            db.craftDao().insertSupplies(
                data.craftSupplies.map {
                    CraftSupplyEntity(it.id, it.craftId, it.name, it.priceCents, it.sortOrder)
                },
            )
            db.craftDao().insertYears(
                data.craftYears.map { CraftYearEntity(it.id, it.editionId, it.craftId, it.status, it.resultPhotoPath, it.createdAt) },
            )
            db.craftDao().insertBought(data.craftSuppliesBought.map { CraftSupplyBoughtEntity(it.editionId, it.supplyId) })
            db.craftDao().insertDecor(
                data.decorIdeas.map {
                    DecorIdeaEntity(it.id, it.title, it.photoPath, it.url, it.comment, it.place, it.favorite, it.createdAt, it.updatedAt)
                },
            )
            db.ideaRequestDao().insertAll(
                data.ideaRequests.map {
                    IdeaRequestEntity(it.id, it.editionId, it.kind, it.personId, it.otherPersonId, it.otherName, it.taskId, it.createdAt)
                },
            )
            db.whisperDao().insertAll(
                data.whispers.map {
                    WhisperEntity(
                        it.id, it.editionId, it.recipientId, it.title, it.priceCents, it.url, it.givenToPersonId, it.givenToName,
                        it.received, it.notes, it.createdAt, it.updatedAt, it.photoPath,
                    )
                },
            )
            db.expenseDao().insertAll(
                data.expenses.map {
                    ExpenseEntity(it.id, it.editionId, it.title, it.amountCents, it.category, it.paid, it.notes, it.createdAt, it.updatedAt)
                },
            )
            db.mealDao().insertMeals(
                data.meals.map {
                    MealEntity(
                        it.id, it.editionId, it.name, it.type, it.date, it.place, it.guests, it.estimatedCostCents, it.notes,
                        it.createdAt, it.updatedAt, it.courses,
                    )
                },
            )
            db.mealDao().insertMealPersons(data.mealPersons.map { MealPersonEntity(it.mealId, it.personId) })
            db.mealDao().insertDishes(
                data.mealDishes.map {
                    MealDishEntity(
                        it.id, it.mealId, it.course, it.recipeId, it.name, it.photoPath, it.broughtBy, it.toOrder, it.ordered,
                        it.sortOrder, it.createdAt,
                    )
                },
            )
            db.taskDao().insertAll(
                data.tasks.map {
                    TaskEntity(
                        it.id, it.editionId, it.title, it.description, it.status, it.module, it.notes, it.linkType, it.linkId,
                        it.suggestionKey, it.createdAt, it.updatedAt,
                    )
                },
            )
            db.taskDao().insertDismissed(data.dismissedSuggestions.map { DismissedSuggestionEntity(it.editionId, it.suggestionKey) })
            val advent = db.adventDao()
            advent.insertCalendars(
                data.adventCalendars.map {
                    AdventCalendarEntity(
                        it.id, it.editionId, it.name, it.description, it.coverPhotoPath, it.theme, it.budgetCents, it.notes,
                        it.boxCount, it.createdAt, it.updatedAt, it.kind, it.priceCents, it.shop, it.bought,
                    )
                },
            )
            advent.insertCalendarPersons(
                data.adventCalendarPersons.map {
                    AdventCalendarPersonEntity(it.calendarId, it.personId)
                },
            )
            advent.insertSupplies(
                data.adventSupplies.map {
                    AdventSupplyEntity(
                        it.id, it.calendarId, it.name, it.quantity, it.priceCents, it.bought, it.sortOrder, it.photoPath, it.type,
                        it.boxTitle, it.supplier,
                    )
                },
            )
            advent.insertBoxes(
                data.adventBoxes.map {
                    AdventBoxEntity(
                        it.id, it.calendarId, it.number, it.type, it.title, it.photoPath, it.priceCents, it.supplier, it.url,
                        it.status, it.notes, it.supplyId,
                    )
                },
            )
            advent.insertTemplates(
                data.adventTemplates.map {
                    AdventTemplateEntity(it.id, it.name, it.theme, it.description, it.boxCount, it.createdAt)
                },
            )
            advent.insertTemplateBoxes(
                data.adventTemplateBoxes.map {
                    AdventTemplateBoxEntity(
                        it.id, it.templateId, it.number, it.type, it.title, it.priceCents, it.supplier, it.url, it.notes,
                    )
                },
            )
        }
    }
}

private fun EditionEntity.toDto() = EditionDto(id, year, status, plannedBudgetCents, createdAt, updatedAt)

private fun EditionDto.toEntity() = EditionEntity(id, year, status, plannedBudgetCents, createdAt, updatedAt)

private fun PersonEntity.toDto() = PersonDto(id, firstName, lastName, photoPath, interests, notes, createdAt, updatedAt)

private fun PersonDto.toEntity() = PersonEntity(id, firstName, lastName, photoPath, interests, notes, createdAt, updatedAt)

private fun GiftIdeaEntity.toDto() = GiftIdeaDto(
    id, title, description, photoPath, estimatedPriceCents, url, shop, notes, categoryId, createdAt, updatedAt,
)

private fun GiftIdeaDto.toEntity() = GiftIdeaEntity(
    id, title, description, photoPath, estimatedPriceCents, url, shop, notes, categoryId, createdAt, updatedAt,
)

private fun PlannedGiftEntity.toDto() = PlannedGiftDto(
    id, editionId, personId, recipientName, ideaId, title, description, photoPath, priceCents, purchaseDate, shop,
    orderUrl, trackingNumber, notes, categoryId, status, createdAt, updatedAt,
)

private fun PlannedGiftDto.toEntity() = PlannedGiftEntity(
    id, editionId, personId, recipientName, ideaId, title, description, photoPath, priceCents, purchaseDate, shop,
    orderUrl, trackingNumber, notes, categoryId, status, createdAt, updatedAt,
)
