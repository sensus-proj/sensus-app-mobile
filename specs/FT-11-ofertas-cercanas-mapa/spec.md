# SPEC — FT-11 Ofertas Cercanas, Mapa Interactivo y Selector de Radio Real

> Rector: `.specify/memory/constitution.md` §2, §3, §5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-010, SE-011, SE-012, SE-013, SE-014**; Pantallas **P-07, P-08, P-09, P-10, P-11**; Componentes **C-03, C-07..C-15**.
> Backend ref: Spec `007-ofertas-cercanas`.

## 1. Contexto y objetivo

Actualmente `BuyerMapScreen` y `BuyerHomeScreen` consumen ofertas locales generadas por `MockDataProvider` a través de `OffersRepositoryImpl`, calculando distancias en la UI sin consultar al servidor ni paginar resultados. Además, el selector de radio (`RadiusSelectorModal`) solo ofrece pasos de km sin el modo fino en metros (100–500 m) que exige `Sensus_Movil_Componentes_y_CRUD.xlsx` (C-08).

**Objetivo:** conectar el mapa interactivo, la vista previa de comercio, el listado paginado de ofertas y el selector de radio (100–500 m / 1–5 km; fijo 2 km en invitado) al endpoint `GET /api/v1/offers/nearby`, implementando estrategia offline-first en Room + SQLCipher (*server-wins*) y retirando `MockDataProvider.sampleOffers` de producción.

**Alcance:** `OffersApiService`, DTOs de oferta cercana y paginación, `GetNearbyOffersUseCase`, actualización de `OffersRepositoryImpl`, clustering de pines en mapa OSMDroid, hoja inferior de vista previa (`P-09`), selector de ubicación (`P-10`) y selector de radio dual m/km (`P-11`), sincronización con `OfferDao`.
**Fuera de alcance:** detalle completo y ruteo (FT-12), apartado QR (FT-13), búsqueda ML (FT-20).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-11-01 | `GET /api/v1/offers/nearby` recibe `lat`, `lng`, `radiusMeters`, `page`, `pageSize` (20 por defecto, máx. 100) y opcionales `category`, `sort`. |
| RF-11-02 | En sesión autenticada, `RadiusSelectorModal` soporta modo Metros (`100, 200, 300, 400, 500 m`) y Kilómetros (`1, 2, 3, 4, 5 km`), persistiendo siempre en metros en perfil/preferencias (`radio_preferido_m`). En modo invitado fija `radiusMeters = 2000` bloqueado. |
| RF-11-03 | `BuyerMapScreen` (`P-07`) carga OSMDroid centrado en la ubicación del usuario (marcador gatito), dibuja el radio en metros y muestra pines diferenciados para comercios abiertos (morado) y cerrados; agrupa pines cercanos (*clustering*) cuando hay alta densidad. |
| RF-11-04 | Al tocar un pin en el mapa se abre la hoja inferior de vista previa (`P-09`, `C-13`) con nombre del comercio, estado calculado (`Abierto ahora` / `Cerrado ahora`), distancia y sus 2 ofertas principales. |
| RF-11-05 | `BuyerHomeScreen` (`P-08`) muestra tarjetas paginadas (20 por página) con foto (`Coil`), descuento, precio original tachado, precio oferta, ahorro, insignia `Verificado`, estado del comercio y distancia en `m`/`km`. |
| RF-11-06 | `OffersRepositoryImpl` aplica *server-wins*: al recibir respuesta exitosa reemplaza el caché en `OfferDao` (Room + SQLCipher); sin conexión sirve el caché local y notifica estado offline. |
| RF-11-07 | Eliminar `MockDataProvider.sampleOffers` y cualquier semilla mock de `OffersRepositoryImpl` en `app/src/main`. |

## 3. Requisitos no funcionales

- **Rendimiento:** tiempo de renderizado del listado y pines `< 300 ms` tras respuesta de red; carga incremental al llegar al final del scroll sin bloquear el hilo principal.
- **Offline-First:** persistencia cifrada en `OfferDao` con SQLCipher AES-256; degradación transparente sin crashes cuando no hay internet.
- **Privacidad:** no enviar coordenadas precisas fuera del query geoespacial ni exponer ubicación exacta de otros usuarios.
- **Design System:** pines y halos en `SensusPurple`, etiquetas de descuento en `SensusDiscountTag`, cero tonos amarillos ni emojis.

## 4. Arquitectura y contratos internos

```text
domain/
  model/StoreModels.kt          # ProductOffer, PagedOffers, GeoSearchFilter
  repository/OffersRepository.kt # suspend fun fetchNearbyOffers(lat, lng, radiusMeters, page, category): Result<PagedOffers>
  usecase/GetNearbyOffersUseCase.kt
data/
  remote/api/OffersApiService.kt # @GET("api/v1/offers/nearby")
  remote/dto/OfferDtos.kt        # NearbyOffersResponseDto, OfferItemDto
  mapper/OfferMapper.kt          # DTO -> Domain <-> OfferEntity
  repository/OffersRepositoryImpl.kt
presentation/buyer/
  BuyerMapScreen.kt, BuyerMapViewModel.kt, BuyerMapContract.kt
  BuyerHomeScreen.kt, BuyerHomeViewModel.kt, BuyerHomeContract.kt
  MerchantPreviewSheet.kt        # Vista previa al pulsar pin (P-09)
  LocationPickerSheet.kt         # Selector manual de ubicación (P-10)
presentation/common/
  RadiusSelectorModal.kt         # Dual Metros (100-500m) / Km (1-5km)
```

## 5. Criterios de aceptacion

- **Given** usuario autenticado en mapa con radio 3 km **When** abre la app o mueve el slider **Then** se consulta `GET /api/v1/offers/nearby` con `radiusMeters = 3000` y se actualizan pines y lista.
- **Given** modo invitado **When** abre `RadiusSelectorModal` **Then** el radio está bloqueado en `2000 m` y el caso de uso rechaza/normaliza cualquier radio distinto.
- **Given** un usuario toca un pin de comercio en el mapa **When** pulsa el marcador **Then** se despliega la hoja `P-09` con datos del comercio y sus 2 ofertas principales.
- **Given** dispositivo sin red con ofertas previas cacheadas **When** abre inicio **Then** muestra ofertas de `OfferDao` sin usar `MockDataProvider`.

## 6. Plan de pruebas

- **Unitarias:** `GetNearbyOffersUseCaseTest` (normalización 2000 m en invitado, rango 100–5000 m en autenticado, ordenamiento por distancia).
- **ViewModel:** `BuyerMapViewModelTest` y `BuyerHomeViewModelTest` con Turbine verificando estados de carga, paginación y selección de pin.
- **Integración (`MockWebServer` + Room SQLCipher):** parseo de `GET /api/v1/offers/nearby`, persistencia en `OfferDao` y estrategia *server-wins*.
- **UI (Compose Test Rule):** interacción de slider m/km, bloqueo en invitado, tarjetas de oferta y apertura de hoja `P-09`.

## 7. Riesgos, limites y deuda

- Desvío documentado del Drive: el mapa usa **OSMDroid** en lugar de Google Maps SDK para evitar costos y facturación de API Key en capa gratuita (decisión D-1).
- El retiro completo de `MockDataProvider.kt` ocurrirá cuando `StoreRepositoryImpl` también migre en `FT-18`/`FT-21`/`FT-24`.
- Requiere Spec `007-ofertas-cercanas` desplegada en AWS para pruebas contra entorno real; entretanto opera con `MockWebServer` en CI y pruebas.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
