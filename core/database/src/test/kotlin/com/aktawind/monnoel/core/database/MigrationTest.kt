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
}
