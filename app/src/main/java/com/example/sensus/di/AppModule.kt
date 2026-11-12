package com.example.sensus.di

import android.content.Context
import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.dao.PendingSyncDao
import com.example.sensus.data.moderation.ImageModerationService
import com.example.sensus.data.network.ConnectivityNetworkMonitor
import com.example.sensus.data.sync.DefaultSyncManager
import com.example.sensus.domain.network.NetworkMonitor
import com.example.sensus.domain.sync.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSensusContainer(@ApplicationContext context: Context): SensusContainer {
        return SensusContainer.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSecurePreferences(container: SensusContainer): SecurePreferences {
        return container.securePreferences
    }

    @Provides
    @Singleton
    fun provideImageModerationService(container: SensusContainer): ImageModerationService {
        return container.moderationService
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return ConnectivityNetworkMonitor(context)
    }

    @Provides
    @Singleton
    fun provideSyncManager(pendingSyncDao: PendingSyncDao, networkMonitor: NetworkMonitor): SyncManager {
        return DefaultSyncManager(pendingSyncDao, networkMonitor)
    }
}
