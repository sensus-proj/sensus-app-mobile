# PLAN — FT-21 Notificaciones Push FCM, Bandeja Local y Preferencias por Categoría

> Derivado de `spec.md`. Historias/Alcance: SE-039, SE-040, SE-041 (Pantallas P-04, P-26).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Registro y baja de dispositivo (`POST/DELETE /api/v1/notifications/devices`).** `RegisterPushDeviceUseCase` registra el token FCM al iniciar sesión y lo desregistra en `LogoutUseCase`.
2. **Preferencias por categoría (`GET/PUT /api/v1/notifications/settings`).** `UpdateNotificationSettingsUseCase` sincroniza `nearbyOffersEnabled`, `expiringFavoritesEnabled`, `communityActivityEnabled` y `reservationUpdatesEnabled`.
3. **Bandeja cifrada (`NotificationDao`).** Las notificaciones recibidas se guardan en SQLCipher; `NotificationsViewModel` soporta `markAllAsRead()` y deep-linking a `P-14`, `P-20` o `P-24`.

## 2. UI y navegación

- `OnboardingNotificationsScreen` (`P-04`) solicita `POST_NOTIFICATIONS` en Android 13+ con rationale.
- `NotificationsScreen` (`P-26`) muestra bandeja real, botón funcional `Marcar todo como leído` y hoja de preferencias conectada desde `BuyerProfileScreen`.
- Previews: `NotificationsLoaded`, `NotificationsEmpty`, `NotificationSettingsSheet`.

## 3. DI

| Binding | Implementación |
|---|---|
| `NotificationsApiService` | Retrofit `/api/v1/notifications/*` en `NetworkModule` |
| `NotificationDao` | Provisión en `DatabaseModule` |
| `NotificationsRepository` | `NotificationsRepositoryImpl` en `RepositoryModule` |

## 4. Gates

- `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes.
- `./gradlew.bat detekt ktlintCheck lint` verdes (0 violaciones).
- `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (cobertura `>= 80%`).
- `python scripts/check_sdd.py` PASS.
- `python scripts/check_layers.py` PASS.
- `python scripts/check_endpoints.py` PASS.
- `python scripts/check_secrets.py` PASS.
- `python scripts/check_coverage.py --min 80` PASS.

## 5. Orden de implementación

1. Crear `NotificationSettings`, `NotificationItem` y casos de uso en `domain/`.
2. Crear `NotificationsApiService`, `NotificationEntity` y `NotificationDao`.
3. Implementar `SensusFirebaseMessagingService` y canales de notificación Android.
4. Refactorizar `NotificationsViewModel` retirando `MockDataProvider` e implementando `markAllAsRead()`.
5. Conectar `Preferencias de notificaciones` en `BuyerProfileScreen`.

## 6. Prohibiciones

- Prohibido dejar `onClick = {}` vacío en `Preferencias de notificaciones` (`BuyerProfileScreen`) o `Marcar todo como leído` (`NotificationsScreen`).
- Prohibido mantener el token FCM activo en backend tras cerrar sesión.

## Tareas

Ver `tasks.md`.
