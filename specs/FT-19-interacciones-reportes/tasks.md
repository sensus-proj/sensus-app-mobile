# TASKS — FT-19 Interacciones del Feed y Reportes

> 1 PR = 1 slice. Orden: Dominio -> Datos -> UI -> Pruebas -> Gates.

## Fase A — Dominio

- [ ] T19.1 Crear `FeedComment`, `ReportReason`, `ReportTargetType` y `ReportsRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T19.2 Ampliar `FeedRepository` con `toggleLike`, `getComments`, `addComment` y `deletePost` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T19.3 Crear `TogglePostLikeUseCase`, `GetPostCommentsUseCase`, `AddCommentUseCase`, `DeletePostUseCase` y `SubmitReportUseCase` — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos

- [ ] T19.4 Añadir endpoints de like, comentarios y borrado en `FeedApiService` y crear `ReportsApiService` (`POST /api/v1/reports`) — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T19.5 Implementar métodos en `FeedRepositoryImpl` (rollback de like en error) y `ReportsRepositoryImpl` (manejo `409`) — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [ ] T19.6 Ampliar `CommunityViewModel` y crear `ReportViewModel` (o integrarlo en detalle/comunidad) con `ViewState`/`ViewIntent`/`ViewEffect` — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T19.7 Crear `PostCommentsSheet.kt` y menú de borrado propio/reporte en `CommunityScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T19.8 Crear `ReportContentDialog.kt` (`P-22`) y añadir enlace `'Reportar producto'` (`C-27`) en `ProductDetailScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [ ] T19.9 Registrar `ReportsApiService`, `ReportsRepository` y nuevos casos de uso en `AppModule.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T19.10 Añadir pruebas unitarias y `MockWebServer` (`409` reporte duplicado, `422` moderación, `403` borrado ajeno, rollback like) — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T19.11 Añadir tests Compose UI para `ReportContentDialog` (`P-22`) y `PostCommentsSheet` — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T19.12 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_endpoints.py`
