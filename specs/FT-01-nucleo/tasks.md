# TASKS — FT-01 Nucleo: Contratos Base, Red y DI

> 1 PR = 1 slice. Orden: contratos -> red -> DI -> navegacion -> migracion -> gates.
> **Pendientes, sin ejecutar.**

## Fase A — Contratos base

- [x] T01.1 Crear `util/Result.kt` (`Success`, `Failure`, `Loading`) — verif: `testDebugUnitTest`
- [x] T01.2 Crear `util/AppError.kt` (`Network`, `Http`, `Serialization`, `Unknown`) — verif: `testDebugUnitTest`
- [x] T01.3 Crear `domain/base/UseCase.kt` y `NoInputUseCase.kt` — verif: `compileDebugKotlin`
- [x] T01.4 Crear `presentation/base/{ViewState,ViewIntent,ViewEffect}.kt` — verif: `compileDebugKotlin`
- [x] T01.5 Crear `presentation/base/MviViewModel.kt` con `StateFlow` y `SharedFlow` — verif: `testDebugUnitTest`

## Fase B — Red

- [x] T01.6 Crear `data/remote/api/` con la interfaz base de servicios — verif: `compileDebugKotlin`
- [x] T01.7 Crear `data/remote/HttpErrorMapper.kt` (`IOException -> Network`, `code -> Http`) — verif: `testDebugUnitTest`
- [x] T01.8 Crear `AuthInterceptor` contra `TokenLocalDataSource` — verif: `testDebugUnitTest`
- [x] T01.9 Crear `RateLimitInterceptor` con `Retry-After` y backoff idempotente — verif: `testDebugUnitTest`
- [x] T01.10 Crear `SanitizedLogger` (solo debug, enmascara `Authorization`/`Cookie`) — verif: `testDebugUnitTest`
- [x] T01.11 Crear `di/NetworkModule` con `Json`, `OkHttpClient`, `Retrofit` y timeouts — verif: `compileDebugKotlin`

## Fase C — DI

- [x] T01.12 Crear `di/AppModule` (dispatchers, clock, logger, correlation id) — verif: `compileDebugKotlin`
- [x] T01.13 Crear `di/RepositoryModule` con bindings de repositorios existentes — verif: `compileDebugKotlin`
- [x] T01.14 Crear `util/NetworkMonitor.kt` y registrarlo en DI — verif: `testDebugUnitTest`

## Fase D — Navegacion

- [x] T01.15 Crear `navigation/Routes.kt` con rutas `@Serializable` — verif: `compileDebugKotlin`
- [x] T01.16 Crear `navigation/SensusNavHost.kt` y montarlo en `MainActivity` — verif: `compileDebugKotlin`
- [x] T01.17 Retirar `NavigationKeys.kt` cuando no queden referencias — verif: `compileDebugKotlin`

## Fase E — Migracion del codigo actual

- [x] T01.18 Extraer interfaces por agregado en `domain/repository/` desde `DataRepository.kt` — verif: `compileDebugKotlin`
- [x] T01.19 Crear `data/repository/*Impl.kt` que devuelvan `Result<T>` — verif: `compileDebugKotlin`
- [x] T01.20 Crear `MainViewModel` MVI que envuelva el estado actual — verif: `testDebugUnitTest`
- [x] T01.21 Migrar `MainScreen.kt` a `collectAsStateWithLifecycle()` — verif: `compileDebugKotlin`
- [x] T01.22 Retirar `MainScreenViewModel.kt` legacy y su test — verif: `testDebugUnitTest`
- [x] T01.23 Verificar que `presentation/` no importa `data/` — verif: `python scripts/check_layers.py`

## Fase F — Cierre

- [x] T01.24 `compileDebugKotlin` verde — verif: `compileDebugKotlin`
- [x] T01.25 `testDebugUnitTest` con `MockWebServer` y Turbine verde — verif: `testDebugUnitTest`
- [x] T01.26 `lint`, `detekt`, `ktlintCheck` verdes — verif: `lint detekt ktlintCheck`
- [x] T01.27 `python scripts/check_endpoints.py` PASS — verif: script
- [x] T01.28 Actualizar `docs/endpoints-internos.md` y `docs/rate-matrix.md` con los contratos de red — verif: revision humana
- [x] T01.29 Actualizar `Memoria.md` §6 y `CHECKLIST_PR.md` — verif: revision humana
- [x] T01.30 Tag `v1.4.0-ft01` — verif: revision humana
