# TASKS — FT-25 Red de Amigos por QR, Invitación y Solicitudes Pendientes

> Estado: En revisión (implementado localmente junto con `sensus-back` Spec `050` y `sensus-landing`).

- [x] T25.1 Definir modelos `ConnectedFriend`, `PendingFriendRequest`, `SentFriendRequest`, `FriendInviteCodec` y `FriendsRepository` en `domain/` — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T25.2 Retirar `MockDataProvider.sampleFriends` de `StoreRepositoryImpl` y `MockDataProvider` — verif: `.\gradlew.bat testDebugUnitTest`
- [x] T25.3 Implementar QR real ZXing en `CommunityQrDialog.kt` y escáner `Camera2` / galería en `QrScannerDialog.kt` — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T25.4 Configurar App Links (`AndroidManifest.xml`), deep links en `MainActivity`/`AppViewModel` y hoja de compartir hacia `https://rodnix.com.mx/sensus?code=...` — verif: `.\gradlew.bat ktlintCheck detekt`
- [x] T25.5 Integrar `FriendsApiService`, `FriendsRepositoryImpl`, `FriendsLocalStore`, `FriendsViewModel`, `PendingRequestsSectionCard` (paloma y tache) y `FriendRequestNotifier` — verif: `.\gradlew.bat testDebugUnitTest`
- [x] T25.6 Añadir pruebas unitarias en `FriendsRepositoryImplTest` y `FriendsViewModelTest` y alinear `spec.md`/`endpoints-internos.md` — verif: `python scripts/check_sdd.py` y `python scripts/check_endpoints.py`
- [ ] T25.7 Verificar flujo E2E en dispositivos físicos tras despliegue de `sensus-back` (AWS) y `sensus-landing` (Neubox) — verif: prueba manual en dispositivo
