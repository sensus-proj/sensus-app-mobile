# SPEC — FT-07 Modo Invitado y Filtro de 2 km

> Rector: `.specify/memory/constitution.md` §2, §4, §5 y §10. Plantilla: `.specify/templates/spec-template.md`.
> Historia: **SE-008**. Conciliada en **FT-10** (2026-11-11) contra la arquitectura real del código.

## 1. Contexto y objetivo

La app permite explorar ofertas cercanas sin cuenta mediante Modo Invitado (`StartGuestSessionUseCase`). Para proteger el modelo de negocio y el acceso gradual, el invitado tiene un límite estricto de radio de **2 km (2000 m)** (frente al rango personalizado de **50 m a 5000 m** para usuarios autenticados) y bloqueo explícito en acciones que requieren cuenta.

**Objetivo:** definir el modo invitado con **filtro estricto de 2000 metros** (`GuestRadiusPolicy`) y bloqueo explícito de favoritos, comunidad, reservas, chat y ampliación de radio.

**Alcance:** entrada como invitado (`POST /api/v1/auth/guest`), política de radio en dominio (`GuestRadiusPolicy`), reutilización de `BuyerMapScreen`/`BuyerHomeScreen`, diálogo único `GuestPromptDialog`, validación de radio en selector y limpieza al iniciar sesión.
**Fuera de alcance:** login/registro (FT-04), perfiles (FT-06), borrado de cuenta (FT-08), integración paginada de ofertas remotas (FT-11).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-07-01 | Desde Login, `Explorar como invitado` crea sesión temporal con `StartGuestSessionUseCase` y navega a `MAIN` (`BuyerMapScreen`). |
| RF-07-02 | En modo invitado el radio máximo es **2000 m** (`GuestRadiusPolicy.GUEST_MAX_RADIUS_KM = 2.0f`); el rango general soporta `50 m` (`MIN_RADIUS_METERS = 50`) a `5000 m`. |
| RF-07-03 | `GuestRadiusPolicy` normaliza y valida el radio tanto en dominio como en `BuyerMapViewModel` y `BuyerHomeViewModel`. |
| RF-07-04 | Favoritos, Comunidad, Apartados, Chat Sensi, Ruteo y radios > 2 km muestran `GuestPromptDialog` (`Inicia sesión para continuar`). |
| RF-07-05 | `BuyerHomeScreen` y `BuyerMapScreen` filtran las ofertas al radio permitido y muestran aviso de modo invitado. |
| RF-07-06 | El invitado no puede acceder a rutas de comercio ni perfil comercial (`BuyerProfileViewModel` muestra perfil temporal de solo lectura). |
| RF-07-07 | Al iniciar sesión registrada, `SessionRepository` descarta el estado invitado y habilita el rango completo de `50 m` a `5 km`. |
| RF-07-08 | El modo invitado almacena únicamente el token JWT temporal de invitado en `SecurePreferences` y ningún dato personal de cuenta. |
| RF-07-09 | `POST /api/v1/auth/guest` emite el token con límite `maxRadiusMeters = 2000`; `GET /api/offers/nearby` (migrado a `GET /api/v1/offers/nearby` en FT-11) recibe centro y `radiusMeters`. |

### Historia

| ID | Historia | Criterio |
|---|---|---|
| SE-008 | Como visitante quiero explorar ofertas cercanas sin registrarme. | Ve solo ofertas dentro de ≤ 2000 m y las acciones que requieren cuenta muestran `GuestPromptDialog`. |

## 3. Requisitos no funcionales

- **Privacidad:** el invitado no envía `userId` ni PII; solo coordenadas locales con consentimiento.
- **Consistencia:** la regla reside en `GuestRadiusPolicy` (capa `domain/`), verificada por `BuyerMapViewModel`, `BuyerHomeViewModel` y backend.
- **UX:** bloqueo explícito con un único diálogo reutilizable (`GuestPromptDialog`); selector `RadiusSelectorBottomSheet` informa el tope de 2 km.
- **Rendimiento:** cálculo geodésico local en `OsmMapUtils.localizeAndMeasureOffers` y consulta espacial preparada para FT-11.
- **Design System:** morado de marca (`SensusPurple`); cero amarillos ni emojis.

