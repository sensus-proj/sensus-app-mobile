package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.SessionType
import com.example.sensus.data.local.StoredMerchantStatus
import com.example.sensus.data.mapper.MerchantMapper.toDomain
import com.example.sensus.data.mapper.MerchantMapper.toDto
import com.example.sensus.data.mapper.UserMapper.toDomain
import com.example.sensus.data.moderation.ImageModerationService
import com.example.sensus.data.remote.api.AuthApiService
import com.example.sensus.data.remote.api.MerchantApiService
import com.example.sensus.data.remote.api.ProfileApiService
import com.example.sensus.data.remote.dto.ActivateMerchantRequest
import com.example.sensus.data.remote.dto.AuthResponse
import com.example.sensus.data.remote.dto.DeleteAccountRequest
import com.example.sensus.data.remote.dto.LoginRequest
import com.example.sensus.data.remote.dto.PhotoSignatureResponse
import com.example.sensus.data.remote.dto.RegisterRequest
import com.example.sensus.data.remote.dto.RequestPasswordResetRequest
import com.example.sensus.data.remote.dto.ResetPasswordRequest
import com.example.sensus.data.remote.dto.UpdateMerchantProfileRequest
import com.example.sensus.data.remote.dto.UpdateProfileRequest
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantActivationInput
import com.example.sensus.domain.model.MerchantCoordinates
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.MerchantProfileUpdateInput
import com.example.sensus.domain.model.ModerationResult
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.ProfileRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import retrofit2.Response

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_METHOD_NOT_ALLOWED = 405
private const val HTTP_CONFLICT = 409
private const val HTTP_UNPROCESSABLE = 422
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR = 500
private const val OFFLINE_ERROR_MESSAGE = "Sin conexión a internet. Verifica tu red e inténtalo de nuevo."
private val URL_LEAK_REGEX = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

internal suspend inline fun <T> safeNetworkCall(unexpectedMessage: String, crossinline block: suspend () -> Result<T>): Result<T> =
    withContext(Dispatchers.IO) {
        try {
            block()
        } catch (_: IOException) {
            Result.Failure(AppError.Network(OFFLINE_ERROR_MESSAGE))
        } catch (_: HttpException) {
            Result.Failure(AppError.Generic(unexpectedMessage))
        } catch (_: SerializationException) {
            Result.Failure(AppError.Generic(unexpectedMessage))
        } catch (_: IllegalStateException) {
            Result.Failure(AppError.Generic(unexpectedMessage))
        }
    }

internal fun parseBackendErrorMessage(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val extracted = extractJsonErrorField(trimmed) ?: trimmed
    return sanitizeUserErrorMessage(extracted)
}

private val errorJsonKeys = listOf("detail", "message", "error", "title")

