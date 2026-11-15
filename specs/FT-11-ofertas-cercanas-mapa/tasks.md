# TASKS — FT-11 Ofertas Cercanas, Mapa Interactivo y Selector de Radio Real

> 1 PR = 1 slice. Orden: Model -> Repository -> UseCase -> Data/API -> ViewModel -> Screen -> DI -> Tests -> Gates.

## Fase A — Dominio

- [ ] T11.1 Crear modelo `PagedOffers` y `GeoSearchFilter` en `domain/model/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.2 Ampliar interfaz `OffersRepository` con `fetchNearbyOffers(lat, lng, radiusMeters, page, category)` retornando `Result<PagedOffers>` — verif: `./gradlew.bat compileDebugKotlin` — **Parcial: `fetchNearbyOffers(lat, lng, radiusKm, category)` existe, pero devuelve `List` sin paginación (auditoría 2026-11-15).**
- [ ] T11.3 Crear `GetNearbyOffersUseCase` validando coordenadas, paginación (20/100) y radio estricto de 2000 m para invitados — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos y Offline-First

- [x] T11.4 Crear `OfferDtos.kt` (`@Serializable`, `@SerialName`) y `OffersApiService` (`GET /api/v1/offers/nearby`) — verif: `./gradlew.bat compileDebugKotlin`
- [x] T11.5 Ampliar `OfferMapper.kt` para mapear `OfferItemDto <-> ProductOffer <-> OfferEntity` — verif: `./gradlew.bat testDebugUnitTest`
- [x] T11.6 Actualizar `OfferDao.java` y `OfferEntity.java` si faltan campos de estado abierto/verificado/paginación — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.7 Conectar `OffersRepositoryImpl` a `OffersApiService` + `OfferDao` (*server-wins*) y eliminar toda referencia a `MockDataProvider.sampleOffers` — verif: `./gradlew.bat testDebugUnitTest` — **Parcial: conectado a `OffersApiService` + `OfferDao`, pero `getOffers()` aún cae a `MockDataProvider.sampleOffers` si no hay DAO (auditoría 2026-11-15).**

## Fase C — Presentación y UI

- [ ] T11.8 Actualizar `BuyerMapViewModel`, `BuyerMapContract`, `BuyerHomeViewModel` y `BuyerHomeContract` para paginación, caché y ubicación real — verif: `./gradlew.bat testDebugUnitTest` — **Parcial: `BuyerMapViewModel` y `BuyerHomeViewModel` consumen `fetchNearbyOffers`; sin paginación (auditoría 2026-11-15).**
- [ ] T11.9 Actualizar `RadiusSelectorModal.kt` con selector segmentado Metros (100–500 m) / Km (1–5 km) y conteo de comercios — verif: `./gradlew.bat compileDebugKotlin` — **Parcial: `RadiusSelectorBottomSheet` ofrece 1–5 km; falta el segmento en metros (100–500 m) (auditoría 2026-11-15).**
- [ ] T11.10 Implementar clustering de marcadores en `OsmMapUtils.kt` y hoja `MerchantPreviewBottomSheet` (`P-09`) en `BuyerMapScreen.kt` — verif: `./gradlew.bat compileDebugKotlin` — **Parcial: clustering con `MapStoreClusterBuilder` (con prueba); falta `MerchantPreviewBottomSheet` (auditoría 2026-11-15).**
- [ ] T11.11 Implementar `LocationPickerBottomSheet` (`P-10`) para cambio manual de ubicación cuando GPS esté desactivado — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.12 Implementar paginación infinita y tarjetas completas (`Verificado`, `Abierto/Cerrado ahora`, descuento) en `BuyerHomeScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [x] T11.13 Proveer `OffersApiService` y dependencias actualizadas en `AppModule.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T11.14 Añadir `GetNearbyOffersUseCaseTest`, tests `MockWebServer` + `OfferDao` y tests Turbine para ViewModels de mapa/inicio — verif: `./gradlew.bat testDebugUnitTest` — **Parcial: `MapStoreClusterBuilderTest` y `OffersRepositoryOfflineTest`; falta `GetNearbyOffersUseCaseTest` (auditoría 2026-11-15).**
- [ ] T11.15 Añadir tests Compose UI para selector de radio m/km, bloqueo de invitado y vista previa `P-09` — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T11.16 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_endpoints.py` — **Parcial: `docs/endpoints-internos.md` y `docs/rate-matrix.md` incluyen `/offers/nearby`; `check_endpoints.py` PASS (auditoría 2026-11-15).**
