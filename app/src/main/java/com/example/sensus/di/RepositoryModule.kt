package com.example.sensus.di

import com.example.sensus.data.DefaultDataRepository
import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.repository.CartRepositoryImpl
import com.example.sensus.data.repository.OffersRepositoryImpl
import com.example.sensus.data.repository.StoreRepositoryImpl
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.CartRepository
import com.example.sensus.domain.repository.DataRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.MapPreferencesRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.StoreRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSessionRepository(container: SensusContainer): SessionRepository = container.sessionRepository

    @Provides
    @Singleton
    fun provideAuthRepository(container: SensusContainer): AuthRepository = container.authRepository

    @Provides
    @Singleton
    fun provideProfileRepository(container: SensusContainer): ProfileRepository = container.profileRepository

    @Provides
    @Singleton
    fun provideMerchantRepository(container: SensusContainer): MerchantRepository = container.merchantRepository

    @Provides
    @Singleton
    fun provideLocationRepository(container: SensusContainer): LocationRepository = container.locationRepository

    @Provides
    @Singleton
    fun provideMapPreferencesRepository(container: SensusContainer): MapPreferencesRepository = container.mapPreferencesRepository

    @Provides
    @Singleton
    fun provideCartRepository(): CartRepository = CartRepositoryImpl()

    @Provides
    @Singleton
    fun provideOffersRepository(offerDao: OfferDao): OffersRepository = OffersRepositoryImpl(offerDao)

    @Provides
    @Singleton
    fun provideStoreRepository(): StoreRepository = StoreRepositoryImpl()

    @Provides
    @Singleton
    fun provideDataRepository(): DataRepository = DefaultDataRepository()
}
