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
}
