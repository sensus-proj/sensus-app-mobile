# PLAN — FT-26 Reseñas de comercio estilo Play Store y calificación real

> Derivado de `spec.md` y alineado a `sensus-back/specs/054-resenas-comercios`.
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §9.2, §10 y §11.

## 1. Diseño

1. **Dominio puro:** `StoreReviewModels.kt` (`StoreReview`, `StoreReviewReply`, `StoreReviewsSummary`, `RatingBreakdownItem`, `ReviewLikeStatus`), puerto `StoreReviewRepository` y casos de uso en `StoreReviewUseCases.kt` (`GetStoreReviewsUseCase`, `CreateStoreReviewUseCase`, `ToggleStoreReviewLikeUseCase`, `ReplyStoreReviewUseCase`) validando rango `1..5`, longitudes y `ProfanityFilter`.
2. **Capa de datos:** `StoreReviewsApiService.kt`, DTOs serializables y `StoreReviewRepositoryImpl.kt` registrados en Hilt (`NetworkModule` / `RepositoryModule`).
3. **Capa MVI y UI:** `StoreReviewsContract.kt`, `StoreReviewsViewModel.kt`, `StoreReviewsScreen.kt` y `StoreReviewsComponents.kt` accesibles al pulsar `StoreCatalogHeaderCard` en `StoreCatalogScreen`. Actualización de `MerchantProfile.kt`, `MerchantProfileViewModel.kt` y `MerchantProfileScreen.kt` para exponer calificación y conteo reales sin datos falsos. Uso estricto de `SensusPurple` en estrellas (Constitución §9.2).

## 2. UI y navegación

- `BuyerViewRoutes.STORE_REVIEWS` con `merchantId` seleccionado desde `StoreCatalogScreen`.
- Cabecera estilo Play Store con promedio a 1 decimal, barras por estrella, botón «Calificar tienda», lista de reseñas con avatar/fecha-año/like/responder y diálogo de respuesta.

## 3. DI

| Binding | Implementación |
|---|---|
| `StoreReviewsApiService` | Retrofit sobre base URL en `NetworkModule` |
| `StoreReviewRepository` | `StoreReviewRepositoryImpl` vía `@Binds` |

## 4. Gates

- `compileDebugKotlin` verde
- `detekt ktlintCheck lint` verdes (0 violaciones)
- `testDebugUnitTest koverVerifyDebug` verdes (`>= 80%`)
- `check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 80` PASS

## 5. Orden de implementación

1. Crear modelos, repositorio y casos de uso en `domain/` (`StoreReviewModels.kt`, `StoreReviewUseCases.kt`).
2. Implementar DTOs, `StoreReviewsApiService.kt` y `StoreReviewRepositoryImpl.kt` en `data/`.
3. Conectar `StoreReviewsScreen`/`StoreReviewsViewModel`, `StoreCatalogHeaderCard` y calificación real en `MerchantProfileScreen` usando `SensusPurple` para estrellas.
4. Completar cobertura unitaria en `StoreReviewsViewModelTest` y `MerchantProfileViewModelTest` hasta `>= 80%`.

## 6. Prohibiciones

- Prohibido usar tonos amarillos o ámbar (`SensusRating`) en estrellas de UI interna (Constitución §9.2).
- Prohibido mostrar promedios o reseñas hardcodeadas cuando el backend no tenga reseñas.
