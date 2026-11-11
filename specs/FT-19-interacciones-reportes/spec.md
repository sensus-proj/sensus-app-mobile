# SPEC — FT-19 Interacciones del Feed (Likes, Comentarios, Borrado) y Reportes

> Rector: `.specify/memory/constitution.md` §2, §4.5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-030, SE-031, SE-032, SE-033**; Pantalla **P-22**; Componente **C-27**.
> Backend ref: Specs `024-publicaciones-feed`, `025-comentarios`, `039-me-gusta-publicaciones`, `040-reportes-contenido`.

## 1. Contexto y objetivo

Las tarjetas del feed comunitario y el detalle de producto (`ProductDetailScreen`) carecen hoy de acciones reales de reacción ("Me gusta"), hilo de comentarios, eliminación de contenido propio y formulario de reporte de ofertas/publicaciones falsas u ofensivas (`P-22`, `C-27`).

**Objetivo:** implementar las interacciones sociales sobre publicaciones (`POST /api/v1/feed/posts/{id}/like`, `GET /api/v1/feed/posts/{id}/comments`, `POST /api/v1/feed/posts/{id}/comments`, `DELETE /api/v1/feed/posts/{id}`) y el flujo transversal de reporte de contenido inapropiado (`POST /api/v1/reports`) tanto en ofertas como en el feed comunitario.

**Alcance:** casos de uso de like (optimista con rollback), comentarios paginados y creación moderada, borrado con confirmación de autoría y diálogo/pantalla de reporte (`P-22`, `C-27`).
**Fuera de alcance:** reporte de explotación infantil en app (prohibido procesar/almacenar localmente según política legal; canalización externa), edición de comentarios, moderación administrativa.

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-19-01 | Alternar `'Me gusta'` con `POST /api/v1/feed/posts/{id}/like` (actualización optimista y rollback si falla; bloqueado en invitado). |
| RF-19-02 | Desplegar hilo de comentarios paginado con `GET /api/v1/feed/posts/{id}/comments` y publicar comentarios nuevos vía `POST /api/v1/feed/posts/{id}/comments` (moderados antes de mostrarse). |
| RF-19-03 | Permitir eliminar únicamente publicaciones propias mediante diálogo de confirmación y `DELETE /api/v1/feed/posts/{id}` (`204` o `200`). |
| RF-19-04 | Incluir menú `'Reportar'` en tarjetas del feed (`CommunityScreen`) y enlace discreto `'Reportar producto'` (`C-27`) en `ProductDetailScreen` abriendo formulario `P-22` con motivos (`SPAM`, `OFFENSIVE`, `FALSE_OFFER`, `PRICE_MISMATCH`, `OTHER`) y detalle opcional (`<= 300` caracteres). |
| RF-19-05 | Enviar `POST /api/v1/reports`; manejar `409 Conflict` cuando el usuario ya reportó el mismo elemento, mostrando confirmación `'Reporte enviado'`. |

## 3. Requisitos no funcionales

- **Seguridad:** no mostrar opciones de eliminación en posts ajenos; todo comentario pasa moderación en servidor (*fail-closed*).
- **UX:** feedback inmediato en likes y comentarios; evitar envíos duplicados de reporte mientras la petición está en curso.
- **Paginación:** comentarios cargados de 20 en 20.

## 4. Arquitectura y contratos internos

```text
domain/
  model/FeedModels.kt           # FeedComment, ReportReason, ReportTargetType
  repository/FeedRepository.kt  # toggleLike, getComments, addComment, deletePost
  repository/ReportsRepository.kt
  usecase/TogglePostLikeUseCase.kt, GetPostCommentsUseCase.kt, AddCommentUseCase.kt, DeletePostUseCase.kt, SubmitReportUseCase.kt
data/
  remote/api/FeedApiService.kt  # like, comments, DELETE post
  remote/api/ReportsApiService.kt # POST /api/v1/reports
presentation/buyer/
  PostCommentsSheet.kt          # Hilo de comentarios
presentation/modals/
  ReportContentDialog.kt        # P-22 / C-27
```

## 5. Criterios de aceptacion

- **Given** usuario autenticado pulsa corazón/like en un post **When** el servidor falla **Then** el contador revierte al valor previo y muestra error.
- **Given** un comentario con lenguaje prohibido **When** intenta publicarse **Then** el backend lo bloquea/cuarentena y no se agrega al hilo visible.
- **Given** usuario pulsa Reportar oferta en `ProductDetailScreen` **When** elige motivo y confirma **Then** envía `POST /api/v1/reports` y muestra pantalla/confirmación `'Reporte enviado'`.
- **Given** usuario invitado **When** intenta dar like, comentar o reportar **Then** aparece `GuestPromptDialog`.

## 6. Plan de pruebas

- **Unitarias:** `SubmitReportUseCaseTest` (validación longitud detalle, motivos válidos, bloqueo invitado), `AddCommentUseCaseTest`, `TogglePostLikeUseCaseTest`.
- **Integración (`MockWebServer`):** pruebas de todos los endpoints (`200`/`201`, `409` reporte duplicado, `422` comentario rechazado, `403` borrado ajeno).
- **UI (Compose Test Rule):** `ReportContentDialog` (`P-22`), `PostCommentsSheet` y rollback visual de like en `CommunityScreen`.

## 7. Riesgos, limites y deuda

- Depende de las specs 024, 025, 039 y 040 en `sensus-back`.
- Si el backend excluye eliminación de comentarios en `025`, la app solo expone borrado de publicaciones propias hasta ampliar el contrato.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
