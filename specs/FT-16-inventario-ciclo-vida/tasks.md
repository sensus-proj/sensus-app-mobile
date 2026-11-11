# TASKS — FT-16 Inventario del Comerciante, Edición, Ciclo de Vida y Métricas

> 1 PR = 1 slice. Orden: Dominio -> Datos -> ViewModel/UI -> DI -> Tests -> Gates.

## Fase A — Dominio

- [ ] T16.1 Crear `OfferLifecycleStatus` y `MerchantOfferMetrics` en `domain/model/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T16.2 Ampliar `OffersRepository` con `getMyOffers`, `updateOffer` y `updateOfferStatus` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T16.3 Crear `GetMerchantInventoryUseCase`, `UpdateOfferUseCase` y `ChangeOfferStatusUseCase` reutilizando validación de precios/stock — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos

- [ ] T16.4 Añadir `GET /api/v1/merchants/my-offers`, `PUT /api/v1/offers/{id}` y `PUT /api/v1/offers/{id}/status` en servicios Retrofit y DTOs — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T16.5 Implementar métodos en `OffersRepositoryImpl` con actualización sincronizada en `OfferDao` — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [ ] T16.6 Actualizar `MerchantDashboardContract.kt` y `MerchantDashboardViewModel.kt` eliminando `StoreRepository.getSampleStore()` y conectando inventario paginado — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T16.7 Añadir pestañas de historial (`Activas`, `Pausadas`, `Finalizadas`), contador de vistas (`SE-045`) y acciones rápidas en `MerchantDashboardScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T16.8 Crear `MerchantEditOfferContract.kt`, `MerchantEditOfferViewModel.kt` y `MerchantEditOfferScreen.kt` (`P-31`) para editar y republicar ofertas — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — Pruebas y Cierre

- [ ] T16.9 Añadir pruebas unitarias de casos de uso y tests `MockWebServer` para `my-offers`, `PUT /offers/{id}` y `/status` — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T16.10 Añadir pruebas Turbine en ViewModels y Compose UI para edición, pausado y republicación — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T16.11 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y verificar todos los gates — verif: `python scripts/check_endpoints.py`
