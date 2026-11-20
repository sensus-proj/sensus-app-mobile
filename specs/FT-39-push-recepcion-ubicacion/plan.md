# PLAN — FT-39 Recepción de notificaciones push, ciclo de sesión y ubicación para ofertas cercanas

> Derivado de `spec.md`. Rector: `.specify/memory/constitution.md` §2, §3, §4, §5, §6 y §9.

## 1. Diseño

1. **Identidad del dispositivo.** `NotificationsInstallationStore` guarda un UUID en un archivo propio
   (`sensus_push_installation`). No va en `SecurePreferences` porque su `clearAll()` corre en cada logout y en cada
   401 (ver `docs/errores-conocidos.md` E-02) y cambiaría el `deviceId`. `allowBackup=false` evita copiarlo a otro
   teléfono. El proveedor Firebase usa este UUID en lugar del ID de Firebase Installations.
2. **Registro.** `NotificationsDeviceSync` (dominio, `@Singleton`) observa la identidad de sesión registrada y
   registra una vez por sesión. `SensusMessagingService.onNewToken` llama a `sync(token)`. Los reintentos solo se
   hacen ante 503 (`AppError.Generic(PUSH_UNAVAILABLE)`) o falta de red. Al cerrar sesión se cancelan.
3. **Logout.** `AuthRepository.logout(deviceId)` agrega `?deviceId=` y `LogoutUseCase` recibe el proveedor del UUID
   y el callback que olvida el consentimiento de ubicación.
4. **Ubicación.** `NotificationsLocationUseCase` exige consentimiento, permiso y sesión registrada. Descarta la
   ubicación por defecto de `FusedLocationRepository` y renueva si pasaron 6 h.
5. **Recepción y apertura.** El canal se crea en `SensusApp.onCreate` y queda declarado en el manifiesto. En primer
   plano, `PushNotificationPresenter` arma la notificación con extras filtrados. `PushLaunchParser` lee los extras
   en `MainActivity` (`onCreate` sin estado guardado y `onNewIntent`) y los borra para que no se reabran al rotar.
   `PushOpenRequests` guarda el destino hasta que la pantalla principal está lista. `PushViewModel` carga la oferta
   con `GetOfferDetailUseCase` y emite el efecto de navegación.

## 2. UI

- Diálogos de explicación previa (notificaciones y ubicación) en `MainPushHost`; cada uno se muestra una sola vez.
- Interruptor «Ofertas nuevas cerca de mí» en `NotificationsScreen`, con diálogo de consentimiento y permiso de
  ubicación.
- Icono vectorial monocromo `ic_stat_sensus` para la barra de estado. Sin emojis ni amarillos.

## 3. DI

| Binding | Implementación |
|---|---|
| `NotificationsLocalStore` | `SensusContainer.notificationsLocalStore` (`NotificationsInstallationStore`) |
| `NotificationPermissionChecker` | `AndroidNotificationPermissionChecker` |
| `NotificationsLocationUseCase` | `NotificationsModule.location` |
| `NotificationsDeviceSync` | `NotificationsModule.deviceSync` (`@Singleton`, `SupervisorJob + Dispatchers.Default`) |
| `PushOpenRequests` | `@Singleton @Inject constructor` |

## 4. Verificación

`compileDebugKotlin`, `testDebugUnitTest`, `compileDebugAndroidTestSources`, `lint`, `detekt`, `ktlintCheck`,
`koverVerify` y los 5 `scripts/check_*.py`; después, la prueba manual en teléfono de `spec.md` §6.

Conservar el registro local del aviso al limpiar la sesión. Mantener el consentimiento y su vigencia separados del registro del diálogo.
