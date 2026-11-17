# PLAN — FT-25 Red de Amigos por QR, Invitación y Solicitudes Pendientes

> Derivado de `spec.md`. Historias/Alcance: Pantalla P-24 (`Amigos y QR`), Componente C-38.
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Identidad e invitación (`FriendInviteCodec`).** Código determinístico `SNS-XXXXXX` vía SHA-256 del correo normalizado, URL compartible `https://rodnix.com.mx/sensus?code=...&name=...`, esquema nativo `sensus://invitacion` y verificación Android App Links con `assetlinks.json`.
2. **Escaneo y QR real.** Generación con ZXing `QRCodeWriter` en `CommunityQrDialog.kt` y lectura con `Camera2` + `ImageReader` + ZXing `QRCodeReader` (y selector de galería) en `QrScannerDialog.kt`.
3. **Sincronización y solicitudes pendientes.** `FriendsApiService` consume `/api/v1/friends*` (Spec `050`); `FriendsLocalStore` persiste caché en `UserPreferenceDao` y controla IDs ya notificados para disparar notificaciones únicas vía `FriendRequestNotifier`.
4. **UI (`FriendsCommunityScreen.kt` + `FriendsRequestsComponents.kt`).** Sección desplegable `"Solicitudes pendientes"` con contador, paloma verde para aceptar y tache rojo para cancelar, además de tarjeta de solicitudes enviadas y estado vacío real sin `MockDataProvider`.

## 2. UI y navegación

- `MainActivity.kt` captura App Links (`https://rodnix.com.mx/sensus?code=...`), esquemas `sensus://invitacion` y taps de notificación (`sensus://amigos/solicitudes`), enrutando mediante `Navigation.kt`, `AppViewModel.kt` y `MainAppContainer.kt` a `Comunidad -> Amigos`.

## 3. DI

| Binding | Implementación |
|---|---|
| `FriendsApiService` | `NetworkClient.friendsApi` |
| `FriendsRepository` | `FriendsRepositoryImpl` en `SensusContainer` y `@Provides` en `PreferencesModule` |

## 4. Gates

- `.\gradlew.bat ktlintCheck detekt compileDebugKotlin compileDebugAndroidTestKotlin testDebugUnitTest --no-daemon` en verde.
- `python scripts/check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`.

## Tareas

Ver `tasks.md`.
