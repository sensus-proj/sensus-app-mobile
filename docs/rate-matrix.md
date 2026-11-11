# rate-matrix.md — vivo

> Limites centrales (constitucion §5 y §6). Empirico antes que teorico: medir antes de fijar.
> La evidencia de cada medicion se registra en `Memoria.md` §6.
> Los valores son **propuestos** hasta que exista backend real y medicion.

## Valores por endpoint (Sprint 1)

| Operacion | Endpoint | Timeout | Reintentos | Pagina | Estado |
|---|---|---|---|---|---|
| Login | `POST /api/auth/login` | 15 s | 0 | — | Propuesto (FT-04) |
| Registro | `POST /api/auth/register` | 20 s | 0 | — | Propuesto (FT-04) |
| Solicitud de recuperacion | `POST /api/auth/recover` | 15 s | 0 | — | Propuesto (FT-05) |
| Confirmacion de recuperacion | `POST /api/auth/recover/confirm` | 15 s | 0 | — | Propuesto (FT-05) |
| Actualizacion de perfil | `PUT /api/users/profile`, `PUT /api/merchants/profile` | 20 s | 1 | — | Propuesto (FT-06) |
| Ofertas cercanas | `GET /api/offers/nearby` | 20 s | 1 | 20 | Propuesto (FT-07) |
| Baja de cuenta | `DELETE /api/users/account` | 30 s | 0 | — | Propuesto (FT-08) |

## Valores por endpoint (Olas 0–5, FT-10..FT-26)

| Operacion | Endpoint | Timeout | Reintentos | Pagina | Estado |
|---|---|---|---|---|---|
| Logout y consulta comercio | `POST /api/v1/auth/logout`, `GET /api/v1/merchants/me` | 15 s | 0 / 1 | — | Alineado (FT-10, cliente HTTP 15 s) |
| Ofertas cercanas y detalle | `GET /api/v1/offers/nearby`, `GET /api/v1/offers/{id}` | 20 s | 1 | 20 | Propuesto (FT-11, FT-12) |
| Contador de vistas | `POST /api/v1/offers/{id}/view` | 10 s | 0 | — | Propuesto (FT-12) |
| Apartado y canje QR | `POST /api/v1/offers/{id}/claim`, `POST /api/v1/offers/claims/{code}/redeem` | 20 s | 0 | — | Propuesto (FT-13, FT-14) |
| Carga multimedia multipart | `POST /api/v1/media/upload` | 45 s | 1 | — | Propuesto (FT-15) |
| Gestión inventario comercio | `GET /api/v1/merchants/my-offers`, `POST`/`PUT /api/v1/offers` | 20 s | 1 / 0 | 20 | Propuesto (FT-15, FT-16) |
| Favoritos | `POST /api/v1/users/favorites/{id}` | 15 s | 0 | 20 | Propuesto (FT-17) |
| Feed social, comentarios y reportes | `GET /api/v1/feed/nearby`, `POST /feed/posts`, `comments`, `reports` | 20 s | 1 / 0 | 20 | Propuesto (FT-18, FT-19) |
| Búsqueda, recomendaciones y sustitutos | `GET /api/v1/ml/search`, `recommendations`, `alternatives` | 15 s | 1 | 20 | Propuesto (FT-20) |
| Device token y settings push | `PUT /api/v1/users/device-token`, `PUT /api/v1/users/settings` | 15 s | 1 | — | Propuesto (FT-21) |
| Sensi IA (Borrador) | `POST /api/v1/ai/conversations`, `POST /api/v1/ai/messages` | 30 s | 0 | — | Borrador (FT-24) |
| Amigos QR y reseñas (Borrador) | `/api/v1/friends`, `/api/v1/reviews` | 20 s | 0 | 20 | Borrador (FT-25, FT-26) |

## Valores iniciales (genericos)

| Operacion | Timeout | Reintentos | Pagina | Evidencia |
|---|---|---|---|---|
| Default de red | 60 s | 0 | — | Valor inicial; pendiente de medicion |
| Peticion paginada | 60 s | 0 | 20 | Valor inicial; medir con la primera entidad |
| Subida de archivo | 90 s | 2 (backoff) | — | Pendiente |
| Operacion de larga duracion | 90 s | 0 | — | Pendiente |
| Llamada a servicio externo de moderacion | 30 s | 1 | — | Pendiente |

## Limites de tasa (propuestos)

| Ambito | Limite | Feature |
|---|---|---|
| Login por IP | 5/min | FT-04 |
| Registro por IP | 3/hora | FT-04 |
| Recuperacion por correo | 1/60 s | FT-05 |
| Recuperacion por IP | 5/hora | FT-05 |
| Perfil por usuario | 30/min | FT-06 |
| Ofertas cercanas por IP | 60/min | FT-07 |
| Baja de cuenta por usuario | 1/hora | FT-08 |

## Politica

- Todo timeout se define en DI (`NetworkModule`), nunca literal en `*.kt`.
- Los reintentos solo aplican a operaciones idempotentes, con backoff y limite.
- El tamano de pagina se mide con la primera entidad real; no se asume.
- Cualquier cambio de limite requiere spec `FT-XX` y actualizacion de esta tabla.
- Cada feature agrega su fila al implementarse; este documento no duplica specs.
- El rate-limit de servidor es la autoridad; el cliente solo muestra el resultado.

## Deuda

Medir latencia real por endpoint cuando exista backend; hoy no hay integracion (datos simulados y `MockWebServer`).
Los valores de esta tabla pasan a `Medido` con fecha y evidencia en `Memoria.md` §6.
