# SPEC — FT-25 Red de Amigos por QR y Ofertas Compartidas [BORRADOR]

> Rector: `.specify/memory/constitution.md` §2, §4, §6 y §11. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **Sensus_Movil_Componentes_y_CRUD.xlsx** (hoja *Amigos*, Pantalla **P-24**, Componente **C-38**); **Tesina Sensus §1, §2.4**.
> Backend ref: **Sin spec aún en `sensus-back` (bloqueada hasta abrir spec equivalente en backend, decisión D-6)**.

## 1. Contexto y objetivo

La Tesina y el Excel del Drive detallan una red privada de amigos vinculados únicamente mediante código QR (preservando la privacidad al no haber búsqueda abierta de personas ni compartir por WhatsApp) para intercambiar ofertas dentro de Sensus. Hoy `CommunityScreen` mezcla una matriz QR visual generada localmente (`StylizedQrCanvas`) y lista simulada de `MockDataProvider.sampleFriends`.

**Objetivo:** implementar la vinculación segura de amigos por código QR real (`GET /api/v1/friends`, `POST /api/v1/friends`, `PATCH /api/v1/friends/{id}`, `DELETE /api/v1/friends/{id}`) y el intercambio directo de ofertas entre amigos aceptados (`POST /api/v1/offers/{id}/share`), retirando `MockDataProvider.sampleFriends`.

**Alcance:** código QR personal de usuario escaneable (ZXing + CameraX/ML Kit reutilizados de `FT-13`/`FT-14`), gestión de solicitudes/amistades, lista de amigos cercanos sin revelar coordenadas exactas y botón `'Compartir con amigo'` en `ProductDetailScreen`.
**Fuera de alcance:** búsqueda abierta de usuarios por nombre/teléfono global; compartir enlaces por WhatsApp.

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-25-01 | Generar el QR personal del usuario autenticado con su código opaco de invitación (sin exponer correo ni teléfono ni coordenadas exactas). |
| RF-25-02 | Escanear el QR de otro usuario (reutilizando `QrCameraPreview` de `FT-14`) para enviar solicitud/vinculación vía `POST /api/v1/friends`, impidiendo agregarse a sí mismo. |
| RF-25-03 | Listar amigos aceptados y ofertas compartidas desde `GET /api/v1/friends`, aceptar solicitudes con `PATCH /api/v1/friends/{id}` y eliminar amistad con `DELETE /api/v1/friends/{id}`. |
| RF-25-04 | Desde `ProductDetailScreen`, permitir compartir una oferta con un amigo aceptado mediante `POST /api/v1/offers/{id}/share` (bloqueado en modo invitado). |
| RF-25-05 | Retirar `MockDataProvider.sampleFriends` de `StoreRepositoryImpl` y `CommunityViewModel`. |

## 3. Requisitos no funcionales

- **Privacidad desde el diseño:** nunca mostrar ubicación exacta en el mapa de un amigo; únicamente distancia aproximada/rango y estado en línea si el usuario lo consiente.
- **Seguridad:** token/código de invitación QR rotativo o de uso controlado validado por el servidor.

## 4. Arquitectura y contratos internos

```text
domain/
  model/FriendModels.kt         # FriendConnection, FriendStatus, SharedOfferItem
  repository/FriendsRepository.kt
  usecase/GetFriendsUseCase.kt, ConnectFriendByQrUseCase.kt, RespondFriendRequestUseCase.kt, ShareOfferWithFriendUseCase.kt
data/
  remote/api/FriendsApiService.kt # GET/POST/PATCH/DELETE /api/v1/friends, POST /api/v1/offers/{id}/share
  repository/FriendsRepositoryImpl.kt
presentation/buyer/
  FriendsViewModel.kt, FriendsContract.kt, FriendsSection.kt
```

## 5. Criterios de aceptacion

- **Given** un usuario escanea el QR de su vecino **When** confirma **Then** envía `POST /api/v1/friends` y registra la conexión sin revelar domicilio ni correo.
- **Given** un amigo compartió una oferta **When** el destinatario abre `P-24` **Then** ve la tarjeta con la oferta y puede abrir su detalle.
- **Given** un usuario escanea su propio QR **When** intenta agregarse **Then** el caso de uso bloquea la acción con mensaje claro.

## 6. Plan de pruebas

- **Unitarias:** `ConnectFriendByQrUseCaseTest` (bloqueo de auto-agregado, validación QR), `ShareOfferWithFriendUseCaseTest`.
- **Integración (`MockWebServer`):** CRUD `/api/v1/friends` y `/offers/{id}/share`.
- **UI (Compose Test Rule):** diálogo QR personal escaneable y lista de ofertas compartidas por amigos.

## 7. Riesgos, limites y deuda

- **BLOQUEANTE:** estos endpoints aún **no existen** en `sensus-back`. Permanece como `Borrador` hasta abrir la spec equivalente en backend (decisión D-6).

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Bloqueada por falta de spec/contrato en backend (D-6) |
