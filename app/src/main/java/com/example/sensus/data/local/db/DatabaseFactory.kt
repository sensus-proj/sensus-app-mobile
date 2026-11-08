package com.example.sensus.data.local.db

import android.content.Context
import androidx.room.Room
import com.example.sensus.data.local.security.DatabaseKeyManager
import net.sqlcipher.database.SupportFactory

object DatabaseFactory {
    private const val DATABASE_NAME = "sensus_encrypted.db"

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
            .build()
    }
}
