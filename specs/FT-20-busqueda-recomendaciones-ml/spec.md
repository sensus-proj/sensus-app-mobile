# SPEC — FT-20 Búsqueda Inteligente, Recomendaciones y Alternativas ML

> Rector: `.specify/memory/constitution.md` §2, §3, §5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-034, SE-035, SE-037, SE-038**; Pantallas **P-12, P-13**; Componentes **C-06, C-34**.
> Backend ref: Specs `041-busqueda-inteligente`, `042-recomendaciones`, `043-alternativas-oferta`.

## 1. Contexto y objetivo

La barra de búsqueda de `BuyerHomeScreen` actualmente solo ejecuta un filtro `.contains(searchQuery)` en memoria local sobre las ofertas de muestra, sin autocompletado, sin carrusel `'Recomendados para ti'`, sin agrupación por categorías preferidas y sin pantalla/estado de alternativas cuando la búsqueda o el stock no arrojan coincidencias.

**Objetivo:** conectar el cliente móvil con los endpoints del motor de búsqueda y recomendación del backend (`GET /api/v1/ml/search`, `GET /api/v1/ml/recommendations`, `GET /api/v1/ml/alternatives`) manteniendo la inferencia 100 % delegada en el servidor (decisión D-5) y añadiendo historial local cifrado de búsquedas recientes en Room.

**Alcance:** `SearchHistoryEntity` / `SearchHistoryDao`, `RecommendationsApiService`, `SearchOffersUseCase` (con *debounce* `>= 300 ms`), `GetRecommendationsUseCase`, `GetOfferAlternativesUseCase`, pantalla dedicada de Búsqueda y Filtros (`P-12`) con estado Sin Resultados y Sustitutos (`P-13`) y carrusel `'Recomendados para ti'` en inicio.
**Fuera de alcance:** modelos ML/ONNX ejecutándose localmente en Android; chat conversacional Sensi IA (FT-24).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-20-01 | Al escribir `>= 2` caracteres en la barra de búsqueda (`C-06`, `P-12`), ejecutar búsqueda paginada en `GET /api/v1/ml/search` con *debounce* de `300 ms`, enviando `q`, `lat`, `lng`, `radiusMeters` y filtros activos. |
| RF-20-02 | En usuarios autenticados, guardar las últimas 5 búsquedas en `SearchHistoryDao` (Room + SQLCipher) y permitir borrar el historial; en invitados no persistir historial. |
| RF-20-03 | En la pantalla de inicio (`BuyerHomeScreen`), mostrar sección `'Recomendados para ti'` y agrupación prioritaria según afinidad obtenida desde `GET /api/v1/ml/recommendations` (fallback a orden por cercanía si es invitado o falla el motor). |
| RF-20-04 | Cuando `GET /api/v1/ml/search` devuelva 0 resultados o al abrir una oferta agotada (`stock == 0`), consultar `GET /api/v1/ml/alternatives` y renderizar la vista `P-13` con mensaje `'No encontramos X, pero te recomendamos estas ofertas similares cerca de ti'`. |

## 3. Requisitos no funcionales

- **Rendimiento:** el *debounce* de `300 ms` y la cancelación automática de corrutinas previas (`flatMapLatest`) evitan saturar la API al teclear rápido.
- **Resiliencia:** si el endpoint ML tarda o devuelve 5xx, degradar limpiamente al filtrado sobre `OfferDao` local sin bloquear la exploración del usuario.
- **Privacidad:** sin almacenamiento de historial de búsqueda para sesiones invitadas.

## 4. Arquitectura y contratos internos

```text
domain/
  model/DiscoveryModels.kt      # RecommendationSection, SearchSuggestion
  repository/DiscoveryRepository.kt
  usecase/SearchOffersUseCase.kt, GetRecommendationsUseCase.kt, GetOfferAlternativesUseCase.kt
data/
  local/db/entity/SearchHistoryEntity.java, dao/SearchHistoryDao.java
  remote/api/DiscoveryApiService.kt # GET /api/v1/ml/search, /recommendations, /alternatives
  repository/DiscoveryRepositoryImpl.kt
presentation/buyer/
  SearchOffersContract.kt, SearchOffersViewModel.kt, SearchOffersScreen.kt # P-12 & P-13
  BuyerHomeScreen.kt (carrusel Recomendados)
```

## 5. Criterios de aceptacion

- **Given** el usuario teclea `"refresco"` en `SearchOffersScreen` **When** transcurren `300 ms` **Then** consulta `GET /api/v1/ml/search` con parámetro `q=refresco` y lista coincidencias cercanas.
- **Given** la búsqueda devuelve lista vacía **When** consulta `GET /api/v1/ml/alternatives` **Then** muestra `P-13` con ofertas sustitutas cercanas de la misma categoría.
- **Given** un usuario autenticado abre `BuyerHomeScreen` **When** hay historial de favoritos/vistas **Then** visualiza el carrusel `'Recomendados para ti'` desde `GET /api/v1/ml/recommendations`.
- **Given** el usuario navega en modo invitado **When** realiza una búsqueda **Then** no persiste historial en `SearchHistoryDao` y respeta `radiusMeters = 2000`.

## 6. Plan de pruebas

- **Unitarias:** `SearchOffersUseCaseTest` (longitud mínima, normalización 2000 m invitado), `GetRecommendationsUseCaseTest`, `GetOfferAlternativesUseCaseTest`.
- **ViewModel:** `SearchOffersViewModelTest` con Turbine y `advanceTimeBy` verificando *debounce* y transición a estado `P-13` con alternativas.
- **Integración (`MockWebServer` + `SearchHistoryDao`):** `GET /api/v1/ml/search`, `/recommendations`, `/alternatives` y persistencia/borrado de últimas 5 búsquedas.
- **UI (Compose Test Rule):** pantalla `SearchOffersScreen` (`P-12`), estado vacío con sustitutos (`P-13`) y carrusel de recomendaciones.

## 7. Riesgos, limites y deuda

- Depende de las specs 041, 042 y 043 en `sensus-back` (implementación determinista ADR 0009 en backend).
- El autocompletado rápido usa los resultados iniciales de `ml/search` junto al historial local de `SearchHistoryDao` hasta verificar si el backend añade endpoint dedicado de sugerencias.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
