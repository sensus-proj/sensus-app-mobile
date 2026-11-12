package com.example.sensus.data.di

import android.content.Context
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.db.DatabaseFactory
import com.example.sensus.data.local.db.SensusDatabase
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.local.db.dao.PendingSyncDao
import com.example.sensus.data.local.db.dao.UserPreferenceDao
import com.example.sensus.data.local.db.dao.UserSessionDao
import com.example.sensus.data.local.security.DatabaseKeyManager
import com.example.sensus.data.location.FusedLocationRepository
import com.example.sensus.data.moderation.ImageModerationService
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.repository.MapPreferencesRepositoryImpl
import com.example.sensus.data.repository.RetrofitAuthRepository
import com.example.sensus.data.repository.RetrofitMerchantRepository
import com.example.sensus.data.repository.RetrofitProfileRepository
import com.example.sensus.data.repository.SessionRepositoryImpl
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.MapPreferencesRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.DeleteAccountUseCase
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.domain.usecase.LoginUseCase
import com.example.sensus.domain.usecase.LogoutUseCase
import com.example.sensus.domain.usecase.RegisterUseCase
import com.example.sensus.domain.usecase.RequestPasswordResetUseCase
import com.example.sensus.domain.usecase.ResetPasswordUseCase
import com.example.sensus.domain.usecase.SavePreferredMapLayerUseCase
import com.example.sensus.domain.usecase.StartGuestSessionUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SensusContainer(
    context: Context,
    baseUrl: String = NetworkClient.DEFAULT_BASE_URL
) {
    private val containerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        com.example.sensus.data.SensusAuthManager.init(context)
    }

    val securePreferences: SecurePreferences by lazy {
        SecurePreferences(context.applicationContext)
    }

    val databaseKeyManager: DatabaseKeyManager by lazy {
        DatabaseKeyManager(securePreferences)
    }

    val database: SensusDatabase by lazy {
        DatabaseFactory.create(context.applicationContext, databaseKeyManager)
    }

    val offerDao: OfferDao by lazy { database.offerDao() }
    val userSessionDao: UserSessionDao by lazy { database.userSessionDao() }
    val pendingSyncDao: PendingSyncDao by lazy { database.pendingSyncDao() }
    val userPreferenceDao: UserPreferenceDao by lazy { database.userPreferenceDao() }

    val networkClient: NetworkClient by lazy {
        NetworkClient(
            securePreferences = securePreferences,
            baseUrl = baseUrl,
            onUnauthorized = {
                containerScope.launch {
                    sessionRepository.clearLocalSession()
                }
            }
        )
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepositoryImpl(
            securePreferences = securePreferences,
            userSessionDao = userSessionDao,
            pendingSyncDao = pendingSyncDao,
            offerDao = offerDao,
            authApiService = networkClient.authApi
        )
    }

    private fun notifySessionUpdated() {
        (sessionRepository as? SessionRepositoryImpl)?.refreshSync()
    }

    val authRepository: AuthRepository by lazy {
        RetrofitAuthRepository(
            authApi = networkClient.authApi,
            securePreferences = securePreferences,
            onSessionUpdated = { notifySessionUpdated() }
        )
    }

    val moderationService: ImageModerationService by lazy {
        ImageModerationService(networkClient.mediaApi)
    }

    val profileRepository: ProfileRepository by lazy {
        RetrofitProfileRepository(
            profileApi = networkClient.profileApi,
            moderationService = moderationService,
            securePreferences = securePreferences,
            onSessionUpdated = { notifySessionUpdated() }
        )
    }

    val merchantRepository: MerchantRepository by lazy {
        RetrofitMerchantRepository(networkClient.merchantApi)
    }

    val locationRepository: LocationRepository by lazy {
        FusedLocationRepository(context.applicationContext)
    }

    val mapPreferencesRepository: MapPreferencesRepository by lazy {
        MapPreferencesRepositoryImpl(userPreferenceDao)
    }

    // Sprint 1 Use Cases
    val loginUseCase by lazy { LoginUseCase(authRepository) }
    val logoutUseCase by lazy { LogoutUseCase(authRepository, sessionRepository) }
    val registerUseCase by lazy { RegisterUseCase(authRepository) }
    val requestPasswordResetUseCase by lazy { RequestPasswordResetUseCase(authRepository) }
    val resetPasswordUseCase by lazy { ResetPasswordUseCase(authRepository) }
    val startGuestSessionUseCase by lazy { StartGuestSessionUseCase(authRepository, sessionRepository) }
    val deleteAccountUseCase by lazy { DeleteAccountUseCase(authRepository, sessionRepository) }
    val updateProfileUseCase by lazy { UpdateProfileUseCase(profileRepository) }
    val updateMerchantProfileUseCase by lazy { UpdateMerchantProfileUseCase(merchantRepository) }
    val getUserLocationUseCase by lazy { GetUserLocationUseCase(locationRepository) }
    val getPreferredMapLayerUseCase by lazy { GetPreferredMapLayerUseCase(mapPreferencesRepository) }
    val savePreferredMapLayerUseCase by lazy { SavePreferredMapLayerUseCase(mapPreferencesRepository) }

    companion object {
        @Volatile
        private var instance: SensusContainer? = null

        fun getInstance(context: Context): SensusContainer {
            return instance ?: synchronized(this) {
                instance ?: SensusContainer(context).also { instance = it }
            }
        }
    }
}
