# TASKS — FT-19 Reportes de Moderación en Productos

> 1 PR = 1 slice. Orden: Dominio -> Datos -> UI -> Pruebas -> Gates.

## Fase A — Dominio

- [x] T19.1 Crear `ReportModels.kt` y `ReportsRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T19.2 Crear `SubmitContentReportUseCase` con exclusión legal CSAM y bloqueo invitado — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos

- [x] T19.3 Crear `ReportsApiService` (`POST /api/v1/reports`) y `ReportsRepositoryImpl` (manejo `409`) — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [x] T19.4 Crear `ReportContentDialog.kt` (`P-22`) y añadir acción 'Reportar producto' (`C-27`) en `ProductDetailScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [x] T19.5 Registrar `ReportsApiService`, `ReportsRepository` y `SubmitContentReportUseCase` en `SocialModule.kt` / `NetworkClient.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T19.6 Añadir pruebas unitarias y `MockWebServer` (`201`, `409` reporte duplicado, CSAM) — verif: `./gradlew.bat testDebugUnitTest`
- [x] T19.7 Retirar el módulo/pestaña de Muro social (`FT-18`) manteniendo intacto el flujo de reportes en ofertas y actualizar specs/docs — verif: `py scripts/check_endpoints.py`
