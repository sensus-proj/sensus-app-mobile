package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.dao.SearchHistoryDao
import com.example.sensus.data.repository.DiscoveryRepositoryImpl
import com.example.sensus.domain.repository.DiscoveryRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.GetOfferAlternativesUseCase
import com.example.sensus.domain.usecase.SearchOffersUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiscoveryModule {

    @Provides
    @Singleton
    fun provideDiscoveryRepository(container: SensusContainer, offerDao: OfferDao, searchHistoryDao: SearchHistoryDao): DiscoveryRepository =
        DiscoveryRepositoryImpl(
            container.networkClient.discoveryApi,
            offerDao,
            searchHistoryDao
        )

    @Provides
    fun provideSearchOffersUseCase(repository: DiscoveryRepository, sessionRepository: SessionRepository): SearchOffersUseCase =
        SearchOffersUseCase(repository, sessionRepository)

    @Provides
    fun provideGetOfferAlternativesUseCase(repository: DiscoveryRepository): GetOfferAlternativesUseCase = GetOfferAlternativesUseCase(repository)
}
