# endpoints-internos.md — vivo

> Canonico (constitucion §6). Las specs enlazan, no duplican.
> Gate: `python scripts/check_endpoints.py` -> PASS/FAIL.

## Estado

Backend desplegado en AWS Serverless (`https://zqqdwovx81.execute-api.us-east-2.amazonaws.com/`). Sprint 1 consume `/api/v1/...` y el resto coexiste temporalmente con `MockDataProvider` hasta completar `FT-10..FT-26`.
Este documento es **generico**: cada feature `specs/FT-XX/spec.md` agrega sus filas al implementarse.
Los endpoints de negocio de FT-04..FT-08 y FT-10..FT-26 quedan declarados aqui antes de su implementacion.

## Tabla canonica

| Endpoint | Origen -> Destino | Auth | Rate | Estado |
|---|---|---|---|---|
| `sessionFlow`, `saveSession`, `clearSession`, `isSessionValid` | `SessionManager` -> `TokenLocalDataSource` (almacenamiento cifrado) | Token en `EncryptedSharedPreferences` | N/A local | Pendiente (FT-02) |
| Interceptor de autenticacion | `NetworkModule` -> cliente HTTP | Lee token de `TokenLocalDataSource` | N/A | Pendiente (FT-01) |
| Mapeo de errores HTTP | `RemoteDataSource` -> `Result`/`AppError` | N/A | N/A | Pendiente (FT-01) |
| `POST /api/auth/login` | `LoginUseCase` -> `AuthRepository` -> `AuthApi` | Publico | 5/min por IP | Pendiente (FT-04) |
| `POST /api/auth/register` | `RegisterUseCase` -> `AuthRepository` -> `AuthApi` | Publico | 3/hora por IP | Pendiente (FT-04) |
| `POST /api/auth/recover` | `RequestRecoveryUseCase` -> `AuthRepository` -> `AuthApi` | Publico | 1/60 s por correo, 5/hora por IP | Pendiente (FT-05) |
| `POST /api/auth/recover/confirm` | `ConfirmRecoveryUseCase` -> `AuthRepository` -> `AuthApi` | Token de un solo uso (15 min) | 5/hora por IP | Pendiente (FT-05) |
| `PUT /api/users/profile` | `UpdateUserProfileUseCase` -> `ProfileRepository` -> `ProfileApi` | Bearer `CONSUMER` | 30/min por usuario | Pendiente (FT-06) |
| `PUT /api/merchants/profile` | `UpdateMerchantProfileUseCase` -> `ProfileRepository` -> `ProfileApi` | Bearer `MERCHANT` | 30/min por usuario | Pendiente (FT-06) |
| `GET /api/offers/nearby` | `GetNearbyOffersUseCase` -> `GuestOfferRepository` -> `GuestOfferApi` | Publico (invitado) o Bearer | 60/min por IP | Pendiente (FT-07) |
| `DELETE /api/users/account` | `DeleteAccountUseCase` -> `AccountRepository` -> `AccountApi` | Bearer | 1/hora por usuario | Pendiente (FT-08) |
| `POST /api/v1/auth/logout` | `LogoutUseCase` -> `AuthRepository` -> `AuthApiService` | Bearer | 30/min por usuario | OK (FT-10, 2026-11-11; Spec 004) |
| `POST /api/v1/auth/guest` | `StartGuestSessionUseCase` -> `AuthApiService` | Publico | 30/min por IP | OK (FT-10, 2026-11-11; AuthEndpoints.cs) |
| `POST /api/v1/auth/reset-password` | `ResetPasswordUseCase` -> `AuthApiService` (concilia con `/recover/confirm`) | Token temporal | 5/hora por IP | OK (FT-10, 2026-11-11; backend expone /reset-password) |
| `POST /api/v1/users/photo-signature` | `RetrofitProfileRepository` -> `ProfileApiService` | Bearer | 10/min por usuario | OK (FT-10, 2026-11-11; UserEndpoints.cs) |
| `POST /api/v1/media/moderate` | `ImageModerationService` -> `MediaApiService` (previo a `/media/upload` en FT-15) | Bearer | 20/min por usuario | OK (FT-10, 2026-11-11; MediaEndpoints.cs) |
| `GET /api/v1/comercios/me` | `MerchantApiService` (legacy espanol; retirado en cliente por `/api/v1/merchants/me`) | Bearer `MERCHANT` | 30/min por usuario | Retirado cliente (FT-10, 2026-11-11; backend transicional) |
| `GET /api/v1/merchants/me` | `GetMyMerchantUseCase` -> `MerchantRepository` -> `MerchantApiService` | Bearer `MERCHANT` | 30/min por usuario | OK cliente (`FT-10` / `FT-29`, alineado a `Spec 006`) |
| `POST /api/v1/merchants/activate` | `ActivateMerchantUseCase` -> `MerchantRepository` -> `MerchantApiService` | Bearer (usuario registrado) | 10/min por usuario | Implementado cliente / MockWebServer validado (`FT-29`, 2026-11-13; alineado a `Spec 005`) |
| `PUT /api/v1/merchants/profile` | `UpdateMerchantProfileUseCase` -> `MerchantRepository` -> `MerchantApiService` | Bearer `MERCHANT` | 30/min por usuario | Implementado cliente / MockWebServer validado (`FT-06` / `FT-29`, 2026-11-13; alineado a `Spec 031`) |
| `GET /api/v1/merchants/nearby` | `GetNearbyMerchantsUseCase` -> `MerchantRepository` -> `MerchantApiService` | Publico (invitado 2 km) o Bearer | 60/min por IP | Implementado cliente (`FT-11`/`SE-008`, alineado a `Spec 008`) |
| `GET /api/v1/offers/nearby` | `GetNearbyOffersUseCase` -> `OffersRepository` -> `OffersApiService` | Publico (invitado 2 km) o Bearer | 60/min por IP | OK cliente (`FT-11`, `Spec 007`) |
| `GET /api/v1/offers/{id}` | `GetOfferDetailUseCase` -> `OffersRepository` -> `OffersApiService` | Publico o Bearer | 60/min por IP | Cliente conectado a detalle MVI; contrato backend 032 y ruta oficial verificados (2026-11-14; cliente y tests locales verificados; E2E real pendiente) |
| `POST /api/v1/offers/{id}/view` | `RegisterOfferViewUseCase` -> `OffersRepository` -> `OffersApiService` | Publico o Bearer | 60/min por IP | Cliente conectado a detalle MVI; contrato backend 032 y ruta oficial verificados (2026-11-14; cliente y tests locales verificados; E2E real pendiente) |
| `POST /api/v1/offers/{id}/claim` | `CreateClaimUseCase` -> `ClaimsRepository` -> `ClaimsApiService` | Bearer (no invitado) | 20/min por usuario | Cliente conectado a confirmación de una unidad/QR real; backend 033 y ruta oficial verificados (2026-11-14; cliente y tests locales verificados; E2E real pendiente) |
| `GET /api/v1/offers/claims` | `GetMyClaimsUseCase` -> `ClaimsRepository` -> `ClaimsApiService` | Bearer | 30/min por usuario | Propuesto / por confirmar backend (FT-13) |
| `PATCH /api/v1/offers/claims/{id}/cancel` | `CancelClaimUseCase` -> `ClaimsRepository` -> `ClaimsApiService` | Bearer | 10/min por usuario | Propuesto / por confirmar backend (FT-13) |
| `POST /api/v1/offers/claims/{code}/redeem` | `RedeemClaimUseCase` -> `ClaimsRepository` -> `ClaimsApiService` | Bearer `MERCHANT` | 30/min por usuario | Cliente conectado a canje manual MVI; backend 033 y ruta oficial verificados (2026-11-14; cliente y tests locales verificados; E2E real pendiente) |
| `POST /api/v1/media/upload` | `MediaRepository` -> `MediaApiService` (`@Multipart <= 4.5 MB`, moderado) | Bearer | 20/dia por usuario | Implementado cliente / MockWebServer validado; contrato desplegado pendiente (FT-15, spec 022) |
| `POST /api/v1/offers` | `CreateOfferUseCase` -> `OffersRepository` -> `OffersApiService` | Bearer `MERCHANT` | 20/hora por usuario | Implementado cliente / MockWebServer validado; contrato desplegado pendiente (FT-15, spec 034) |
| `GET /api/v1/merchants/my-offers` | `GetMerchantInventoryUseCase` -> `OffersRepository` -> `OffersApiService` | Bearer `MERCHANT` | 30/min por usuario | Implementado cliente / MockWebServer validado (`FT-16`, `Spec 037`) |
| `PUT /api/v1/offers/{id}` | `UpdateOfferUseCase` -> `OffersRepository` -> `OffersApiService` | Bearer `MERCHANT` | 30/min por usuario | Implementado cliente / MockWebServer validado (`FT-16`, `Spec 035`) |
| `PUT /api/v1/offers/{id}/status` | `ChangeOfferStatusUseCase` -> `OffersRepository` -> `OffersApiService` | Bearer `MERCHANT` | 30/min por usuario | Implementado cliente / MockWebServer validado (`FT-16`, `Spec 036`) |
| `POST /api/v1/users/favorites/{id}` | `ToggleFavoriteUseCase` -> `FavoritesRepository` -> `FavoritesApiService` | Bearer (no invitado) | 120/hora por usuario | Pendiente (FT-17, spec 038) |
| `GET /api/v1/users/favorites` | `GetFavoritesUseCase` -> `FavoritesRepository` -> `FavoritesApiService` | Bearer | 30/min por usuario | Propuesto / por confirmar backend (FT-17) |
| `GET /api/v1/feed/nearby` | `GetNearbyFeedUseCase` -> `FeedRepository` -> `FeedApiService` | Bearer o Publico | 60/min por usuario | Cliente implementado; GET oficial 404; backend spec 026 pendiente en main 01dad6e (2026-11-14, FT-18) |
| `POST /api/v1/feed/posts` | `CreateFeedPostUseCase` -> `FeedRepository` -> `FeedApiService` | Bearer (no invitado) | 10/hora por usuario | Cliente implementado; backend spec 024 pendiente en main 01dad6e; sin POST real de prueba (2026-11-14, FT-18) |
| `DELETE /api/v1/feed/posts/{id}` | `DeletePostUseCase` -> `FeedRepository` -> `FeedApiService` | Bearer autor | 20/hora por usuario | Pendiente (FT-19, spec 024) |
| `POST /api/v1/feed/posts/{id}/like` | `TogglePostLikeUseCase` -> `FeedRepository` -> `FeedApiService` | Bearer (no invitado) | 120/hora por usuario | Pendiente (FT-19, spec 039) |
| `GET /api/v1/feed/posts/{id}/comments` | `GetPostCommentsUseCase` -> `FeedRepository` -> `FeedApiService` | Bearer o Publico | 60/min por usuario | Pendiente (FT-19, spec 025) |
| `POST /api/v1/feed/posts/{id}/comments` | `AddCommentUseCase` -> `FeedRepository` -> `FeedApiService` | Bearer (no invitado) | 30/hora por usuario | Pendiente (FT-19, spec 025) |
| `POST /api/v1/reports` | `SubmitReportUseCase` -> `ReportsRepository` -> `ReportsApiService` | Bearer (no invitado) | 10/hora por usuario | Pendiente (FT-19, spec 040) |
| `GET /api/v1/ml/search` | `SearchOffersUseCase` -> `DiscoveryRepository` -> `DiscoveryApiService` | Publico o Bearer | 60/min por IP | Pendiente (FT-20, spec 041) |
| `GET /api/v1/ml/recommendations` | `GetRecommendationsUseCase` -> `DiscoveryRepository` -> `DiscoveryApiService` | Bearer | 30/min por usuario | Pendiente (FT-20, spec 042) |
| `GET /api/v1/ml/alternatives` | `GetOfferAlternativesUseCase` -> `DiscoveryRepository` -> `DiscoveryApiService` | Publico o Bearer | 60/min por IP | Pendiente (FT-20, spec 043) |
| `PUT /api/v1/users/device-token` | `RegisterDeviceTokenUseCase` -> `NotificationsRepository` -> `NotificationsApiService` | Bearer | 10/hora por usuario | Cliente conectado a registro FCM real; requiere configuración Firebase oficial (2026-11-14; cliente y tests locales verificados; E2E real pendiente) |
| `PUT /api/v1/users/settings` | `UpdateNotificationSettingsUseCase` -> `NotificationsRepository` -> `NotificationsApiService` | Bearer | 20/min por usuario | Cliente conectado a ajustes MVI; backend 044 y ruta oficial verificados (2026-11-14; cliente y tests locales verificados; E2E real pendiente) |
| `GET /api/v1/users/settings` | `NotificationsGetSettingsUseCase` -> `NotificationsRepository` -> `NotificationsApiService` | Bearer | Política backend; sin medición local | Cliente conectado a ajustes; backend 044 y GET oficial 401 verificados (2026-11-14; cliente y tests locales verificados; E2E real pendiente) |
| `POST /api/v1/offers/{id}/republish` | `RepublishOfferUseCase` -> `OffersRepository` -> `OffersApiService` | Bearer `MERCHANT` | Política backend; sin medición local | Cliente conectado a historial/edición; contrato backend y Allow POST oficial verificados (2026-11-14; cliente y tests locales verificados; E2E real pendiente, FT-16) |
| `GET /api/v1/notifications` | `NotificationsRepository` -> `NotificationsApiService` | Bearer | 30/min por usuario | Propuesto / por confirmar backend (FT-21) |
| `PATCH /api/v1/notifications/read-all` | `MarkNotificationsReadUseCase` -> `NotificationsRepository` -> `NotificationsApiService` | Bearer | 20/min por usuario | Propuesto / por confirmar backend (FT-21) |
| `POST /api/v1/ai/conversations` | `LoadAiConversationUseCase` -> `AiChatRepository` -> `AiChatApiService` | Bearer (no invitado) | 10/min por usuario | Especificado en backend (Spec 046, ADR 0012, 2026-11-15); despliegue pendiente (FT-24) |
| `POST /api/v1/ai/messages` | `SendSensiMessageUseCase` -> `AiChatRepository` -> `AiChatApiService` | Bearer (no invitado) | 10/min por usuario (20/día) | Especificado en backend (Spec 046, ADR 0012, 2026-11-15); despliegue pendiente (FT-24) |
| `GET /api/v1/ai/conversations/{id}` | `LoadAiConversationUseCase` -> `AiChatRepository` -> `AiChatApiService` | Bearer | 30/min por usuario | Especificado en backend (Spec 046, ADR 0012, 2026-11-15); despliegue pendiente (FT-24) |
| `DELETE /api/v1/ai/conversations/{id}` | `AiChatRepository` -> `AiChatApiService` | Bearer (no invitado) | 10/min por usuario | Especificado en backend (Spec 046, ADR 0012, 2026-11-15); despliegue pendiente (FT-24) |
| `GET /api/v1/friends` | `FriendsRepository` -> `FriendsApiService` | Bearer (no invitado) | 30/min por usuario | Implementado móvil + backend (Spec 050 / FT-25, despliegue AWS pendiente) |
| `POST /api/v1/friends/requests` | `FriendsRepository` -> `FriendsApiService` | Bearer (no invitado) | 20/hora por usuario | Implementado móvil + backend (Spec 050 / FT-25, despliegue AWS pendiente) |
| `POST /api/v1/friends/requests/{requestId}/accept` | `FriendsRepository` -> `FriendsApiService` | Bearer (destinatario) | 20/hora por usuario | Implementado móvil + backend (Spec 050 / FT-25, despliegue AWS pendiente) |
| `POST /api/v1/friends/requests/{requestId}/reject` | `FriendsRepository` -> `FriendsApiService` | Bearer (participante) | 20/hora por usuario | Implementado móvil + backend (Spec 050 / FT-25, despliegue AWS pendiente) |
| `DELETE /api/v1/friends/{friendUserId}` | `FriendsRepository` -> `FriendsApiService` | Bearer (no invitado) | 20/hora por usuario | Implementado móvil + backend (Spec 050 / FT-25, despliegue AWS pendiente) |
| `GET /api/v1/friends/{friendUserId}/shared-offers` | `FriendsRepository` -> `FriendsApiService` | Bearer (amigos aceptados) | 30/min por usuario | Implementado móvil + backend (Spec 050 / FT-25, despliegue AWS pendiente) |
| `POST /api/v1/offers/{offerId}/share` | `FriendsRepository` -> `FriendsApiService` | Bearer (1..10 amigos) | 30/hora por usuario | Implementado móvil + backend (Spec 050 / FT-25, despliegue AWS pendiente) |
| `GET /api/v1/merchants/{merchantId}/reviews` | `GetStoreReviewsUseCase` -> `StoreReviewRepository` -> `StoreReviewsApiService` | Bearer (Invitado o Registrado) | 60/min por IP | Implementado cliente + backend (`FT-26`, `Spec 056`, 2026-11-19) |
| `POST /api/v1/merchants/{merchantId}/reviews` | `CreateStoreReviewUseCase` -> `StoreReviewRepository` -> `StoreReviewsApiService` | Bearer (no invitado) | 20/hora por usuario | Implementado cliente + backend (`FT-26`, `Spec 056`, 2026-11-19) |
| `POST /api/v1/merchants/reviews/{reviewId}/like` | `ToggleStoreReviewLikeUseCase` -> `StoreReviewRepository` -> `StoreReviewsApiService` | Bearer (no invitado) | 120/hora por usuario | Implementado cliente + backend (`FT-26`, `Spec 056`, 2026-11-19) |
| `POST /api/v1/merchants/reviews/{reviewId}/replies` | `ReplyStoreReviewUseCase` -> `StoreReviewRepository` -> `StoreReviewsApiService` | Bearer (no invitado) | 30/hora por usuario | Implementado cliente + backend (`FT-26`, `Spec 056`, 2026-11-19) |
| `POST /api/v1/offers/{id}/like` | `ToggleOfferLikeUseCase` -> `OffersRepository` -> `OfferDetailsApiService` | Bearer (no invitado) | 120/hora por usuario | Implementado cliente + backend (`FT-31`, `Spec 057`, 2026-11-19) |
| `GET /api/v1/community/friends-activity` | `FriendsActivityViewModel` -> `FriendsActivityRepository` -> `FriendsApiService` | Bearer (no invitado) | 30/min por usuario | Implementado cliente + backend (`FT-32`, `Spec 058`, 2026-11-19) |

