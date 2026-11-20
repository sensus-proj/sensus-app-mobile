# FT-26 — Reseñas de comercio estilo Play Store y calificación real

| Campo | Valor |
|---|---|
| Feature | `FT-26-resenas-comercio` |
| Estado | `En revision` |
| Depende de | `FT-12-detalle-filtros-ruteo`, `FT-14-canje-qr-comercio`, `FT-29-merchant-location-activation` |
| Spec backend | [`054-resenas-comercios`](../../../sensus-back/specs/054-resenas-comercios/spec.md) |
| Pantalla / Componentes | `StoreCatalogScreen` (tarjeta cabecera), `StoreReviewsScreen`, `StoreReviewsComponents`, `MerchantProfileScreen` |

## 1. Contexto y objetivo

Al pulsar la tarjeta superior del comercio dentro del catálogo (`StoreCatalogHeaderCard`, con foto de perfil, dirección y calificación), el usuario abre la pantalla de reseñas estilo Play Store (`StoreReviewsScreen`) para consultar el promedio real con estrellas, el total de reseñas, el desglose de 5 a 1 estrellas y los comentarios de otros usuarios con nombre, foto de perfil y fecha/año. Cualquier usuario registrado puede calificar (1..5 estrellas) con comentario moderado, dar «Me gusta» a una reseña y responder en hilo. Además, el perfil del comerciante (`MerchantProfileScreen`) reemplaza cualquier calificación ficticia por el promedio real (`rating`) y total de reseñas (`reviewCount`) devueltos por `GET /api/v1/merchants/me`.

## 2. Usuarios y flujo principal

1. **Consumidor / Invitado** abre el catálogo del comercio desde el mapa y pulsa la tarjeta superior (`StoreCatalogHeaderCard`).
2. Se navega a `StoreReviewsScreen`, que invoca `GET /api/v1/merchants/{merchantId}/reviews` mediante `StoreReviewsViewModel`.
3. Se muestra el promedio numérico (ej. `4.4`), ícono de estrella en color de marca (`SensusPurple`, Constitución §9.2), barras de desglose de 5 a 1 estrellas y tarjetas de reseña con foto, nombre, fecha/año (`dd MMM yyyy`), calificación, comentario, botón de «Me gusta» y respuestas anidadas.
4. Si el usuario está registrado (no invitado), puede enviar una nueva calificación + comentario (`POST /api/v1/merchants/{merchantId}/reviews`), dar/quitar «Me gusta» (`POST /api/v1/merchants/reviews/{reviewId}/like`) o abrir el diálogo de respuesta (`POST /api/v1/merchants/reviews/{reviewId}/replies`), validando groserías en cliente antes de enviar.
5. El **Comerciante** consulta su perfil (`MerchantProfileScreen`) y ve su calificación real y conteo real provenientes de `GET /api/v1/merchants/me`.

## 3. Requisitos funcionales

- **`RF-26-01`:** Hacer clicable la tarjeta superior del catálogo (`StoreCatalogHeaderCard`) para abrir `StoreReviewsScreen` de ese comercio.
- **`RF-26-02`:** Consultar y mostrar resumen estilo Play Store (`averageRating`, `totalReviews`, desglose `5..1` estrellas y lista de reseñas) mediante `GET /api/v1/merchants/{merchantId}/reviews`.
- **`RF-26-03`:** Permitir a usuarios registrados publicar reseñas de `1..5` estrellas con comentario de `1..600` caracteres vía `POST /api/v1/merchants/{merchantId}/reviews`, pasando primero por `ProfanityFilter`.
- **`RF-26-04`:** Permitir a usuarios registrados alternar «Me gusta» sobre reseñas (`POST /api/v1/merchants/reviews/{reviewId}/like`) y responder comentarios (`POST /api/v1/merchants/reviews/{reviewId}/replies`, `1..500` caracteres moderados).
- **`RF-26-05`:** Mostrar en `MerchantProfileScreen` la calificación y conteo real (`averageRating` / `reviewCount`) del perfil comercial obtenido de `GET /api/v1/merchants/me` (si es `0`, indicar estado inicial real sin datos falsos).
- **`RF-26-06`:** Bloquear acciones de escritura para usuarios invitados con mensaje claro y usar paleta morada de marca (`SensusPurple`, sin amarillo/ámbar per Constitución §9.2).

## 4. Requisitos no funcionales y arquitectura

- Clean Architecture estricta: `domain/` (`StoreReviewModels`, `StoreReviewRepository`, `StoreReviewUseCases`) sin dependencias Android/Retrofit; `data/` (`StoreReviewsApiService`, `StoreReviewRepositoryImpl`); `presentation/` (`StoreReviewsViewModel` heredando de `MviViewModel<StoreReviewsViewState, StoreReviewsViewIntent, StoreReviewsViewEffect>`).
- Cumplimiento de diseño Sensus (§9.2): íconos de estrella en `SensusPurple` / `SensusTextMuted`, cero amarillo/ámbar en UI interna.
- Cobertura de pruebas unitarias Kover `>= 80%`.

## 5. Contratos y navegación

| Endpoint | Origen -> Destino | Uso |
|---|---|---|
| `GET /api/v1/merchants/{merchantId}/reviews` | `GetStoreReviewsUseCase` -> `StoreReviewRepository` -> `StoreReviewsApiService` | Cargar promedio y reseñas del comercio |
| `POST /api/v1/merchants/{merchantId}/reviews` | `CreateStoreReviewUseCase` -> `StoreReviewRepository` -> `StoreReviewsApiService` | Enviar calificación y comentario |
| `POST /api/v1/merchants/reviews/{reviewId}/like` | `ToggleStoreReviewLikeUseCase` -> `StoreReviewRepository` -> `StoreReviewsApiService` | Alternar «Me gusta» en reseña |
| `POST /api/v1/merchants/reviews/{reviewId}/replies` | `ReplyStoreReviewUseCase` -> `StoreReviewRepository` -> `StoreReviewsApiService` | Publicar respuesta en hilo |
| `GET /api/v1/merchants/me` | `MerchantProfileViewModel` -> `MerchantRepository` -> `MerchantApiService` | Calificación real del comerciante |

## 6. Criterios de aceptación

- Pulsar la tarjeta del comercio en el catálogo navega a `StoreReviewsScreen` del comercio seleccionado.
- Se muestran foto de perfil, nombre completo, fecha con año (`yyyy`), calificación, botón de «Me gusta» con conteo y respuestas.
- Comentarios o respuestas con groserías son rechazados inmediatamente antes o durante el envío.
- El perfil del comerciante nunca muestra promedios simulados hardcodeados (`4.9` falso).

## 7. Pruebas requeridas

- `StoreReviewsViewModelTest` (carga, validación local con `ProfanityFilter`, creación de reseña, toggle de like optimista/revertido, respuesta y errores).
- `MerchantProfileViewModelTest` (mapeo de calificación y conteo real en perfil comercial).

## Aprobación

| Rol | Responsable | Estado | Fecha |
|---|---|---|---|
| Producto / SDD | Equipo Sensus | Aprobada (retro-alineación a Spec 054) | 2026-11-19 |
| Android Lead | Equipo Móvil | En revisión de cobertura y gates | 2026-11-19 |
