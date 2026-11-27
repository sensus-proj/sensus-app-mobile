package com.example.sensus.data.local.db

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import com.example.sensus.data.local.security.DatabaseKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseFactory {
    private const val DATABASE_NAME = "sensus_encrypted.db"
    private const val TAG = "DatabaseFactory"

    val MIGRATION_1_2: Migration = SensusDatabaseMigrations.MIGRATION_1_2

    @Volatile
    private var instance: SensusDatabase? = null

    fun create(context: Context, keyManager: DatabaseKeyManager): SensusDatabase {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context.applicationContext, keyManager).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context, keyManager: DatabaseKeyManager): SensusDatabase {
        val first = openValidated(context, keyManager)
        if (first != null) return first

        // La base no se puede descifrar con la clave actual (clave perdida o archivo corrupto). Es solo una
        // caché local: se elimina y se recrea vacía para que la app no se cierre al abrir. Ver
        // docs/errores-conocidos.md (E-02).
        Log.w(TAG, "Base local ilegible; se elimina y se recrea vacia.")
        context.deleteDatabase(DATABASE_NAME)
        return createRoomDatabase(context, keyManager).also { it.openHelper.writableDatabase }
    }

    /** Abre la base y valida que la clave la descifra. Devuelve `null` si el archivo no es legible. */
    private fun openValidated(context: Context, keyManager: DatabaseKeyManager): SensusDatabase? {
        val database = createRoomDatabase(context, keyManager)
        return try {
            database.openHelper.writableDatabase
            database
        } catch (_: android.database.sqlite.SQLiteException) {
            discard(database)
        } catch (_: IllegalStateException) {
            discard(database)
        }
    }

    private fun discard(database: SensusDatabase): SensusDatabase? {
        Log.w(TAG, "No se pudo abrir la base local.")
        runCatching { database.close() }
        return null
    }

    private fun createRoomDatabase(context: Context, keyManager: DatabaseKeyManager): SensusDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = keyManager.getOrCreatePassphrase()
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context,
            SensusDatabase::class.java,
            DATABASE_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(
                MIGRATION_1_2,
                SensusDatabaseMigrations.MIGRATION_2_3,
                SensusDatabaseMigrations.MIGRATION_3_4,
                SensusDatabaseMigrations.MIGRATION_4_5,
                SensusDatabaseMigrations.MIGRATION_5_6,
                SensusDatabaseMigrations.MIGRATION_6_7,
                SensusDatabaseMigrations.MIGRATION_7_8,
                SensusDatabaseMigrations.MIGRATION_8_9,
                SensusDatabaseMigrations.MIGRATION_9_10
            )
            .build()
    }
}