## Contratos por feature

| Feature | Endpoints | Regla de negocio |
|---|---|---|
| FT-04 | `POST /api/auth/login`, `POST /api/auth/register` | Rol `CONSUMER`/`MERCHANT`; sin documentos oficiales. |
| FT-05 | `POST /api/auth/recover`, `POST /api/auth/recover/confirm` | Token de 15 min, un solo uso; respuesta neutra. |
| FT-06 | `PUT /api/users/profile`, `PUT /api/merchants/profile` | Recalculo `GEOGRAPHY(POINT, 4326)` en backend. |
| FT-07 | `GET /api/offers/nearby` | Invitado: `radiusMeters = 2000` estricto. |
| FT-08 | `DELETE /api/users/account` | 204/404 exito; purga local y remota. |
| FT-29 | `POST /api/v1/merchants/activate`, `GET /api/v1/merchants/me`, `PUT /api/v1/merchants/profile` | Coordenadas opcionales de mapa interactivo; JWT renovado y estado comercial persistidos. |
| FT-26 | `GET /api/v1/merchants/{merchantId}/reviews`, `POST /api/v1/merchants/{merchantId}/reviews`, `POST /api/v1/merchants/reviews/{reviewId}/like`, `POST /api/v1/merchants/reviews/{reviewId}/replies` | Calificación 1..5, comentarios/respuestas moderados (`ProfanityFilter` + IA), likes en reseñas y promedio real en `MerchantProfileScreen`. |
| FT-31 | `GET /api/v1/offers/{id}`, `POST /api/v1/offers/{id}/like` | Alternancia de Me gusta sobre ofertas de producto y conteo público (`LikesCount`). |
| FT-32 | `GET /api/v1/community/friends-activity` | Solo comentarios y likes de ofertas de amigos conectados y del usuario actual. |

## Plantilla para nuevas filas

| Endpoint | Origen -> Destino | Auth | Rate | Estado |
|---|---|---|---|---|
| `METODO /ruta` | `UseCase` -> `Repository` -> `ApiService` | Bearer / publico | timeout segun `docs/rate-matrix.md` | Pendiente / OK (FT-XX, fecha) |

## Reglas

- Todo endpoint real se registra aqui antes de implementarse.
- Sin token, password, PII ni nombres de clientes en esta tabla.
- Un contrato interno no cambia sin spec `FT-XX`.
- Cada fila pasa a `OK` con fecha y feature cuando exista evidencia.
- `python scripts/check_endpoints.py` falla si una spec cita un endpoint ausente en la tabla canonica.
