# SPEC — FT-17 Favoritos y Guardados de Ofertas

> Rector: `.specify/memory/constitution.md` §2, §5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-036**; Pantalla **P-23**; Componentes **C-36, C-37**.
> Backend ref: Spec `038-favoritos`.

## 1. Contexto y objetivo

Actualmente el detalle de oferta (`ProductDetailScreen`) permite guardar ofertas mediante el flujo local de **Intereses guardados** (`SavedOffersRepository` / `SavedInterestsViewModel`), usando icono de pin y persistencia local en `user_preferences` (SQLCipher). Este flujo no sincroniza con backend, no expone la pantalla `P-23 Favoritos` definida en Drive y no cubre el contrato de favoritos de Spec 038.

**Objetivo:** implementar **Favoritos de ofertas** como capacidad backend-synced para compradores autenticados: alternancia real contra `POST /api/v1/users/favorites/{offerId}` con actualización optimista y rollback, persistencia offline en Room + SQLCipher, y pantalla `Favoritos` (`P-23`) con filtros `Todos`, `Cerca de mí` y `Por agotarse`.

**Alcance:** modelo de dominio de favoritos, `FavoritesRepository`, `ToggleFavoriteUseCase`, `GetFavoritesUseCase`, `ObserveFavoritesUseCase`/flujo reactivo, `FavoriteEntity`/`FavoriteDao` en SQLCipher, `FavoritesApiService`, `FavoritesScreen` (`P-23`) con chips `C-37` y estado vacío, integración del corazón de favorito en `ProductDetailScreen` y tarjetas de oferta donde aplique, entrada de navegación desde perfil/comunidad, y reconciliación del guardado de ofertas actual en detalle.

**Fuera de alcance:** entrenamiento ML en móvil; favoritos de publicaciones/comercios; cambios al feed de Promociones por intereses (FT-30) salvo la convivencia explícita definida abajo; listado remoto `GET /api/v1/users/favorites` como dependencia dura si backend no lo expone (se deja contrato propuesto y sincronización diferida).

### Decisión de producto/arquitectura

- **Favoritos (FT-17)**: ofertas, comprador autenticado, sincronizados con backend, pantalla P-23.
- **Intereses guardados (existente/FT-30)**: ofertas y publicaciones, persistencia local, usados por flujos de intereses/feed. FT-17 **no elimina** SavedInterests para publicaciones; para ofertas, el control visible de “guardar” en detalle pasa a semántica de **Favorito** backend-synced. La implementación debe evitar dos acciones equivalentes visibles para la misma oferta. Cualquier reutilización de `SavedOffersRepository` debe quedar documentada en `plan.md` y no romper publicaciones guardadas.

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-17-01 | Al pulsar favorito en tarjeta o `ProductDetailScreen`, si el usuario es invitado se muestra `GuestPromptDialog` y no se invoca endpoint; si está autenticado se invoca `ToggleFavoriteUseCase`. |
| RF-17-02 | `ToggleFavoriteUseCase` aplica actualización optimista en estado local/Room, envía `POST /api/v1/users/favorites/{offerId}` y revierte ante error no recuperable (`401`, `404` oferta no visible, timeout/red sin confirmación según política definida, o error de servidor). |
| RF-17-03 | `FavoritesScreen` (`P-23`) muestra favoritos del comprador con foto, nombre, comercio, estado abierto/cerrado cuando esté disponible, precio y badge dinámico: `Disponible`, `Quedan N` si `1 <= stock <= 3`, `Agotado` si `stock == 0`. |
| RF-17-04 | Chips `C-37`: `Todos`, `Cerca de mí` y `Por agotarse`. `Por agotarse` filtra `1 <= stock <= 3`. `Cerca de mí` filtra por distancia dentro del radio efectivo del comprador (FT-07/radio configurado) cuando hay ubicación y distancia calculable; si no hay ubicación, la UI muestra estado informativo y no finge resultados. |
| RF-17-05 | La fuente local es Room + SQLCipher (`FavoriteDao`). Si backend expone `GET /api/v1/users/favorites`, se sincroniza al entrar a P-23/refrescar; si no, el comportamiento offline-first local sigue operativo y el contrato queda documentado como propuesto en docs vivos. |
| RF-17-06 | La acción de favorito en `ProductDetailScreen` reemplaza el guardado local de oferta actualmente cableado a `SavedInterestsViewModel.ToggleOffer`, manteniendo una sola semántica visible para ofertas. |

## 3. Requisitos no funcionales

- **Offline-First:** favoritos visibles sin red desde Room + SQLCipher. Los cambios sin red no deben perder coherencia local; si no se puede confirmar con backend, la UI informa el estado y aplica la política de rollback/reintento definida en plan.
- **Seguridad:** sin PII, JWT, payloads sensibles ni coordenadas exactas en logs. Datos locales cifrados con SQLCipher. Cumplir §9 de la constitución.
- **Tasa/UX:** control de doble toque/debounce en UI; respetar límite backend de alternancias (objetivo: 120/hora por usuario, a conciliar con Spec 038 y `docs/rate-matrix.md` antes de cerrar).
- **Rendimiento:** lectura de favoritos desde Room; filtros locales sin bloquear UI; consultas en `Dispatchers.IO`.
- **Accesibilidad/DS:** estado vacío sin emojis, icono vectorial, CTA hacia explorar ofertas, touch targets >= 48dp, paleta morada de marca; sin ámbar.
- **Cobertura:** >= 80% en UseCases, repositorios y ViewModels afectados; pruebas de integración y UI según plan.

