package com.example.sensus.di

import com.example.sensus.data.di.SensusContainer
import com.example.sensus.domain.usecase.DeleteAccountUseCase
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.domain.usecase.LoginUseCase
import com.example.sensus.domain.usecase.LogoutUseCase
import com.example.sensus.domain.usecase.RegisterUseCase
import com.example.sensus.domain.usecase.RequestPasswordResetUseCase
import com.example.sensus.domain.usecase.ResetPasswordUseCase
import com.example.sensus.domain.usecase.StartGuestSessionUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideLoginUseCase(container: SensusContainer): LoginUseCase = container.loginUseCase

    @Provides
    @Singleton
    fun provideLogoutUseCase(container: SensusContainer): LogoutUseCase = container.logoutUseCase

    @Provides
    @Singleton
    fun provideRegisterUseCase(container: SensusContainer): RegisterUseCase = container.registerUseCase

    @Provides
    @Singleton
    fun provideRequestPasswordResetUseCase(container: SensusContainer): RequestPasswordResetUseCase = container.requestPasswordResetUseCase

    @Provides
    @Singleton
    fun provideResetPasswordUseCase(container: SensusContainer): ResetPasswordUseCase = container.resetPasswordUseCase

    @Provides
    @Singleton
    fun provideStartGuestSessionUseCase(container: SensusContainer): StartGuestSessionUseCase = container.startGuestSessionUseCase

    @Provides
    @Singleton
    fun provideDeleteAccountUseCase(container: SensusContainer): DeleteAccountUseCase = container.deleteAccountUseCase

    @Provides
    @Singleton
    fun provideUpdateProfileUseCase(container: SensusContainer): UpdateProfileUseCase = container.updateProfileUseCase

    @Provides
    @Singleton
    fun provideUpdateMerchantProfileUseCase(container: SensusContainer): UpdateMerchantProfileUseCase = container.updateMerchantProfileUseCase

    @Provides
    @Singleton
    fun provideGetUserLocationUseCase(container: SensusContainer): GetUserLocationUseCase = container.getUserLocationUseCase
}
