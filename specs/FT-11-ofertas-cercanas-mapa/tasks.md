# TASKS — FT-11 Ofertas Cercanas, Mapa Interactivo y Selector de Radio Real

> 1 PR = 1 slice. Orden: Model -> Repository -> UseCase -> Data/API -> ViewModel -> Screen -> DI -> Tests -> Gates.

## Fase A — Dominio

- [ ] T11.1 Crear modelo `PagedOffers` y `GeoSearchFilter` en `domain/model/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.2 Ampliar interfaz `OffersRepository` con `fetchNearbyOffers(lat, lng, radiusMeters, page, category)` retornando `Result<PagedOffers>` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.3 Crear `GetNearbyOffersUseCase` validando coordenadas, paginación (20/100) y radio estricto de 2000 m para invitados — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos y Offline-First

- [ ] T11.4 Crear `OfferDtos.kt` (`@Serializable`, `@SerialName`) y `OffersApiService` (`GET /api/v1/offers/nearby`) — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.5 Ampliar `OfferMapper.kt` para mapear `OfferItemDto <-> ProductOffer <-> OfferEntity` — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T11.6 Actualizar `OfferDao.java` y `OfferEntity.java` si faltan campos de estado abierto/verificado/paginación — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.7 Conectar `OffersRepositoryImpl` a `OffersApiService` + `OfferDao` (*server-wins*) y eliminar toda referencia a `MockDataProvider.sampleOffers` — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación y UI

- [ ] T11.8 Actualizar `BuyerMapViewModel`, `BuyerMapContract`, `BuyerHomeViewModel` y `BuyerHomeContract` para paginación, caché y ubicación real — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T11.9 Actualizar `RadiusSelectorModal.kt` con selector segmentado Metros (100–500 m) / Km (1–5 km) y conteo de comercios — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.10 Implementar clustering de marcadores en `OsmMapUtils.kt` y hoja `MerchantPreviewBottomSheet` (`P-09`) en `BuyerMapScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.11 Implementar `LocationPickerBottomSheet` (`P-10`) para cambio manual de ubicación cuando GPS esté desactivado — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.12 Implementar paginación infinita y tarjetas completas (`Verificado`, `Abierto/Cerrado ahora`, descuento) en `BuyerHomeScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [ ] T11.13 Proveer `OffersApiService` y dependencias actualizadas en `AppModule.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.14 Añadir `GetNearbyOffersUseCaseTest`, tests `MockWebServer` + `OfferDao` y tests Turbine para ViewModels de mapa/inicio — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T11.15 Añadir tests Compose UI para selector de radio m/km, bloqueo de invitado y vista previa `P-09` — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T11.16 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_endpoints.py`
