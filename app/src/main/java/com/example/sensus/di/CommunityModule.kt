package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.local.db.SensusDatabase
import com.example.sensus.data.repository.FeedRepositoryImpl
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.repository.FeedRepository
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.CreateFeedPostUseCase
import com.example.sensus.domain.usecase.GetNearbyFeedUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommunityModule {
    @Provides
    @Singleton
    fun provideFeedRepository(container: SensusContainer, database: SensusDatabase): FeedRepository =
        FeedRepositoryImpl(container.networkClient.feedApi, database.feedPostDao())

    @Provides
    fun provideGetNearbyFeedUseCase(repository: FeedRepository, session: SessionRepository): GetNearbyFeedUseCase = GetNearbyFeedUseCase(repository, session)

    @Provides
    fun provideCreateFeedPostUseCase(
        repository: FeedRepository,
        session: SessionRepository,
        media: MediaRepository,
        compressor: ImageCompressor
    ): CreateFeedPostUseCase = CreateFeedPostUseCase(repository, session, media, compressor)
}
