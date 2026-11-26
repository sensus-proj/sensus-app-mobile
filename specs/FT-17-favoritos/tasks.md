# TASKS — FT-17 Favoritos y Guardados de Ofertas

> 1 PR = 1 slice. No ejecutar tareas automáticamente; dejarlas pendientes hasta implementación aprobada.
> Orden: Dominio -> Datos/Room -> Remoto/Repo -> Presentación -> Integración -> Tests -> Docs/Gates.

## Fase A — Dominio y contratos

- [ ] T17.1 Definir modelo de dominio de favoritos (`FavoriteFilter`, modelo de favorito/oferta para P-23) y contrato `FavoritesRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin --no-daemon`
- [ ] T17.2 Crear `ToggleFavoriteUseCase` (bloqueo invitado, debounce/regla de tasa UI, resultado tipado) y `GetFavoritesUseCase`/`ObserveFavoritesUseCase` con filtros `ALL`, `NEARBY`, `LOW_STOCK` — verif: `./gradlew.bat testDebugUnitTest --no-daemon`

## Fase B — Persistencia local SQLCipher

- [ ] T17.3 Crear `FavoriteEntity.java` y `FavoriteDao.java` en `data/local/db/`; agregar entidad/DAO a `SensusDatabase.java` y subir versión 8→9 — verif: `./gradlew.bat compileDebugKotlin --no-daemon`
- [ ] T17.4 Agregar `MIGRATION_8_9` en `SensusDatabaseMigrations.java` y registrarla en la factory/builder de DB; proveer `FavoriteDao` en `DatabaseModule` — verif: `./gradlew.bat testDebugUnitTest --no-daemon`
- [ ] T17.5 Añadir prueba de migración Room 8→9 y pruebas de `FavoriteDao` (insert/toggle/list/delete, lectura offline) — verif: `./gradlew.bat testDebugUnitTest --no-daemon`

## Fase C — Remoto y repositorio

- [ ] T17.6 Crear `FavoritesApiService.kt` para `POST /api/v1/users/favorites/{offerId}` y DTO/borrador para `GET /api/v1/users/favorites` (si backend lo confirma); registrar en el cliente/red DI existente — verif: `./gradlew.bat compileDebugKotlin --no-daemon`
- [ ] T17.7 Implementar `FavoritesRepositoryImpl` con actualización optimista, rollback ante error no recuperable, persistencia Room y sincronización remota; bindear en DI — verif: `./gradlew.bat testDebugUnitTest --no-daemon`
- [ ] T17.8 Añadir tests de repositorio con `MockWebServer` (éxito 200, 401/404/error, rollback) y verificar que no se registren PII/tokens en logs — verif: `./gradlew.bat testDebugUnitTest --no-daemon`

## Fase D — Presentación y navegación

- [ ] T17.9 Crear `FavoritesContract.kt`, `FavoritesViewModel.kt` y `FavoritesScreen.kt` (P-23, chips `C-37`, estados Loading/Loaded/Empty/Error) — verif: `./gradlew.bat compileDebugKotlin --no-daemon`
- [ ] T17.10 Agregar navegación/ruta a `FavoritesScreen` y entrada desde `BuyerProfileScreen` (o punto de navegación acordado) — verif: `./gradlew.bat compileDebugKotlin --no-daemon`
- [ ] T17.11 Reconciliar `ProductDetailScreen` para usar Favorito backend-synced en ofertas (reemplazar pin/guardado de oferta actual) mostrando `GuestPromptDialog` a invitados; preservar guardado de publicaciones en Intereses guardados — verif: `./gradlew.bat compileDebugKotlin --no-daemon`
- [ ] T17.12 Evaluar e integrar corazón de favorito en tarjetas de `BuyerHomeScreen` solo si el componente existente lo soporta sin ampliar alcance; si no, documentar deuda en `spec.md`/`Memoria.md` — verif: `./gradlew.bat compileDebugKotlin --no-daemon`

## Fase E — Pruebas UI, docs y gates

- [ ] T17.13 Añadir tests de `FavoritesViewModel` con Turbine y pruebas Compose/UI para P-23 (filtros, vacío, badge stock, invitado) — verif: `./gradlew.bat testDebugUnitTest --no-daemon` y `./gradlew.bat compileDebugAndroidTestSources --no-daemon`
- [ ] T17.14 Actualizar `docs/endpoints-internos.md` y `docs/rate-matrix.md` con contrato final de favoritos; reconciliar límite 120/hora vs Spec 038 — verif: `python scripts/check_endpoints.py`
- [ ] T17.15 Actualizar `specs/README.md` estado de FT-17 si aplica, `Memoria.md` §6 con decisión SavedInterests/Favoritos y evidencia, y completar `checklists/checklist.md` — verif: `python scripts/check_sdd.py`
- [ ] T17.16 Ejecutar gates completos de slice: compile/tests/lint/detekt/ktlint/kover/checks SDD/cobertura/secretos según configuración real del repo — verif: gates en PASS según `checklists/checklist.md`