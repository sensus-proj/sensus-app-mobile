# PLAN — FT-31 Me gusta en publicaciones de producto (`OfferLike`)

> Derivado de `spec.md` y alineado a `sensus-back/specs/057-me-gusta-ofertas`.
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6 y §10.

## 1. Diseño

1. Extender modelo de oferta con `likesCount` e `isLikedByMe` y crear `ToggleOfferLikeUseCase` en `domain/`.
2. Agregar `@POST("api/v1/offers/{id}/like")` en `OfferDetailsApiService.kt` y mapear respuesta `OfferLikeStatusDto` en repositorio.
3. Conectar `ProductDetailContract`, `ProductDetailViewModel` (estado optimista + rollback), `ProductSocialLikeRow` en `ProductDetailComponents.kt` y resumen visual en `StoreCatalogComponents.kt`.

## 2. UI y navegación

- Componente `ProductSocialLikeRow` integrado en `ProductDetailScreen.kt` (sin modificar lógica de `OfferCategory`).

## 3. DI

| Binding | Implementación |
|---|---|
| `OfferDetailsApiService` | Retrofit existente (`NetworkModule`) |
| `ToggleOfferLikeUseCase` | Inyectado por constructor `@Inject` en `ProductDetailViewModel` |

## 4. Gates

- `compileDebugKotlin`, `detekt`, `ktlintCheck`, `lint`, `testDebugUnitTest koverVerifyDebug`
- `check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 80`

## 5. Orden de implementación

1. Crear `ToggleOfferLikeUseCase` y propiedades `likesCount`/`isLikedByMe`.
2. Conectar `OfferDetailsApiService.toggleOfferLike` y `ProductDetailViewModel`.
3. Añadir tests unitarios en `ProductDetailViewModelTest`.

## 6. Prohibiciones

- No modificar `OfferCategory` ni flujos de creación/edición de oferta.
