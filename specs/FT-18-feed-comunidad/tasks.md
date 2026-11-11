# TASKS — FT-18 Feed Comunitario Geolocalizado y Publicaciones

> 1 PR = 1 slice. Orden: Dominio -> DB/API -> ViewModel/UI -> Tests -> Gates.

## Fase A — Dominio

- [ ] T18.1 Crear `FeedPost`, `PagedFeed`, `CreatePostInput` y `FeedRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T18.2 Crear `GetNearbyFeedUseCase` y `CreateFeedPostUseCase` (validación de texto + flujo upload foto) — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos y Caché

- [ ] T18.3 Crear `FeedPostEntity.java`, `FeedPostDao.java` y registrar en `SensusDatabase.java` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T18.4 Crear `FeedDtos.kt`, `FeedApiService.kt` (`GET /api/v1/feed/nearby`, `POST /api/v1/feed/posts`) y `FeedMapper.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T18.5 Implementar `FeedRepositoryImpl` desconectando el feed de `StoreRepository.getSampleFriends()` — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [ ] T18.6 Actualizar `CommunityContract.kt` y `CommunityViewModel.kt` con paginación, pull-to-refresh y creación de post — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T18.7 Crear `CreateFeedPostSheet.kt` (texto, foto moderada y selector de comercio cercano) y actualizar `CommunityScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [ ] T18.8 Registrar `FeedApiService`, `FeedRepository` y casos de uso en `AppModule.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T18.9 Añadir tests unitarios, `FeedPostDao` SQLCipher y `MockWebServer` (`nearby` 200, `posts` 201 y 422 moderación) — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T18.10 Añadir tests Turbine y Compose UI para pull-to-refresh, modal de publicación y etiqueta de comercio — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T18.11 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_endpoints.py`
