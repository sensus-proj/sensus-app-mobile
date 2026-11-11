# SPEC — FT-21 Notificaciones Push FCM, Bandeja y Preferencias

> Rector: `.specify/memory/constitution.md` §2, §3, §4.3 y §6. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-039, SE-040, SE-041**; Pantallas **P-04, P-26**; Componentes **C-05, C-40**.
> Backend ref: Specs `027-registro-dispositivo`, `044-ajustes-notificaciones`.

## 1. Contexto y objetivo

La aplicación móvil no tiene integrado Firebase Cloud Messaging (`firebase-messaging`) ni solicita el permiso `POST_NOTIFICATIONS` (Android 13+). `NotificationsViewModel` y `NotificationsScreen` consumen únicamente `MockDataProvider.sampleNotifications`, su botón `"Marcar todo como leído"` tiene handler vacío, y en `BuyerProfileScreen.kt` la fila `"Preferencias de notificaciones"` tiene `onClick = {}`.

**Objetivo:** integrar Firebase Cloud Messaging (FCM) con permiso runtime `POST_NOTIFICATIONS`, sincronizar el token del dispositivo vía `PUT /api/v1/users/device-token`, crear la pantalla de preferencias de notificaciones conectada a `PUT /api/v1/users/settings`, y respaldar la bandeja de notificaciones (`P-26`) con almacenamiento local cifrado y limpieza de mocks.

**Alcance:** permiso `POST_NOTIFICATIONS`, `SensusFirebaseMessagingService`, canales de notificación (`offers_nearby`, `claims_alerts`, `community_social`), navegación por deep-link al tocar una push, registro de token en login/renovación y baja al hacer logout, `NotificationSettingsScreen`, persistencia `NotificationDao` y conexión con `NotificationsViewModel`.
**Fuera de alcance:** envío de correos transaccionales, cron jobs del lado servidor.

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-21-01 | Solicitar permiso `android.permission.POST_NOTIFICATIONS` en Android 13+ (API 33+) desde `OnboardingLocationScreen` (`P-04`) o ajustes de notificaciones sin bloquear el acceso si se deniega. |
| RF-21-02 | Al autenticarse o cuando FCM emita `onNewToken`, enviar `PUT /api/v1/users/device-token` con el token FCM actual y plataforma `"android"`; al cerrar sesión o eliminar cuenta, desasociar/limpiar token local. |
| RF-21-03 | Al recibir una notificación push en segundo o primer plano, mostrar notificación nativa con el canal correspondiente y abrir directamente la pantalla destino (`ProductDetail`, `ActiveApartado` o `MerchantDashboard`) al tocarla. |
| RF-21-04 | Pantalla `NotificationSettingsScreen` (accesible desde `BuyerProfileScreen` `"Preferencias de notificaciones"`) con interruptores independientes para ofertas cercanas, avisos de red social y alertas comerciales sincronizados mediante `PUT /api/v1/users/settings`. |
| RF-21-05 | Actualizar `NotificationsViewModel` y `NotificationsScreen` (`P-26`) para leer desde `NotificationDao` (y `GET /api/v1/notifications` cuando esté disponible en backend), habilitar `"Marcar todo como leído"` (`PATCH /api/v1/notifications/read-all`) y reflejar el contador en la campana (`C-05`), retirando `MockDataProvider.sampleNotifications`. |

## 3. Requisitos no funcionales

- **Privacidad/Seguridad:** el token FCM nunca se imprime en logs ni se expone en errores UI; `google-services.json` no se versiona en claro si contiene credenciales restringidas, o se gestiona mediante secretos en CI.
- **Compatibilidad:** uso de `NotificationCompat` y `NotificationChannel` solo en API 26+.

## 4. Arquitectura y contratos internos

```text
domain/
  model/NotificationModels.kt   # NotificationPreferences, DeviceTokenPayload
  repository/NotificationsRepository.kt
  usecase/RegisterDeviceTokenUseCase.kt, UpdateNotificationSettingsUseCase.kt, MarkNotificationsReadUseCase.kt
data/
  local/db/entity/NotificationEntity.java, dao/NotificationDao.java
  push/SensusFirebaseMessagingService.kt
  remote/api/NotificationsApiService.kt # PUT /api/v1/users/device-token, PUT /api/v1/users/settings
  repository/NotificationsRepositoryImpl.kt
presentation/buyer/
  NotificationsContract.kt, NotificationsViewModel.kt, NotificationsScreen.kt
  NotificationSettingsScreen.kt, NotificationSettingsViewModel.kt
```

## 5. Criterios de aceptacion

- **Given** un usuario inicia sesión en Android 13+ **When** acepta notificaciones **Then** registra el token FCM con `PUT /api/v1/users/device-token`.
- **Given** usuario cambia un switch en `NotificationSettingsScreen` **When** guarda **Then** envía `PUT /api/v1/users/settings` y persiste la preferencia localmente.
- **Given** el usuario toca `"Marcar todo como leído"` en `NotificationsScreen` **When** hay avisos sin leer **Then** actualiza estado leído en `NotificationDao` y el contador de la campana pasa a `0`.
- **Given** llega una push con `offerId` **When** el usuario toca la notificación **Then** navega directo al detalle de esa oferta.

## 6. Plan de pruebas

- **Unitarias:** `RegisterDeviceTokenUseCaseTest`, `UpdateNotificationSettingsUseCaseTest`, `MarkNotificationsReadUseCaseTest`, parseo de payload FCM.
- **ViewModel:** `NotificationsViewModelTest` y `NotificationSettingsViewModelTest` con Turbine.
- **Integración (`MockWebServer` + `NotificationDao`):** `PUT /api/v1/users/device-token`, `PUT /api/v1/users/settings`, marcado como leído en Room.
- **UI (Compose Test Rule):** switches en `NotificationSettingsScreen`, botón `"Marcar todo como leído"` y badge de campana `C-05`.

## 7. Riesgos, limites y deuda

- **Bloqueante externo (decisión D-3):** la activación funcional de FCM en dispositivo exige contar con el proyecto Firebase y `google-services.json` bajo la cuenta única del equipo (`constitution.md` §12). Si aún no está aprovisionado, la abstracción `NotificationsRepository` y los endpoints REST se implementan completos mientras el servicio Firebase queda desacoplado detrás de interfaz hasta recibir credenciales.
- Los endpoints `GET /api/v1/notifications` y `PATCH /api/v1/notifications/read-all` no figuran aún en `sensus-back` (que hoy cubre device-token en Spec 027 y settings en Spec 044); se proponen con persistencia local inmediata en `NotificationDao`.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución y provisión Firebase (D-3) |
