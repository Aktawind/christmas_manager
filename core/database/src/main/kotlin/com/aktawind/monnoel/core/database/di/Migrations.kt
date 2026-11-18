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

    /** Fournitures enrichies : photo, type de surprise, contenu des cases, fournisseur. */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "ALTER TABLE `advent_supply` ADD COLUMN `photo_path` TEXT",
                "ALTER TABLE `advent_supply` ADD COLUMN `type` TEXT",
                "ALTER TABLE `advent_supply` ADD COLUMN `box_title` TEXT NOT NULL DEFAULT ''",
                "ALTER TABLE `advent_supply` ADD COLUMN `supplier` TEXT NOT NULL DEFAULT ''",
            ).forEach(db::execSQL)
        }
    }

    /** v0.4.0 : ma liste de lutin (tâches) et suggestions supprimées. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "CREATE TABLE IF NOT EXISTS `lutin_task` (`id` TEXT NOT NULL, `edition_id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `status` TEXT NOT NULL, `module` TEXT NOT NULL, `notes` TEXT NOT NULL, `link_type` TEXT, `link_id` TEXT, `suggestion_key` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`edition_id`) REFERENCES `christmas_edition`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_lutin_task_edition_id` ON `lutin_task` (`edition_id`)",
                "CREATE TABLE IF NOT EXISTS `dismissed_suggestion` (`edition_id` TEXT NOT NULL, `suggestion_key` TEXT NOT NULL, PRIMARY KEY(`edition_id`, `suggestion_key`), FOREIGN KEY(`edition_id`) REFERENCES `christmas_edition`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            ).forEach(db::execSQL)
        }
    }

    /** v0.5.0 : recettes (nom et photo), catégories de recettes, productions. */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "CREATE TABLE IF NOT EXISTS `recipe_category` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `sort_order` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                "CREATE TABLE IF NOT EXISTS `recipe` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `photo_path` TEXT, `category_id` TEXT, `url` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`category_id`) REFERENCES `recipe_category`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                "CREATE INDEX IF NOT EXISTS `index_recipe_category_id` ON `recipe` (`category_id`)",
                "CREATE TABLE IF NOT EXISTS `production` (`id` TEXT NOT NULL, `edition_id` TEXT NOT NULL, `recipe_id` TEXT, `recipe_name` TEXT NOT NULL, `recipe_photo_path` TEXT, `quantity` INTEGER NOT NULL, `destination` TEXT NOT NULL, `status` TEXT NOT NULL, `photo_path` TEXT, `notes` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`edition_id`) REFERENCES `christmas_edition`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`recipe_id`) REFERENCES `recipe`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                "CREATE INDEX IF NOT EXISTS `index_production_edition_id` ON `production` (`edition_id`)",
                "CREATE INDEX IF NOT EXISTS `index_production_recipe_id` ON `production` (`recipe_id`)",
            ).forEach(db::execSQL)
            DefaultData.insertRecipeCategories(db)
        }
    }

    /** v0.6.0 : repas, convives et plats du menu. */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "CREATE TABLE IF NOT EXISTS `meal` (`id` TEXT NOT NULL, `edition_id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `date` TEXT, `place` TEXT NOT NULL, `guests` TEXT NOT NULL, `estimated_cost_cents` INTEGER, `notes` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`edition_id`) REFERENCES `christmas_edition`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_meal_edition_id` ON `meal` (`edition_id`)",
                "CREATE TABLE IF NOT EXISTS `meal_person` (`meal_id` TEXT NOT NULL, `person_id` TEXT NOT NULL, PRIMARY KEY(`meal_id`, `person_id`), FOREIGN KEY(`meal_id`) REFERENCES `meal`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`person_id`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_meal_person_person_id` ON `meal_person` (`person_id`)",
                "CREATE TABLE IF NOT EXISTS `meal_dish` (`id` TEXT NOT NULL, `meal_id` TEXT NOT NULL, `course` TEXT NOT NULL, `recipe_id` TEXT, `name` TEXT NOT NULL, `photo_path` TEXT, `brought_by` TEXT NOT NULL, `to_order` INTEGER NOT NULL, `ordered` INTEGER NOT NULL, `sort_order` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`meal_id`) REFERENCES `meal`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`recipe_id`) REFERENCES `recipe`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                "CREATE INDEX IF NOT EXISTS `index_meal_dish_meal_id` ON `meal_dish` (`meal_id`)",
                "CREATE INDEX IF NOT EXISTS `index_meal_dish_recipe_id` ON `meal_dish` (`recipe_id`)",
            ).forEach(db::execSQL)
        }
    }

    /** v0.6.0 : chaque repas choisit ses moments (vide = moments par défaut de son type). */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `meal` ADD COLUMN `courses` TEXT NOT NULL DEFAULT ''")
        }
    }

    /** v0.7.0 : autres dépenses (sapin, décoration…) pour le budget. */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `expense` (`id` TEXT NOT NULL, `edition_id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`amount_cents` INTEGER NOT NULL, `category` TEXT NOT NULL, `paid` INTEGER NOT NULL, `notes` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`edition_id`) " +
                    "REFERENCES `christmas_edition`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_edition_id` ON `expense` (`edition_id`)")
        }
    }

    /** v0.7.0 : idées soufflées (pour qui, à qui, pour quel Noël). */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `whispered_idea` (`id` TEXT NOT NULL, `edition_id` TEXT NOT NULL, " +
                    "`recipient_id` TEXT NOT NULL, `title` TEXT NOT NULL, `price_cents` INTEGER, `url` TEXT NOT NULL, " +
                    "`given_to_person_id` TEXT, `given_to_name` TEXT NOT NULL, `received` INTEGER NOT NULL, `notes` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`edition_id`) REFERENCES `christmas_edition`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                    "FOREIGN KEY(`recipient_id`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                    "FOREIGN KEY(`given_to_person_id`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_whispered_idea_edition_id` ON `whispered_idea` (`edition_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_whispered_idea_recipient_id` ON `whispered_idea` (`recipient_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_whispered_idea_given_to_person_id` ON `whispered_idea` (`given_to_person_id`)")
        }
    }

    /** v0.7.0 : photo des idées soufflées. */
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `whispered_idea` ADD COLUMN `photo_path` TEXT")
        }
    }

    /** v0.7.0 : demandes d'idées (qui attend des idées, de qui on en attend). */
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `idea_request` (`id` TEXT NOT NULL, `edition_id` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                    "`person_id` TEXT NOT NULL, `other_person_id` TEXT, `other_name` TEXT NOT NULL, `task_id` TEXT, " +
                    "`created_at` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`edition_id`) REFERENCES `christmas_edition`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                    "FOREIGN KEY(`person_id`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                    "FOREIGN KEY(`other_person_id`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_idea_request_edition_id` ON `idea_request` (`edition_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_idea_request_person_id` ON `idea_request` (`person_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_idea_request_other_person_id` ON `idea_request` (`other_person_id`)")
        }
    }

    /** v0.7.0 : calendriers tout prêts (type, prix, boutique, acheté). */
    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "ALTER TABLE `advent_calendar` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'HOMEMADE'",
                "ALTER TABLE `advent_calendar` ADD COLUMN `price_cents` INTEGER",
                "ALTER TABLE `advent_calendar` ADD COLUMN `shop` TEXT NOT NULL DEFAULT ''",
                "ALTER TABLE `advent_calendar` ADD COLUMN `bought` INTEGER NOT NULL DEFAULT 0",
            ).forEach(db::execSQL)
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
    )
}
