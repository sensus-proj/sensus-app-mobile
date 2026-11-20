# FT-31 — Me gusta en publicaciones de producto (`OfferLike`)

| Campo | Valor |
|---|---|
| Feature | `FT-31-me-gusta-ofertas` |
| Estado | `En revision` |
| Depende de | `FT-12-detalle-filtros-ruteo`, `FT-15-oferta-creacion-comercio` |
| Spec backend | [`057-me-gusta-ofertas`](../../../sensus-back/specs/057-me-gusta-ofertas/spec.md) |
| Pantalla / Componentes | `ProductDetailScreen`, `ProductDetailComponents`, `StoreCatalogComponents` |

## 1. Contexto y objetivo

Permitir que los usuarios registrados reaccionen con «Me gusta» desde el detalle de cualquier oferta/producto (`ProductDetailScreen`) y desde tarjetas de catálogo, mostrando de forma pública cuántas personas han dado «Me gusta» (`likesCount`) y si el usuario actual ya reaccionó (`isLikedByMe`), sincronizado con el servidor y con actualización optimista reversible ante errores.

> Distinta de `FT-19-interacciones-reportes` (`POST /api/v1/feed/posts/{id}/like` sobre posts de feed): esta feature opera sobre productos/ofertas comerciales (`/api/v1/offers/{id}/like`).

## 2. Usuarios y flujo principal

1. El **Consumidor** abre la ficha de un producto desde el mapa o el catálogo de tienda (`ProductDetailScreen`).
2. `ProductDetailViewModel` carga el detalle actualizado mediante `GET /api/v1/offers/{id}` y refleja `likesCount` e `isLikedByMe`.
3. Al pulsar el botón de «Me gusta» (`ProductSocialLikeRow`), el ViewModel aplica una transición optimista inmediata e invoca `ToggleOfferLikeUseCase` -> `POST /api/v1/offers/{id}/like`.
4. Si la petición es exitosa, sincroniza los contadores reales del servidor; si falla o es invitado (`403`), revierte el estado optimista y muestra retroalimentación clara.

## 3. Requisitos funcionales

- **`RF-31-01`:** Mostrar el botón interactivo «Me gusta» y el conteo público de reacciones (`1 persona dio me gusta` / `N personas dieron me gusta`) en `ProductDetailScreen`.
- **`RF-31-02`:** Exponer `likesCount` e `isLikedByMe` en `Offer` / `ProductDetailViewState` poblados desde `GET /api/v1/offers/{id}` y listados de catálogo.
- **`RF-31-03`:** Ejecutar `ToggleOfferLikeUseCase` contra `POST /api/v1/offers/{id}/like` con actualización optimista, bloqueo de doble clic (`isTogglingLike`) y reversión automática si falla.
- **`RF-31-04`:** Mostrar el conteo de likes en tarjetas del catálogo de tienda (`StoreCatalogComponents`).

## 4. Requisitos no funcionales y arquitectura

- Clean Architecture: `ToggleOfferLikeUseCase` en `domain/usecase/`, `OfferDetailsApiService` y `OfferRepositoryImpl` en `data/`, `ProductDetailViewModel` MVI en `presentation/buyer/`.
- Cobertura unitaria Kover >= 80 % sobre `ToggleOfferLikeUseCase` y `ProductDetailViewModel`.

## 5. Contratos y navegación

| Endpoint | Origen -> Destino | Uso |
|---|---|---|
| `GET /api/v1/offers/{id}` | `GetOfferDetailUseCase` -> `OffersRepository` -> `OfferDetailsApiService` | Cargar detalle y conteo real de likes |
| `POST /api/v1/offers/{id}/like` | `ToggleOfferLikeUseCase` -> `OffersRepository` -> `OfferDetailsApiService` | Alternar «Me gusta» del usuario |

## 6. Criterios de aceptación

- Pulsar «Me gusta» incrementa/decrementa el conteo visualmente y lo persiste al reabrir la pantalla.
- Todos los usuarios (incluidos invitados) ven cuántos «Me gusta» acumula el producto; invitados no pueden mutar.

## 7. Pruebas requeridas

- `ProductDetailViewModelTest` (carga de likes iniciales, toggle optimista exitoso, prevención de doble toque y reversión ante fallo HTTP).

## Aprobación

| Rol | Responsable | Estado | Fecha |
|---|---|---|---|
| Producto / SDD | Equipo Sensus | Aprobada (retro-spec `057-me-gusta-ofertas`) | 2026-11-19 |
| Android Lead | Equipo Móvil | En revisión de cobertura | 2026-11-19 |
