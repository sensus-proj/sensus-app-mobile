# PLAN — FT-20 Búsqueda Semántica, Recomendaciones ML y Telemetría de Interacción

> Derivado de `spec.md`. Historias/Alcance: SE-034, SE-035, SE-037, SE-038 (Pantallas P-12, P-13; Componentes C-05, C-13).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Búsqueda con debounce (`GET /api/v1/ml/search`).** `SearchOffersUseCase` aplica `debounce(350ms)`, cancelación de `Job` previo y guarda las últimas 10 consultas en `SearchHistoryDao` (SQLCipher).
2. **Carrusel Para Ti (`GET /api/v1/ml/recommendations`).** `GetRecommendationsUseCase` obtiene recomendaciones personalizadas con fallback a ofertas populares cercanas para invitados o cold-start.
3. **Telemetría en lotes (`POST /api/v1/ml/interactions`).** `TrackInteractionUseCase` encola eventos (`VIEW`, `CLICK`, `FAVORITE`, `CLAIM`, `SHARE`) sin PII en `TelemetryEventDao` y los despacha en lotes.

## 2. UI y navegación

- `SearchOffersScreen` (`P-12`) con barra activa, chips de búsquedas recientes y tendencias.
- `SearchEmptyState` (`P-13`) con sugerencias cuando no hay coincidencias.
- Carrusel `Para ti` integrado en `BuyerHomeScreen` (`P-07`).
- Previews: `SearchRecent`, `SearchResults`, `SearchEmpty`, `RecommendationsRow`.

## 3. DI

| Binding | Implementación |
|---|---|
| `MlApiService` | Retrofit `/api/v1/ml/*` en `NetworkModule` |
| `SearchHistoryDao` | Provisión en `DatabaseModule` |
| `TelemetryEventDao` | Provisión en `DatabaseModule` |

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

1. Crear `InteractionType`, `TelemetryEvent`, `SearchOffersUseCase`, `GetRecommendationsUseCase` y `TrackInteractionUseCase`.
2. Crear `MlApiService`, `SearchHistoryDao` y `TelemetryEventDao`.
3. Implementar `SearchOffersScreen` (`P-12`/`P-13`) y `SearchViewModel` con `debounce(350ms)`.
4. Integrar carrusel `Para ti` en `BuyerHomeScreen`.
5. Añadir tests de debounce, cancelación y loteo de telemetría.

## 6. Prohibiciones

- Prohibido enviar PII (correo, teléfono, nombre) en payloads de `/api/v1/ml/interactions`.
- Prohibido disparar una llamada HTTP por cada tecla sin debounce.

## Tareas

Ver `tasks.md`.
