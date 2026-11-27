# SPEC — FT-19 Reportes de Moderación en Productos

> Rector: `.specify/memory/constitution.md` §2, §4.5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-033**; Pantalla **P-22**; Componente **C-27**.
> Backend ref: Spec `040-reportes-contenido`.

## 1. Contexto y objetivo

El detalle de producto (`ProductDetailScreen`) requiere un flujo real para reportar ofertas falsas, precios alterados o contenido ofensivo (`P-22`, `C-27`). *(Nota 2026-11-27: el Muro comunitario `FT-18` y las interacciones sobre posts fueron retirados de Comunidad por decisión del equipo).*

**Objetivo:** mantener el flujo transversal de reporte de contenido inapropiado (`POST /api/v1/reports`) en ofertas desde `ProductDetailScreen`.

**Alcance:** caso de uso `SubmitContentReportUseCase`, repositorio `ReportsRepositoryImpl` y diálogo de reporte (`ReportContentDialog`, `P-22`, `C-27`).
**Fuera de alcance:** reporte de explotación infantil en app (prohibido procesar/almacenar localmente según política legal; canalización externa), Muro social retirado.

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-19-01 | Incluir enlace discreto `'Reportar producto'` (`C-27`) en `ProductDetailScreen` abriendo formulario `P-22` con motivos (`SPAM`, `OFFENSIVE`, `FALSE_OFFER`, `PRICE_MISMATCH`, `FRAUD`, `OTHER`) y detalle opcional (`<= 500` caracteres). |
| RF-19-02 | Enviar `POST /api/v1/reports`; manejar `409 Conflict` cuando el usuario ya reportó el mismo elemento, mostrando confirmación `'Reporte enviado'` o el detalle del servidor. |
| RF-19-03 | Bloquear reportes en modo invitado (`GuestPromptDialog`) y rechazar localmente términos de explotación infantil redirigiendo a canal legal externo. |

## 3. Requisitos no funcionales

- **Seguridad:** evitar envíos duplicados mientras la petición está en curso; validación previa en dominio antes de red.
- **Privacidad:** sin logs de cuerpo HTTP ni PII en diagnósticos.

## 4. Arquitectura y contratos internos

```text
domain/
  model/ReportModels.kt          # ContentReportInput, ReportReason, ReportTargetType, ReportPolicy
  repository/ReportsRepository.kt
  usecase/SubmitContentReportUseCase.kt
data/
  remote/api/ReportsApiService.kt # POST /api/v1/reports
  repository/ReportsRepositoryImpl.kt
presentation/buyer/
  ReportContentDialog.kt          # P-22 / C-27
  ProductDetailScreen.kt
```

## 5. Criterios de aceptacion

- **Given** usuario pulsa Reportar producto en `ProductDetailScreen` **When** elige motivo y confirma **Then** envía `POST /api/v1/reports` y muestra confirmación.
- **Given** usuario ya reportó la oferta (`409 Conflict`) **When** envía el reporte **Then** informa que ya fue reportado sin duplicar estado de carga.
- **Given** usuario invitado **When** intenta reportar **Then** aparece `GuestPromptDialog`.

## 6. Plan de pruebas

- **Unitarias:** `FeedInteractionsAndReportUseCasesTest` (bloqueo invitado, longitud máxima, exclusión legal CSAM, motivos válidos).
- **Integración (`MockWebServer`):** `FeedInteractionsAndReportsRepositoryTest` (`201 Created`, `409 Conflict`).

## 7. Riesgos, limites y deuda

- Depende de la spec `040-reportes-contenido` en `sensus-back`.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| En revision | Equipo Sensus | 2026-11-27 | Reportes en `ProductDetailScreen`; Muro comunitario retirado |
