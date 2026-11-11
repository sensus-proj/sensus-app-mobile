# PLAN — FT-11 Exploración de Ofertas Cercanas, Mapa OSM y Paginación Cursor

> Derivado de `spec.md`. Historias/Alcance: SE-010, SE-011, SE-012, SE-013, SE-014 (Pantallas P-07..P-11, Componentes C-01..C-20).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Contrato HTTP (`GET /api/v1/offers/nearby`).** `OffersApiService` recibe `lat`, `lng`, `radiusKm`, `category`, `cursor` y `limit` (default 20, máx. 100) y retorna `CursorPageDto<OfferDto>`.
2. **Caché cifrada offline-first.** `OfferDao` en SQLCipher almacena la página vigente con `cachedAtMillis` (TTL 5 min online, lectura stale offline con banner `C-06`).
3. **Política de radio por rol (`FT-07`).** `GetNearbyOffersUseCase` aplica tope duro de `2.0 km` para invitados y permite `1..15 km` para compradores autenticados.
4. **Vistas Lista (`P-07`/`P-08`) y Mapa OSM (`P-09`/`P-10`).** `BuyerHomeViewModel` gestiona el toggle `ViewMode` (`LIST` / `MAP`), clusters por categoría (`C-15`) y mini-card emergente (`C-16`).
5. **Estado vacío (`P-11`).** Cuando `offers.isEmpty()`, renderiza `EmptyRadiusCard` (`C-19`) con CTA `Ampliar radio de búsqueda`.

## 2. UI y navegación

- `BuyerHomeScreen` (`presentation/buyer/home/`) integra `TopAppBarLocation` (`C-01`), `SearchTriggerBar` (`C-03`), `CategoryChipsRow` (`C-04`), `PromoHeroBanner` (`C-05`), `OfferGridCard` (`C-07`) y `FloatingMapToggle` (`C-08`).
- `MapDiscoveryView` renderiza marcadores OSM (`C-15`), `MapClusterPreviewCard` (`C-16`) y `RadiusSliderSheet` (`C-18`).
- Previews: `ListLoaded`, `ListShimmer`, `MapPins`, `EmptyRadius`, `OfflineStale`.

## 3. DI

| Binding | Implementación |
|---|---|
| `OffersApiService` | Retrofit `/api/v1/offers/*` en `NetworkModule` |
| `OffersRepository` | `OffersRepositoryImpl` en `RepositoryModule` |
| `GetNearbyOffersUseCase` | Inyección por constructor con `OffersRepository` + `GuestRadiusPolicy` |

## 4. Gates

- `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes.
- `./gradlew.bat detekt ktlintCheck lint` verdes (0 violaciones).
- `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (cobertura `>= 80%`).
- `python scripts/check_sdd.py` PASS.
- `python scripts/check_layers.py` PASS.
- `python scripts/check_endpoints.py` PASS.
- `python scripts/check_secrets.py` PASS.
- `python scripts/check_coverage.py --min 80` PASS.

## 5. Orden de implementación

1. Definir `CursorPage<Offer>`, `NearbyOffersQuery` y `OffersRepository` en `domain/`.
2. Implementar `GetNearbyOffersUseCase` con validación de coordenadas y tope de radio invitado.
3. Crear `OffersApiService` y `CursorPageDto<OfferDto>` + actualizar `OfferDao` en SQLCipher.
4. Implementar `OffersRepositoryImpl` con estrategia offline-first y tests `MockWebServer`.
5. Actualizar `BuyerHomeContract`, `BuyerHomeViewModel` y `BuyerHomeScreen` (`P-07..P-11`).
6. Añadir pruebas unitarias (JUnit5/Turbine) y Compose UI.

## 6. Prohibiciones

- Prohibido devolver colecciones sin paginación o con `limit > 100`.
- Prohibido permitir `radiusKm > 2.0` en sesión de invitado.
- Prohibido usar Google Maps SDK propietario; usar exclusivamente OSM (`osmdroid`).

## Tareas

Ver `tasks.md`.
