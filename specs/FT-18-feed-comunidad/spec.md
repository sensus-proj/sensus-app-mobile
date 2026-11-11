# SPEC — FT-18 Feed Comunitario Geolocalizado y Publicaciones

> Rector: `.specify/memory/constitution.md` §2, §4.5, §5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-027, SE-028, SE-029**; Pantalla **P-24** (sección comunidad).
> Backend ref: Specs `024-publicaciones-feed`, `026-feed-cercano`.

## 1. Contexto y objetivo

`CommunityScreen` y `CommunityViewModel` consumen actualmente `StoreRepository.getSampleFriends()` (`MockDataProvider`), sin publicaciones vecinales por proximidad ni creación de posts reales con texto, foto moderada, etiqueta de comercio y coordenadas geográficas.

**Objetivo:** implementar el feed comunitario geolocalizado conectado a `GET /api/v1/feed/nearby` (con paginación y *pull-to-refresh*) y el formulario de creación de publicaciones con texto, foto opcional subida vía `POST /api/v1/media/upload` y etiqueta de comercio cercano conectado a `POST /api/v1/feed/posts`.

**Alcance:** `FeedRepository`, `FeedApiService`, `GetNearbyFeedUseCase`, `CreateFeedPostUseCase`, caché de lectura en `FeedPostEntity`/`FeedPostDao`, *pull-to-refresh* en `CommunityScreen` y hoja/pantalla de creación de post comunitario con bloqueo en modo invitado.
**Fuera de alcance:** likes, comentarios y reportes (FT-19), amigos privados por QR (FT-25).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-18-01 | `GET /api/v1/feed/nearby` consulta publicaciones dentro del radio del usuario (`lat`, `lng`, `radiusMeters`, `page`, `pageSize = 20`) respetando los 2000 m si aplica o restringiendo creación/interacción a usuarios autenticados. |
| RF-18-02 | `CommunityScreen` soporta gesto *Pull-to-Refresh* y paginación al hacer scroll hacia abajo. |
| RF-18-03 | Al pulsar el botón de nueva publicación (bloqueado con `GuestPromptDialog` si es invitado), permitir redactar texto, adjuntar imagen opcional (comprimida y subida con moderación previa vía `MediaRepository`) y seleccionar opcionalmente un comercio cercano (`merchantId`). |
| RF-18-04 | `POST /api/v1/feed/posts` envía texto, `mediaId` opcional, `merchantId` opcional y coordenadas del usuario; si el texto o imagen incumple moderación (`400`/`422`), bloquear sin publicar. |
| RF-18-05 | Al pulsar el nombre del comercio etiquetado dentro de una publicación, navegar a las ofertas de ese comercio. |

## 3. Requisitos no funcionales

- **Privacidad:** el feed público muestra nombre visible o alias y distancia aproximada, nunca coordenadas exactas de domicilio del autor.
- **Moderación:** todo texto e imagen pasa por moderación del backend antes de hacerse visible; comportamiento *fail-closed* ante rechazo o cuarentena.
- **Paginación:** 20 ítems por defecto, máx. 100.

## 4. Arquitectura y contratos internos

```text
domain/
  model/FeedModels.kt           # FeedPost, PagedFeed, CreatePostInput
  repository/FeedRepository.kt
  usecase/GetNearbyFeedUseCase.kt, CreateFeedPostUseCase.kt
data/
  local/db/entity/FeedPostEntity.java, dao/FeedPostDao.java
  remote/api/FeedApiService.kt  # GET /api/v1/feed/nearby, POST /api/v1/feed/posts
  remote/dto/FeedDtos.kt
  repository/FeedRepositoryImpl.kt
presentation/buyer/
  CommunityContract.kt, CommunityViewModel.kt, CommunityScreen.kt
  CreateFeedPostSheet.kt
```

## 5. Criterios de aceptacion

- **Given** un usuario autenticado abre Comunidad **When** carga el feed o hace pull-to-refresh **Then** consulta `GET /api/v1/feed/nearby` y renderiza posts locales ordenados por cercanía/fecha.
- **Given** un usuario crea un post con texto ofensivo **When** envía a `POST /api/v1/feed/posts` **Then** el backend lo rechaza/envía a cuarentena y la UI informa el bloqueo sin mostrar el post como publicado.
- **Given** un post con comercio etiquetado **When** el usuario toca el enlace del comercio **Then** navega a las ofertas de ese local.
- **Given** un invitado pulsa publicar **When** intenta crear post **Then** muestra `GuestPromptDialog`.

## 6. Plan de pruebas

- **Unitarias:** `CreateFeedPostUseCaseTest` (validación longitud de texto, bloqueo invitado), `GetNearbyFeedUseCaseTest`.
- **ViewModel:** `CommunityViewModelTest` con Turbine (carga, pull-to-refresh, paginación, creación exitosa y rechazada por moderación).
- **Integración (`MockWebServer` + `FeedPostDao`):** `GET /api/v1/feed/nearby`, `POST /api/v1/feed/posts` (201, 422 moderación).
- **UI (Compose Test Rule):** feed renderizado, apertura de modal crear post y clic en comercio etiquetado.

## 7. Riesgos, limites y deuda

- Depende de la publicación en backend de las specs 024 y 026 (y 017/022 para moderación/media).
- En esta feature se desconecta el feed comunitario de `MockDataProvider.sampleFriends` (dejando `Amigos QR` aislado en `FT-25`).

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
