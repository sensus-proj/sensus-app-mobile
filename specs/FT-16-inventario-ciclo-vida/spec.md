# SPEC — FT-16 Inventario del Comerciante, Edición, Ciclo de Vida y Métricas

> Rector: `.specify/memory/constitution.md` §2, §5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-023, SE-024, SE-025, SE-026, SE-045**; Pantallas **P-30, P-31**; Componente **C-47**.
> Backend ref: Specs `035-edicion-oferta`, `036-ciclo-vida-oferta`, `037-inventario-comerciante`.

## 1. Contexto y objetivo

`MerchantDashboardScreen` y `MerchantDashboardViewModel` actualmente muestran `StoreRepository.getSampleStore()` y las primeras 3 ofertas locales de `OffersRepository`, sin paginación por estado, sin edición de ofertas existentes, sin pausar/reanudar/republicar ni métricas de vistas por oferta.

**Objetivo:** implementar la gestión completa del inventario del comercio conectada a `GET /api/v1/merchants/my-offers`, `PUT /api/v1/offers/{id}` y `PUT /api/v1/offers/{id}/status`, permitiendo filtrar historial por estado/periodo, editar precios/stock/vigencia, pausar manualmente, republicar ofertas vencidas con formulario precargado y consultar vistas y canjes.

**Alcance:** `GetMerchantInventoryUseCase`, `UpdateOfferUseCase`, `ChangeOfferStatusUseCase`, pantallas/hojas de edición e historial (`P-30`, `P-31`), precarga rápida para republicar ofertas pasadas (`SE-026`) y retiro de datos simulados en `MerchantDashboardViewModel`.
**Fuera de alcance:** alta de ofertas nuevas desde cero (FT-15), escaneo de QR (FT-14).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-16-01 | `GET /api/v1/merchants/my-offers` devuelve lista paginada del inventario del comercio filtrable por estado (`ACTIVE`, `PAUSED`, `EXPIRED`, `SOLD_OUT`) y periodo. |
| RF-16-02 | Mostrar en cada tarjeta de inventario del panel (`P-30`, `C-47`) stock actual, vistas recibidas (`SE-045`), canjes realizados y estado calculado. |
| RF-16-03 | Permitir abrir cualquier oferta en pantalla de edición (`P-31`) y guardar cambios de descripción, precios, stock y fecha límite mediante `PUT /api/v1/offers/{id}` con validación de dominio. |
| RF-16-04 | Soportar pausar o reanudar manualmente una oferta con `PUT /api/v1/offers/{id}/status` actualizando su visibilidad pública de inmediato. |
| RF-16-05 | Incluir acción `"Republicar"` en ofertas finalizadas o agotadas del historial (`SE-026`) que precargue título, categoría, foto y descripción solicitando nuevo precio, stock y fecha futura antes de guardar. |

## 3. Requisitos no funcionales

- **Consistencia:** tras editar o cambiar estado, invalidar/actualizar el registro correspondiente en `OfferDao`.
- **Concurrencia:** confirmar antes de retirar/pausar si el comercio indica acción destructiva; impedir guardar precios incoherentes.
- **Paginación:** 20 ítems por defecto, 100 máximo en historial de inventario.

## 4. Arquitectura y contratos internos

```text
domain/
  model/OfferLifecycleStatus.kt  # ACTIVE, PAUSED, EXPIRED, SOLD_OUT
  model/MerchantOfferMetrics.kt  # offer, viewsCount, claimsCount, redeemedCount
  repository/OffersRepository.kt # getMyOffers, updateOffer, updateOfferStatus
  usecase/GetMerchantInventoryUseCase.kt, UpdateOfferUseCase.kt, ChangeOfferStatusUseCase.kt
data/
  remote/api/MerchantApiService.kt # GET /api/v1/merchants/my-offers
  remote/api/OffersApiService.kt   # PUT /api/v1/offers/{id}, PUT /api/v1/offers/{id}/status
presentation/merchant/
  MerchantDashboardContract.kt, MerchantDashboardViewModel.kt, MerchantDashboardScreen.kt
  MerchantEditOfferScreen.kt, MerchantEditOfferViewModel.kt, MerchantEditOfferContract.kt # P-31
```

## 5. Criterios de aceptacion

- **Given** un comerciante abre su panel **When** consulta el inventario **Then** visualiza ofertas activas e inactivas paginadas desde `GET /api/v1/merchants/my-offers` con stock y contador de vistas.
- **Given** el comerciante edita stock o precio de una oferta **When** guarda **Then** se valida en dominio, se envía `PUT /api/v1/offers/{id}` y se refleja en lista y `OfferDao`.
- **Given** el comerciante pulsa Pausar en una oferta activa **When** confirma **Then** envía `PUT /api/v1/offers/{id}/status` y cambia a `PAUSED`.
- **Given** una oferta expirada en historial **When** pulsa Republicar **Then** abre el formulario precargado exigiendo nueva vigencia y stock `>= 1`.

## 6. Plan de pruebas

- **Unitarias:** `UpdateOfferUseCaseTest`, `ChangeOfferStatusUseCaseTest`, `GetMerchantInventoryUseCaseTest`.
- **ViewModel:** `MerchantDashboardViewModelTest` y `MerchantEditOfferViewModelTest` con Turbine.
- **Integración (`MockWebServer`):** `GET /api/v1/merchants/my-offers`, `PUT /api/v1/offers/{id}`, `PUT /api/v1/offers/{id}/status`.
- **UI (Compose Test Rule):** filtros de historial activas/inactivas, botón Pausar/Reanudar y flujo Republicar.

## 7. Riesgos, limites y deuda

- Depende de las specs 035, 036 y 037 en `sensus-back`.
- Si el backend aún no consolida `viewsCount` y `redeemedCount` en el DTO de `my-offers`, el modelo de UI degradará de forma segura mostrando `0` hasta que esté disponible.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
