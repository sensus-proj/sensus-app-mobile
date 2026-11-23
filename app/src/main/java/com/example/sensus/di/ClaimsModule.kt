package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.repository.ClaimsRepositoryImpl
import com.example.sensus.domain.repository.ClaimsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.CancelClaimUseCase
import com.example.sensus.domain.usecase.CreateClaimUseCase
import com.example.sensus.domain.usecase.GetMyClaimsUseCase
import com.example.sensus.domain.usecase.RedeemClaimUseCase
import com.example.sensus.domain.usecase.RestoreClaimUseCase
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
        ClaimsRepositoryImpl(container.networkClient.claimsApi, sessions, container.securePreferences.claimIndex)

    @Provides
    fun provideCreateClaimUseCase(repository: ClaimsRepository): CreateClaimUseCase = CreateClaimUseCase(repository)

    @Provides
    fun provideRestoreClaimUseCase(repository: ClaimsRepository): RestoreClaimUseCase = RestoreClaimUseCase(repository)

    @Provides
    fun provideRedeemClaimUseCase(repository: ClaimsRepository): RedeemClaimUseCase = RedeemClaimUseCase(repository)

    @Provides
    fun provideGetMyClaimsUseCase(repository: ClaimsRepository): GetMyClaimsUseCase = GetMyClaimsUseCase(repository)

    @Provides
    fun provideCancelClaimUseCase(repository: ClaimsRepository): CancelClaimUseCase = CancelClaimUseCase(repository)
}
