# PLAN — FT-20 Búsqueda Semántica, Recomendaciones ML y Telemetría de Interacción

> Derivado de `spec.md`. Historias/Alcance: SE-034, SE-035, SE-037, SE-038 (Pantallas P-12, P-13; Componentes C-05, C-13).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Búsqueda con debounce (`GET /api/v1/offers/search`).** `SearchOffersUseCase` aplica `debounce(350ms)`, cancelación de `Job` previo y guarda las últimas 10 consultas en `SearchHistoryDao` (SQLCipher).
2. **Retirado (2026-11-20):** el carrusel Para Ti y la telemetría en lotes salen de FT-20. Las recomendaciones son el feed de promociones de FT-30 y las interacciones las registra el backend (spec 055), sin envío desde el cliente.

## 2. UI y navegación

- `SearchOffersScreen` (`P-12`) con barra activa, chips de búsquedas recientes y tendencias.
- `SearchEmptyState` (`P-13`) con sugerencias cuando no hay coincidencias.
- Previews: `SearchRecent`, `SearchResults`, `SearchEmpty`, `RecommendationsRow`.

## 3. DI

| Binding | Implementación |
|---|---|
| `MlApiService` | Retrofit `/api/v1/ml/*` en `NetworkModule` |
| `SearchHistoryDao` | Provisión en `DatabaseModule` |

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

1. Crear `SearchOffersUseCase` y `GetOfferAlternativesUseCase`.
2. Crear `MlApiService` y `SearchHistoryDao`.
3. Implementar `SearchOffersScreen` (`P-12`/`P-13`) y `SearchViewModel` con `debounce(350ms)`.
4. Añadir tests de debounce, cancelación y alternativas.

## 6. Prohibiciones

- Prohibido enviar telemetría desde el cliente: las interacciones las registra el backend (spec 055).
- Prohibido disparar una llamada HTTP por cada tecla sin debounce.

## Tareas

Ver `tasks.md`.
