# SPEC — FT-39 Recepción de notificaciones push, ciclo de sesión y ubicación para ofertas cercanas

> Rector: `.specify/memory/constitution.md` §2, §3, §4, §5, §6 y §9. Plantilla: `specs/_template.md`.
> Continúa `FT-21-push-fcm-ajustes` (preferencias y activación) y cierra su deuda «servicio receptor, canales,
> renovación y ciclo login/logout». Alineada con `sensus-back` Spec 027 (device-token), Spec 044 (ajustes),
> SE-004 (logout con `deviceId`), notificaciones de ofertas cercanas (`NearbyOfferNotifier`), apartados
> (`ClaimPushNotifier`), amigos (Spec 050) y sanciones.

## 1. Contexto y objetivo

El backend ya envía notificaciones por FCM HTTP v1 con un bloque `notification` (título y cuerpo) y otro `data`
(`type` e identificadores). La app solo permitía guardar preferencias y registrar el dispositivo a mano desde
ajustes. No recibía mensajes, no tenía canal ni receptor, no renovaba el token, no avisaba al backend al cerrar
sesión y no compartía ubicación para «oferta nueva cerca de ti».

**Objetivo:** que un usuario registrado reciba las notificaciones del backend en primer plano, en segundo plano y
con la app cerrada, que al tocarlas llegue a la pantalla correcta, y que deje de recibirlas al cerrar sesión.

**Alcance:** registro del token FCM tras iniciar sesión y en `onNewToken`, con un `deviceId` estable por
instalación; `deviceId` en el logout; canal y receptor FCM; navegación según `data["type"]`; ubicación en primer
plano con consentimiento explícito; opción de ubicación en ajustes.

**Fuera de alcance:** bandeja local de notificaciones (no hay contrato de backend), ubicación en segundo plano
(`ACCESS_BACKGROUND_LOCATION`) y notificaciones para invitados.

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-39-01 | Con sesión registrada (no invitado), la app pide `POST_NOTIFICATIONS` en Android 13+ tras mostrar una explicación previa, una sola vez por instalación. |
| RF-39-02 | Al iniciar sesión o abrir la app con sesión registrada, y en `FirebaseMessagingService.onNewToken`, se llama a `PUT /api/v1/users/device-token` con `{ deviceId, platform: "Android", token }`. |
| RF-39-03 | `deviceId` es un UUID aleatorio guardado por instalación (máx. 100). No cambia al cerrar sesión ni con un 401, y nunca es el token. |
| RF-39-04 | Si el registro responde 503 (FCM no disponible) o falla la red, se reintenta a los 30 s, 2 min y 10 min, y otra vez al abrir la app. 400 y 401 no se reintentan. |
| RF-39-05 | Al cerrar sesión: `POST /api/v1/auth/logout` con el parámetro de consulta `deviceId`; además se olvida el consentimiento de ubicación del usuario que sale. |
| RF-39-06 | Tras un consentimiento explícito y con permiso de ubicación en primer plano: `PUT /api/v1/users/notification-location` con `{ latitude, longitude }` (204). Se envía al activarlo y al volver a la app si pasaron 6 h (el backend la descarta a las 24 h). Nunca se envía la ubicación por defecto de la app. |
| RF-39-07 | Al desactivar «Ofertas nuevas cerca de mí»: `DELETE /api/v1/users/notification-location` (204). Los invitados nunca llaman a estos endpoints (el backend responde 403). |
| RF-39-08 | Ajustes muestra `pushEnabled`, `offerAlerts`, `socialAlerts` y `radiusKm` (1..50) con `GET`/`PUT /api/v1/users/settings` (heredado de FT-21) y el interruptor de ubicación. |
| RF-39-09 | Existe el canal `sensus_alerts` (importancia alta) y está declarado como canal por defecto de FCM. En primer plano, `onMessageReceived` muestra la notificación. |
| RF-39-10 | Al tocar la notificación (en primer plano, en segundo plano o con la app cerrada), `MainActivity` lee `data` de los extras y navega según `type`: `nearby_offer` y `friend_offer_shared` → detalle de la oferta; `claim_confirmed` → pantalla del apartado; `merchant_offer_claimed` → ventas del comercio; `friend_request` y `friend_accepted` → amigos; `sanction_*` u otro tipo → inicio. |

## 3. Requisitos no funcionales

- **Seguridad:** el token FCM y el JWT nunca se escriben en logs ni se guardan fuera del almacenamiento cifrado. El
  JWT sigue en `SecurePreferences` (Keystore). El archivo de instalación solo guarda un UUID, banderas y una marca
  de tiempo. Release sin logs (`-assumenosideeffects` de `Log.v/d/i` y HTTP `Level.NONE`).
- **Privacidad del apartado:** la notificación no muestra datos del apartado ni el QR; la app solo usa el título y
  el cuerpo del backend. Los extras del toque se limitan a `type`, `offerId`, `claimId` y `requestId`, con valores
  validados. El QR solo aparece en la pantalla del apartado, cargada desde el backend con sesión validada.
