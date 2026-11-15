package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.repository.ClaimsRepositoryImpl
import com.example.sensus.domain.repository.ClaimsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.CreateClaimUseCase
import com.example.sensus.domain.usecase.RedeemClaimUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClaimsModule {
    @Provides
    @Singleton
    fun provideClaimsRepository(container: SensusContainer, sessions: SessionRepository): ClaimsRepository =
        ClaimsRepositoryImpl(container.networkClient.claimsApi, sessions)

    @Provides
    fun provideCreateClaimUseCase(repository: ClaimsRepository): CreateClaimUseCase = CreateClaimUseCase(repository)

    @Provides
    fun provideRedeemClaimUseCase(repository: ClaimsRepository): RedeemClaimUseCase = RedeemClaimUseCase(repository)
}
