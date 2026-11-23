package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.local.db.SensusDatabase
import com.example.sensus.data.repository.FeedRepositoryImpl
import com.example.sensus.data.repository.ReportsRepositoryImpl
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.repository.FeedRepository
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.ReportsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.AddFeedCommentUseCase
import com.example.sensus.domain.usecase.CreateFeedPostUseCase
import com.example.sensus.domain.usecase.DeleteFeedPostUseCase
import com.example.sensus.domain.usecase.GetFeedCommentsUseCase
import com.example.sensus.domain.usecase.GetNearbyFeedUseCase
import com.example.sensus.domain.usecase.SubmitContentReportUseCase
import com.example.sensus.domain.usecase.TogglePostLikeUseCase
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
    @Singleton
    fun provideReportsRepository(container: SensusContainer): ReportsRepository = ReportsRepositoryImpl(container.networkClient.reportsApi)

    @Provides
    fun provideGetNearbyFeedUseCase(repository: FeedRepository, session: SessionRepository): GetNearbyFeedUseCase = GetNearbyFeedUseCase(repository, session)

    @Provides
    fun provideCreateFeedPostUseCase(
        repository: FeedRepository,
        session: SessionRepository,
        media: MediaRepository,
        compressor: ImageCompressor
    ): CreateFeedPostUseCase = CreateFeedPostUseCase(repository, session, media, compressor)

    @Provides
    fun provideTogglePostLikeUseCase(repository: FeedRepository, session: SessionRepository): TogglePostLikeUseCase = TogglePostLikeUseCase(repository, session)

    @Provides
    fun provideGetFeedCommentsUseCase(repository: FeedRepository): GetFeedCommentsUseCase = GetFeedCommentsUseCase(repository)

    @Provides
    fun provideAddFeedCommentUseCase(repository: FeedRepository, session: SessionRepository): AddFeedCommentUseCase = AddFeedCommentUseCase(repository, session)

    @Provides
    fun provideDeleteFeedPostUseCase(repository: FeedRepository, session: SessionRepository): DeleteFeedPostUseCase = DeleteFeedPostUseCase(repository, session)

    @Provides
    fun provideSubmitContentReportUseCase(repository: ReportsRepository, session: SessionRepository): SubmitContentReportUseCase =
        SubmitContentReportUseCase(repository, session)
}
