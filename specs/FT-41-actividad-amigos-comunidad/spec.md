# FT-41 — Actividad de amigos en Comunidad (`FriendsActivity`)

| Campo | Valor |
|---|---|
| Feature | `FT-41-actividad-amigos-comunidad` |
| Estado | `En revision` |
| Depende de | `FT-25-amigos-qr`, `FT-26-resenas-comercio`, `FT-40-me-gusta-ofertas` |
| Spec backend | [`058-actividad-amigos-comunidad`](../../../sensus-back/specs/058-actividad-amigos-comunidad/spec.md) |
| Pantalla / Componentes | `CommunityScreen`, `FriendsActivityContent`, `FriendsActivityViewModel` |

## 1. Contexto y objetivo

Mostrar en la pestaña **Comunidad** únicamente las interacciones sociales relevantes de tus amigos conectados y las tuyas: comentarios/respuestas dejados en comercios (`StoreReview` / `ReviewReply`) y productos que les gustaron (`OfferLike`), para compartir actividad mutua sin ruido de publicaciones genéricas.

> Distinta de `FT-18-feed-comunidad` (muro público `/api/v1/feed/nearby` y `/api/v1/feed/posts`): esta feature consume `GET /api/v1/community/friends-activity`.

## 2. Usuarios y flujo principal

1. El **Usuario registrado** entra a la pestaña **Comunidad**.
2. `FriendsActivityViewModel` dispara `FriendsActivityViewIntent.Load` e invoca `FriendsActivityRepository.getFriendsActivity()`.
3. `FriendsApiService` consulta `GET /api/v1/community/friends-activity`.
4. `FriendsActivityContent` renderiza cada tarjeta con avatar del amigo (o tuyo), acción realizada («le dio me gusta a...», «comentó en...», «respondió en...»), título del producto/comercio, extracto del comentario o calificación con ícono de estrella en color `SensusPurple` (Constitución §9.2) y fecha/año.
5. Incluye refresco pull-to-refresh / botón recargar y estado vacío cuando aún no hay amigos con actividad.

## 3. Requisitos funcionales

- **`RF-41-01`:** Consumir `GET /api/v1/community/friends-activity` desde `FriendsApiService` -> `FriendsActivityRepositoryImpl` -> `FriendsActivityViewModel` (MVI).
- **`RF-41-02`:** Mostrar exclusivamente comentarios/reseñas/respuestas y likes en publicaciones de producto de amigos conectados y del usuario actual.
- **`RF-41-03`:** Soportar recarga manual y estados `Loading`, `Loaded`, `Empty` y `Error` sin romper la pestaña de Amigos/QR (`FT-25`).
- **`RF-41-04`:** Respetar Constitución §9.2 en badges de calificación dentro de las tarjetas (color `SensusPurple`, nunca amarillo ni ámbar).

## 4. Requisitos no funcionales y arquitectura

- Clean Architecture: `FriendsActivityRepository` en `domain/repository/`, DTO y `FriendsActivityRepositoryImpl` en `data/`, `FriendsActivityViewModel` heredando de `MviViewModel` en `presentation/buyer/`.
- Cero tonos amarillos/ámbar en UI interna (§9.2).
- Cobertura unitaria Kover >= 80 %.

## 5. Contratos y navegación

| Endpoint | Origen -> Destino | Uso |
|---|---|---|
| `GET /api/v1/community/friends-activity` | `FriendsActivityViewModel` -> `FriendsActivityRepository` -> `FriendsApiService` | Feed de likes y comentarios de amigos y propios |

## 6. Criterios de aceptación

- Al dar like a un producto o comentar en un comercio, la actividad aparece en Comunidad tanto para el autor como para sus amigos conectados.
- No muestra posts externos ni datos de usuarios no conectados como amigos.

## 7. Pruebas requeridas

- `FriendsActivityViewModelTest` (carga exitosa con items de like/reseña/respuesta, lista vacía, refresh y manejo de error).

## Aprobación

| Rol | Responsable | Estado | Fecha |
|---|---|---|---|
| Producto / SDD | Equipo Sensus | Aprobada (retro-spec `058-actividad-amigos-comunidad`) | 2026-11-19 |
| Android Lead | Equipo Móvil | En revisión de cobertura y paleta UI | 2026-11-19 |
