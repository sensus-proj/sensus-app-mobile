# TASKS — FT-30 Feed de Promociones por Intereses

> 1 PR = 1 slice. Orden: Dominio -> API/Room -> ViewModel/UI -> Tests -> Gates.

## Fase A — Dominio

- [x] T30.1 Crear `OfferFeedModels.kt`, `OfferFeedRepository` e `InterestsRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T30.2 Crear `GetOfferFeedUseCase` (reinicio y reutilización de `snapshotId`, deduplicación), `GetMyInterestsUseCase` y `UpdateMyInterestsUseCase` — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos

- [x] T30.3 Crear `OfferFeedApiService` (`GET /api/v1/feed/offers`) e `InterestsApiService` (`GET`/`PUT /api/v1/users/me/interests`) con DTOs — verif: `./gradlew.bat compileDebugKotlin`
- [x] T30.4 Crear `FeedOfferEntity`, `FeedOfferDao` y registrar en `SensusDatabase` con migración — verif: `./gradlew.bat compileDebugKotlin`
- [x] T30.5 Implementar `OfferFeedRepositoryImpl` e `InterestsRepositoryImpl` con caché cifrada y bloqueo de invitado — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [x] T30.6 Crear `InterestsPickerContract/ViewModel/Screen` e integrarlo al final del registro y en `BuyerProfileScreen` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T30.7 Crear `OfferFeedContract/ViewModel/Screen` con paginación, *pull to refresh* y estados — verif: `./gradlew.bat compileDebugKotlin`
- [x] T30.8 Añadir la pestaña `Promociones` a la navegación y el `GuestPromptDialog` para invitados — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [x] T30.9 Registrar servicios, DAO, repositorios y casos de uso en Hilt — verif: `./gradlew.bat compileDebugKotlin`
- [x] T30.10 Pruebas unitarias, Turbine y `MockWebServer` (paginación, `403`, offline) — verif: `./gradlew.bat testDebugUnitTest`
- [x] T30.11 Pruebas Compose UI del feed y del selector — verif: `./gradlew.bat compileDebugAndroidTestSources` (compilación verificada; ejecución en emulador pendiente)
- [x] T30.12 Conciliar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y validar gates — verif: `python scripts/check_endpoints.py` (PASS 54 endpoints)
