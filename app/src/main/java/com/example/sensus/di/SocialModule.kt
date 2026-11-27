package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.repository.FriendsActivityRepositoryImpl
import com.example.sensus.data.repository.ReportsRepositoryImpl
import com.example.sensus.data.repository.StoreReviewRepositoryImpl
import com.example.sensus.domain.repository.FriendsActivityRepository
import com.example.sensus.domain.repository.ReportsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.StoreReviewRepository
import com.example.sensus.domain.usecase.CreateStoreReviewUseCase
import com.example.sensus.domain.usecase.GetFriendsCommunityActivityUseCase
import com.example.sensus.domain.usecase.GetStoreReviewsUseCase
import com.example.sensus.domain.usecase.ReplyStoreReviewUseCase
import com.example.sensus.domain.usecase.SubmitContentReportUseCase
import com.example.sensus.domain.usecase.ToggleStoreReviewLikeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SocialModule {

    @Provides
    @Singleton
    fun provideStoreReviewRepository(container: SensusContainer): StoreReviewRepository = StoreReviewRepositoryImpl(container.networkClient.storeReviewsApi)

    @Provides
    fun provideGetStoreReviewsUseCase(repository: StoreReviewRepository): GetStoreReviewsUseCase = GetStoreReviewsUseCase(repository)

    @Provides
    fun provideCreateStoreReviewUseCase(repository: StoreReviewRepository): CreateStoreReviewUseCase = CreateStoreReviewUseCase(repository)

    @Provides
    fun provideToggleStoreReviewLikeUseCase(repository: StoreReviewRepository): ToggleStoreReviewLikeUseCase = ToggleStoreReviewLikeUseCase(repository)

    @Provides
    fun provideReplyStoreReviewUseCase(repository: StoreReviewRepository): ReplyStoreReviewUseCase = ReplyStoreReviewUseCase(repository)

    @Provides
    @Singleton
    fun provideFriendsActivityRepository(container: SensusContainer): FriendsActivityRepository =
        FriendsActivityRepositoryImpl(container.networkClient.friendsApi)

    @Provides
    fun provideGetFriendsCommunityActivityUseCase(repository: FriendsActivityRepository): GetFriendsCommunityActivityUseCase =
        GetFriendsCommunityActivityUseCase(repository)

    @Provides
    @Singleton
    fun provideReportsRepository(container: SensusContainer): ReportsRepository = ReportsRepositoryImpl(container.networkClient.reportsApi)

    @Provides
    fun provideSubmitContentReportUseCase(repository: ReportsRepository, session: SessionRepository): SubmitContentReportUseCase =
        SubmitContentReportUseCase(repository, session)
}
