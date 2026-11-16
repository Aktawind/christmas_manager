package com.aktawind.monnoel.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aktawind.monnoel.core.database.dao.AdventDao
import com.aktawind.monnoel.core.database.dao.CookingDao
import com.aktawind.monnoel.core.database.dao.EditionDao
import com.aktawind.monnoel.core.database.dao.ExpenseDao
import com.aktawind.monnoel.core.database.dao.GiftCategoryDao
import com.aktawind.monnoel.core.database.dao.GiftIdeaDao
import com.aktawind.monnoel.core.database.dao.MealDao
import com.aktawind.monnoel.core.database.dao.PersonDao
import com.aktawind.monnoel.core.database.dao.PlannedGiftDao
import com.aktawind.monnoel.core.database.dao.TaskDao
import com.aktawind.monnoel.core.database.entity.AdventBoxEntity
import com.aktawind.monnoel.core.database.entity.AdventCalendarEntity
import com.aktawind.monnoel.core.database.entity.AdventCalendarPersonEntity
import com.aktawind.monnoel.core.database.entity.AdventSupplyEntity
import com.aktawind.monnoel.core.database.entity.AdventTemplateBoxEntity
import com.aktawind.monnoel.core.database.entity.AdventTemplateEntity
import com.aktawind.monnoel.core.database.entity.DismissedSuggestionEntity
import com.aktawind.monnoel.core.database.entity.EditionEntity
import com.aktawind.monnoel.core.database.entity.ExpenseEntity
import com.aktawind.monnoel.core.database.entity.GiftCategoryEntity
import com.aktawind.monnoel.core.database.entity.GiftIdeaEntity
import com.aktawind.monnoel.core.database.entity.IdeaPersonEntity
import com.aktawind.monnoel.core.database.entity.MealDishEntity
import com.aktawind.monnoel.core.database.entity.MealEntity
import com.aktawind.monnoel.core.database.entity.MealPersonEntity
import com.aktawind.monnoel.core.database.entity.PersonBudgetEntity
import com.aktawind.monnoel.core.database.entity.PersonEntity
import com.aktawind.monnoel.core.database.entity.PlannedGiftEntity
import com.aktawind.monnoel.core.database.entity.ProductionEntity
import com.aktawind.monnoel.core.database.entity.RecipeCategoryEntity
import com.aktawind.monnoel.core.database.entity.RecipeEntity
import com.aktawind.monnoel.core.database.entity.TaskEntity

/**
 * Base unique de l'application (ADR 0002). Toute évolution du schéma doit être
 * accompagnée d'une migration testée — jamais de migration destructive.
 *
 * Historique : v1 (v0.1.0) années ; v2 (v0.2.0 tests) personnes, idées, cadeaux, catégories,
 * enveloppes, souhaits ; v3 (v0.2.0) suppression de la liste de souhaits ;
 * v4 (v0.3.0) calendriers de l'Avent, cases, fournitures, modèles ;
 * v5 (v0.3.0) fournitures : photo, type, contenu des cases, fournisseur ;
 * v6 (v0.4.0) ma liste de lutin (tâches) et suggestions supprimées ;
 * v7 (v0.5.0) recettes (nom et photo), catégories de recettes, productions ;
 * v8 (v0.6.0 test) repas, convives, plats du menu ; v9 (v0.6.0) moments du menu par repas ;
 * v10 (v0.7.0) autres dépenses (budget).
 */
@Database(
    entities = [
        EditionEntity::class,
        PersonEntity::class,
        GiftCategoryEntity::class,
        GiftIdeaEntity::class,
        IdeaPersonEntity::class,
        PlannedGiftEntity::class,
        PersonBudgetEntity::class,
        AdventCalendarEntity::class,
        AdventCalendarPersonEntity::class,
        AdventSupplyEntity::class,
        AdventBoxEntity::class,
        AdventTemplateEntity::class,
        AdventTemplateBoxEntity::class,
        TaskEntity::class,
        DismissedSuggestionEntity::class,
        RecipeCategoryEntity::class,
        RecipeEntity::class,
        ProductionEntity::class,
        MealEntity::class,
        MealPersonEntity::class,
        MealDishEntity::class,
        ExpenseEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
abstract class MonNoelDatabase : RoomDatabase() {
    abstract fun editionDao(): EditionDao

    abstract fun personDao(): PersonDao

    abstract fun giftCategoryDao(): GiftCategoryDao

    abstract fun giftIdeaDao(): GiftIdeaDao

    abstract fun plannedGiftDao(): PlannedGiftDao

    abstract fun adventDao(): AdventDao

    abstract fun taskDao(): TaskDao

    abstract fun cookingDao(): CookingDao

    abstract fun mealDao(): MealDao

    abstract fun expenseDao(): ExpenseDao

    companion object {
        const val NAME = "mon-noel.db"
    }
}
