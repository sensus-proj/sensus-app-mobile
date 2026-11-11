# TASKS — FT-17 Favoritos y Guardados de Ofertas

> 1 PR = 1 slice. Orden: Dominio -> Room/API -> ViewModel/Screen -> Integración tarjetas -> Tests -> Gates.

## Fase A — Dominio

- [ ] T17.1 Crear `FavoriteFilter` (`ALL`, `NEARBY`, `LOW_STOCK`) y `FavoritesRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T17.2 Crear `ToggleFavoriteUseCase` (bloqueo invitado + rollback) y `GetFavoritesUseCase` (filtros de distancia y stock `<= 3`) — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos y Room SQLCipher

- [ ] T17.3 Crear `FavoriteEntity.java`, `FavoriteDao.java`, actualizar `SensusDatabase.java` y `DatabaseModule.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T17.4 Crear `FavoritesApiService.kt` (`POST /api/v1/users/favorites/{id}`, borrador `GET /api/v1/users/favorites`) y DTOs — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T17.5 Implementar `FavoritesRepositoryImpl` con actualización optimista/rollback — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [ ] T17.6 Crear `FavoritesContract.kt`, `FavoritesViewModel.kt` y `FavoritesScreen.kt` (`P-23`, chips `C-37`, estado vacío) — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T17.7 Reemplazar el `Toast` de favorito en `ProductDetailScreen.kt` por estado real de `ProductDetailViewModel` y agregar corazón en `BuyerHomeScreen` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T17.8 Añadir entrada a `FavoritesScreen` desde `BuyerProfileScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — Pruebas y Cierre

- [ ] T17.9 Añadir `ToggleFavoriteUseCaseTest`, `GetFavoritesUseCaseTest`, tests `FavoriteDao` y `MockWebServer` — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T17.10 Añadir tests Turbine para `FavoritesViewModel` y Compose UI para filtros `C-37` y estado vacío — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T17.11 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_endpoints.py`
