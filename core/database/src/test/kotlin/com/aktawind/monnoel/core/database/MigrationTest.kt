package com.aktawind.monnoel.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.aktawind.monnoel.core.database.di.Migrations
import com.aktawind.monnoel.core.database.entity.asModel
import com.aktawind.monnoel.core.model.CourseType
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MonNoelDatabase::class.java)

    @Test
    fun `migration 1 vers 2 conserve les annees et ajoute les categories`() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 2, true, Migrations.MIGRATION_1_2).close()

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            assertEquals(2026, db.editionDao().getAll().single().year)
            assertEquals(DefaultData.giftCategories.size, db.giftCategoryDao().getAll().size)
        }
        db.close()
    }

    @Test
    fun `migration 2 vers 3 supprime les souhaits et garde les cadeaux`() {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO person (id, first_name, last_name, photo_path, interests, notes, " +
                    "created_at, updated_at) VALUES ('p1', 'Léa', '', NULL, '', '', 1, 1)",
            )
            db.execSQL(
                "INSERT INTO wish_item (id, person_id, text, done, sort_order) VALUES ('w1', 'p1', 'Livre', 0, 0)",
            )
            db.execSQL(
                "INSERT INTO planned_gift (id, edition_id, person_id, recipient_name, idea_id, title, description, " +
                    "photo_path, price_cents, purchase_date, shop, order_url, tracking_number, notes, category_id, " +
                    "status, created_at, updated_at) VALUES ('g1', 'e1', 'p1', 'Léa', NULL, 'Livre', '', NULL, 1500, " +
                    "NULL, '', '', '', '', NULL, 'TO_BUY', 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 3, true, Migrations.MIGRATION_2_3).close()

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            assertEquals("Livre", db.plannedGiftDao().getAll().single().title)
            assertEquals("Léa", db.personDao().getAll().single().firstName)
        }
        db.close()
    }

    @Test
    fun `migration 3 vers 4 ajoute les calendriers et garde les donnees`() {
        helper.createDatabase(dbName, 3).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 4, true, Migrations.MIGRATION_3_4).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            assertEquals(2026, db.editionDao().getAll().single().year)
            assertEquals(0, db.adventDao().getAllCalendars().size)
        }
        db.close()
    }

    @Test
    fun `toutes les migrations depuis la v0_1_0`() {
        helper.createDatabase(dbName, 1).close()
        helper.runMigrationsAndValidate(dbName, 15, true, *Migrations.ALL).close()
    }

    @Test
    fun `migration 4 vers 5 garde les fournitures`() {
        helper.createDatabase(dbName, 4).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO advent_calendar (id, edition_id, name, description, cover_photo_path, theme, budget_cents, notes, " +
                    "box_count, created_at, updated_at) VALUES ('c1', 'e1', 'Lucas', '', NULL, '', NULL, '', 24, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO advent_supply (id, calendar_id, name, quantity, price_cents, bought, sort_order) " +
                    "VALUES ('s1', 'c1', 'Chocolats', 1, 1200, 0, 0)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 5, true, Migrations.MIGRATION_4_5).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            val supply = db.adventDao().getAllSupplies().single()
            assertEquals("Chocolats", supply.name)
            assertEquals("", supply.boxTitle)
        }
        db.close()
    }

    @Test
    fun `migration 5 vers 6 ajoute la liste de lutin`() {
        helper.createDatabase(dbName, 5).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 6, true, Migrations.MIGRATION_5_6).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            assertEquals(2026, db.editionDao().getAll().single().year)
            assertEquals(0, db.taskDao().getAll().size)
        }
        db.close()
    }

    @Test
    fun `migration 6 vers 7 ajoute la cuisine et ses categories`() {
        helper.createDatabase(dbName, 6).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 7, true, Migrations.MIGRATION_6_7).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            assertEquals(2026, db.editionDao().getAll().single().year)
            assertEquals(DefaultData.recipeCategories.size, db.cookingDao().getAllCategories().size)
        }
        db.close()
    }

    @Test
    fun `migration 7 vers 8 ajoute les repas et garde les recettes`() {
        helper.createDatabase(dbName, 7).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO recipe (id, name, photo_path, category_id, url, created_at, updated_at) VALUES ('r1', 'Bûche', NULL, NULL, '', 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 8, true, Migrations.MIGRATION_7_8).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            assertEquals("Bûche", db.cookingDao().getAllRecipes().single().name)
            assertEquals(0, db.mealDao().getAllMeals().size)
        }
        db.close()
    }

    @Test
    fun `migration 8 vers 9 donne a chaque repas les moments de son type`() {
        helper.createDatabase(dbName, 8).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO meal (id, edition_id, name, type, date, place, guests, estimated_cost_cents, notes, created_at, updated_at) " +
                    "VALUES ('m1', 'e1', 'Goûter', 'SNACK', '2026-12-26', '', '', NULL, '', 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 9, true, Migrations.MIGRATION_8_9).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            val meal = db.mealDao().getAllMeals().single().asModel(emptySet())
            assertEquals(listOf(CourseType.SWEET, CourseType.DRINKS), meal.courses)
        }
        db.close()
    }

    @Test
    fun `migration 9 vers 10 ajoute les autres depenses et garde les repas`() {
        helper.createDatabase(dbName, 9).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', 50000, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO meal (id, edition_id, name, type, date, place, guests, estimated_cost_cents, notes, created_at, " +
                    "updated_at, courses) " +
                    "VALUES ('m1', 'e1', 'Réveillon', 'CHRISTMAS_EVE', '2026-12-24', '', '', 12000, '', 1, 1, '')",
            )
        }
        helper.runMigrationsAndValidate(dbName, 10, true, Migrations.MIGRATION_9_10).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            assertEquals(12_000L, db.mealDao().getAllMeals().single().estimatedCostCents)
            assertEquals(0, db.expenseDao().getAll().size)
        }
        db.close()
    }

    @Test
    fun `migration 10 vers 11 ajoute les idees soufflees et garde les depenses`() {
        helper.createDatabase(dbName, 10).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO expense (id, edition_id, title, amount_cents, category, paid, notes, created_at, updated_at) " +
                    "VALUES ('x1', 'e1', 'Sapin', 4500, 'TREE', 1, '', 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 11, true, Migrations.MIGRATION_10_11).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            assertEquals("Sapin", db.expenseDao().getAll().single().title)
            assertEquals(0, db.whisperDao().getAll().size)
        }
        db.close()
    }

    @Test
    fun `migration 11 vers 12 ajoute la photo des idees soufflees`() {
        helper.createDatabase(dbName, 11).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO person (id, first_name, last_name, photo_path, interests, notes, created_at, updated_at) " +
                    "VALUES ('p1', 'Yannick', '', NULL, '', '', 1, 1)",
            )
            db.execSQL(
                "INSERT INTO whispered_idea (id, edition_id, recipient_id, title, price_cents, url, given_to_person_id, " +
                    "given_to_name, received, notes, created_at, updated_at) " +
                    "VALUES ('w1', 'e1', 'p1', 'Montre', NULL, '', NULL, 'Christine', 0, '', 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 12, true, Migrations.MIGRATION_11_12).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            val whisper = db.whisperDao().getAll().single()
            assertEquals("Montre", whisper.title)
            assertEquals(null, whisper.photoPath)
        }
        db.close()
    }

    @Test
    fun `migration 12 vers 13 ajoute les demandes d'idees`() {
        helper.createDatabase(dbName, 12).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 13, true, Migrations.MIGRATION_12_13).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            assertEquals(2026, db.editionDao().getAll().single().year)
            assertEquals(0, db.ideaRequestDao().getAll().size)
        }
        db.close()
    }

    @Test
    fun `migration 13 vers 14 garde les calendriers faits maison`() {
        helper.createDatabase(dbName, 13).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO advent_calendar (id, edition_id, name, description, cover_photo_path, theme, budget_cents, notes, " +
                    "box_count, created_at, updated_at) VALUES ('c1', 'e1', 'Tom', '', NULL, '', NULL, '', 24, 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 14, true, Migrations.MIGRATION_13_14).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            val calendar = db.adventDao().getAllCalendars().single()
            assertEquals("HOMEMADE", calendar.kind)
            assertEquals(false, calendar.bought)
        }
        db.close()
    }

    @Test
    fun `migration 14 vers 15 ajoute les traditions et garde les calendriers`() {
        helper.createDatabase(dbName, 14).use { db ->
            db.execSQL(
                "INSERT INTO christmas_edition (id, year, status, planned_budget_cents, created_at, updated_at) " +
                    "VALUES ('e1', 2026, 'ACTIVE', NULL, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO advent_calendar (id, edition_id, name, description, cover_photo_path, theme, budget_cents, notes, " +
                    "box_count, created_at, updated_at, kind, price_cents, shop, bought) " +
                    "VALUES ('c1', 'e1', 'Tisanes', '', NULL, '', NULL, '', 0, 1, 1, 'READY_MADE', 2990, '', 1)",
            )
        }
        helper.runMigrationsAndValidate(dbName, 15, true, Migrations.MIGRATION_14_15).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MonNoelDatabase::class.java,
            dbName,
        ).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        runBlocking {
            assertEquals("READY_MADE", db.adventDao().getAllCalendars().single().kind)
            assertEquals(0, db.traditionDao().getAllTraditions().size)
        }
        db.close()
    }
}
