package com.example.sensus.di

import android.content.Context
import com.example.sensus.data.di.SensusContainer
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.moderation.ImageModerationService
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.usecase.DeleteAccountUseCase
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.domain.usecase.LoginUseCase
import com.example.sensus.domain.usecase.RegisterUseCase
import com.example.sensus.domain.usecase.RequestPasswordResetUseCase
import com.example.sensus.domain.usecase.ResetPasswordUseCase
import com.example.sensus.domain.usecase.StartGuestSessionUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSensusContainer(
        @ApplicationContext context: Context
    ): SensusContainer {
        return SensusContainer.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSecurePreferences(container: SensusContainer): SecurePreferences {
        return container.securePreferences
    }

    @Provides
    @Singleton
    fun provideAuthRepository(container: SensusContainer): AuthRepository {
        return container.authRepository
    }

    @Provides
    @Singleton
    fun provideProfileRepository(container: SensusContainer): ProfileRepository {
        return container.profileRepository
    }

    @Provides
    @Singleton
    fun provideMerchantRepository(container: SensusContainer): MerchantRepository {
        return container.merchantRepository
    }

    @Provides
    @Singleton
    fun provideLocationRepository(container: SensusContainer): LocationRepository {
        return container.locationRepository
    }

    @Provides
    @Singleton
    fun provideImageModerationService(container: SensusContainer): ImageModerationService {
        return container.moderationService
    }

    @Provides
    @Singleton
    fun provideLoginUseCase(container: SensusContainer): LoginUseCase {
        return container.loginUseCase
    }

    @Provides
    @Singleton
    fun provideRegisterUseCase(container: SensusContainer): RegisterUseCase {
        return container.registerUseCase
    }

    @Provides
    @Singleton
    fun provideRequestPasswordResetUseCase(container: SensusContainer): RequestPasswordResetUseCase {
        return container.requestPasswordResetUseCase
    }

    @Provides
    @Singleton
    fun provideResetPasswordUseCase(container: SensusContainer): ResetPasswordUseCase {
        return container.resetPasswordUseCase
    }

    @Provides
    @Singleton
    fun provideStartGuestSessionUseCase(container: SensusContainer): StartGuestSessionUseCase {
        return container.startGuestSessionUseCase
    }

    @Provides
    @Singleton
    fun provideDeleteAccountUseCase(container: SensusContainer): DeleteAccountUseCase {
        return container.deleteAccountUseCase
    }

    @Provides
    @Singleton
    fun provideUpdateProfileUseCase(container: SensusContainer): UpdateProfileUseCase {
        return container.updateProfileUseCase
    }

    @Provides
    @Singleton
    fun provideUpdateMerchantProfileUseCase(container: SensusContainer): UpdateMerchantProfileUseCase {
        return container.updateMerchantProfileUseCase
    }

    @Provides
    @Singleton
    fun provideGetUserLocationUseCase(container: SensusContainer): GetUserLocationUseCase {
        return container.getUserLocationUseCase
    }
}
