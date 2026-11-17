# SPEC — FT-25 Red de Amigos por QR, Invitación y Solicitudes Pendientes

> Rector: `.specify/memory/constitution.md` §2, §4, §6 y §11. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **Sensus_Movil_Componentes_y_CRUD.xlsx** (hoja *Amigos*, Pantalla **P-24**, Componente **C-38**); **Tesina Sensus §1, §2.4**.
> Backend ref: Spec `050-amigos-y-solicitudes` en `sensus-back`.

## 1. Contexto y objetivo

La pantalla `Amigos y QR` (`P-24`) anteriormente mostraba una matriz QR decorativa dibujada aleatoriamente con `Random` y la lista falsa `MockDataProvider.sampleFriends`. Además, se requiere que los vecinos puedan compartir su enlace de invitación por aplicaciones de mensajería y que quien recibe un escaneo o invitación reciba aviso y decida aceptar (`✓`) o cancelar (`✕`) la solicitud pendiente.

**Objetivo:** implementar la red real de amigos por código QR escaneable, enlace compartido (`https://rodnix.com.mx/sensus?code=...&name=...` con App Link / fallback a Play Store) y flujo completo de solicitudes pendientes sincronizadas con `sensus-back` (`GET /api/v1/friends`, `POST /api/v1/friends/requests`, `POST /api/v1/friends/requests/{requestId}/accept`, `POST /api/v1/friends/requests/{requestId}/reject`, `DELETE /api/v1/friends/{friendUserId}`), retirando todos los amigos simulados.

**Alcance:**
- Código personal determinístico `SNS-XXXXXX` (`FriendInviteCodec`) y QR escaneable real generado con ZXing (`CommunityQrDialog.kt`).
- Escáner real con cámara `Camera2` + selector desde galería + captura manual de código/enlace (`QrScannerDialog.kt`, `FriendsCommunityScreen.kt`).
- Compartir invitación por hoja nativa Android (`ACTION_SEND`) apuntando a `https://rodnix.com.mx/sensus?code=...&name=...` con App Links (`assetlinks.json`, `autoVerify="true"`), redirección `intent://` desde `sensus-landing` y esquema `sensus://invitacion`.
- Sección pulsable `"Solicitudes pendientes"` con contador y acciones de aceptar (`✓`) o rechazar (`✕`), resumen de `"Solicitudes enviadas"`, sondeo rápido (`3 s`) cuando existen solicitudes enviadas o está abierta la pantalla de amigos, y notificaciones locales en barra de estado (`FriendRequestNotifier`) tanto al llegar solicitudes entrantes como cuando un amigo acepta una solicitud enviada.
- Carga de foto de perfil real (`photoUrl` / `FriendAvatar`) en tarjetas de amigos, solicitudes pendientes y encabezado del hilo de ofertas compartidas.
- Compartición interna de ofertas desde el detalle de producto (`ShareOfferToFriendsDialog`, seleccionando de 1 a 10 amigos conectados mediante `POST /api/v1/offers/{offerId}/share`) y pantalla dedicada de ofertas compartidas por amigo (`FriendSharedOffersScreen`, vía `GET /api/v1/friends/{friendUserId}/shared-offers`, sin mensajes de texto libre).

**Fuera de alcance:** búsqueda global por nombre o teléfono; chat de texto libre entre usuarios.

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-25-01 | Generar el código determinístico `SNS-XXXXXX` del usuario autenticado (`FriendInviteCodec`) y mostrar un bitmap QR real escaneable con ZXing (`QRCodeWriter`). |
| RF-25-02 | Escanear QR de otros vecinos mediante cámara (`Camera2` + ZXing `QRCodeReader`), imagen de galería o captura manual, impidiendo auto-invitación y uso en modo invitado. |
| RF-25-03 | Compartir la invitación mediante `Intent.ACTION_SEND` con la URL `https://rodnix.com.mx/sensus?code=...&name=...` para abrir la app directamente si está instalada o mostrar la landing/Play Store si no lo está. |
| RF-25-04 | Enviar solicitudes vía `POST /api/v1/friends/requests`, consultar estado en `GET /api/v1/friends` y reflejar amigos conectados, pendientes recibidas y pendientes enviadas con respaldo local cifrado en Room (`UserPreferenceDao`). |
| RF-25-05 | Mostrar una sección interactiva `"Solicitudes pendientes"` donde el destinatario pulsa para ver quién envió cada invitación y decide aceptar (`POST /api/v1/friends/requests/{requestId}/accept`) o cancelar (`POST /api/v1/friends/requests/{requestId}/reject`), además de eliminar amigos con `DELETE /api/v1/friends/{friendUserId}` y emitir notificación local en Android al detectar nuevas solicitudes entrantes. |
| RF-25-06 | Retirar `MockDataProvider.sampleFriends` de `StoreRepositoryImpl` y mostrar estado vacío real cuando no existan conexiones. |
| RF-25-07 | Mostrar foto real de perfil (`photoUrl`) en amigos conectados y solicitudes, acelerar la sincronización para el remitente cuando tenga solicitudes enviadas (`3 s` + aviso local al aceptarse) y permitir compartir ofertas de producto dentro de Sensus hasta a 10 amigos (`ShareOfferToFriendsDialog` -> `FriendSharedOffersScreen`). |

