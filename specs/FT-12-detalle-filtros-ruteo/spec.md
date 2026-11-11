# SPEC — FT-12 Detalle de Oferta, Contador de Vistas, Filtros y Ruteo GPS

> Rector: `.specify/memory/constitution.md` §2, §5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-015, SE-016, SE-017, SE-018, SE-045 (vista consumidor)**; Pantallas **P-14, P-19**; Componentes **C-16..C-18, C-21..C-25, C-35**.
> Backend ref: Spec `032-detalle-oferta-y-vistas`.

## 1. Contexto y objetivo

La UI de `ProductDetailScreen` y `RouteMapScreen` junto con `NavigationIntentUtils` y el filtrado local por categoría/distancia ya fueron desarrollados visualmente, pero los detalles completos provienen de objetos en memoria y **no** registran la apertura real de la oferta en el servidor ni consultan `GET /api/v1/offers/{id}`.

**Objetivo:** formalizar la retro-especificación de `SE-015..SE-018` y conectar la pantalla de detalle con el backend mediante `GET /api/v1/offers/{id}` y `POST /api/v1/offers/{id}/view` (al abrir detalle, con *debounce*/idempotencia de sesión), manteniendo filtros avanzados y ruteo externo a Google Maps / navegador.

**Alcance:** `ProductDetailViewModel`, `GetOfferDetailUseCase`, `RegisterOfferViewUseCase`, actualización de `OffersApiService`, chips de filtro extendidos (categoría, precio máximo, descuento mínimo, `Abierto ahora`, orden por distancia/descuento/precio) y estado `Oferta agotada` (`P-17`).
**Fuera de alcance:** apartado QR (FT-13), favoritos (FT-17), reportes (FT-19), perfil completo de comercio y reseñas (FT-26).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-12-01 | Al abrir `ProductDetailScreen`, consultar `GET /api/v1/offers/{id}` para obtener stock actualizado, fecha límite exacta, descripción completa, formas de pago y horario del comercio (`Abierto ahora` / `Cerrado ahora`), usando caché local si no hay red. |
| RF-12-02 | Enviar asíncronamente `POST /api/v1/offers/{id}/view` al entrar al detalle (máximo una vez por sesión/oferta o *debounce* para no inflar métricas), tolerando fallos de red silenciosamente. |
| RF-12-03 | Mostrar aviso de pocas unidades (`"Solo quedan N unidades"`) cuando `1 <= stock <= 3` y mostrar estado `Oferta agotada` (`P-17`) deshabilitando el botón de apartar cuando `stock == 0` o esté vencida. |
| RF-12-04 | Soportar barra de filtros por categorías, precio máximo, descuento mínimo, solo `Abierto ahora` y ordenamiento (`distancia`, `descuento`, `precio`) tanto en backend como sobre caché local. |
| RF-12-05 | Botón `"Cómo llegar"` (`SE-017`) abre Intent explícito a Google Maps con coordenadas del comercio y fallback a navegador HTTPS o `RouteMapScreen` interno. |

## 3. Requisitos no funcionales

- **UX:** la vista de detalle debe renderizarse de inmediato con los datos de resumen previos mientras refresca el detalle completo desde red sin parpadeos.
- **Resiliencia:** el fallo de `POST /api/v1/offers/{id}/view` nunca debe mostrar error bloqueante al comprador.
- **Accesibilidad:** todos los botones, chips de ahorro e iconos de pago cuentan con `contentDescription` y contraste AA.

## 4. Arquitectura y contratos internos

```text
domain/
  model/StoreModels.kt            # OfferDetail (extiende info de horario, pagos, vigencia exacta)
  repository/OffersRepository.kt  # getOfferDetail(id), registerOfferView(id)
  usecase/GetOfferDetailUseCase.kt
  usecase/RegisterOfferViewUseCase.kt
data/
  remote/api/OffersApiService.kt  # GET /api/v1/offers/{id}, POST /api/v1/offers/{id}/view
  remote/dto/OfferDtos.kt         # OfferDetailResponseDto
presentation/buyer/
  ProductDetailContract.kt, ProductDetailViewModel.kt, ProductDetailScreen.kt
  RouteMapScreen.kt, RouteMapViewModel.kt
```

## 5. Criterios de aceptacion

- **Given** un comprador toca una tarjeta de oferta **When** entra a `ProductDetailScreen` **Then** se invoca `GET /api/v1/offers/{id}` y `POST /api/v1/offers/{id}/view` y se muestra stock, vigencia, horarios y métodos de pago reales.
- **Given** una oferta con `stock = 2` **When** se abre su detalle **Then** muestra chip `"Solo quedan 2 unidades"`.
- **Given** una oferta con `stock = 0` **When** se abre su detalle **Then** muestra estado agotado (`P-17`) con botón de apartar deshabilitado.
- **Given** el comprador pulsa `"Cómo llegar"` **When** no existe la app de Google Maps instalada **Then** abre la ruta en navegador web sin lanzar `ActivityNotFoundException`.

## 6. Plan de pruebas

- **Unitarias:** `GetOfferDetailUseCaseTest`, `RegisterOfferViewUseCaseTest`, lógica de filtros y cálculo de descuento/ahorro.
- **ViewModel:** `ProductDetailViewModelTest` con Turbine (carga inicial, refresco de stock, estado agotado, cantidad respetando stock).
- **Integración (`MockWebServer`):** `GET /api/v1/offers/{id}` (200 y 404), `POST /api/v1/offers/{id}/view` (204 / timeout tolerado).
- **UI (Compose Test Rule):** visualización de badge pocas unidades, estado `stock = 0`, selector de filtro y botón `"Cómo llegar"`.

## 7. Riesgos, limites y deuda

- Depende de la publicación de `GET /api/v1/offers/{id}` y `POST /api/v1/offers/{id}/view` (Spec 032 del backend).
- Mientras no exista pantalla completa `P-15` (FT-26), tocar la fila del comercio muestra un resumen en el mismo detalle o navega al mapa del comercio.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
