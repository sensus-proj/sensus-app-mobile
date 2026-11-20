package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.local.db.dao.FeedOfferDao
import com.example.sensus.data.repository.InterestsRepositoryImpl
import com.example.sensus.data.repository.OfferFeedRepositoryImpl
import com.example.sensus.domain.repository.InterestsRepository
import com.example.sensus.domain.repository.OfferFeedRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.GetMyInterestsUseCase
import com.example.sensus.domain.usecase.GetOfferFeedUseCase
import com.example.sensus.domain.usecase.UpdateMyInterestsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OfferFeedModule {

    @Provides
    @Singleton
    fun provideOfferFeedRepository(container: SensusContainer, feedOfferDao: FeedOfferDao): OfferFeedRepository =
        OfferFeedRepositoryImpl(container.networkClient.offerFeedApi, feedOfferDao)

    @Provides
    @Singleton
    fun provideInterestsRepository(container: SensusContainer): InterestsRepository = InterestsRepositoryImpl(container.networkClient.interestsApi)

    @Provides
    fun provideGetOfferFeedUseCase(repository: OfferFeedRepository, sessionRepository: SessionRepository): GetOfferFeedUseCase =
        GetOfferFeedUseCase(repository, sessionRepository)

    @Provides
    fun provideGetMyInterestsUseCase(repository: InterestsRepository, sessionRepository: SessionRepository): GetMyInterestsUseCase =
        GetMyInterestsUseCase(repository, sessionRepository)

    @Provides
    fun provideUpdateMyInterestsUseCase(repository: InterestsRepository, sessionRepository: SessionRepository): UpdateMyInterestsUseCase =
        UpdateMyInterestsUseCase(repository, sessionRepository)
}