private fun extractJsonErrorField(trimmed: String): String? = try {
    val json = Json.parseToJsonElement(trimmed).jsonObject
    errorJsonKeys.firstNotNullOfOrNull { key -> json[key]?.jsonPrimitive?.content }
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

private fun sanitizeUserErrorMessage(candidate: String?): String? {
    val clean = candidate?.replace(URL_LEAK_REGEX, "")?.trim()
    return clean?.takeIf { it.isNotBlank() && !it.startsWith("<") && !it.startsWith("{") }
}

private val httpDefaultErrorMessages = mapOf(
    HTTP_BAD_REQUEST to "Datos de entrada inválidos.",
    HTTP_FORBIDDEN to "Acceso restringido.",
    HTTP_UNPROCESSABLE to "Entidad no procesable.",
    HTTP_TOO_MANY_REQUESTS to "Límite de peticiones alcanzado. Intenta de nuevo en unos minutos.",
    HTTP_SERVER_ERROR to "Error interno en el servidor."
)

internal fun mapCommonHttpStatus(code: Int, message: String?, default401: String, default404: String): AppError {
    val resolved = message ?: when (code) {
        HTTP_UNAUTHORIZED -> default401
        HTTP_NOT_FOUND -> default404
        else -> httpDefaultErrorMessages[code] ?: "Error del servidor HTTP $code."
    }
    return buildHttpAppError(code, resolved)
}

private fun buildHttpAppError(code: Int, message: String): AppError = when (code) {
    HTTP_BAD_REQUEST -> AppError.Validation(message)
    HTTP_UNAUTHORIZED -> AppError.Unauthorized(message)
    HTTP_FORBIDDEN -> AppError.Forbidden(message)
    HTTP_NOT_FOUND -> AppError.NotFound(message)
    HTTP_UNPROCESSABLE, HTTP_TOO_MANY_REQUESTS -> AppError.Unprocessable(message)
    else -> AppError.Generic(message)
}

class RetrofitAuthRepository(
    private val authApi: AuthApiService,
    private val securePreferences: SecurePreferences,
    private val onSessionUpdated: (() -> Unit)? = null
) : AuthRepository {

    private fun handleAuthSuccess(body: AuthResponse): Result<UserProfile> {
        securePreferences.saveAuthToken(body.token)
        securePreferences.saveSessionType(SessionType.REGISTERED)
        val domainUser = body.user.toDomain()
        securePreferences.saveUserSession(
            name = domainUser.nombreCompleto,
            email = domainUser.correo,
            photoUrl = domainUser.fotoPerfilUrl
        )
        val hasMerchantRole = domainUser.roles.any {
            it.equals("Comerciante", ignoreCase = true) || it.equals("Merchant", ignoreCase = true)
        }
        val existing = securePreferences.storedMerchantStatus
        securePreferences.storedMerchantStatus = if (hasMerchantRole) {
            existing.copy(isRegistered = true)
        } else {
            StoredMerchantStatus()
        }
        onSessionUpdated?.invoke()
        return Result.Success(domainUser)
    }

    override suspend fun login(correo: String, password: String): Result<UserProfile> = safeNetworkCall("Error inesperado al iniciar sesión.") {
        val response = authApi.login(LoginRequest(correo = correo, password = password))
        val body = response.body()
        if (response.isSuccessful && body != null) handleAuthSuccess(body) else mapHttpError(response)
    }

    override suspend fun register(nombreCompleto: String, correo: String, password: String): Result<UserProfile> =
        safeNetworkCall("Error inesperado al registrar usuario.") {
            val response = authApi.register(
                RegisterRequest(nombreCompleto = nombreCompleto, correo = correo, password = password)
            )
            val body = response.body()
            if (response.isSuccessful && body != null) handleAuthSuccess(body) else mapHttpError(response)
        }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = safeNetworkCall("Error inesperado.") {
        val response = authApi.requestPasswordReset(RequestPasswordResetRequest(correo = email))
        if (response.isSuccessful) Result.Success(Unit) else mapHttpError(response)
    }

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> = safeNetworkCall("Error inesperado.") {
        val response = authApi.resetPassword(ResetPasswordRequest(token = token, nuevaPassword = newPassword))
        if (response.isSuccessful) Result.Success(Unit) else mapHttpError(response)
    }

    override suspend fun createGuestSession(): Result<String> = safeNetworkCall("Error inesperado.") {
        val response = authApi.createGuestSession()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            securePreferences.saveGuestSession(body.token, body.maxRadiusMeters)
            onSessionUpdated?.invoke()
            Result.Success(body.token)
        } else {
            mapHttpError(response)
        }
    }

    override suspend fun logout(deviceId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            authApi.logout(deviceId)
            Result.Success(Unit)
        } catch (_: IOException) {
            Result.Success(Unit)
        } catch (_: HttpException) {
            Result.Success(Unit)
        } finally {
            securePreferences.clearAll()
            onSessionUpdated?.invoke()
        }
    }

    override suspend fun deleteAccount(password: String): Result<Unit> = safeNetworkCall("Error inesperado.") {
        val response = authApi.deleteAccount(DeleteAccountRequest(password = password))
        if (response.isSuccessful) {
            securePreferences.clearAll()
            onSessionUpdated?.invoke()
            Result.Success(Unit)
        } else {
            mapHttpError(response)
        }
    }

    private fun <T> mapHttpError(response: Response<T>): Result<Nothing> {
        val rawError = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
        val parsedMsg = parseBackendErrorMessage(rawError)
        return Result.Failure(
            mapCommonHttpStatus(
                code = response.code(),
                message = parsedMsg,
                default401 = "Correo electrónico o contraseña incorrectos.",
                default404 = "No existe una cuenta registrada con este correo."
            )
        )
    }
}

