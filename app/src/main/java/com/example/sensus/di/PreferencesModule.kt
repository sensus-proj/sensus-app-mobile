package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.SavePreferredMapLayerUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideGetPreferredMapLayerUseCase(container: SensusContainer): GetPreferredMapLayerUseCase = container.getPreferredMapLayerUseCase

    @Provides
    @Singleton
    fun provideSavePreferredMapLayerUseCase(container: SensusContainer): SavePreferredMapLayerUseCase = container.savePreferredMapLayerUseCase
}
