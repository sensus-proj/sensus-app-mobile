package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.push.NotificationsFirebaseDeviceProvider
import com.example.sensus.data.remote.api.NotificationsApiService
import com.example.sensus.data.repository.NotificationsRepositoryImpl
import com.example.sensus.domain.repository.NotificationsPushDeviceProvider
import com.example.sensus.domain.repository.NotificationsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.NotificationsGetSettingsUseCase
import com.example.sensus.domain.usecase.NotificationsRegisterDeviceUseCase
import com.example.sensus.domain.usecase.NotificationsUpdateSettingsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {
    @Binds
    abstract fun repository(implementation: NotificationsRepositoryImpl): NotificationsRepository

    @Binds
    abstract fun provider(implementation: NotificationsFirebaseDeviceProvider): NotificationsPushDeviceProvider

    companion object {
        @Provides
        fun api(container: SensusContainer): NotificationsApiService = container.networkClient.notificationsApi

        @Provides
        fun getSettings(repository: NotificationsRepository, session: SessionRepository): NotificationsGetSettingsUseCase =
            NotificationsGetSettingsUseCase(repository, session)

        @Provides
        fun updateSettings(repository: NotificationsRepository, session: SessionRepository): NotificationsUpdateSettingsUseCase =
            NotificationsUpdateSettingsUseCase(repository, session)

        @Provides
        fun registerDevice(
            provider: NotificationsPushDeviceProvider,
            repository: NotificationsRepository,
            session: SessionRepository
        ): NotificationsRegisterDeviceUseCase = NotificationsRegisterDeviceUseCase(provider, repository, session)
    }
}
