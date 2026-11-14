package com.aktawind.monnoel.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.aktawind.monnoel.core.database.di.Migrations
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
        helper.runMigrationsAndValidate(dbName, 4, true, *Migrations.ALL).close()
    }
}
