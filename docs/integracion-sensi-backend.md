# Instructivo de integración — Sensi IA (móvil ↔ backend)

| Campo | Valor |
|---|---|
| Spec móvil | `FT-24-sensi-ia` |
| Spec backend | `046-asistente-sensi-ia` (`sensus-back`) |
| Contrato fuente | `sensus-back/specs/046-asistente-sensi-ia/contracts/openapi.yaml` y `src/Sensus.Application/Ai/DTOs/AiDtos.cs` |
| Fecha | 2026-11-15 |

> **Requisito previo:** los endpoints `/api/v1/ai/*` existen en la rama `Back_Rodrigo`, pero **aún no están
> desplegados** en API Gateway. Hasta que se despliegue, pruébalos contra el backend local
> (`dotnet run --project src/Sensus.Api`) con Redis y MongoDB arriba (`docker compose up -d`).
> Sin Redis el backend **siempre responde en modo degradado** (`degraded = true`), por diseño (fail-closed de presupuesto).

---

## 1. Reglas generales

| Tema | Regla |
|---|---|
| Autenticación | `Authorization: Bearer <jwt>` (ya lo pone `AuthInterceptor`). Solo **Consumidor** y **Comerciante**; el **Invitado** recibe `403`. |
| JSON | `camelCase`. Los montos (`decimal` en C#) llegan como número JSON → usa `Double` en Kotlin y formatea a 2 decimales al mostrar. Fechas en ISO-8601 UTC. |
| Timeout del cliente | El backend corta la llamada al modelo a los **25 s**. El cliente por defecto (15 s) es corto: usa **30 s** de lectura para `/ai/messages`. |
| Logs HTTP | **Nunca** registres el cuerpo de `/ai/*` (lleva lo que escribe el usuario y su ubicación). Usa el patrón `publishingHttpClient` sin `Level.BODY`. |
| Correlation-id | Lee el header `X-Correlation-Id` de la respuesta (también viene en `correlationId` del cuerpo) y guárdalo solo en logs locales de error para soporte. |
| Rate limit | `POST /ai/conversations` y `POST /ai/messages`: **10 peticiones por minuto** por usuario. `429` trae `Retry-After` en segundos. |
| Cuota diaria | **20 mensajes por usuario al día**. Al superarla el backend no falla: responde `200` con `degraded = true`. |
| Mensaje | 1 a **1,500 caracteres**. Valida el límite en la UI antes de enviar. |
| Radio | `radiusKm` entre **1.0 y 5.0**. Fuera de rango → `400`. |
| Ubicación | Envía `latitude`/`longitude` reales; el backend las redondea a 2 decimales (~1 km) antes de usarlas. |
| `screenContext` | Solo valores del catálogo (sección 3). Cualquier otro texto → `400`. |

---

## 2. Endpoints

Base: `https://<api>/api/v1/ai`

### 2.1 Crear o recuperar conversación

`POST /api/v1/ai/conversations`

```json
{ "forceNew": false, "screenContext": "home" }
```

- `forceNew = false` devuelve la conversación activa si existe; si no, crea una.
- `forceNew = true` crea una conversación nueva (botón “Nueva conversación”). **Ojo:** hoy el backend no archiva la
  anterior, así que si después se llama con `forceNew = false` puede regresar la vieja. Guarda siempre el
  `conversationId` más reciente y envíalo en cada mensaje.
- Respuesta `200` (trátala igual que `201`): `ConversationSummaryResponseDto`.

```json
{
  "conversationId": "3f0c…",
  "status": "Active",
  "assignedModel": "gpt-4o-mini",
  "routingTier": "Fast",
  "messageCount": 4,
  "lastScreenContext": "home",
  "markdownVersion": "1.0.0",
  "createdAtUtc": "2026-11-15T18:20:00Z",
  "updatedAtUtc": "2026-11-15T18:25:00Z"
}
```

> No muestres `assignedModel` en la UI: la identidad pública es siempre “Sensi” (Hard Rules, FR-046-04).

### 2.2 Enviar mensaje

`POST /api/v1/ai/messages`

```json
{
  "conversationId": "3f0c…",
  "message": "Tengo $100 para comida",
  "screenContext": "map",
  "latitude": 19.4326,
  "longitude": -99.1332,
  "radiusKm": 3.0
}
```

- `conversationId` puede ir `null`: el backend usa la activa o crea una. Guarda el que regrese.
- Respuesta `200`: `AiTurnResponseDto`.

```json
{
  "conversationId": "3f0c…",
  "messageId": "a81b…",
  "reply": "Con $100 te recomiendo este paquete…",
  "recommendedPackage": {
    "budgetMxn": 100.0,
    "totalPriceMxn": 95.0,
    "savingsMxn": 40.0,
    "items": [
      {
        "offerId": "…", "title": "Torta de pierna", "merchantId": "…", "merchantName": "Tortas Lupita",
        "offerPriceMxn": 45.0, "regularPriceMxn": 60.0, "availableStock": 7, "isOpenNow": true, "distanceKm": 0.8
      }
    ]
  },
  "offersCited": [ { "offerId": "…", "title": "…", "…": "…" } ],
  "toolsInvoked": ["search_offers"],
  "degraded": false,
  "degradationReason": null,
  "routingTier": "Fast",
  "markdownVersion": "1.0.0",
  "correlationId": "b7e2…",
  "usage": { "promptTokens": 1830, "cachedTokens": 1280, "completionTokens": 120, "cacheHit": true }
}
```

Reglas de UI:

- `recommendedPackage` y `offersCited` son **opcionales** (`null`). Pinta las tarjetas solo si vienen.
- El backend ya validó `totalPriceMxn <= budgetMxn`, stock > 0 y vigencia. **No recalcules** el total en el cliente; muéstralo tal cual.
- Al tocar una tarjeta, navega al detalle con `offerId` (`GET /api/v1/offers/{id}`, FT-12) y al apartado QR (FT-13).
- Si `degraded = true`, muestra la respuesta normalmente y un aviso discreto (“Sensi está en modo básico; algunas
  recomendaciones pueden ser limitadas”). **No es un error.**
- `usage` y `toolsInvoked` son de diagnóstico: no los muestres.

### 2.3 Consultar historial

`GET /api/v1/ai/conversations/{conversationId}?page=1&pageSize=20`

- `pageSize` entre 1 y **100** (20 por defecto). Fuera de rango → `400`.
- Los mensajes vienen **del más reciente al más antiguo**. Invierte el orden para pintar el chat.
- `role` vale `"User"` o `"Assistant"`.
- Respuesta `200`: `{ conversation, messages[], pagination { page, pageSize, totalCount, totalPages } }`.

### 2.4 Eliminar conversación

`DELETE /api/v1/ai/conversations/{conversationId}` → `204` sin cuerpo.

- Borra en el servidor la conversación, sus mensajes y sus trazas. Después **borra también la caché local** de Room.
- Al eliminar la cuenta (Spec 030) el backend purga todo lo de Sensi; el móvil debe limpiar la tabla local en el mismo flujo de cierre de sesión.

---

## 3. Catálogo de `screenContext`

| Valor | Pantalla móvil sugerida |
|---|---|
| `home` | `BuyerHomeScreen` |
| `map` | `BuyerMapScreen` |
| `offer_detail` | `PublishedOfferDetailsScreen` / `ProductDetailScreen` |
| `my_claims` | Mis apartados (FT-13) |
| `merchant_inventory` | Inventario del comerciante (FT-16) |
| `merchant_profile` | Perfil del comercio |
| `user_profile` | Perfil del usuario |
| `settings` | Ajustes |

Defínelo como `enum class` en Kotlin con `@SerialName` y nunca envíes texto libre. Si la pantalla no está en la lista, manda `null`.

---

## 4. Errores (`application/problem+json`)

El backend responde `ProblemDetails`. El código de negocio viene en **`title`**; el texto para el usuario en `detail`.

| HTTP | `title` | Qué hacer en la app |
|---|---|---|
| 400 | `Error de validación` (trae `errors`) | Campo inválido (`screenContext`, `radiusKm`, `pageSize`). Es un bug del cliente: regístralo. |
| 400 | `Ai.EmptyMessage` / `Ai.MessageTooLong` | Valida en la UI antes de enviar. |
| 400 | `Ai.PromptInjectionDetected` | Muestra `detail`; conserva el texto en el campo para que el usuario lo edite. |
| 400 | `Ai.MessageBlockedByModeration` | Muestra `detail`. No reintentes automáticamente. |
| 401 | `Ai.Unauthorized` / `Ai.UserNotFound` | Sesión inválida: `AuthInterceptor` limpia la sesión y manda a login. |
| 403 | `Ai.GuestForbidden` | Oculta o deshabilita Sensi para invitados; ofrece registrarse. |
| 404 | `Ai.ConversationNotFound` | Borra el `conversationId` local y vuelve a llamar `POST /ai/conversations`. |
| 429 | — (header `Retry-After`) | Deshabilita el botón de enviar durante `Retry-After` segundos con cuenta regresiva. |
| 5xx / timeout | — | Muestra “Sensi no está disponible, intenta de nuevo” con botón de reintento. No reintentes en bucle. |

> El timeout del modelo **no** llega como error: el backend responde `200` con `degraded = true`.

---

## 5. Código Kotlin de referencia

### 5.1 DTOs (`data/remote/dto/AiDtos.kt`)

```kotlin
@Serializable
data class CreateConversationRequest(
    val forceNew: Boolean = false,
    val screenContext: ScreenContext? = null,
)

@Serializable
data class SendAiMessageRequest(
    val conversationId: String?,
    val message: String,
    val screenContext: ScreenContext? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusKm: Double? = null,
)

@Serializable
enum class ScreenContext {
    @SerialName("home") HOME,
    @SerialName("map") MAP,
    @SerialName("offer_detail") OFFER_DETAIL,
    @SerialName("my_claims") MY_CLAIMS,
    @SerialName("merchant_inventory") MERCHANT_INVENTORY,
    @SerialName("merchant_profile") MERCHANT_PROFILE,
    @SerialName("user_profile") USER_PROFILE,
    @SerialName("settings") SETTINGS,
}

@Serializable
data class ConversationSummaryDto(
    val conversationId: String,
    val status: String,
    val routingTier: String,
    val messageCount: Int,
    val lastScreenContext: String? = null,
    val markdownVersion: String,
    val createdAtUtc: String,
    val updatedAtUtc: String? = null,
)

@Serializable
data class CitedOfferDto(
    val offerId: String,
    val title: String,
    val merchantId: String,
    val merchantName: String,
    val offerPriceMxn: Double,
    val regularPriceMxn: Double,
    val availableStock: Int,
    val isOpenNow: Boolean,
    val distanceKm: Double? = null,
)

@Serializable
data class RecommendedPackageDto(
    val budgetMxn: Double,
    val totalPriceMxn: Double,
    val savingsMxn: Double,
    val items: List<CitedOfferDto>,
)

@Serializable
data class AiTurnResponseDto(
    val conversationId: String,
    val messageId: String,
    val reply: String,
    val recommendedPackage: RecommendedPackageDto? = null,
    val offersCited: List<CitedOfferDto>? = null,
    val degraded: Boolean = false,
    val degradationReason: String? = null,
    val correlationId: String,
)

@Serializable
data class ConversationMessageDto(
    val messageId: String,
    val role: String,               // "User" | "Assistant"
    val content: String,
    val recommendedPackage: RecommendedPackageDto? = null,
    val offersCited: List<CitedOfferDto>? = null,
    val degraded: Boolean = false,
    val createdAtUtc: String,
)

@Serializable
data class PaginationDto(val page: Int, val pageSize: Int, val totalCount: Int, val totalPages: Int)

@Serializable
data class ConversationDetailDto(
    val conversation: ConversationSummaryDto,
    val messages: List<ConversationMessageDto>,
    val pagination: PaginationDto,
)
```

> `ignoreUnknownKeys = true` (ya configurado en `NetworkClient`) permite omitir `assignedModel`, `usage` y
> `toolsInvoked`.
>
> **Cambio respecto a la spec FT-24:** RF-24-03 menciona `budgetPackageItems`; el contrato real es
> `recommendedPackage.items`. Actualiza la spec móvil al implementar.

### 5.2 Servicio Retrofit (`data/remote/api/AiChatApiService.kt`)

```kotlin
interface AiChatApiService {
    @POST("api/v1/ai/conversations")
    suspend fun createOrResume(@Body body: CreateConversationRequest): Response<ConversationSummaryDto>

    @POST("api/v1/ai/messages")
    suspend fun sendMessage(@Body body: SendAiMessageRequest): Response<AiTurnResponseDto>

    @GET("api/v1/ai/conversations/{id}")
    suspend fun getConversation(
        @Path("id") conversationId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<ConversationDetailDto>

    @DELETE("api/v1/ai/conversations/{id}")
    suspend fun deleteConversation(@Path("id") conversationId: String): Response<Unit>
}
```

### 5.3 Registro en `NetworkClient`

```kotlin
/** POST /api/v1/ai/messages: el backend corta a 25 s; margen de red → 30 s. */
internal const val AI_CHAT_TIMEOUT_SECONDS = 30L

val aiChatApi: AiChatApiService by lazy {
    // publishingHttpClient no registra cuerpos: el texto del usuario y su ubicación no van a logcat.
    publishingService<AiChatApiService>(publishingHttpClient(AI_CHAT_TIMEOUT_SECONDS))
}
```

Agrega la ruta a `docs/endpoints-internos.md` y `docs/rate-matrix.md` (10/min, 30 s) para que pase `check_endpoints.py`.

### 5.4 Manejo de errores en el repositorio

```kotlin
sealed interface SensiResult {
    data class Success(val turn: AiTurnResponseDto) : SensiResult
    data class Rejected(val code: String, val message: String) : SensiResult   // 400 de negocio
    data class RateLimited(val retryAfterSeconds: Int) : SensiResult           // 429
    data object GuestBlocked : SensiResult                                      // 403
    data object ConversationGone : SensiResult                                  // 404
    data object Unavailable : SensiResult                                       // 5xx / red / timeout
}

@Serializable
private data class ProblemDto(val title: String? = null, val detail: String? = null)

suspend fun send(request: SendAiMessageRequest): SensiResult = try {
    val r = api.sendMessage(request)
    when {
        r.isSuccessful -> SensiResult.Success(r.body()!!)
        r.code() == 429 -> SensiResult.RateLimited(r.headers()["Retry-After"]?.toIntOrNull() ?: 60)
        r.code() == 403 -> SensiResult.GuestBlocked
        r.code() == 404 -> SensiResult.ConversationGone
        r.code() == 400 -> {
            val p = r.errorBody()?.string()?.let { json.decodeFromString<ProblemDto>(it) }
            SensiResult.Rejected(p?.title.orEmpty(), p?.detail ?: "No pudimos procesar tu mensaje.")
        }
        else -> SensiResult.Unavailable
    }
} catch (e: IOException) {
    SensiResult.Unavailable
}
```

### 5.5 Flujo MVI en `SensiChatViewModel`

1. **Al abrir la pantalla:** `createOrResume(forceNew = false, screenContext = <pantalla de origen>)` → guarda `conversationId`
   → `getConversation(id)` → invierte `messages` → pinta e inserta en Room.
2. **Al enviar:**
   - Valida 1–1,500 caracteres y que no haya `RateLimited` activo.
   - Agrega el mensaje del usuario a la lista **de inmediato** (optimista) con estado “enviando”.
   - Llama a `sendMessage` con la ubicación aproximada y el radio actual del selector (1–5 km).
   - `Success` → agrega la respuesta y las tarjetas del paquete; guarda en Room.
   - `ConversationGone` → `createOrResume` y reintenta **una sola vez**.
   - `Rejected` / `RateLimited` / `Unavailable` → marca el mensaje del usuario como no enviado y muestra el aviso de la sección 4.
3. **“Nueva conversación”:** `createOrResume(forceNew = true)` y limpia la lista.
4. **“Borrar conversación”:** `deleteConversation(id)` → borra la tabla local → vuelve al paso 1.
5. **Invitado:** si la sesión es de invitado, no abras el chat; muestra la invitación a registrarse (evita el 403).
6. **Retira** `MockDataProvider` y toda respuesta simulada del flujo de producción (criterio de FT-24).

### 5.6 Caché local (Room + SQLCipher)

- Tabla `ai_messages(messageId PK, conversationId, role, content, packageJson, degraded, createdAtUtc)`.
- Es solo caché de lectura; la fuente de verdad es el backend.
- Purga al cerrar sesión, al eliminar la cuenta y tras `DELETE` de la conversación.
- No guardes `latitude`/`longitude` en la tabla.

---

## 6. Pruebas mínimas en el móvil

| Prueba | Tipo |
|---|---|
| Serialización de `SendAiMessageRequest` (incluye `screenContext` en `snake_case`) | Unitaria |
| Deserialización de `AiTurnResponseDto` con y sin `recommendedPackage` | Unitaria |
| `MockWebServer`: 200 normal, 200 `degraded`, 400 `Ai.MessageBlockedByModeration`, 403, 404 → recrea, 429 con `Retry-After`, timeout | Integración |
| `SensiChatViewModel`: envío optimista, deshabilitar envío durante 429, invitado sin acceso | Unitaria |
| Compose: tarjetas del paquete con total y ahorro; aviso de modo básico | UI |
| El cuerpo de `/ai/*` no aparece en logs (`HttpLoggingInterceptor`) | Unitaria |

---

## 7. Checklist antes de cerrar FT-24

- [ ] Endpoints `/ai/*` desplegados en API Gateway (backend).
- [ ] Redis accesible desde la Lambda; si no, todo responde `degraded = true`.
- [ ] `AiChatApiService` con timeout de 30 s y sin logs de cuerpo.
- [ ] `screenContext` como `enum`, nunca texto libre.
- [ ] Manejo de 400/401/403/404/429/5xx según la sección 4.
- [ ] Sin respuestas simuladas en producción.
- [ ] Spec FT-24 actualizada (`recommendedPackage.items` en lugar de `budgetPackageItems`).
- [ ] `docs/endpoints-internos.md` y `docs/rate-matrix.md` actualizados; `check_endpoints.py` en PASS.
