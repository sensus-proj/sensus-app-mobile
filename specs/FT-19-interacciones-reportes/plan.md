# PLAN — FT-19 Reportes de Moderación en Productos

> Derivado de `spec.md`. Alcance: SE-033 (Pantalla P-22).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Reportes (`POST /api/v1/reports`).** `SubmitContentReportUseCase` valida sesión autenticada, longitud de detalle (`<= 500`), excluye CSAM redirigiendo a canal legal externo (`constitution.md` §8) y delega en `ReportsRepositoryImpl` con manejo de `409 Conflict`.

## 2. UI y navegación

- `ReportContentDialog` (`P-22`) presenta motivos tipificados (`ReportReason`), detalle opcional y confirmación.
- Accesible desde `'Reportar producto'` (`C-27`) en `ProductDetailScreen`.

## 3. DI

| Binding | Implementación |
|---|---|
| `ReportsApiService` | Retrofit `/api/v1/reports` en `NetworkClient` |
| `ReportsRepository` | `@Singleton` en `SocialModule` |
| `SubmitContentReportUseCase` | Inyección en `SocialModule` |

## 4. Gates

- `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes.
- `./gradlew.bat detekt ktlintCheck lint` verdes (0 violaciones).
- `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (cobertura `>= 90%`).
- `python scripts/check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 90` PASS.

## 5. Orden de implementación

1. Modelos de reporte y `SubmitContentReportUseCase` en `domain/`.
2. `ReportsApiService` (`POST /api/v1/reports`) y `ReportsRepositoryImpl` en `data/`.
3. `ReportContentDialog` (`P-22`) conectado en `ProductDetailScreen`.
4. Pruebas unitarias y `MockWebServer` (`201` y `409 Conflict`).

## 6. Prohibiciones

- Prohibido almacenar o procesar reportes de explotación infantil en la app o backend (canalizar por vía legal externa).
- Prohibido permitir reportes en modo invitado.

## Tareas

Ver `tasks.md`.