class RetrofitProfileRepository(
    private val profileApi: ProfileApiService,
    private val moderationService: ImageModerationService = ImageModerationService(),
    private val securePreferences: SecurePreferences? = null,
    private val onSessionUpdated: (() -> Unit)? = null
) : ProfileRepository {

    private val cloudinaryUploader = CloudinarySignedUploader()

    override suspend fun getProfile(): Result<UserProfile> = safeNetworkCall("Error al obtener perfil.") {
        val response = profileApi.getProfile()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            val profile = body.toDomain()
            persistSessionProfile(profile)
            Result.Success(profile)
        } else {
            mapHttpError(response)
        }
    }

    override suspend fun updateProfile(nombreCompleto: String, fotoPerfilUrl: String?): Result<UserProfile> = safeNetworkCall("Error al actualizar perfil.") {
        val response = profileApi.updateProfile(
            UpdateProfileRequest(nombreCompleto = nombreCompleto, fotoPerfilUrl = fotoPerfilUrl)
        )
        val body = response.body()
        if (response.isSuccessful && body != null) {
            val updated = body.toDomain()
            persistSessionProfile(updated)
            Result.Success(updated)
        } else {
            mapHttpError(response)
        }
    }

    override suspend fun requestPhotoSignature(): Result<Pair<String, Long>> = safeNetworkCall("Error al generar firma de foto.") {
        val response = profileApi.generatePhotoSignature()
        val dto = response.body()
        if (response.isSuccessful && dto != null) {
            Result.Success(Pair(dto.signature, dto.timestamp))
        } else {
            mapHttpError(response)
        }
    }

    override suspend fun uploadProfilePhoto(imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        val moderationResult = moderationService.moderateImage(imageBytes, "Foto de perfil de usuario")
        if (moderationResult is ModerationResult.Rejected) {
            return@withContext Result.Failure(AppError.Validation(moderationResult.reason))
        }
        if (moderationResult is ModerationResult.Quarantine) {
            return@withContext Result.Failure(AppError.Validation("La imagen está en revisión preventiva: ${moderationResult.reason}"))
        }
        safeNetworkCall("Error al procesar la subida de foto.") {
            val sigResponse = profileApi.generatePhotoSignature()
            val sig = sigResponse.body()
            if (!sigResponse.isSuccessful || sig == null) {
                mapHttpError(sigResponse)
            } else {
                uploadBytesToCloudinary(imageBytes, sig)
            }
        }
    }

    private fun uploadBytesToCloudinary(imageBytes: ByteArray, sig: PhotoSignatureResponse): Result<String> =
        when (val upload = cloudinaryUploader.upload(imageBytes, "image/jpeg", "profile_avatar.jpg", sig)) {
            is Result.Success -> Result.Success(upload.data.url)
            is Result.Failure -> upload
        }

    private fun persistSessionProfile(profile: UserProfile) {
        securePreferences?.saveUserSession(
            name = profile.nombreCompleto,
            email = profile.correo,
            photoUrl = profile.fotoPerfilUrl
        )
        onSessionUpdated?.invoke()
    }

    private fun <T> mapHttpError(response: Response<T>): Result<Nothing> {
        val errorMsg = parseBackendErrorMessage(response.errorBody()?.string())
        return Result.Failure(
            mapCommonHttpStatus(
                code = response.code(),
                message = errorMsg,
                default401 = "Sesión requerida.",
                default404 = "Perfil no encontrado."
            )
        )
    }
}

