package com.example.sensus.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sensus.data.local.security.DatabaseKeyManager
import net.sqlcipher.database.SupportFactory

object DatabaseFactory {
    private const val DATABASE_NAME = "sensus_encrypted.db"

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `user_preferences` (" +
                    "`prefKey` TEXT NOT NULL, " +
                    "`prefValue` TEXT, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`prefKey`)" +
                    ")"
            )
        }
    }

    @Volatile
    private var instance: SensusDatabase? = null

    fun create(context: Context, keyManager: DatabaseKeyManager): SensusDatabase {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context.applicationContext, keyManager).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context, keyManager: DatabaseKeyManager): SensusDatabase {
        val passphrase = keyManager.getOrCreatePassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            SensusDatabase::class.java,
            DATABASE_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
