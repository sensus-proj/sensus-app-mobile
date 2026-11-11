# PLAN — FT-12 Detalle de Oferta, Filtros Avanzados y Ruteo Externo

> Derivado de `spec.md`. Historias/Alcance: SE-015, SE-016, SE-017, SE-018 (Pantallas P-14, P-17, P-19; Componentes C-21..C-27, C-32).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Detalle de oferta (`GET /api/v1/offers/{id}`).** `GetOfferDetailUseCase` consulta `OffersRepository.getOfferById(id)` con fallback a `OfferDao` en SQLCipher si no hay red.
2. **Filtros avanzados (`P-19` / `C-32`).** `OfferFilterCriteria` modela ordenamiento (`DISTANCE`, `PRICE_ASC`, `DISCOUNT_DESC`, `EXPIRING_SOON`), rango de precios, solo stock disponible y vigencia.
3. **Semáforo de disponibilidad (`C-24`).** Cálculo determinista en dominio: `AVAILABLE` (`stock > 3`), `LAST_UNITS` (`1..3`), `SOLD_OUT` (`0`), `EXPIRED` (`expiresAt <= now`).
4. **Ruteo externo (`SE-018` / `P-17`).** Emisión de `ViewEffect.LaunchExternalMap(lat, lng, label)` que construye URI `geo:lat,lng?q=lat,lng(label)` con `try/catch (ActivityNotFoundException)`.

## 2. UI y navegación

- `ProductDetailScreen` (`P-14`) muestra hero progressive image (`C-21`), bloque de ahorro (`C-22`), semáforo (`C-24`), tarjeta de comercio (`C-25`) y barra inferior fija (`C-27`).
- `OfferFilterBottomSheet` (`P-19` / `C-32`) aplica filtros reactivos sobre `BuyerHomeViewModel`.
- Previews: `DetailAvailable`, `DetailLastUnits`, `DetailSoldOut`, `FilterSheetOpen`.

## 3. DI

| Binding | Implementación |
|---|---|
| `GetOfferDetailUseCase` | Inyección por constructor con `OffersRepository` |
| `FilterOffersUseCase` | Caso de uso puro sin dependencias Android |

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

1. Crear `OfferAvailabilityStatus`, `OfferFilterCriteria`, `GetOfferDetailUseCase` y `FilterOffersUseCase`.
2. Añadir `@GET("api/v1/offers/{id}")` en `OffersApiService` y mapeo en `OffersRepositoryImpl`.
3. Implementar `ProductDetailContract`, `ProductDetailViewModel` y `OfferFilterBottomSheet`.
4. Conectar `ViewEffect.LaunchExternalMap` con manejo seguro de `ActivityNotFoundException`.
5. Agregar tests unitarios y de UI para semáforo de stock, filtros y ruteo.

## 6. Prohibiciones

- Prohibido habilitar el CTA `Apartar con QR` cuando el estado sea `SOLD_OUT` o `EXPIRED`.
- Prohibido lanzar `Intent` implícito sin bloque `try/catch (ActivityNotFoundException)`.

## Tareas

Ver `tasks.md`.
