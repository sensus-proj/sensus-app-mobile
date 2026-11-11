# TASKS — FT-20 Búsqueda Inteligente, Recomendaciones y Alternativas ML

> 1 PR = 1 slice. Orden: Dominio -> Room/API -> ViewModel/UI -> Tests -> Gates.

## Fase A — Dominio

- [ ] T20.1 Crear modelos de descubrimiento y `DiscoveryRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T20.2 Crear `SearchOffersUseCase`, `GetRecommendationsUseCase` y `GetOfferAlternativesUseCase` — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos y Caché

- [ ] T20.3 Crear `SearchHistoryEntity.java`, `SearchHistoryDao.java` (top 5, limpieza) y registrar en `SensusDatabase.java` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T20.4 Crear `DiscoveryApiService.kt` (`GET /api/v1/ml/search`, `/recommendations`, `/alternatives`) y DTOs — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T20.5 Implementar `DiscoveryRepositoryImpl` con fallback a `OfferDao` offline y regla sin historial para invitado — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [ ] T20.6 Crear `SearchOffersContract.kt` y `SearchOffersViewModel.kt` con *debounce* `300 ms` y consulta automática de alternativas cuando 0 resultados — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T20.7 Crear `SearchOffersScreen.kt` (`P-12` y estado `P-13` con sustitutos) y conectar desde `BuyerHomeScreen.kt` (`C-06`) — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T20.8 Integrar sección `'Recomendados para ti'` en `BuyerHomeViewModel.kt` y `BuyerHomeScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [ ] T20.9 Registrar `DiscoveryApiService`, `DiscoveryRepository` y casos de uso en `AppModule.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T20.10 Añadir pruebas unitarias, `SearchHistoryDao`, `MockWebServer` y tests Turbine de *debounce*/alternativas — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T20.11 Añadir tests Compose UI para `SearchOffersScreen` (`P-12`/`P-13`) y carrusel de recomendaciones — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T20.12 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_endpoints.py`
