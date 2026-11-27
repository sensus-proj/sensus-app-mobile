# SPEC — FT-30 Feed de Promociones por Intereses

> Rector: `.specify/memory/constitution.md` §2, §3, §5, §6 y §9 (v1.3.0, seguridad de nivel fintech). Plantilla: `.specify/templates/spec-template.md`.
> Backend ref: Specs `042-recomendaciones` (v2.0.0, feed de promociones), `054-intereses-usuario` y `055-interacciones-ofertas`.
> Sustituye el carrusel «Recomendados para ti» y la telemetría desde el cliente que proponía FT-20.

## 1. Contexto y objetivo

El usuario registrado requiere un lugar donde ver, en *scroll* continuo, las promociones cercanas que más le interesan. Por decisión del responsable del producto (2026-11-20), el «ML» de Sensus se materializa como un **feed de promociones propio**, ordenado en el servidor según los intereses del usuario.

**Objetivo:** añadir la pestaña **Promociones** con un feed paginado de ofertas desde `GET /api/v1/feed/offers`, y permitir que el usuario elija sus categorías de interés de forma **opcional** al terminar el registro y las edite después desde su perfil (`GET`/`PUT /api/v1/users/me/interests`).

**Alcance:** `InterestsRepository`, `GetMyInterestsUseCase`, `UpdateMyInterestsUseCase`, `OfferFeedRepository`, `GetOfferFeedUseCase` con paginación por `snapshotId`, pantalla `InterestsPickerScreen` (tras registro, omitible), sección de intereses en `BuyerProfileScreen`, pestaña `OfferFeedScreen` y estados de carga, vacío, error y sin conexión.

**Fuera de alcance:** ranking o modelos en el dispositivo (todo el orden lo decide el backend); mezclar ofertas con publicaciones de Comunidad; acceso de invitados al feed; envío de telemetría desde el cliente (el backend registra impresiones, aperturas y apartados por su cuenta, spec 055).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-30-01 | Tras completar el registro, mostrar `InterestsPickerScreen` con las 9 categorías de producto del catálogo `OfferCategory` (las mismas de `StoreCategory` salvo `ALL`), selección de 0 a 5 y botones `Guardar` y `Omitir`. `Guardar` invoca `PUT /api/v1/users/me/interests`; `Omitir` no llama al backend. |
| RF-30-02 | En `BuyerProfileScreen`, mostrar los intereses actuales (`GET /api/v1/users/me/interests`) y permitir editarlos con el mismo selector. |
| RF-30-03 | Añadir la pestaña `Promociones`, separada de `Comunidad`, que carga `GET /api/v1/feed/offers` con `lat`, `lng`, `radiusKm`, `page` y `pageSize = 20`. |
| RF-30-04 | Guardar el `snapshotId` de la página 1 y enviarlo en las páginas siguientes; al refrescar (*pull to refresh*) o al cambiar de ubicación de forma significativa (> 500 m), descartarlo y pedir la página 1. |
| RF-30-05 | Si la respuesta trae `snapshotId` nulo, seguir paginando y eliminar en el cliente ofertas repetidas por `offerId`. |
| RF-30-06 | En sesión de invitado, la pestaña `Promociones` muestra `GuestPromptDialog` con CTA de registro y no llama al endpoint; ante un `403` se aplica el mismo comportamiento. |
| RF-30-07 | Al tocar una oferta del feed, abrir `ProductDetailScreen` con el flujo existente (FT-12), que registra la vista en el backend. |
| RF-30-08 | Estado vacío con icono vectorial y CTA `Elegir intereses` cuando no hay ofertas ni intereses, o `Ampliar radio` cuando no hay ofertas cercanas. |

## 3. Requisitos no funcionales

