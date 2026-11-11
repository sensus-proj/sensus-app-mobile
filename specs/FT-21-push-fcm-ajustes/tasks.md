# TASKS — FT-21 Notificaciones Push FCM, Bandeja y Preferencias

> 1 PR = 1 slice. Orden: Dominio -> DB/API -> FCM Service -> ViewModel/UI -> Tests -> Gates.

## Fase A — Dominio

- [ ] T21.1 Crear `NotificationPreferences`, `DeviceTokenPayload` y `NotificationsRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T21.2 Crear `RegisterDeviceTokenUseCase`, `UpdateNotificationSettingsUseCase` y `MarkNotificationsReadUseCase` — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos, Room y API

- [ ] T21.3 Crear `NotificationEntity.java`, `NotificationDao.java` y registrar en `SensusDatabase.java` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T21.4 Crear `NotificationsApiService.kt` (`PUT /api/v1/users/device-token`, `PUT /api/v1/users/settings`, borrador `GET /api/v1/notifications`, `PATCH /api/v1/notifications/read-all`) y DTOs — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T21.5 Implementar `NotificationsRepositoryImpl` retirando `MockDataProvider.sampleNotifications` — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — FCM y Permisos (condicionado a D-3)

- [ ] T21.6 Configurar `POST_NOTIFICATIONS` en `AndroidManifest.xml`, canales en `SensusApp.kt` y dependencias FCM en `libs.versions.toml` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T21.7 Implementar `SensusFirebaseMessagingService.kt` con `onNewToken`, `onMessageReceived` y deep-link seguro en `MainActivity.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — Presentación

- [ ] T21.8 Actualizar `NotificationsContract.kt`, `NotificationsViewModel.kt` y `NotificationsScreen.kt` activando `"Marcar todo como leído"` y contador de campana — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T21.9 Crear `NotificationSettingsScreen.kt` + `NotificationSettingsViewModel.kt` y conectarlo al botón `"Preferencias de notificaciones"` en `BuyerProfileScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase E — Pruebas y Cierre

- [ ] T21.10 Añadir pruebas unitarias, `NotificationDao`, `MockWebServer` (`device-token`, `settings`) y parseo de mensajes FCM — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T21.11 Añadir tests Compose UI para `NotificationSettingsScreen` y `NotificationsScreen` — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T21.12 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `docs/asvs-l2-checklist.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_secrets.py`
