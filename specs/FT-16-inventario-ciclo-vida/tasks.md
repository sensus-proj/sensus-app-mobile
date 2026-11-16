# TASKS — FT-16 Inventario del Comerciante, Edición, Ciclo de Vida y Métricas

> 1 PR = 1 slice. Orden: Dominio -> Datos -> ViewModel/UI -> DI -> Tests -> Gates.

## Fase A — Dominio

- [ ] T16.1 Crear `OfferLifecycleStatus` y `MerchantOfferMetrics` en `domain/model/` — verif: `./gradlew.bat compileDebugKotlin` — **Parcial: existe `OfferLifecycleStatus`; falta `MerchantOfferMetrics` (auditoría 2026-11-15).**
- [x] T16.2 Ampliar `OffersRepository` con `getMyOffers`, `updateOffer` y `updateOfferStatus` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T16.3 Crear `GetMerchantInventoryUseCase`, `UpdateOfferUseCase` y `ChangeOfferStatusUseCase` reutilizando validación de precios/stock — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos

- [x] T16.4 Añadir `GET /api/v1/merchants/my-offers`, `PUT /api/v1/offers/{id}` y `PUT /api/v1/offers/{id}/status` en servicios Retrofit y DTOs — verif: `./gradlew.bat compileDebugKotlin`
- [x] T16.5 Implementar métodos en `OffersRepositoryImpl` con actualización sincronizada en `OfferDao` — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [ ] T16.6 Actualizar `MerchantDashboardContract.kt` y `MerchantDashboardViewModel.kt` eliminando `StoreRepository.getSampleStore()` y conectando inventario paginado — verif: `./gradlew.bat testDebugUnitTest` — **Parcial: inventario real con `fetchMerchantOffers`, pero `getSampleStore()` sigue como respaldo del nombre del comercio (auditoría 2026-11-15).**
- [ ] T16.7 Añadir pestañas de historial (`Activas`, `Pausadas`, `Finalizadas`), contador de vistas (`SE-045`) y acciones rápidas en `MerchantDashboardScreen.kt` — verif: `./gradlew.bat compileDebugKotlin` — **Parcial: pestañas `Activas` y `Pausadas`; faltan `Finalizadas` y el contador de vistas (auditoría 2026-11-15).**
- [x] T16.8 Crear `MerchantEditOfferContract.kt`, `MerchantEditOfferViewModel.kt` y `MerchantEditOfferScreen.kt` (`P-31`) para editar y republicar ofertas — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — Pruebas y Cierre

- [ ] T16.9 Añadir pruebas unitarias de casos de uso y tests `MockWebServer` para `my-offers`, `PUT /offers/{id}` y `/status` — verif: `./gradlew.bat testDebugUnitTest` — **Parcial: `RepublishOfferUseCaseTest`, `RepublishRepositoryTest` y `MerchantViewModelsTest`; sin `MockWebServer` para `my-offers` y `/status` (auditoría 2026-11-15).**
- [ ] T16.10 Añadir pruebas Turbine en ViewModels y Compose UI para edición, pausado y republicación — verif: `./gradlew.bat compileDebugAndroidTestSources` — **Parcial: `MerchantEditOfferViewModelTest`, `RepublishViewModelTest` y `RepublishOfferButtonTest`; falta Compose UI de edición y pausado (auditoría 2026-11-15).**
- [ ] T16.11 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y verificar todos los gates — verif: `python scripts/check_endpoints.py` — **Parcial: endpoints documentados y `check_endpoints.py` PASS (auditoría 2026-11-15).**

## Slice autorizada REPUBLISH — 2026-11-14

- [x] T16.12 Integrar POST /api/v1/offers/{id}/republish: entrada tipada, fechas futuras y stock positivo, solo EXPIRED/ARCHIVED, HTTP 200 y cache server-wins, Hilt, boton explicito, pruebas dominio/repositorio/VM/UI. Gradle y gates coordinados por main sin paralelismo. Sin cambios de conexion, backend, gates, memoria ni docs globales.

Evidencia T16.12: implementación y pruebas fuente preparadas en UTF-8; diff --check sin errores. Sin ejecución Gradle por coordinación main; cobertura y E2E no acreditados. Integración main: registrar endpoint/rate y bitácora global autorizada en su slice, ejecutar gates completos y pruebas instrumentadas. Python del PATH apunta a alias WindowsApps no ejecutable en esta sesión.

Validación integración principal T16.12: contrato y Allow POST oficial verificados, 257 tests globales sin fallos, Kover 82.82%, Debug e instrumentadas compilan, lint/detekt/ktlint y cinco gates Python PASS. Tests Compose añadidos y compilados; ejecución en dispositivo/E2E pendiente. Sin cierre de la feature completa.
