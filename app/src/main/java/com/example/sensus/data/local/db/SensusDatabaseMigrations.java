package com.example.sensus.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** Explicit, non-destructive Room schema migrations for SensusDatabase. */
public final class SensusDatabaseMigrations {

    private static final String MIGRATION_1_2_DDL =
        "CREATE TABLE IF NOT EXISTS `user_preferences` ("
            + "`prefKey` TEXT NOT NULL, "
            + "`prefValue` TEXT, "
            + "`updatedAt` INTEGER NOT NULL, "
            + "PRIMARY KEY(`prefKey`))";

    private SensusDatabaseMigrations() {
    }

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(MIGRATION_1_2_DDL);
        }
    };
    private static final String MIGRATION_2_3_DDL =
        "CREATE TABLE IF NOT EXISTS `feed_posts` (`id` TEXT NOT NULL, `queryKey` TEXT NOT NULL, "
            + "`page` INTEGER NOT NULL, `position` INTEGER NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`id`, `queryKey`))";

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(MIGRATION_2_3_DDL);
            db.execSQL("ALTER TABLE `offers` ADD COLUMN `unitsLeft` INTEGER NOT NULL DEFAULT 5");
            db.execSQL("ALTER TABLE `offers` ADD COLUMN `expirationDate` TEXT");
        }
    };
}
