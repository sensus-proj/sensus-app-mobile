# TASKS — FT-12 Detalle de Oferta, Contador de Vistas, Filtros y Ruteo GPS

> 1 PR = 1 slice. Orden: Model -> Repository -> UseCase -> Data -> ViewModel -> Screen -> DI -> Tests -> Gates.

## Fase A — Dominio

- [ ] T12.1 Crear `OfferDetail` y ampliar `GeoSearchFilter` (abierto ahora, precio máx., descuento mín., orden) en `domain/model/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T12.2 Añadir `getOfferDetail(offerId)` y `registerOfferView(offerId)` a `OffersRepository` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T12.3 Crear `GetOfferDetailUseCase` y `RegisterOfferViewUseCase` con control de unicidad por sesión — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos

- [ ] T12.4 Añadir `@GET("api/v1/offers/{id}")` y `@POST("api/v1/offers/{id}/view")` en `OffersApiService` y DTOs — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T12.5 Implementar carga de detalle con fallback a caché local `OfferDao` y registro no bloqueante de vista en `OffersRepositoryImpl` — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [ ] T12.6 Crear `ProductDetailContract.kt` y `ProductDetailViewModel.kt` (`@HiltViewModel`) — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T12.7 Actualizar `ProductDetailScreen.kt` para consumir `ProductDetailViewModel`, mostrar estado agotado (`P-17`), alerta `stock <= 3`, métodos de pago y horario real — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T12.8 Añadir chips de filtros avanzados (`C-35`: Abierto ahora, descuento, precio, orden) en `BuyerHomeScreen.kt` y `BuyerHomeViewModel.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — Pruebas y Cierre

- [ ] T12.9 Añadir `GetOfferDetailUseCaseTest`, `RegisterOfferViewUseCaseTest` y `MockWebServer` tests de detalle/vistas — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T12.10 Añadir `ProductDetailViewModelTest` (Turbine) y tests Compose UI para `stock <= 3`, `stock == 0` y filtros `C-35` — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T12.11 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y verificar todos los gates — verif: `python scripts/check_endpoints.py`
