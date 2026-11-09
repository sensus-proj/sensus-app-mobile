package com.example.sensus.di

import android.content.Context
import com.example.sensus.data.local.db.DatabaseFactory
import com.example.sensus.data.local.db.SensusDatabase
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.dao.PendingSyncDao
import com.example.sensus.data.local.db.dao.UserSessionDao
import com.example.sensus.data.local.security.DatabaseKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSensusDatabase(
        @ApplicationContext context: Context,
        keyManager: DatabaseKeyManager
    ): SensusDatabase {
        return DatabaseFactory.create(context, keyManager)
    }

    @Provides
    @Singleton
    fun provideOfferDao(database: SensusDatabase): OfferDao {
        return database.offerDao()
    }

    @Provides
    @Singleton
    fun provideUserSessionDao(database: SensusDatabase): UserSessionDao {
        return database.userSessionDao()
    }

    @Provides
    @Singleton
    fun providePendingSyncDao(database: SensusDatabase): PendingSyncDao {
        return database.pendingSyncDao()
    }
}
