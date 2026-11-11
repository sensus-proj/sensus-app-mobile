# SPEC — FT-26 Detalle Público del Comercio y Reseñas Post-Canje [BORRADOR]

> Rector: `.specify/memory/constitution.md` §2, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **Sensus_Movil_Componentes_y_CRUD.xlsx** (hoja *Reseñas* y *Comercios*, Pantalla **P-15**, Componentes **C-28..C-31**).
> Backend ref: **Sin spec aún en `sensus-back` (bloqueada hasta abrir spec equivalente en backend, decisión D-6)**.

## 1. Contexto y objetivo

En `Sensus_Movil_Componentes_y_CRUD.xlsx` existe la pantalla **Detalle del comercio (`P-15`)** con insignia Verificado, calificación promedio, total de canjes, formas de pago aceptadas, ofertas activas del local y reseñas de clientes que ya canjearon una reserva (`1–5` estrellas y comentario opcional). Actualmente en móvil no existe `MerchantPublicDetailScreen` ni flujo para emitir reseñas tras un canje.

**Objetivo:** implementar la pantalla pública de detalle de comercio (`P-15`) conectada a `GET /api/v1/merchants/{id}` y `GET /api/v1/merchants/{id}/reviews`, y habilitar el diálogo de reseña post-canje enviado vía `POST /api/v1/reviews` únicamente sobre reservas en estado `REDEEMED`.

**Alcance:** `MerchantPublicDetailScreen`, `MerchantPublicDetailViewModel`, `ReviewsRepository`, `SubmitReviewUseCase`, componentes `C-28..C-31` y diálogo de calificación 1–5 estrellas desde `MyClaimsScreen`.
**Fuera de alcance:** edición de reseñas pasados 7 días si el backend no lo contempla; respuesta de comercios a reseñas.

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-26-01 | Al tocar el nombre o fila del comercio en `ProductDetailScreen` o en la vista previa del mapa (`P-09`), navegar a `MerchantPublicDetailScreen` (`P-15`). |
| RF-26-02 | `GET /api/v1/merchants/{id}` devuelve datos públicos, dirección, estado `Abierto ahora` con hora de cierre, insignia `Verificado`, calificación promedio, total de canjes, métodos de pago (`C-31`) y ofertas activas. |
| RF-26-03 | `GET /api/v1/merchants/{id}/reviews` carga reseñas paginadas (20 por página) con filtro opcional de destacadas (`>= 4` estrellas). |
| RF-26-04 | En reservas con estado `REDEEMED`, habilitar acción `'Calificar comercio'` que valida calificación entera en `[1, 5]` y comentario `<= 300` caracteres enviando `POST /api/v1/reviews` (una sola reseña por `claimId`). |

## 3. Requisitos no funcionales

- **Integridad:** nunca permitir enviar reseña si la reserva no está en estado canjeado (`REDEEMED`) o si es invitado.
- **Moderación:** el comentario de la reseña pasa por moderación previa en el servidor.

## 4. Arquitectura y contratos internos

```text
domain/
  model/MerchantPublicModels.kt # MerchantPublicProfile, MerchantReview, PaymentMethodType
  repository/ReviewsRepository.kt
  usecase/GetMerchantPublicProfileUseCase.kt, GetMerchantReviewsUseCase.kt, SubmitReviewUseCase.kt
data/
  remote/api/ReviewsApiService.kt # GET /api/v1/merchants/{id}, /reviews, POST /api/v1/reviews
  repository/ReviewsRepositoryImpl.kt
presentation/buyer/
  MerchantPublicDetailContract.kt, MerchantPublicDetailViewModel.kt, MerchantPublicDetailScreen.kt # P-15
presentation/modals/
  SubmitReviewDialog.kt
```

## 5. Criterios de aceptacion

- **Given** un usuario toca un comercio en `ProductDetailScreen` **When** abre `P-15` **Then** visualiza métricas (`calificación`, `canjes`), formas de pago, horario y lista paginada de reseñas.
- **Given** un comprador tiene una reserva en estado `REDEEMED` sin reseña **When** selecciona 5 estrellas y escribe comentario **Then** envía `POST /api/v1/reviews` y recalcula el promedio mostrado.
- **Given** el usuario intenta enviar calificación `0` o `6` **When** valida `SubmitReviewUseCase` **Then** bloquea antes de enviar.

## 6. Plan de pruebas

- **Unitarias:** `SubmitReviewUseCaseTest` (rango `1..5`, longitud comentario, reserva debe estar `REDEEMED`), `GetMerchantReviewsUseCaseTest`.
- **ViewModel:** `MerchantPublicDetailViewModelTest` con Turbine.
- **Integración (`MockWebServer`):** endpoints públicos de comercio y reseñas (`200`, `201`, `409` reseña duplicada).
- **UI (Compose Test Rule):** `MerchantPublicDetailScreen` (`C-28..C-31`) y `SubmitReviewDialog`.

## 7. Riesgos, limites y deuda

- **BLOQUEANTE:** `GET /api/v1/merchants/{id}`, `GET /api/v1/merchants/{id}/reviews` y `POST /api/v1/reviews` aún no existen en `sensus-back` (decisión D-6). Queda en `Borrador` hasta definir contrato en backend.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Bloqueada por falta de spec/contrato en backend (D-6) |
