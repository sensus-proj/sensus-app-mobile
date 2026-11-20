# TASKS — FT-20 Búsqueda Inteligente, Recomendaciones y Alternativas ML

> 1 PR = 1 slice. Orden: Dominio -> Room/API -> ViewModel/UI -> Tests -> Gates.

## Fase A — Dominio

- [x] T20.1 Crear modelos de descubrimiento y `DiscoveryRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T20.2 Crear `SearchOffersUseCase` y `GetOfferAlternativesUseCase` — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos y Caché

- [x] T20.3 Crear `SearchHistoryEntity.java`, `SearchHistoryDao.java` (top 5, limpieza) y registrar en `SensusDatabase.java` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T20.4 Crear `DiscoveryApiService.kt` (`GET /api/v1/offers/search` y `/offers/{id}/alternatives`) y DTOs — verif: `./gradlew.bat compileDebugKotlin`
- [x] T20.5 Implementar `DiscoveryRepositoryImpl` con fallback a `OfferDao` offline y regla sin historial para invitado — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [x] T20.6 Crear `SearchOffersContract.kt` y `SearchOffersViewModel.kt` con *debounce* `300 ms` y consulta automática de alternativas cuando 0 resultados — verif: `./gradlew.bat testDebugUnitTest`
- [x] T20.7 Crear `SearchOffersScreen.kt` (`P-12` y estado `P-13` con sustitutos) y conectar desde `BuyerHomeScreen.kt` (`C-06`) — verif: `./gradlew.bat compileDebugKotlin`
- [x] T20.8 *Retirada (2026-11-20):* las recomendaciones pasan al feed de promociones de FT-30 — verif: `specs/FT-30-feed-promociones-intereses/`

## Fase D — DI, Pruebas y Cierre

- [x] T20.9 Registrar `DiscoveryApiService`, `DiscoveryRepository` y casos de uso en `DiscoveryModule.kt` (`di/`) — verif: `./gradlew.bat compileDebugKotlin`
- [x] T20.10 Añadir pruebas unitarias, `SearchHistoryDao`, `MockWebServer` y tests Turbine de *debounce*/alternativas — verif: `./gradlew.bat testDebugUnitTest`
- [x] T20.11 Añadir tests Compose UI para `SearchOffersScreen` (`P-12`/`P-13`) — verif: `./gradlew.bat compileDebugAndroidTestSources` (compilación verificada; ejecución en emulador pendiente)
- [x] T20.12 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_endpoints.py` (PASS 54 endpoints)
