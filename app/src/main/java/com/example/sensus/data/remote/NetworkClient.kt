package com.example.sensus.data.remote

import com.example.sensus.BuildConfig
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.remote.api.AiChatApiService
import com.example.sensus.data.remote.api.AuthApiService
import com.example.sensus.data.remote.api.ClaimsApiService
import com.example.sensus.data.remote.api.DiscoveryApiService
import com.example.sensus.data.remote.api.FavoritesApiService
import com.example.sensus.data.remote.api.FeedApiService
import com.example.sensus.data.remote.api.FriendsApiService
import com.example.sensus.data.remote.api.InterestsApiService
import com.example.sensus.data.remote.api.MediaApiService
import com.example.sensus.data.remote.api.MerchantApiService
import com.example.sensus.data.remote.api.NotificationsApiService
import com.example.sensus.data.remote.api.OfferDetailsApiService
import com.example.sensus.data.remote.api.OfferFeedApiService
import com.example.sensus.data.remote.api.OffersApiService
import com.example.sensus.data.remote.api.ProfileApiService
import com.example.sensus.data.remote.api.ReportsApiService
import com.example.sensus.data.remote.api.StoreReviewsApiService
import com.example.sensus.data.remote.interceptor.AuthInterceptor
import com.example.sensus.data.remote.interceptor.TokenRefreshAuthenticator
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val TIMEOUT_SECONDS = 15L

/** `POST /api/v1/offers` (docs/rate-matrix.md, FT-15): 20 s. */
internal const val OFFER_CREATE_TIMEOUT_SECONDS = 20L

/** `POST /api/v1/media/upload` multipart (docs/rate-matrix.md, FT-15): 45 s. */
internal const val MEDIA_UPLOAD_TIMEOUT_SECONDS = 45L

/** `POST /api/v1/ai/messages` (docs/rate-matrix.md, FT-24): el backend corta a 25 s; margen de red -> 30 s. */
internal const val AI_CHAT_TIMEOUT_SECONDS = 30L

/**
 * Nivel de logging HTTP (FT-27, RF-27-03). En `release` es `NONE`: ningún dato de red llega a logcat. En `debug`
 * es `BASIC` (método, URL y código): sin cabeceras ni cuerpos, así que no se imprimen tokens JWT, contraseñas
 * ni PII ni en desarrollo.
 */
internal fun httpLoggingLevel(isDebug: Boolean): HttpLoggingInterceptor.Level =
    if (isDebug) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE

/** Interceptor de logging con cabeceras sensibles redactadas por defensa en profundidad. */
internal fun buildLoggingInterceptor(isDebug: Boolean, logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT): HttpLoggingInterceptor =
    HttpLoggingInterceptor(logger).apply {
        level = httpLoggingLevel(isDebug)
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }

class NetworkClient(
    private val securePreferences: SecurePreferences,
    baseUrl: String = DEFAULT_BASE_URL,
    private val onUnauthorized: (() -> Unit)? = null
) {

    companion object {
        // Apunta al backend oficial en AWS API Gateway / Lambda (.NET 10)
        const val DEFAULT_BASE_URL = "https://zqqdwovx81.execute-api.us-east-2.amazonaws.com/"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    internal val loggingInterceptor = buildLoggingInterceptor(BuildConfig.DEBUG)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(securePreferences, onUnauthorized))
        .authenticator(TokenRefreshAuthenticator(securePreferences, onUnauthorized))
        .addInterceptor(loggingInterceptor)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val authApi: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
    val profileApi: ProfileApiService by lazy { retrofit.create(ProfileApiService::class.java) }
    val merchantApi: MerchantApiService by lazy { retrofit.create(MerchantApiService::class.java) }
    val storeReviewsApi: StoreReviewsApiService by lazy { retrofit.create(StoreReviewsApiService::class.java) }
    val mediaApi: MediaApiService by lazy { retrofit.create(MediaApiService::class.java) }
    val feedApi: FeedApiService by lazy { publishingService<FeedApiService>(publishingHttpClient(TIMEOUT_SECONDS)) }
    val reportsApi: ReportsApiService by lazy { publishingService<ReportsApiService>(publishingHttpClient(TIMEOUT_SECONDS)) }
    val offerFeedApi: OfferFeedApiService by lazy { publishingService<OfferFeedApiService>(publishingHttpClient(TIMEOUT_SECONDS)) }
    val interestsApi: InterestsApiService by lazy { publishingService<InterestsApiService>(publishingHttpClient(TIMEOUT_SECONDS)) }
    val discoveryApi: DiscoveryApiService by lazy { publishingService<DiscoveryApiService>(publishingHttpClient(TIMEOUT_SECONDS)) }
    val favoritesApi: FavoritesApiService by lazy { publishingService<FavoritesApiService>(publishingHttpClient(TIMEOUT_SECONDS)) }
    val friendsApi: FriendsApiService by lazy { retrofit.create(FriendsApiService::class.java) }
    val claimsApi: ClaimsApiService by lazy { publishingService<ClaimsApiService>(offersHttpClient) }
    val notificationsApi: NotificationsApiService by lazy { publishingService<NotificationsApiService>(publishingHttpClient(TIMEOUT_SECONDS)) }
    val offerDetailsApi: OfferDetailsApiService by lazy { publishingService<OfferDetailsApiService>(offersHttpClient) }
    val offersApi: OffersApiService by lazy { publishingService<OffersApiService>(offersHttpClient) }
    val mediaUploadApi: MediaApiService by lazy { publishingService<MediaApiService>(mediaUploadHttpClient) }
    internal val aiChatHttpClient: OkHttpClient by lazy { publishingHttpClient(AI_CHAT_TIMEOUT_SECONDS) }
    val aiChatApi: AiChatApiService by lazy { publishingService<AiChatApiService>(aiChatHttpClient) }

    internal val offersHttpClient: OkHttpClient by lazy { publishingHttpClient(OFFER_CREATE_TIMEOUT_SECONDS) }
    internal val mediaUploadHttpClient: OkHttpClient by lazy { publishingHttpClient(MEDIA_UPLOAD_TIMEOUT_SECONDS) }

    /** Keep publication text, images and location out of HTTP body logs; timeouts per docs/rate-matrix.md. */
    private fun publishingHttpClient(timeoutSeconds: Long): OkHttpClient = okHttpClient.newBuilder()
        .apply { interceptors().remove(loggingInterceptor) }
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    private inline fun <reified T> publishingService(client: OkHttpClient): T = retrofit.newBuilder().client(client).build().create(T::class.java)
}