## 3. Requisitos no funcionales

- **Privacidad y seguridad:** no exponer correo electrónico, credenciales ni coordenadas exactas en invitaciones ni en tarjetas de amigos; caché local cifrada en SQLCipher y endpoints protegidos con Bearer JWT.
- **Compatibilidad UI:** mantener firma compatible de `FriendsCommunityScreen(friendsList)` para pruebas existentes sin incluir textos literales prohibidos en la interfaz.
- **Calidad:** `ktlintCheck`, `detekt`, `check_layers.py` y pruebas unitarias sin hallazgos.

## 4. Arquitectura y contratos internos

```text
domain/
  model/FriendModels.kt                 # ConnectedFriend, PendingFriendRequest, SentFriendRequest, AddFriendResult, FriendInviteCodec
  repository/FriendsRepository.kt
data/
  remote/api/FriendsApiService.kt       # /api/v1/friends*, /api/v1/friends/{friendUserId}/shared-offers, /api/v1/offers/{offerId}/share
  remote/dto/FriendsDtos.kt
  repository/FriendsLocalStore.kt       # Caché cifrada Room + registro de notificaciones vistas
  repository/FriendsRepositoryImpl.kt
presentation/
  buyer/FriendsContract.kt, FriendsViewModel.kt, FriendsCommunityScreen.kt, FriendsRequestsComponents.kt, CommunityQrDialog.kt, FriendSharedOffersScreen.kt, ShareOfferToFriendsSheet.kt
  common/QrScannerDialog.kt, FriendRequestNotifier.kt
```

## 5. Criterios de aceptacion

- **Given** un usuario autenticado abre `Amigos y QR` **When** abre su QR o comparte su enlace **Then** genera su código real `SNS-XXXXXX` y enlace `https://rodnix.com.mx/sensus?code=...`.
- **Given** un usuario escanea el QR de otro usuario **When** envía la solicitud **Then** el solicitante ve la solicitud en `"Solicitudes enviadas"` y el destinatario recibe notificación y puede pulsar `"Solicitudes pendientes"` para aceptar (`✓`) o cancelar (`✕`).
- **Given** el destinatario acepta con la paloma (`✓`) **When** sincronizan ambos dispositivos **Then** ambos usuarios aparecen en `"Mis vecinos conectados"`.
- **Given** el usuario escanea su propio QR o está en modo invitado **When** intenta agregar **Then** la acción se bloquea con retroalimentación clara.

## 6. Plan de pruebas

- **Unitarias:** `FriendsRepositoryImplTest` (codificación/parseo URL web y `sensus://`, sincronización, notificación única por `requestId`, envío, aceptación y rechazo) y `FriendsViewModelTest` (flujo completo MVI y bloqueo de invitado).
- **Análisis estático y compilación:** `ktlintCheck`, `detekt`, `compileDebugKotlin`, `compileDebugAndroidTestKotlin`.

## 7. Riesgos, limites y deuda

- **Despliegue externo:** requiere desplegar `sensus-back` (Spec `050`) en AWS y `sensus-landing` (`inviteHandler.js`) en Neubox para operación de extremo a extremo entre dispositivos físicos.
- **Push en segundo plano:** la app móvil no contiene aún `app/google-services.json` y `FcmPushDispatcher.cs` en backend opera simulado, por lo que las notificaciones usan sincronización automática al abrir/reanudar la app y sondeo periódico en primer plano (`15 s`) con `NotificationManagerCompat`.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| En revision | Pendiente revisión de PR | 2026-11-16 | Implementación alineada con `sensus-back` Spec `050` y `sensus-landing`; pruebas locales en verde |
