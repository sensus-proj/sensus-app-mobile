# TASKS — FT-19 Interacciones del Feed y Reportes

> 1 PR = 1 slice. Orden: Dominio -> Datos -> UI -> Pruebas -> Gates.

## Fase A — Dominio

- [x] T19.1 Crear `FeedComment`, `ReportReason`, `ReportTargetType` y `ReportsRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T19.2 Ampliar `FeedRepository` con `toggleLike`, `getComments`, `addComment` y `deletePost` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T19.3 Crear `TogglePostLikeUseCase`, `GetFeedCommentsUseCase`, `AddFeedCommentUseCase`, `DeleteFeedPostUseCase` y `SubmitContentReportUseCase` — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos

- [x] T19.4 Añadir endpoints de like, comentarios y borrado en `FeedApiService` y crear `ReportsApiService` (`POST /api/v1/reports`) — verif: `./gradlew.bat compileDebugKotlin`
- [x] T19.5 Implementar métodos en `FeedRepositoryImpl` y `ReportsRepositoryImpl` (manejo `409`) — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [x] T19.6 Ampliar `CommunityViewModel` y `ProductDetailViewModel` con `ViewState`/`ViewIntent`/`ViewEffect` y rollback optimista — verif: `./gradlew.bat testDebugUnitTest`
- [x] T19.7 Crear `PostCommentsSheet.kt` y menú de borrado propio/reporte/like/comentarios en `CommunityScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T19.8 Crear `ReportContentDialog.kt` (`P-22`) y añadir acción 'Reportar producto' (`C-27`) en `ProductDetailScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [x] T19.9 Registrar `ReportsApiService`, `ReportsRepository` y nuevos casos de uso en `CommunityModule.kt` / `NetworkClient.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T19.10 Añadir pruebas unitarias y `MockWebServer` (`409` reporte duplicado, `ProfanityFilter`, rollback like, CSAM) — verif: `./gradlew.bat testDebugUnitTest`
- [x] T19.11 Añadir tests Compose UI para `ReportContentDialog` (`P-22`), acciones de feed y `PostCommentsSheet` — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [x] T19.12 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `specs/README.md` y validar gates locales — verif: `py scripts/check_endpoints.py`