- **Rendimiento:** paginación incremental con Paging 3 o equivalente; prefetch a 5 elementos del final; sin recargar páginas ya mostradas.
- **Seguridad (constitución v1.3.0):** sin PII ni coordenadas exactas en logs; las coordenadas viajan solo en la consulta HTTPS con *pinning*; los intereses no se guardan en texto plano fuera de Room + SQLCipher.
- **Offline-First:** la última página 1 recibida se conserva cifrada en Room y se muestra con aviso de «sin conexión»; los intereses editados sin red se reintentan al recuperar conexión o se informa el fallo.
- **Design System:** paleta morada, sin ámbar ni emojis, iconos vectoriales y *touch targets* ≥ 48 dp.
- **Cobertura:** ≥ 80 % en casos de uso, repositorios y ViewModels (Kover).

## 4. Arquitectura y contratos internos

```text
domain/
  model/OfferFeedModels.kt        # OfferFeedItem, OfferFeedPage, FeedSlot, InterestCategory
  repository/OfferFeedRepository.kt, InterestsRepository.kt
  usecase/GetOfferFeedUseCase.kt, GetMyInterestsUseCase.kt, UpdateMyInterestsUseCase.kt
data/
  remote/api/OfferFeedApiService.kt   # GET /api/v1/feed/offers
  remote/api/InterestsApiService.kt   # GET, PUT /api/v1/users/me/interests
  local/db/entity/FeedOfferEntity.java, dao/FeedOfferDao.java   # caché cifrada de la página 1
  repository/OfferFeedRepositoryImpl.kt, InterestsRepositoryImpl.kt
presentation/buyer/
  OfferFeedContract.kt, OfferFeedViewModel.kt, OfferFeedScreen.kt
  InterestsPickerContract.kt, InterestsPickerViewModel.kt, InterestsPickerScreen.kt
  BuyerProfileScreen.kt (sección Mis intereses)
```

Endpoints consumidos: `GET /api/v1/feed/offers`, `GET /api/v1/users/me/interests` y `PUT /api/v1/users/me/interests`.

## 5. Criterios de aceptacion

- **Given** un usuario recién registrado **When** termina el registro **Then** ve `InterestsPickerScreen`; si pulsa `Omitir` llega al inicio sin llamar a `PUT /api/v1/users/me/interests`.
- **Given** el usuario elige `Panadería` y `Lácteos` **When** pulsa `Guardar` **Then** se envía `PUT /api/v1/users/me/interests` con `["Bakery","Dairy"]` y el perfil las muestra.
- **Given** un usuario registrado en la pestaña `Promociones` **When** hace *scroll* hasta el final de la página 1 **Then** se pide la página 2 con el mismo `snapshotId` y no se repite ninguna oferta.
- **Given** un invitado **When** abre `Promociones` **Then** ve `GuestPromptDialog` y no se llama a `GET /api/v1/feed/offers`.
- **Given** sin conexión **When** abre `Promociones` **Then** ve la última página 1 guardada con aviso de sin conexión.

## 6. Plan de pruebas

- **Unitarias:** `GetOfferFeedUseCaseTest` (reinicio de `snapshotId` al refrescar o moverse > 500 m, deduplicación con `snapshotId` nulo), `UpdateMyInterestsUseCaseTest` (0–5 categorías, sin `Otros`).
- **ViewModel:** `OfferFeedViewModelTest` e `InterestsPickerViewModelTest` con Turbine (carga, paginación, vacío, error, invitado).
- **Integración (`MockWebServer` + Room):** contratos de los tres endpoints, `403` de invitado y caché cifrada de la página 1.
- **UI (Compose Test Rule):** pestaña `Promociones`, selector de intereses y estado vacío.

## 7. Riesgos, limites y deuda

- Depende de las specs backend 042, 054 y 055 (pendientes). Hasta entonces se implementa contra `MockWebServer` y la pestaña queda tras un *feature flag*.
- La lista de categorías replica `OfferCategory` (spec backend 034) mediante `StoreCategory.apiCode` (T15.15); si el backend añade una, hay que actualizar el enum y sus textos.
- La personalización y el registro de interacciones solo se informan en el aviso de privacidad (decisión del 2026-11-20); no hay ajuste para desactivarlos.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Aprobada (alcance) | Rodrigo Sánchez | 2026-11-20 | Feed separado del social, intereses opcionales al registrarse, sin invitados, ruta `/api/v1/feed/offers` |
