package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.push.AndroidNotificationPermissionChecker
import com.example.sensus.data.push.NotificationsFirebaseDeviceProvider
import com.example.sensus.data.remote.api.NotificationsApiService
import com.example.sensus.data.repository.NotificationsRepositoryImpl
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.NotificationPermissionChecker
import com.example.sensus.domain.repository.NotificationsLocalStore
import com.example.sensus.domain.repository.NotificationsPushDeviceProvider
import com.example.sensus.domain.repository.NotificationsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.NotificationsDeviceSync
import com.example.sensus.domain.usecase.NotificationsGetSettingsUseCase
import com.example.sensus.domain.usecase.NotificationsLocationUseCase
import com.example.sensus.domain.usecase.NotificationsRegisterDeviceUseCase
import com.example.sensus.domain.usecase.NotificationsUpdateSettingsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {
    @Binds
    abstract fun repository(implementation: NotificationsRepositoryImpl): NotificationsRepository

    @Binds
    abstract fun provider(implementation: NotificationsFirebaseDeviceProvider): NotificationsPushDeviceProvider

    @Binds
    abstract fun permissionChecker(implementation: AndroidNotificationPermissionChecker): NotificationPermissionChecker

    companion object {
        @Provides
        fun api(container: SensusContainer): NotificationsApiService = container.networkClient.notificationsApi

        @Provides
        fun localStore(container: SensusContainer): NotificationsLocalStore = container.notificationsLocalStore

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
            session: SessionRepository,
            permission: NotificationPermissionChecker
        ): NotificationsRegisterDeviceUseCase = NotificationsRegisterDeviceUseCase(provider, repository, session, permission)

        @Provides
        fun location(
            repository: NotificationsRepository,
            session: SessionRepository,
            location: LocationRepository,
            store: NotificationsLocalStore
        ): NotificationsLocationUseCase = NotificationsLocationUseCase(repository, session, location, store)

        @Provides
        @Singleton
        fun deviceSync(
            register: NotificationsRegisterDeviceUseCase,
            session: SessionRepository,
            permission: NotificationPermissionChecker
        ): NotificationsDeviceSync = NotificationsDeviceSync(
            register = register,
            session = session,
            permission = permission,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }
}
