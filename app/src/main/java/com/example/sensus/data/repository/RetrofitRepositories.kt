package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.SessionType
import com.example.sensus.data.mapper.MerchantMapper.toDomain
import com.example.sensus.data.mapper.MerchantMapper.toDto
import com.example.sensus.data.mapper.UserMapper.toDomain
import com.example.sensus.data.moderation.ImageModerationService
import com.example.sensus.data.remote.api.AuthApiService
import com.example.sensus.data.remote.api.MerchantApiService
import com.example.sensus.data.remote.api.ProfileApiService
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
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.ModerationResult
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.ProfileRepository
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Response

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_UNPROCESSABLE = 422
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR = 500
private const val CLOUDINARY_TIMEOUT_SECONDS = 30L

internal suspend inline fun <T> safeNetworkCall(unexpectedMessage: String, crossinline block: suspend () -> Result<T>): Result<T> =
    withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: IOException) {
            Result.Failure(AppError.Network("No se pudo conectar con el servidor: ${e.localizedMessage}"))
        } catch (e: HttpException) {
            Result.Failure(AppError.Generic(e.localizedMessage ?: unexpectedMessage))
        } catch (e: SerializationException) {
            Result.Failure(AppError.Generic(e.localizedMessage ?: unexpectedMessage))
        } catch (e: IllegalStateException) {
            Result.Failure(AppError.Generic(e.localizedMessage ?: unexpectedMessage))
        }
    }

internal fun parseBackendErrorMessage(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        val json = Json.parseToJsonElement(raw).jsonObject
        json["detail"]?.jsonPrimitive?.content
            ?: json["message"]?.jsonPrimitive?.content
            ?: json["error"]?.jsonPrimitive?.content
            ?: json["title"]?.jsonPrimitive?.content
            ?: raw
    } catch (_: SerializationException) {
        raw
    } catch (_: IllegalArgumentException) {
        raw
    }
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

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            authApi.logout()
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

    private val cloudinaryHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CLOUDINARY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(CLOUDINARY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(CLOUDINARY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

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
        val moderationResult = moderationService.moderateImage(imageBytes)
        if (moderationResult is ModerationResult.Rejected) {
            return@withContext Result.Failure(AppError.Validation(moderationResult.reason))
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

    private fun uploadBytesToCloudinary(imageBytes: ByteArray, sig: PhotoSignatureResponse): Result<String> {
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "profile_avatar.jpg", imageBytes.toRequestBody("image/jpeg".toMediaType()))
            .addFormDataPart("api_key", sig.apiKey)
            .addFormDataPart("timestamp", sig.timestamp.toString())
            .addFormDataPart("folder", sig.folder)
            .addFormDataPart("signature", sig.signature)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/${sig.cloudName}/image/upload")
            .post(multipartBody)
            .build()

        cloudinaryHttpClient.newCall(request).execute().use { uploadResp ->
            if (!uploadResp.isSuccessful) {
                return Result.Failure(AppError.Generic("Error al subir imagen a Cloudinary (${uploadResp.code})."))
            }
            val respBody = uploadResp.body?.string().orEmpty()
            val secureUrl = Json.parseToJsonElement(respBody).jsonObject["secure_url"]?.jsonPrimitive?.content
            return if (secureUrl.isNullOrBlank()) {
                Result.Failure(AppError.Generic("Cloudinary no devolvió una URL segura válida."))
            } else {
                Result.Success(secureUrl)
            }
        }
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
        val errorMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
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
    private val merchantApi: MerchantApiService
) : MerchantRepository {

    override suspend fun getMyMerchant(): Result<MerchantProfile> = safeNetworkCall("Error al consultar comercio.") {
        val response = merchantApi.getMyComercio()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.Success(body.toDomain())
        } else {
            mapMerchantError(response)
        }
    }

    override suspend fun updateMerchantProfile(
        nombreComercial: String?,
        direccion: String,
        codigoPostal: String,
        horarios: List<HorarioDia>
    ): Result<MerchantProfile> = safeNetworkCall("Error al actualizar comercio.") {
        val request = UpdateMerchantProfileRequest(
            nombreComercial = nombreComercial,
            direccion = direccion,
            codigoPostal = codigoPostal,
            horarios = horarios.map { it.toDto() }
        )
        val response = merchantApi.updateMerchantProfile(request)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.Success(body.toDomain(fallbackHorarios = horarios))
        } else {
            mapMerchantError(response)
        }
    }

    private fun <T> mapMerchantError(response: Response<T>): Result<Nothing> {
        val errorMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
        val mapped = when (response.code()) {
            HTTP_UNPROCESSABLE -> AppError.Unprocessable(errorMsg ?: "No fue posible geocodificar la dirección.")
            HTTP_FORBIDDEN -> AppError.Forbidden(errorMsg ?: "Solo comerciantes pueden gestionar datos de local.")
            else -> mapCommonHttpStatus(
                code = response.code(),
                message = errorMsg,
                default401 = "Sesión requerida.",
                default404 = "Comercio no encontrado."
            )
        }
        return Result.Failure(mapped)
    }
}
