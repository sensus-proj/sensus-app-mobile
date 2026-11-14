package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.domain.usecase.ActivateMerchantUseCase
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.domain.usecase.GetNearbyMerchantsUseCase
import com.example.sensus.domain.usecase.ResolveAddressFromCoordinatesUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MerchantUseCaseModule {

    @Provides
    @Singleton
    fun provideActivateMerchantUseCase(container: SensusContainer): ActivateMerchantUseCase = container.activateMerchantUseCase

    @Provides
    @Singleton
    fun provideGetMyMerchantUseCase(container: SensusContainer): GetMyMerchantUseCase = container.getMyMerchantUseCase

    @Provides
    @Singleton
    fun provideGetNearbyMerchantsUseCase(container: SensusContainer): GetNearbyMerchantsUseCase = container.getNearbyMerchantsUseCase

    @Provides
    @Singleton
    fun provideUpdateMerchantProfileUseCase(container: SensusContainer): UpdateMerchantProfileUseCase = container.updateMerchantProfileUseCase

    @Provides
    @Singleton
    fun provideResolveAddressFromCoordinatesUseCase(container: SensusContainer): ResolveAddressFromCoordinatesUseCase =
        container.resolveAddressFromCoordinatesUseCase
}
