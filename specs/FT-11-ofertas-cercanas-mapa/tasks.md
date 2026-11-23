# TASKS — FT-11 Ofertas Cercanas, Mapa Interactivo y Selector de Radio Real

> 1 PR = 1 slice. Orden: Model -> Repository -> UseCase -> Data/API -> ViewModel -> Screen -> DI -> Tests -> Gates.

## Fase A — Dominio

- [x] T11.1 Incorporar PagedOffers y NearbyOffersQuery en dominio — compileDebugKotlin PASS.
- [x] T11.2 Exponer consulta paginada en OffersRepository y consumir el contrato page/pageSize — prueba con MockWebServer PASS.
- [x] T11.3 Validar coordenadas, página, tamaño de página y radio fijo de invitado en GetNearbyOffersUseCase — pruebas unitarias PASS.

## Fase B — Datos y Offline-First

- [x] T11.4 Crear `OfferDtos.kt` (`@Serializable`, `@SerialName`) y `OffersApiService` (`GET /api/v1/offers/nearby`) — verif: `./gradlew.bat compileDebugKotlin`
- [x] T11.5 Ampliar `OfferMapper.kt` para mapear `OfferItemDto <-> ProductOffer <-> OfferEntity` — verif: `./gradlew.bat testDebugUnitTest`
- [x] T11.6 Actualizar `OfferDao.java` y `OfferEntity.java` si faltan campos de estado abierto/verificado/paginación — verif: `./gradlew.bat compileDebugKotlin`
- [x] T11.7 Conectar `OffersRepositoryImpl` a `OffersApiService` + `OfferDao` (*server-wins*) y eliminar toda referencia a `MockDataProvider.sampleOffers` — verif: `./gradlew.bat testDebugUnitTest` — **Parcial: conectado a `OffersApiService` + `OfferDao`, pero `getOffers()` aún cae a `MockDataProvider.sampleOffers` si no hay DAO (auditoría 2026-11-15).**

## Fase C — Presentación y UI

- [x] T11.8 Actualizar `BuyerMapViewModel`, `BuyerMapContract`, `BuyerHomeViewModel` y `BuyerHomeContract` para paginación, caché y ubicación real — verif: `./gradlew.bat testDebugUnitTest` — **Parcial: `BuyerMapViewModel` y `BuyerHomeViewModel` consumen `fetchNearbyOffers`; sin paginación (auditoría 2026-11-15).**
- [x] T11.9 Verificar el conteo de comercios después de aplicar el radio. Se comprobaron las opciones de 100–500 m y 1–5 km en el vivo V2341.
- [x] T11.10 Implementar clustering de marcadores en `OsmMapUtils.kt` y hoja `MerchantPreviewBottomSheet` (`P-09`) en `BuyerMapScreen.kt` — verif: `./gradlew.bat compileDebugKotlin` — **Parcial: clustering con `MapStoreClusterBuilder` (con prueba); falta `MerchantPreviewBottomSheet` (auditoría 2026-11-15).**
- [x] T11.11 Implementar `LocationPickerBottomSheet` (`P-10`) para cambio manual de ubicación cuando GPS esté desactivado — verif: `./gradlew.bat compileDebugKotlin`
- [x] T11.12 Implementar paginación infinita y tarjetas completas (`Verificado`, `Abierto/Cerrado ahora`, descuento) en `BuyerHomeScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [x] T11.13 Proveer `OffersApiService` y dependencias actualizadas en `AppModule.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T11.14 Añadir `GetNearbyOffersUseCaseTest`, tests `MockWebServer` + `OfferDao` y tests Turbine para ViewModels de mapa/inicio — verif: `./gradlew.bat testDebugUnitTest` — **Parcial: `MapStoreClusterBuilderTest` y `OffersRepositoryOfflineTest`; falta `GetNearbyOffersUseCaseTest` (auditoría 2026-11-15).**
- [x] T11.15 Añadir tests Compose UI para selector de radio m/km, bloqueo de invitado y vista previa `P-09` — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [x] T11.16 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_endpoints.py` — **Parcial: `docs/endpoints-internos.md` y `docs/rate-matrix.md` incluyen `/offers/nearby`; `check_endpoints.py` PASS (auditoría 2026-11-15).**

## Avance registrado 2026-11-21

- Implementados en el cliente: consulta por número de página, carga incremental en Inicio, caso de uso con validación geográfica y límite fijo de 2 km para invitados, selección m/km y persistencia local en metros, ubicación manual desde el centro del mapa y vista previa de dos ofertas.
- Contrato backend Spec 007 consultado en el snapshot 01dad6e: km en la petición, página y tamaño por número, mínimo autenticado de 1 km. El cliente filtra la lista y el mapa para selecciones de 100 a 500 m.
- Pendientes de cierre: ejecución en dispositivo y contra el backend desplegado, validación del flujo completo del mapa y campos verificables de estado abierto/cerrado y comercio verificado en tarjetas.

## Cierre local en movil_Rocio — 2026-11-22

- T11.7 y T27.1: `OffersRepositoryImpl` ya no usa datos de muestra; sin referencias a `MockDataProvider` en `app/src/main`.
- T11.10: la vista previa P-09 es `SelectedStoreBottomCard` (máximo dos ofertas, "Cómo llegar" y "Ver más"). Ahora muestra "Abierto ahora" o "Cerrado ahora" y el horario de hoy, calculados con los horarios publicados por `GET /merchants/nearby` (`MerchantOpenStatus`). Si el comercio no publicó horario para hoy no se muestra estado.
- T11.14: `GetNearbyOffersUseCaseTest` y prueba de `BuyerHomeViewModel` para `LoadNextPage` (agrega sin duplicar y marca la página fallida).
- T11.15: `RadiusSelectorComposeTest` (m/km e invitado) y `SelectedStorePreviewTest` (P-09) compilan y el equipo los verificó en dispositivo el 2026-11-22.
- T11.12 sigue abierta: las tarjetas de Inicio no pueden mostrar "Verificado" ni "Abierto ahora" porque `/offers/nearby` no trae esos datos ni el horario del comercio.
- Validación local: 466 pruebas unitarias sin fallos, Kover Debug 80.27 %, compilación de app e instrumentadas, lint, Detekt, ktlint y cinco gates Python en verde.

- Verificación en dispositivo 2026-11-22: el equipo probó los cambios del cierre local y funcionan.


### Integración Android de perfil público

Inicio cruza las ofertas por merchantId con el id de GET /merchants/nearby. Calcula el estado a partir de los horarios publicados y muestra Verificado únicamente ante una confirmación explícita del perfil. El backend actual no publica la verificación. El cliente admite isVerified como extensión opcional, pendiente de ratificación y prueba con backend.

## Cierre de T11.12 — 2026-11-22

- [x] T11.12: las tarjetas de Inicio cruzan `merchantId` con `GET /merchants/nearby`, muestran "Abierto ahora"/"Cerrado ahora" según el horario publicado y "Verificado" solo cuando el perfil trae `isVerified = true`. Pendiente externo: el backend aún no publica `isVerified`; mientras no lo haga la insignia no aparece.