## 4. Arquitectura y contratos internos

```text
domain/
  model/FavoriteFilter.kt                  # ALL, NEARBY, LOW_STOCK
  model/FavoriteOffer.kt                   # modelo de dominio para P-23 (o mapeo equivalente)
  repository/FavoritesRepository.kt
  usecase/ToggleFavoriteUseCase.kt
  usecase/GetFavoritesUseCase.kt
  usecase/ObserveFavoritesUseCase.kt       # si se separa observación reactiva

data/
  local/db/entity/FavoriteEntity.java
  local/db/dao/FavoriteDao.java
  remote/api/FavoritesApiService.kt
  remote/dto/FavoriteDtos.kt               # si aplica
  repository/FavoritesRepositoryImpl.kt

presentation/buyer/
  FavoritesContract.kt
  FavoritesViewModel.kt
  FavoritesScreen.kt

di/
  DatabaseModule.kt                        # provee FavoriteDao
  NetworkModule.kt / módulo API equivalente
  RepositoryModule.kt / módulo equivalente
```

**Contratos HTTP:**

| Endpoint | Uso | Auth | Estado |
|---|---|---|---|
| `POST /api/v1/users/favorites/{offerId}` | Alternar favorito | Bearer | Requerido (Spec 038) |
| `GET /api/v1/users/favorites` | Listar/sincronizar favoritos | Bearer | Propuesto / confirmar backend |

**Integraciones existentes a tocar:** `SensusDatabase` (v8→v9), `DatabaseModule`, `ProductDetailScreen/Components`, `BuyerHomeScreen` o tarjetas de oferta, `BuyerProfileScreen`/navegación, y reconciliación con `SavedInterests`/`SavedOffersRepository` solo donde corresponda.

## 5. Criterios de aceptacion

- **Given** un comprador autenticado pulsa corazón en `ProductDetailScreen` **When** el backend responde éxito en `POST /api/v1/users/favorites/{offerId}` **Then** el icono queda activo y la oferta aparece en `FavoritesScreen`.
- **Given** un usuario invitado pulsa corazón **When** intenta guardar favorito **Then** aparece `GuestPromptDialog` y no se llama al endpoint.
- **Given** una oferta favorita con `stock = 2` **When** filtra por `Por agotarse` **Then** se muestra con badge `Quedan 2`.
- **Given** el servidor responde `404` o error no recuperable al alternar **When** falla la operación **Then** se revierte el estado optimista y se muestra aviso no sensible.
- **Given** el usuario no tiene favoritos **When** abre `FavoritesScreen` **Then** ve estado vacío con icono vectorial y CTA a explorar ofertas.
- **Given** no hay conectividad **When** abre `FavoritesScreen` con favoritos persistidos localmente **Then** ve sus favoritos desde Room + SQLCipher.

## 6. Plan de pruebas

- **Unitarias (JUnit4, MockK):** `ToggleFavoriteUseCaseTest` (invitado bloqueado, éxito, rollback por 404/error, debounce/regla de tasa si aplica), `GetFavoritesUseCaseTest` (filtros `ALL`, `NEARBY`, `LOW_STOCK`, sin ubicación).
- **Repositorio:** `FavoritesRepositoryImplTest` con actualización optimista, rollback y persistencia Room.
- **ViewModel (Turbine):** `FavoritesViewModelTest` para carga, filtros, vacío, error y refresh.
- **Integración:** `MockWebServer` para `POST /api/v1/users/favorites/{offerId}`; prueba Room/SQLCipher para `FavoriteDao` y migración v8→v9.
- **UI (Compose Test Rule):** `FavoritesScreen` chips `C-37`, estado vacío, badge de stock; `ProductDetailScreen`/tarjeta muestra estado favorito y abre `GuestPromptDialog` en invitado.

## 7. Riesgos, limites y deuda

- **Solapamiento con Intereses guardados:** existe `SavedOffersRepository`/`SavedInterestsScreen`. FT-17 debe evitar duplicar UX para ofertas; la reconciliación se detalla en `plan.md`.
- **Contrato backend parcial:** Spec 038 define toggle; listado remoto puede no estar disponible. P-23 debe funcionar offline-first con Room.
- **Distancia “Cerca de mí”:** depende de ubicación/radio disponible. Sin ubicación precisa, el filtro no debe producir falsos positivos.
- **Migración DB:** agregar `FavoriteEntity` requiere migración v8→v9 y prueba de migración.
- **Dependencia cruzada:** FT-20 depende de FT-17; cualquier contrato de favoritos consumido por FT-20 debe quedar estable y documentado.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | 2026-11-23 | Spec revisada y alineada con código actual; pendiente decisión de ejecución/implementación |
