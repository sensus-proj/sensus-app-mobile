# TASKS — FT-25 Red de Amigos por QR, Invitación y Solicitudes Pendientes

> Estado: En revisión (implementado localmente junto con `sensus-back` Spec `050` y `sensus-landing`).

- [x] T25.1 Definir modelos `ConnectedFriend`, `PendingFriendRequest`, `SentFriendRequest`, `FriendInviteCodec` y `FriendsRepository` en `domain/` — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T25.2 Retirar `MockDataProvider.sampleFriends` de `StoreRepositoryImpl` y `MockDataProvider` — verif: `.\gradlew.bat testDebugUnitTest`
- [x] T25.3 Implementar QR real ZXing en `CommunityQrDialog.kt` y escáner `Camera2` / galería en `QrScannerDialog.kt` — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T25.4 Configurar App Links (`AndroidManifest.xml`), deep links en `MainActivity`/`AppViewModel` y hoja de compartir hacia `https://rodnix.com.mx/sensus?code=...` — verif: `.\gradlew.bat ktlintCheck detekt`
- [x] T25.5 Integrar `FriendsApiService`, `FriendsRepositoryImpl`, `FriendsLocalStore`, `FriendsViewModel`, `PendingRequestsSectionCard` (paloma y tache) y `FriendRequestNotifier` — verif: `.\gradlew.bat testDebugUnitTest`
- [x] T25.6 Añadir pruebas unitarias en `FriendsRepositoryImplTest` y `FriendsViewModelTest` y alinear `spec.md`/`endpoints-internos.md` — verif: `python scripts/check_sdd.py` y `python scripts/check_endpoints.py`
- [x] T25.7 Añadir fotos de perfil (`FriendAvatar`), sondeo rápido al remitente (`3 s`), aviso de aceptación, selector multi-amigo en producto (máx. 10) y hilo `FriendSharedOffersScreen` — verif: `.\gradlew.bat testDebugUnitTest`
- [ ] T25.8 Verificar flujo E2E en dispositivos físicos tras despliegue de `sensus-back` (AWS) y `sensus-landing` (Neubox) — verif: prueba manual en dispositivo

## Avance en movil_Rocio — 2026-11-22

- El lector admite códigos manuales y enlaces con rutas de invitación exactas. Rechaza enlaces con parámetros de invitación repetidos, información de usuario en la autoridad o puertos explícitos. Se añadieron regresiones de compatibilidad y rechazo.
- La consulta a assetlinks.json agotó el tiempo de espera. Sigue pendiente T25.8 en dispositivos físicos.
- Android reportó `rodnix.com.mx: 1024` para `sensus.com` en el vivo V2341. El dominio sigue sin verificar y la nueva consulta HTTPS a `/.well-known/assetlinks.json` agotó el tiempo de conexión.
- Validación local: 458 pruebas unitarias con 0 fallos, Kover Debug 80.10%, compilación de la app e instrumentadas, lint, Detekt, ktlint y cinco gates Python en verde.
