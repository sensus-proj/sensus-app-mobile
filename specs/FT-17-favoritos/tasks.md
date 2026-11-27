# TASKS — FT-17 Favoritos y Guardados de Ofertas

> 1 PR = 1 slice. No ejecutar tareas automáticamente; dejarlas pendientes hasta implementación aprobada.
> Orden: Dominio -> Datos/Room -> Remoto/Repo -> Presentación -> Integración -> Tests -> Docs/Gates.

## Fase A — Dominio y contratos

- [x] T17.1 Definir modelo de dominio de favoritos (`FavoriteFilter`, `FavoriteOffer`, `FavoriteToggleResult`) y contrato `FavoritesRepository` en `domain/` — verif: `check_layers.py` (`373 archivos`) y `compileDebugKotlin` PASS.
- [x] T17.2 Crear `ToggleFavoriteUseCase` (bloqueo invitado, `debounce`/idempotencia en VM, resultado tipado) y `GetFavoritesUseCase`/`ObserveFavoritesUseCase` con filtros `ALL` (`"Todas"`) y `AVAILABLE_ONLY` (`"Solo disponibles"`, conciliado por plan §3 Decisión 7) — verif: `FavoritesUseCasesTest` (`4/4` PASS).

## Fase B — Persistencia local SQLCipher

- [x] T17.3 Crear `FavoriteEntity.java` y `FavoriteDao.java` en `data/local/db/`; agregar entidad/DAO a `SensusDatabase.java` y subir versión 8→9 — verif: `:app:kaptDebugKotlin` y `compileDebugKotlin` PASS.
- [x] T17.4 Agregar `MIGRATION_8_9` en `SensusDatabaseMigrations.java` y registrarla en `DatabaseFactory.kt`; proveer `FavoriteDao` en `DatabaseModule.kt` y `SensusContainer.kt` (purga en `SessionRepositoryImpl.clearSession()`) — verif: `OffersRepositoryOfflineTest` + `compileDebugAndroidTestSources` PASS.
- [x] T17.5 Añadir prueba de migración Room 8→9 y pruebas de `FavoriteDao` en `EncryptedDatabaseIntegrationTest.kt` (CRUD/clear en SQLCipher y migración `MIGRATION_8_9`) — verif: `compileDebugAndroidTestSources` PASS (ejecución en dispositivo pendiente `§5.Z-3`).

## Fase C — Remoto y repositorio

- [x] T17.6 Crear `FavoritesApiService.kt` y `FavoriteDtos.kt` para `POST /api/v1/users/favorites/{offerId}` y `GET /api/v1/users/favorites` (Decisión 7) registrados en `NetworkClient.kt` y `FavoritesModule.kt` — verif: `compileDebugKotlin` PASS.
- [x] T17.7 Implementar `FavoritesRepositoryImpl` con actualización optimista, rollback ante error no recuperable, preservación de caché Room cuando `GET` responde `404`, aislamiento por `ownerKey` y sincronización en `Dispatchers.IO` — verif: `FavoritesRepositoryImplTest` PASS.
- [x] T17.8 Añadir `FavoritesRepositoryImplTest` con `MockWebServer` (éxito 200, 404 en GET conserva caché, 401/429/JSON malformado con rollback, aislamiento por cuenta) y `check_secrets.py` PASS — verif: `FavoritesRepositoryImplTest` (`3/3` PASS) y `check_secrets.py` (`540 archivos`).

## Fase D — Presentación y navegación

- [x] T17.9 Crear `FavoritesContract.kt`, `FavoritesViewModel.kt`, `FavoritesScreen.kt` y `ProductOfferCard.kt` (`P-23`, chips `C-37`, estados Loading/Loaded/Empty/Error, 4 `@Preview`) — verif: `compileDebugKotlin`, `detekt` y `ktlintCheck` PASS.
- [x] T17.10 Agregar ruta/overlay de `FavoritesScreen` (`MainAppBuyerOverlays.kt`, `MainAppMenu.kt`, `MainAppContainer.kt`) y entrada «Mis favoritos» desde `BuyerProfileScreen` — verif: `compileDebugKotlin` PASS.
- [x] T17.11 Reconciliar `ProductDetailScreen`/`ProductDetailComponents` con `FavoritesViewModel` (`CircleFavoriteIconButton` para ofertas y `GuestPromptDialog` a invitados, eliminando la doble acción `ProductSaveInterestPill`); se preserva `SavedOffersRepository` exclusivo para publicaciones de Comunidad — verif: `compileDebugKotlin` PASS.
- [x] T17.12 Integrar corazón de favorito en `BuyerHomeScreen` reutilizando `FavoriteProductOfferCard` y `FavoritesViewModel` — verif: `compileDebugKotlin` PASS.

## Fase E — Pruebas UI, docs y gates

- [x] T17.13 Añadir `FavoritesViewModelTest` (`2/2` PASS con Turbine) y `FavoritesScreenTest` (`androidTest` Compose: filtros, vacío, invitado, toggle); además ampliado `OffersRepositoryOfflineTest` con limpieza de `FavoriteDao` — verif: `testDebugUnitTest` (`541/541`) y `compileDebugAndroidTestSources` PASS.
- [x] T17.14 Actualizar `docs/endpoints-internos.md` y `docs/rate-matrix.md` con contrato final de favoritos (`POST /api/v1/users/favorites/{offerId}`, `GET /api/v1/users/favorites` 120 req/h según `FavoritesPolicy` Spec 038) — verif: `python scripts/check_endpoints.py` (`61 endpoints`) PASS.
- [x] T17.15 Actualizar `specs/README.md` (`En revision`), `memoria.md` §6 con decisión SavedInterests/Favoritos y evidencia, y completar `checklists/checklist.md` — verif: `python scripts/check_sdd.py` (`43 features`) PASS.
- [x] T17.16 Ejecutar gates completos de slice (`compileDebugKotlin`, `compileDebugAndroidTestSources`, `detekt`, `ktlintCheck`, `lint`, `testDebugUnitTest`, `koverXmlReportDebug`, `koverVerifyDebug`, `check_coverage.py --min 80` = `80.75%`, y `check_*.py`) — verif: todo PASS (ejecución física instrumentada pendiente `§5.Z-3`).