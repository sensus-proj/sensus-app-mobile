# SPEC — FT-20 Búsqueda Inteligente, Recomendaciones y Alternativas ML

> Rector: `.specify/memory/constitution.md` §2, §3, §5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-034, SE-035, SE-037, SE-038**; Pantallas **P-12, P-13**; Componentes **C-06, C-34**.
> Backend ref: Specs `041-busqueda-inteligente` y `043-alternativas-oferta` (v1.1.0).
> **Rutas (2026-11-20):** `GET /api/v1/offers/search` y `GET /api/v1/offers/{id}/alternatives` sustituyen a las rutas `/ml/*`. Los invitados usan ambas con radio recortado a 2 km en el servidor.
> **Cambio de alcance (2026-11-20):** el carrusel «Recomendados para ti» se retira de esta feature. Las recomendaciones pasan a ser el feed de promociones de **FT-30** (`GET /api/v1/feed/offers`, spec backend 042 v2.0.0).

## 1. Contexto y objetivo

La barra de búsqueda de `BuyerHomeScreen` actualmente solo ejecuta un filtro `.contains(searchQuery)` en memoria local sobre las ofertas de muestra, sin autocompletado, sin pantalla/estado de alternativas cuando la búsqueda o el stock no arrojan coincidencias.

**Objetivo:** conectar el cliente móvil con los endpoints del motor de búsqueda y recomendación del backend (`GET /api/v1/offers/search` y `GET /api/v1/offers/{id}/alternatives`) manteniendo la inferencia 100 % delegada en el servidor (decisión D-5) y añadiendo historial local cifrado de búsquedas recientes en Room.

**Alcance:** `SearchHistoryEntity` / `SearchHistoryDao`, `RecommendationsApiService`, `SearchOffersUseCase` (con *debounce* `>= 300 ms`), `GetOfferAlternativesUseCase`, pantalla dedicada de Búsqueda y Filtros (`P-12`) con estado Sin Resultados y Sustitutos (`P-13`).
**Fuera de alcance:** modelos ML/ONNX ejecutándose localmente en Android; chat conversacional Sensi IA (FT-24); feed de promociones y recomendaciones personalizadas (FT-30); telemetría desde el cliente (el backend registra interacciones, spec 055).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-20-01 | Al escribir `>= 2` caracteres en la barra de búsqueda (`C-06`, `P-12`), ejecutar búsqueda paginada en `GET /api/v1/offers/search` con *debounce* de `300 ms`, enviando `q`, `lat`, `lng`, `radiusMeters` y filtros activos. |
| RF-20-02 | En usuarios autenticados, guardar las últimas 5 búsquedas en `SearchHistoryDao` (Room + SQLCipher) y permitir borrar el historial; en invitados no persistir historial. |
| RF-20-03 | *Retirado (2026-11-20):* las recomendaciones personalizadas se entregan en el feed de promociones de FT-30. |
| RF-20-04 | Cuando `GET /api/v1/offers/search` devuelva 0 resultados o al abrir una oferta agotada (`stock == 0`), consultar `GET /api/v1/offers/{id}/alternatives` y renderizar la vista `P-13` con mensaje `'No encontramos X, pero te recomendamos estas ofertas similares cerca de ti'`. |

## 3. Requisitos no funcionales

- **Rendimiento:** el *debounce* de `300 ms` y la cancelación automática de corrutinas previas (`flatMapLatest`) evitan saturar la API al teclear rápido.
- **Resiliencia:** si el endpoint ML tarda o devuelve 5xx, degradar limpiamente al filtrado sobre `OfferDao` local sin bloquear la exploración del usuario.
- **Privacidad:** sin almacenamiento de historial de búsqueda para sesiones invitadas.

## 4. Arquitectura y contratos internos

```text
domain/
  model/DiscoveryModels.kt      # RecommendationSection, SearchSuggestion
  repository/DiscoveryRepository.kt
  usecase/SearchOffersUseCase.kt, GetOfferAlternativesUseCase.kt
data/
  local/db/entity/SearchHistoryEntity.java, dao/SearchHistoryDao.java
  remote/api/DiscoveryApiService.kt # GET /api/v1/offers/search, /recommendations, /alternatives
  repository/DiscoveryRepositoryImpl.kt
presentation/buyer/
  SearchOffersContract.kt, SearchOffersViewModel.kt, SearchOffersScreen.kt # P-12 & P-13
  BuyerHomeScreen.kt (carrusel Recomendados)
```

## 5. Criterios de aceptacion

- **Given** el usuario teclea `"refresco"` en `SearchOffersScreen` **When** transcurren `300 ms` **Then** consulta `GET /api/v1/offers/search` con parámetro `q=refresco` y lista coincidencias cercanas.
- **Given** la búsqueda devuelve lista vacía **When** consulta `GET /api/v1/offers/{id}/alternatives` **Then** muestra `P-13` con ofertas sustitutas cercanas de la misma categoría.
- **Given** el usuario navega en modo invitado **When** realiza una búsqueda **Then** no persiste historial en `SearchHistoryDao` y respeta `radiusMeters = 2000`.

## 6. Plan de pruebas

- **Unitarias:** `SearchOffersUseCaseTest` (longitud mínima, normalización 2000 m invitado) y `GetOfferAlternativesUseCaseTest`.
- **ViewModel:** `SearchOffersViewModelTest` con Turbine y `advanceTimeBy` verificando *debounce* y transición a estado `P-13` con alternativas.
- **Integración (`MockWebServer` + `SearchHistoryDao`):** `GET /api/v1/offers/search`, `/offers/{id}/alternatives` y persistencia/borrado de últimas 5 búsquedas.
- **UI (Compose Test Rule):** pantalla `SearchOffersScreen` (`P-12`), estado vacío con sustitutos (`P-13`).

## 7. Riesgos, limites y deuda

- Depende de las specs 041 y 043 en `sensus-back` (implementación determinista ADR 0009 en backend).
- El autocompletado rápido usa los resultados iniciales de `offers/search` junto al historial local de `SearchHistoryDao` hasta verificar si el backend añade endpoint dedicado de sugerencias.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