- **Arquitectura:** `domain/` puro (`PushPayload`, `NotificationsLocationUseCase`, `NotificationsDeviceSync`). El
  servicio FCM vive en `presentation/push` porque `data/` no puede importar presentación.
- **Calidad:** `detekt`, `ktlintCheck`, `lint`, Kover ≥ 80 % y los 5 `scripts/check_*.py` en PASS.

## 4. Arquitectura y contratos internos

Endpoints (registrados en `docs/endpoints-internos.md` y `docs/rate-matrix.md`):

- `PUT /api/v1/users/device-token` → 200 · 400 · 401 · 409 · 503
- `POST /api/v1/auth/logout` (parámetro de consulta `deviceId`) → 204
- `PUT /api/v1/users/notification-location` → 204 · 400 · 403 · 429
- `DELETE /api/v1/users/notification-location` → 204 · 403
- `GET`/`PUT /api/v1/users/settings` → 200

```text
domain/
  model/PushModels.kt                  # PushTarget, PushPayload (whitelist y parseo de data["type"])
  repository/NotificationsRepository.kt # + shareLocation/deleteLocation, NotificationsLocalStore, NotificationPermissionChecker
  usecase/NotificationsUseCases.kt      # + NotificationsLocationUseCase; RegisterDevice acepta token
  usecase/NotificationsDeviceSync.kt    # registro tras login, onNewToken y reintentos 503
  usecase/LogoutUseCase.kt              # deviceId en el logout
data/
  push/NotificationsInstallationStore.kt     # UUID por instalación (fuera de SecurePreferences)
  push/AndroidNotificationPermissionChecker.kt
  push/NotificationsFirebaseDeviceProvider.kt # deviceId = UUID de instalación
  remote/api/NotificationsApiService.kt      # + notification-location PUT/DELETE
  remote/api/AuthApiService.kt               # logout(@Query deviceId)
presentation/
  push/SensusMessagingService.kt   # onNewToken / onMessageReceived
  push/PushNotificationPresenter.kt # canal + notificación en primer plano
  push/PushLaunch.kt               # PushLaunchParser + PushOpenRequests
  push/PushViewModel.kt            # permisos, ubicación y apertura de destino
  main/MainPushHost.kt             # diálogos y navegación en la pantalla principal
```

## 5. Criterios de aceptacion

- **Given** un usuario registrado en Android 13+ **When** entra a la pantalla principal por primera vez **Then** ve
  la explicación, concede el permiso y el backend responde 200 a `PUT /api/v1/users/device-token`.
- **Given** la app en primer plano, en segundo plano o cerrada **When** llega «Nuevo {producto} cerca de ti» y el
  usuario la toca **Then** se abre el detalle de esa oferta.
- **Given** un comprador aparta una oferta **When** llegan los avisos **Then** el comprador abre la pantalla del
  apartado (con el QR solo ahí) y el comercio abre sus ventas.
- **Given** el usuario cierra sesión **When** el backend envía un aviso a ese usuario **Then** ya no llega a este
  teléfono.
- **Given** un invitado **When** usa la app **Then** no se piden permisos de notificación ni se llama a
  device-token ni a notification-location.

## 6. Plan de pruebas

- **Unitarias (JUnit 4 + MockK + Turbine + MockWebServer):** `PushPayloadTest`, `NotificationsLocationUseCaseTest`,
  `NotificationsDeviceSyncTest`, `LogoutUseCaseTest` (deviceId), `NotificationsPushApiTest` (rutas, cuerpo
  camelCase, 503 y `?deviceId=`), `NotificationsInstallationStoreTest`, `PushViewModelTest`, y las de FT-21
  adaptadas.
- **En teléfono físico (manual, pendiente):** permiso, registro 200, apertura en primer plano, en segundo plano y
  con la app cerrada; oferta cercana; apartado (comprador y comercio); y que dejen de llegar tras cerrar sesión.

## 7. Riesgos, limites y deuda

- La ubicación solo se renueva con la app abierta; si el usuario no la abre en 24 h, deja de recibir avisos de
  ofertas cercanas hasta la siguiente apertura (decisión de privacidad: sin ubicación en segundo plano).
- Los reintentos del registro viven mientras el proceso esté activo (no hay WorkManager en el proyecto); el
  siguiente arranque vuelve a intentarlo.
- `merchant_offer_claimed` abre la pestaña de ventas, no un apartado concreto: el backend no expone un detalle de
  apartado por `claimId` para el comercio.
- Las pruebas E2E dependen del backend desplegado (hoy con pausa nocturna 22:30–07:00 hora de CDMX).

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| En implementacion | Pendiente revisión del equipo y prueba en teléfono físico | 2026-11-19 | 413 pruebas unitarias PASS (0 fallos), Kover 80.46 %, `compileDebugKotlin`, `compileDebugAndroidTestSources`, `lint`, `detekt`, `ktlintCheck` y 5 `check_*.py` PASS; E2E en dispositivo pendiente |

El aviso automático de ubicación se responde una vez por instalación. El cierre de sesión borra el consentimiento y la marca de último envío. El usuario puede volver a consentir desde Preferencias.
