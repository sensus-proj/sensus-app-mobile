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

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `offers` ADD COLUMN `status` TEXT DEFAULT 'Published'");
            db.execSQL("ALTER TABLE `offers` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE `offers` ADD COLUMN `expiresAtMillis` INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final String MIGRATION_4_5_DDL =
        "CREATE TABLE IF NOT EXISTS `ai_messages` ("
            + "`messageId` TEXT NOT NULL, "
            + "`conversationId` TEXT NOT NULL, "
            + "`role` TEXT NOT NULL, "
            + "`content` TEXT NOT NULL, "
            + "`packageJson` TEXT, "
            + "`degraded` INTEGER NOT NULL, "
            + "`createdAtUtc` TEXT NOT NULL, "
            + "PRIMARY KEY(`messageId`))";

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(MIGRATION_4_5_DDL);
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_messages_conversationId` ON `ai_messages` (`conversationId`)");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `ai_messages` ADD COLUMN `offersJson` TEXT");
        }
    };

    private static final String MIGRATION_6_7_DDL =
        "CREATE TABLE IF NOT EXISTS `feed_offers` ("
            + "`offerId` TEXT NOT NULL, "
            + "`position` INTEGER NOT NULL, "
            + "`payloadJson` TEXT NOT NULL, "
            + "`rankingVersion` TEXT NOT NULL, "
            + "`cachedAtMillis` INTEGER NOT NULL, "
            + "PRIMARY KEY(`offerId`))";

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(MIGRATION_6_7_DDL);
        }
    };

    private static final String MIGRATION_7_8_DDL =
        "CREATE TABLE IF NOT EXISTS `search_history` ("
            + "`query` TEXT NOT NULL, "
            + "`searchedAtMillis` INTEGER NOT NULL, "
            + "PRIMARY KEY(`query`))";

    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(MIGRATION_7_8_DDL);
        }
    };

    private static final String MIGRATION_8_9_DDL =
        "CREATE TABLE IF NOT EXISTS `favorites` ("
            + "`offerId` TEXT NOT NULL, "
            + "`ownerKey` TEXT NOT NULL, "
            + "`merchantId` TEXT NOT NULL, "
            + "`merchantName` TEXT NOT NULL, "
            + "`category` TEXT NOT NULL, "
            + "`title` TEXT NOT NULL, "
            + "`description` TEXT NOT NULL, "
            + "`originalPrice` REAL NOT NULL, "
            + "`discountPercentage` INTEGER NOT NULL, "
            + "`discountedPrice` REAL NOT NULL, "
            + "`stock` INTEGER NOT NULL, "
            + "`imageUrl` TEXT, "
            + "`startsAtUtc` TEXT NOT NULL, "
            + "`endsAtUtc` TEXT NOT NULL, "
            + "`expiresAtMillis` INTEGER NOT NULL, "
            + "`latitude` REAL, "
            + "`longitude` REAL, "
            + "`likesCount` INTEGER NOT NULL, "
            + "`favoritedAtUtc` TEXT NOT NULL, "
            + "`updatedAtMillis` INTEGER NOT NULL DEFAULT 0, "
            + "PRIMARY KEY(`offerId`))";

    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(MIGRATION_8_9_DDL);
        }
    };
}
