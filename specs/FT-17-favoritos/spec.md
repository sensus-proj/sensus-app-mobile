# SPEC — FT-17 Favoritos y Guardados de Ofertas

> Rector: `.specify/memory/constitution.md` §2, §5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-036**; Pantalla **P-23**; Componentes **C-36, C-37**.
> Backend ref: Spec `038-favoritos`.

## 1. Contexto y objetivo

En `ProductDetailScreen.kt` pulsar el botón de corazón únicamente muestra un `Toast("Guardado en favoritos")`, sin persistencia local ni sincronización con el backend ni pantalla de listado de guardados.

**Objetivo:** implementar la alternancia real de favoritos contra `POST /api/v1/users/favorites/{id}` (con actualización optimista y rollback ante fallo, bloqueada en modo invitado) y crear la pantalla `Favoritos` (`P-23`) respaldada por Room + SQLCipher con filtros `Todos`, `Cerca de mí` y `Por agotarse`.

**Alcance:** `FavoriteEntity` / `FavoriteDao` en SQLCipher, `FavoritesRepository`, `ToggleFavoriteUseCase`, `GetFavoritesUseCase`, `FavoritesScreen` (`P-23`) con chips `C-37`, estado vacío e integración del icono corazón en tarjetas (`BuyerHomeScreen`) y detalle (`ProductDetailScreen`).
**Fuera de alcance:** entrenamiento directo de modelo ML en móvil (lo procesa el backend al recibir la alternancia).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-17-01 | Al pulsar el icono de favorito en tarjeta o `ProductDetailScreen`, si el usuario es invitado abrir `GuestPromptDialog`; si está autenticado invocar `ToggleFavoriteUseCase`. |
| RF-17-02 | `ToggleFavoriteUseCase` envía `POST /api/v1/users/favorites/{id}`, actualiza `FavoriteDao` de forma optimista y revierte en UI si la API devuelve error (`404` oferta no visible, `401` o fallo no recuperable). |
| RF-17-03 | Pantalla `Favoritos` (`P-23`) accesible desde perfil/navegación, mostrando foto, comercio, estado `Abierto/Cerrado ahora`, precio y badge dinámico (`Disponible`, `Quedan N` si `1 <= stock <= 3`, `Agotado` si `stock == 0`). |
| RF-17-04 | Chips de filtro (`C-37`): `Todos`, `Cerca de mí` (dentro del radio configurado) y `Por agotarse` (`1 <= stock <= 3`), más estado vacío cuando no haya elementos. |
| RF-17-05 | Sincronizar con `GET /api/v1/users/favorites` cuando el backend habilite listado remoto *(propuesto; entretanto `FavoriteDao` combinado con `OfferDao` es la fuente offline-first)*. |

## 3. Requisitos no funcionales

- **Offline-First:** favoritos disponibles sin red desde `FavoriteDao` (SQLCipher); si se marca/desmarca sin red, se encola o informa claramente sin perder coherencia.
- **Tasa:** respetar límite de 120 alternancias/hora por usuario en UI con control de doble toque.
- **Design System:** estado vacío sin emojis, con icono vectorial y CTA hacia explorar ofertas.

## 4. Arquitectura y contratos internos

```text
domain/
  model/FavoriteFilter.kt       # ALL, NEARBY, LOW_STOCK
  repository/FavoritesRepository.kt
  usecase/ToggleFavoriteUseCase.kt, GetFavoritesUseCase.kt
data/
  local/db/entity/FavoriteEntity.java, dao/FavoriteDao.java
  remote/api/FavoritesApiService.kt # POST /api/v1/users/favorites/{id}
  repository/FavoritesRepositoryImpl.kt
presentation/buyer/
  FavoritesContract.kt, FavoritesViewModel.kt, FavoritesScreen.kt
```

## 5. Criterios de aceptacion

- **Given** un comprador autenticado pulsa corazón en `ProductDetailScreen` **When** el backend responde `200` **Then** el icono cambia a activo y la oferta aparece en `FavoritesScreen`.
- **Given** un usuario invitado pulsa corazón **When** intenta guardar favorito **Then** aparece `GuestPromptDialog` y no se llama al endpoint.
- **Given** una oferta favorita con `stock = 2` **When** filtra por `Por agotarse` en `FavoritesScreen` **Then** se muestra con badge `"Quedan 2"`.
- **Given** el servidor responde `404` al alternar favorito **When** falla **Then** se revierte el estado del corazón y se muestra aviso.

## 6. Plan de pruebas

- **Unitarias:** `ToggleFavoriteUseCaseTest` (invitado rechazado, alternancia), `GetFavoritesUseCaseTest` (filtros `Todos`, `Cerca de mí`, `Por agotarse`).
- **ViewModel:** `FavoritesViewModelTest` con Turbine.
- **Integración (`MockWebServer` + Room SQLCipher):** `POST /api/v1/users/favorites/{id}` y persistencia en `FavoriteDao`.
- **UI (Compose Test Rule):** filtros `C-37`, estado vacío y rollback/Toast sustituido por estado reactivo en `ProductDetailScreen`.

## 7. Riesgos, limites y deuda

- En `sensus-back` Spec 038 solo existe `POST /api/v1/users/favorites/{id}` (toggle); el listado remoto `GET /api/v1/users/favorites` queda documentado como contrato propuesto para sincronización multi-dispositivo.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
