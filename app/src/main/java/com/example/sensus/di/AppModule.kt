package com.example.sensus.di

import android.content.Context
import com.example.sensus.data.DefaultDataRepository
import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.moderation.ImageModerationService
import com.example.sensus.data.repository.CartRepositoryImpl
import com.example.sensus.data.repository.OffersRepositoryImpl
import com.example.sensus.data.repository.StoreRepositoryImpl
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.CartRepository
import com.example.sensus.domain.repository.DataRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.domain.usecase.DeleteAccountUseCase
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.domain.usecase.LoginUseCase
import com.example.sensus.domain.usecase.RegisterUseCase
import com.example.sensus.domain.usecase.RequestPasswordResetUseCase
import com.example.sensus.domain.usecase.ResetPasswordUseCase
import com.example.sensus.domain.usecase.StartGuestSessionUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.dao.PendingSyncDao
import com.example.sensus.data.local.db.dao.UserSessionDao
import com.example.sensus.data.repository.SessionRepositoryImpl
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
    fun provideSensusContainer(
        @ApplicationContext context: Context
    ): SensusContainer {
        return SensusContainer.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSecurePreferences(container: SensusContainer): SecurePreferences {
        return container.securePreferences
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        securePreferences: SecurePreferences,
        userSessionDao: UserSessionDao,
        pendingSyncDao: PendingSyncDao,
        offerDao: OfferDao
    ): SessionRepository {
        return SessionRepositoryImpl(
            securePreferences = securePreferences,
            userSessionDao = userSessionDao,
            pendingSyncDao = pendingSyncDao,
            offerDao = offerDao
        )
    }

    @Provides
    @Singleton
    fun provideAuthRepository(container: SensusContainer): AuthRepository {
        return container.authRepository
    }

    @Provides
    @Singleton
    fun provideProfileRepository(container: SensusContainer): ProfileRepository {
        return container.profileRepository
    }

    @Provides
    @Singleton
    fun provideMerchantRepository(container: SensusContainer): MerchantRepository {
        return container.merchantRepository
    }

    @Provides
    @Singleton
    fun provideLocationRepository(container: SensusContainer): LocationRepository {
        return container.locationRepository
    }

    @Provides
    @Singleton
    fun provideImageModerationService(container: SensusContainer): ImageModerationService {
        return container.moderationService
    }

    @Provides
    @Singleton
    fun provideCartRepository(): CartRepository {
        return CartRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideOffersRepository(offerDao: OfferDao): OffersRepository {
        return OffersRepositoryImpl(offerDao)
    }

    @Provides
    @Singleton
    fun provideStoreRepository(): StoreRepository {
        return StoreRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideDataRepository(): DataRepository {
        return DefaultDataRepository()
    }

    @Provides
    @Singleton
    fun provideLoginUseCase(container: SensusContainer): LoginUseCase {
        return container.loginUseCase
    }

    @Provides
    @Singleton
    fun provideRegisterUseCase(container: SensusContainer): RegisterUseCase {
        return container.registerUseCase
    }

    @Provides
    @Singleton
    fun provideRequestPasswordResetUseCase(container: SensusContainer): RequestPasswordResetUseCase {
        return container.requestPasswordResetUseCase
    }

    @Provides
    @Singleton
    fun provideResetPasswordUseCase(container: SensusContainer): ResetPasswordUseCase {
        return container.resetPasswordUseCase
    }

    @Provides
    @Singleton
    fun provideStartGuestSessionUseCase(container: SensusContainer): StartGuestSessionUseCase {
        return container.startGuestSessionUseCase
    }

    @Provides
    @Singleton
    fun provideDeleteAccountUseCase(container: SensusContainer): DeleteAccountUseCase {
        return container.deleteAccountUseCase
    }

    @Provides
    @Singleton
    fun provideUpdateProfileUseCase(container: SensusContainer): UpdateProfileUseCase {
        return container.updateProfileUseCase
    }

    @Provides
    @Singleton
    fun provideUpdateMerchantProfileUseCase(container: SensusContainer): UpdateMerchantProfileUseCase {
        return container.updateMerchantProfileUseCase
    }

    @Provides
    @Singleton
    fun provideGetUserLocationUseCase(container: SensusContainer): GetUserLocationUseCase {
        return container.getUserLocationUseCase
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return ConnectivityNetworkMonitor(context)
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        pendingSyncDao: PendingSyncDao,
        networkMonitor: NetworkMonitor
    ): SyncManager {
        return DefaultSyncManager(pendingSyncDao, networkMonitor)
    }
}