## 4. Arquitectura y contratos internos (conciliada con el código real)

```text
domain/
  model/GuestRadiusPolicy.kt           # MIN_RADIUS_METERS = 50, MAX_REGISTERED_RADIUS_METERS = 5000, GUEST_MAX_RADIUS_KM = 2.0f
  usecase/StartGuestSessionUseCase.kt  # Consume POST /api/v1/auth/guest y guarda SessionType.GUEST
  repository/SessionRepository.kt      # Expone SessionState (isSessionActive, isGuest)

data/
  remote/api/AuthApiService.kt         # @POST("api/v1/auth/guest")
  repository/SessionRepositoryImpl.kt  # Fuente única de verdad reactiva + SecurePreferences

presentation/
  buyer/BuyerMapScreen.kt              # Reutiliza mapa OSMDroid con tope 2 km para invitado
  buyer/BuyerMapViewModel.kt           # Aplica GuestRadiusPolicy y emite RequireAuthForRadius
  buyer/BuyerHomeScreen.kt             # Feed filtrado reutilizando BuyerHomeViewModel
  common/RadiusSelectorModal.kt        # Selector personalizado 50 m - 5 km con restricción invitado
  modals/GuestPromptDialog.kt          # Diálogo único para acciones protegidas
```

Contrato efectivo en dominio:

```kotlin
object GuestRadiusPolicy {
    const val MIN_RADIUS_METERS: Int = 50
    const val GUEST_MAX_RADIUS_KM: Float = 2.0f
    const val REGISTERED_MAX_RADIUS_KM: Float = 5.0f

    fun isRadiusAllowedForGuest(radiusKm: Float): Boolean
    fun clampRadius(radiusKm: Float, isGuest: Boolean): Float
}
```

## 5. Criterios de aceptacion

- **Given** un invitado en el mapa **When** intenta seleccionar un radio mayor a `2000 m` **Then** `BuyerMapViewModel` fija `2.0f km` y muestra `GuestPromptDialog`.
- **Given** ofertas fuera de `2 km` **When** el invitado consulta mapa o inicio **Then** quedan fuera del alcance visible.
- **Given** un invitado **When** pulsa Favoritos, Comunidad o Apartar **Then** se muestra `GuestPromptDialog` con opciones de Login y Registro.
- **Given** un invitado que inicia sesión con su cuenta **When** regresa al mapa **Then** desbloquea el selector completo de `50 m` a `5 km`.

## 6. Plan de pruebas

- **Unitarias:** `PoliciesAndModelsTest` (`GuestRadiusPolicy`), `Sprint1UseCasesTest` (`StartGuestSessionUseCase`), `BuyerViewModelsTest` (`BuyerMapViewModel`, `BuyerHomeViewModel`, `BuyerProfileViewModel`).
- **Integración (`MockWebServer`):** `RetrofitRepositoriesTest.authRepo_createGuestSession_shouldSaveTokenAndReturnIt`.
- **E2E UI:** `GuestModeAndAccountDialogsE2ETest` (`GuestPromptDialog` y redirección a auth).
- **Cobertura:** Kover >= 80% verificado en `reportDebug.xml`.

## 7. Riesgos, limites y deuda

- Mientras se integra `GET /api/v1/offers/nearby` paginado (FT-11), el filtrado geodésico corre sobre el catálogo local sincronizado en `OffersRepositoryImpl`.
- `GuestPromptDialog` y `RadiusSelectorBottomSheet` permanecen en `presentation/` sin dependencias directas a `data/`.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Aprobada (Conciliada FT-10) | Equipo Sensus Móvil | 2026-11-11 | `GuestRadiusPolicy.kt`, `StartGuestSessionUseCase.kt`, `PoliciesAndModelsTest.kt`, `GuestModeAndAccountDialogsE2ETest.kt` |
