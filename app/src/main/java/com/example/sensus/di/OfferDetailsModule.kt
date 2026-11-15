package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.repository.OfferDetailsRepositoryImpl
import com.example.sensus.domain.repository.OfferDetailsRepository
import com.example.sensus.domain.usecase.GetOfferDetailUseCase
import com.example.sensus.domain.usecase.PrepareCartClaimUseCase
import com.example.sensus.domain.usecase.RegisterOfferViewUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OfferDetailsModule {
    @Provides
    @Singleton
    fun provideOfferDetailsRepository(container: SensusContainer, dao: OfferDao): OfferDetailsRepository =
        OfferDetailsRepositoryImpl(container.networkClient.offerDetailsApi, dao)

    @Provides
    fun provideGetOfferDetailUseCase(repository: OfferDetailsRepository): GetOfferDetailUseCase = GetOfferDetailUseCase(repository)

    @Provides
    fun provideRegisterOfferViewUseCase(repository: OfferDetailsRepository): RegisterOfferViewUseCase = RegisterOfferViewUseCase(repository)

    @Provides
    fun providePrepareCartClaimUseCase(): PrepareCartClaimUseCase = PrepareCartClaimUseCase()
}
