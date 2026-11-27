# TASKS — FT-16 Inventario del Comerciante, Edición, Ciclo de Vida y Métricas

> 1 PR = 1 slice. Orden: Dominio -> Datos -> ViewModel/UI -> DI -> Tests -> Gates.

## Fase A — Dominio

- [x] T16.1 Crear `OfferLifecycleStatus` y `MerchantOfferMetrics` en `domain/model/` — verif: `./gradlew.bat compileDebugKotlin` — **Parcial: existe `OfferLifecycleStatus`; falta `MerchantOfferMetrics` (auditoría 2026-11-15).**
- [x] T16.2 Ampliar `OffersRepository` con `getMyOffers`, `updateOffer` y `updateOfferStatus` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T16.3 Crear `GetMerchantInventoryUseCase`, `UpdateOfferUseCase` y `ChangeOfferStatusUseCase` reutilizando validación de precios/stock — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos

- [x] T16.4 Añadir `GET /api/v1/merchants/my-offers`, `PUT /api/v1/offers/{id}` y `PUT /api/v1/offers/{id}/status` en servicios Retrofit y DTOs — verif: `./gradlew.bat compileDebugKotlin`
- [x] T16.5 Implementar métodos en `OffersRepositoryImpl` con actualización sincronizada en `OfferDao` — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [x] T16.6 Actualizar `MerchantDashboardContract.kt` y `MerchantDashboardViewModel.kt` eliminando `StoreRepository.getSampleStore()` y conectando inventario paginado — verif: `./gradlew.bat testDebugUnitTest` — **Parcial: inventario real con `fetchMerchantOffers`, pero `getSampleStore()` sigue como respaldo del nombre del comercio (auditoría 2026-11-15).**
- [x] T16.7 Añadir pestañas de historial (`Activas`, `Pausadas`, `Finalizadas`), contador de vistas (`SE-045`) y acciones rápidas en `MerchantDashboardScreen.kt` — verif: `./gradlew.bat compileDebugKotlin` — **Parcial: pestañas `Activas` y `Pausadas`; faltan `Finalizadas` y el contador de vistas (auditoría 2026-11-15).**
- [x] T16.8 Crear `MerchantEditOfferContract.kt`, `MerchantEditOfferViewModel.kt` y `MerchantEditOfferScreen.kt` (`P-31`) para editar y republicar ofertas — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — Pruebas y Cierre

- [x] T16.9 Añadir pruebas unitarias de casos de uso y tests `MockWebServer` para `my-offers`, `PUT /offers/{id}` y `/status` — verificado en `PublishingApiTest` y la suite unitaria.
- [x] T16.10 Añadir pruebas de ViewModel y Compose UI para edición, pausado y republicación — verificado con `./gradlew.bat compileDebugAndroidTestSources` y los dos casos nuevos de edición y pausa en el V2341.
- [x] T16.11 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y verificar todos los gates — verif: `python scripts/check_endpoints.py` — **Parcial: endpoints documentados y `check_endpoints.py` PASS (auditoría 2026-11-15).**

## Slice autorizada REPUBLISH — 2026-11-14

- [x] T16.12 Integrar POST /api/v1/offers/{id}/republish: entrada tipada, fechas futuras y stock positivo, solo EXPIRED/ARCHIVED, HTTP 200 y cache server-wins, Hilt, boton explicito, pruebas dominio/repositorio/VM/UI. Gradle y gates coordinados por main sin paralelismo. Sin cambios de conexion, backend, gates, memoria ni docs globales.

Evidencia T16.12: implementación y pruebas fuente preparadas en UTF-8; diff --check sin errores. Sin ejecución Gradle por coordinación main; cobertura y E2E no acreditados. Integración main: registrar endpoint/rate y bitácora global autorizada en su slice, ejecutar gates completos y pruebas instrumentadas. Python del PATH apunta a alias WindowsApps no ejecutable en esta sesión.

Validación integración principal T16.12: contrato y Allow POST oficial verificados, 257 tests globales sin fallos, Kover 82.82%, Debug e instrumentadas compilan, lint/detekt/ktlint y cinco gates Python PASS. Tests Compose añadidos y compilados; ejecución en dispositivo/E2E pendiente. Sin cierre de la feature completa.

## Avance registrado 2026-11-21

- T16.6 parcial: el panel consulta el perfil real y GET my-offers, sin datos de ejemplo ni catálogo público como respaldo. Maneja carga, fallo y reintento. El contrato actual no pagina.
- T16.7 parcial: Activas, Pausadas y Finalizadas comparten regla para el conteo y el listado. Se retiraron los contadores de canjes y las reseñas de ejemplo del panel. Vistas y canjes por oferta siguen pendientes de datos publicados por el backend.
- T16.9 parcial: regresión con MockWebServer para GET my-offers con status y manejo de 403. Las pruebas del ViewModel verifican los tres estados, el stock en cero y el fallo de inventario.
- Validación local: 451 pruebas unitarias sin fallos y Kover Debug de 80.80 %. Gradle compiló la app y las pruebas instrumentadas. Lint, Detekt, ktlint y los cinco gates Python pasaron. Las pruebas de inventario propio y pausa usan MockWebServer.

## Cierre local en movil_Rocio — 2026-11-22

- T16.1: `MerchantOfferMetrics` (activas, pausadas, finalizadas, piezas disponibles y ofertas con 3 piezas o menos) calculado con `GET /merchants/my-offers`. El panel muestra el resumen del inventario.
- T16.6: el panel no usa `getSampleStore()`; `GET /merchants/my-offers` no pagina en el contrato vigente.
- T16.7: Android lee el `viewCount` opcional y muestra las vistas por oferta cuando recibe un valor válido, incluido cero. Pendiente comprobar el dato en el inventario del backend desplegado.
- T16.10: se añadieron pruebas Compose que verifican el stock enviado al guardar y que cancelar la confirmación de pausa no emite el cambio de estado. La pantalla solicita confirmación antes de enviar PAUSED, como pide RF-16-04. Los dos casos pasaron en el V2341.

- Verificación en dispositivo 2026-11-22: el equipo probó los cambios del cierre local y funcionan.

## Cierre de T16.7 — 2026-11-22

- [x] T16.7: pestañas Activas, Pausadas y Finalizadas, acciones rápidas y contador de vistas por oferta (`viewCount` de `GET /merchants/my-offers`, incluido cero). Edición y pausa verificadas en teléfono. Pendiente externo: confirmar que el inventario desplegado publique `viewCount`; sin el dato no se muestra la línea de vistas.

## Ajuste al contrato paginado y compresión de media — 2026-11-27

- [x] T16.13 Parseo de `PagedResult<MerchantOfferDto>` (`items`) y `viewsCount`/`viewCount` en `GET /api/v1/merchants/my-offers` para mostrar el inventario y métricas del propietario sin fallo por objeto paginado.
- [x] T16.14 Endurecimiento de `AndroidImageCompressor`/`ImageUploadPolicy` (`MAX_UPLOAD_BYTES = 4_000_000`, muestreo `inSampleSize` en cámara/galería) y mapeo diferenciado `400 -> Validation`, `202 -> Unprocessable`, `422 -> ContentRejected` al subir fotos moderadas.
- [x] T16.15 Soporte dual en `OffersRepositoryImpl.fetchMerchantOffers` para respuesta paginada (`PagedOffersResponseDto`) y arreglo JSON legado, recarga del panel tras editar/pausar en `MainAppContainer`, clave de expansión por filtro en `MerchantCategoryGroup` y acceso al inventario/edición con tienda temporalmente cerrada (`sensus-back`).
