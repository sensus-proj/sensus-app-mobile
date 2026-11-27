package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.domain.repository.FavoritesRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.GetFavoritesUseCase
import com.example.sensus.domain.usecase.ObserveFavoritesUseCase
import com.example.sensus.domain.usecase.ToggleFavoriteUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FavoritesModule {

    @Provides
    @Singleton
    fun provideFavoritesRepository(container: SensusContainer): FavoritesRepository = container.favoritesRepository

    @Provides
    fun provideToggleFavoriteUseCase(repository: FavoritesRepository, sessionRepository: SessionRepository): ToggleFavoriteUseCase =
        ToggleFavoriteUseCase(repository, sessionRepository)

    @Provides
    fun provideGetFavoritesUseCase(repository: FavoritesRepository, sessionRepository: SessionRepository): GetFavoritesUseCase =
        GetFavoritesUseCase(repository, sessionRepository)

    @Provides
    fun provideObserveFavoritesUseCase(repository: FavoritesRepository): ObserveFavoritesUseCase = ObserveFavoritesUseCase(repository)
}