class RetrofitMerchantRepository(
    private val merchantApi: MerchantApiService,
    private val securePreferences: SecurePreferences? = null,
    private val onSessionUpdated: (() -> Unit)? = null
) : MerchantRepository {

    override suspend fun getMyMerchant(): Result<MerchantProfile> {
        val networkResult = safeNetworkCall("Error al consultar comercio.") {
            val primary = merchantApi.getMyComercio()
            val primaryBody = primary.body()
            if (primary.isSuccessful && primaryBody != null) {
                Result.Success(persistAndMapMerchant(primaryBody.toDomain()))
            } else if (shouldTryLegacyFallback(primary.code())) {
                fetchLegacyMyComercio(primary)
            } else {
                mapMerchantError(primary, default404 = "Comercio no encontrado.")
            }
        }
        return if (networkResult is Result.Failure) resolveCachedMerchantOrFailure(networkResult) else networkResult
    }

    private suspend fun fetchLegacyMyComercio(primaryResponse: Response<com.example.sensus.data.remote.dto.ComercioDto>): Result<MerchantProfile> = try {
        val legacy = merchantApi.getMyComercioFallback()
        val legacyBody = legacy.body()
        if (legacy.isSuccessful && legacyBody != null) {
            Result.Success(persistAndMapMerchant(legacyBody.toDomain()))
        } else {
            mapMerchantError(legacy, default404 = "Comercio no encontrado.")
        }
    } catch (_: Exception) {
        mapMerchantError(primaryResponse, default404 = "Comercio no encontrado.")
    }

    override suspend fun activateMerchant(
        nombreComercial: String,
        direccion: String,
        codigoPostal: String,
        latitud: Double?,
        longitud: Double?
    ): Result<MerchantProfile> = activateMerchant(
        MerchantActivationInput(
            nombreComercial = nombreComercial,
            direccion = direccion,
            codigoPostal = codigoPostal,
            coordinates = latitud?.let { lat ->
                longitud?.let { lng -> MerchantCoordinates(lat, lng) }
            }
        )
    )

    override suspend fun activateMerchant(input: MerchantActivationInput): Result<MerchantProfile> = safeNetworkCall("Error al activar comercio.") {
        val request = ActivateMerchantRequest(
            nombreComercial = input.nombreComercial,
            direccion = input.direccion,
            codigoPostal = input.codigoPostal,
            latitud = input.coordinates?.latitude,
            longitud = input.coordinates?.longitude,
            fotoFachadaUrl = input.fotoFachadaUrl
        )
        val primary = merchantApi.activateMerchant(request)
        val primaryBody = primary.body()
        if (primary.isSuccessful && primaryBody != null) {
            Result.Success(handleActivationSuccess(primaryBody))
        } else if (shouldTryLegacyFallback(primary.code())) {
            activateViaLegacyEndpoint(request, primary)
        } else {
            mapMerchantError(
                response = primary,
                default404 = "No fue posible registrar el comercio. Verifica tu sesión e inténtalo de nuevo."
            )
        }
    }

    private suspend fun activateViaLegacyEndpoint(
        request: ActivateMerchantRequest,
        primaryResponse: Response<com.example.sensus.data.remote.dto.RegisterComercioResponseDto>
    ): Result<MerchantProfile> = try {
        val legacy = merchantApi.registerComercioFallback(request)
        val legacyBody = legacy.body()
        if (legacy.isSuccessful && legacyBody != null) {
            Result.Success(handleActivationSuccess(legacyBody))
        } else {
            mapMerchantError(
                response = legacy,
                default404 = "No fue posible registrar el comercio. Verifica tu sesión e inténtalo de nuevo."
            )
        }
    } catch (_: Exception) {
        mapMerchantError(
            response = primaryResponse,
            default404 = "No fue posible registrar el comercio. Verifica tu sesión e inténtalo de nuevo."
        )
    }

    override suspend fun updateMerchantProfile(
        nombreComercial: String?,
        direccion: String,
        codigoPostal: String,
        horarios: List<HorarioDia>
    ): Result<MerchantProfile> = updateMerchantProfile(nombreComercial, direccion, codigoPostal, horarios, null)

    override suspend fun updateMerchantProfile(
        nombreComercial: String?,
        direccion: String,
        codigoPostal: String,
        horarios: List<HorarioDia>,
        coordinates: MerchantCoordinates?
    ): Result<MerchantProfile> = updateMerchantProfile(
        MerchantProfileUpdateInput(
            nombreComercial = nombreComercial,
            direccion = direccion,
            codigoPostal = codigoPostal,
            horarios = horarios,
            coordinates = coordinates
        )
    )

    override suspend fun updateMerchantProfile(input: MerchantProfileUpdateInput): Result<MerchantProfile> = safeNetworkCall("Error al actualizar comercio.") {
        val request = UpdateMerchantProfileRequest(
            nombreComercial = input.nombreComercial,
            direccion = input.direccion,
            codigoPostal = input.codigoPostal,
            horarios = input.horarios.map { it.toDto() },
            latitud = input.coordinates?.latitude,
            longitud = input.coordinates?.longitude,
            fotoFachadaUrl = input.fotoFachadaUrl,
            activo = input.activo
        )
        val response = merchantApi.updateMerchantProfile(request)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.Success(persistAndMapMerchant(body.toDomain(fallbackHorarios = input.horarios)))
        } else if (shouldTryLegacyFallback(response.code())) {
            updateViaLegacyEndpoint(request, input.horarios, response)
        } else {
            mapMerchantError(response, default404 = "Comercio no encontrado.")
        }
    }

    private suspend fun updateViaLegacyEndpoint(
        request: UpdateMerchantProfileRequest,
        horarios: List<HorarioDia>,
        primaryResponse: Response<com.example.sensus.data.remote.dto.ComercioDto>
    ): Result<MerchantProfile> = try {
        val legacy = merchantApi.updateMyComercioFallback(request)
        val legacyBody = legacy.body()
        if (legacy.isSuccessful && legacyBody != null) {
            Result.Success(persistAndMapMerchant(legacyBody.toDomain(fallbackHorarios = horarios)))
        } else {
            mapMerchantError(legacy, default404 = "Comercio no encontrado.")
        }
    } catch (_: Exception) {
        mapMerchantError(primaryResponse, default404 = "Comercio no encontrado.")
    }

    override suspend fun getNearbyMerchants(latitude: Double, longitude: Double, radiusMeters: Int): Result<List<MerchantProfile>> {
        val networkResult = safeNetworkCall("Error al cargar comercios cercanos.") {
            val primary = merchantApi.getNearbyMerchants(latitude, longitude, radiusMeters)
            val resolved = if (shouldTryLegacyFallback(primary.code())) {
                merchantApi.getNearbyComerciosFallback(latitude, longitude, radiusMeters)
            } else {
                primary
            }
            val body = resolved.body()
            when {
                resolved.isSuccessful && body != null -> Result.Success(body.map { it.toDomain() }.filter { it.activo })
                resolved.code() == HTTP_NOT_FOUND -> Result.Success(emptyList())
                else -> mapMerchantError(resolved, default404 = "Comercios no disponibles.")
            }
        }
        return if (networkResult is Result.Failure) Result.Success(emptyList()) else networkResult
    }

    private fun handleActivationSuccess(body: com.example.sensus.data.remote.dto.RegisterComercioResponseDto): MerchantProfile {
        if (body.token.isNotBlank()) {
            securePreferences?.saveAuthToken(body.token)
        }
        return persistAndMapMerchant(body.comercio.toDomain())
    }

    private fun persistAndMapMerchant(domain: MerchantProfile): MerchantProfile {
        val existingPhoto = securePreferences?.storedMerchantStatus?.photoUrl?.takeIf { it.isNotBlank() }
        securePreferences?.storedMerchantStatus = StoredMerchantStatus(
            isRegistered = true,
            storeName = domain.nombreComercial,
            address = domain.direccion,
            postalCode = domain.codigoPostal,
            photoUrl = domain.fotoFachadaUrl?.takeIf { it.isNotBlank() } ?: existingPhoto,
            isStoreOpen = domain.activo
        )
        onSessionUpdated?.invoke()
        return domain
    }

    private fun resolveCachedMerchantOrFailure(failure: Result.Failure): Result<MerchantProfile> {
        val cached = securePreferences?.storedMerchantStatus
        val cachedName = cached?.storeName?.takeIf { cached.isRegistered && it.isNotBlank() } ?: return failure
        return Result.Success(
            MerchantProfile(
                id = "local_merchant",
                usuarioId = "local_user",
                nombreComercial = cachedName,
                direccion = cached.address.orEmpty(),
                codigoPostal = cached.postalCode.orEmpty(),
                fotoFachadaUrl = cached.photoUrl,
                activo = cached.isStoreOpen
            )
        )
    }
}

private fun shouldTryLegacyFallback(code: Int): Boolean = code == HTTP_NOT_FOUND || code == HTTP_METHOD_NOT_ALLOWED

private fun <T> mapMerchantError(response: Response<T>, default404: String): Result<Nothing> {
    val errorMsg = parseBackendErrorMessage(response.errorBody()?.string())
    val mapped = when (response.code()) {
        HTTP_UNPROCESSABLE -> AppError.Unprocessable(errorMsg ?: "No fue posible geocodificar la dirección.")
        HTTP_FORBIDDEN -> AppError.Forbidden(errorMsg ?: "Solo comerciantes pueden gestionar datos de local.")
        HTTP_CONFLICT -> AppError.Validation(errorMsg ?: "Ya cuentas con un comercio activo registrado.")
        else -> mapCommonHttpStatus(
            code = response.code(),
            message = errorMsg,
            default401 = "Sesión requerida.",
            default404 = default404
        )
    }
    return Result.Failure(mapped)
}
