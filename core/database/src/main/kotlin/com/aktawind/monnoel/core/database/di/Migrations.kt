package com.aktawind.monnoel.core.database.di

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aktawind.monnoel.core.database.DefaultData

/** Liste des migrations du schéma. Chaque migration ajoutée ici doit avoir son test. */
object Migrations {
    /** v0.1.0 -> v0.2.0 : personnes, idées, cadeaux, catégories, enveloppes. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "CREATE TABLE IF NOT EXISTS `person` (`id` TEXT NOT NULL, `first_name` TEXT NOT NULL, `last_name` TEXT NOT NULL, `photo_path` TEXT, `interests` TEXT NOT NULL, `notes` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                "CREATE TABLE IF NOT EXISTS `wish_item` (`id` TEXT NOT NULL, `person_id` TEXT NOT NULL, `text` TEXT NOT NULL, `done` INTEGER NOT NULL, `sort_order` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`person_id`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_wish_item_person_id` ON `wish_item` (`person_id`)",
                "CREATE TABLE IF NOT EXISTS `gift_category` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `sort_order` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                "CREATE TABLE IF NOT EXISTS `gift_idea` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `photo_path` TEXT, `estimated_price_cents` INTEGER, `url` TEXT NOT NULL, `shop` TEXT NOT NULL, `notes` TEXT NOT NULL, `category_id` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`category_id`) REFERENCES `gift_category`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                "CREATE INDEX IF NOT EXISTS `index_gift_idea_category_id` ON `gift_idea` (`category_id`)",
                "CREATE TABLE IF NOT EXISTS `idea_person` (`idea_id` TEXT NOT NULL, `person_id` TEXT NOT NULL, PRIMARY KEY(`idea_id`, `person_id`), FOREIGN KEY(`idea_id`) REFERENCES `gift_idea`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`person_id`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_idea_person_person_id` ON `idea_person` (`person_id`)",
                "CREATE TABLE IF NOT EXISTS `planned_gift` (`id` TEXT NOT NULL, `edition_id` TEXT NOT NULL, `person_id` TEXT, `recipient_name` TEXT NOT NULL, `idea_id` TEXT, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `photo_path` TEXT, `price_cents` INTEGER, `purchase_date` INTEGER, `shop` TEXT NOT NULL, `order_url` TEXT NOT NULL, `tracking_number` TEXT NOT NULL, `notes` TEXT NOT NULL, `category_id` TEXT, `status` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`edition_id`) REFERENCES `christmas_edition`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`person_id`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`idea_id`) REFERENCES `gift_idea`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`category_id`) REFERENCES `gift_category`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                "CREATE INDEX IF NOT EXISTS `index_planned_gift_edition_id` ON `planned_gift` (`edition_id`)",
                "CREATE INDEX IF NOT EXISTS `index_planned_gift_person_id` ON `planned_gift` (`person_id`)",
                "CREATE INDEX IF NOT EXISTS `index_planned_gift_idea_id` ON `planned_gift` (`idea_id`)",
                "CREATE INDEX IF NOT EXISTS `index_planned_gift_category_id` ON `planned_gift` (`category_id`)",
                "CREATE TABLE IF NOT EXISTS `person_budget` (`edition_id` TEXT NOT NULL, `person_id` TEXT NOT NULL, `amount_cents` INTEGER NOT NULL, PRIMARY KEY(`edition_id`, `person_id`), FOREIGN KEY(`edition_id`) REFERENCES `christmas_edition`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`person_id`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_person_budget_person_id` ON `person_budget` (`person_id`)",
            ).forEach(db::execSQL)
            DefaultData.insertGiftCategories(db)
        }
    }

    /** Suppression de la liste de souhaits (doublon avec les idées, décision utilisatrice). */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `wish_item`")
        }
    }

    /** v0.3.0 : calendriers de l'Avent, cases, fournitures, modèles. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "CREATE TABLE IF NOT EXISTS `advent_calendar` (`id` TEXT NOT NULL, `edition_id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `cover_photo_path` TEXT, `theme` TEXT NOT NULL, `budget_cents` INTEGER, `notes` TEXT NOT NULL, `box_count` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`edition_id`) REFERENCES `christmas_edition`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_advent_calendar_edition_id` ON `advent_calendar` (`edition_id`)",
                "CREATE TABLE IF NOT EXISTS `advent_calendar_person` (`calendar_id` TEXT NOT NULL, `person_id` TEXT NOT NULL, PRIMARY KEY(`calendar_id`, `person_id`), FOREIGN KEY(`calendar_id`) REFERENCES `advent_calendar`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`person_id`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_advent_calendar_person_person_id` ON `advent_calendar_person` (`person_id`)",
                "CREATE TABLE IF NOT EXISTS `advent_supply` (`id` TEXT NOT NULL, `calendar_id` TEXT NOT NULL, `name` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `price_cents` INTEGER, `bought` INTEGER NOT NULL, `sort_order` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`calendar_id`) REFERENCES `advent_calendar`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_advent_supply_calendar_id` ON `advent_supply` (`calendar_id`)",
                "CREATE TABLE IF NOT EXISTS `advent_box` (`id` TEXT NOT NULL, `calendar_id` TEXT NOT NULL, `number` INTEGER NOT NULL, `type` TEXT, `title` TEXT NOT NULL, `photo_path` TEXT, `price_cents` INTEGER, `supplier` TEXT NOT NULL, `url` TEXT NOT NULL, `status` TEXT NOT NULL, `notes` TEXT NOT NULL, `supply_id` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`calendar_id`) REFERENCES `advent_calendar`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`supply_id`) REFERENCES `advent_supply`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                "CREATE INDEX IF NOT EXISTS `index_advent_box_calendar_id` ON `advent_box` (`calendar_id`)",
                "CREATE INDEX IF NOT EXISTS `index_advent_box_supply_id` ON `advent_box` (`supply_id`)",
                "CREATE TABLE IF NOT EXISTS `advent_template` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `theme` TEXT NOT NULL, `description` TEXT NOT NULL, `box_count` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                "CREATE TABLE IF NOT EXISTS `advent_template_box` (`id` TEXT NOT NULL, `template_id` TEXT NOT NULL, `number` INTEGER NOT NULL, `type` TEXT, `title` TEXT NOT NULL, `price_cents` INTEGER, `supplier` TEXT NOT NULL, `url` TEXT NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`template_id`) REFERENCES `advent_template`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_advent_template_box_template_id` ON `advent_template_box` (`template_id`)",
            ).forEach(db::execSQL)
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
