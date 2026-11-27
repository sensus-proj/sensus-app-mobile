# TASKS — FT-18 Feed Comunitario Geolocalizado y Publicaciones

> 1 PR = 1 slice. Orden: Dominio -> DB/API -> ViewModel/UI -> Tests -> Gates.

## Fase A — Dominio

- [x] T18.1 Crear `FeedPost`, `PagedFeed`, `CreatePostInput` y `FeedRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T18.2 Crear `GetNearbyFeedUseCase` y `CreateFeedPostUseCase` (validación de texto + flujo upload foto) — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos y Caché

- [x] T18.3 Crear `FeedPostEntity.java`, `FeedPostDao.java` y registrar en `SensusDatabase.java` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T18.4 Crear `FeedDtos.kt`, `FeedApiService.kt` (`GET /api/v1/feed/nearby`, `POST /api/v1/feed/posts`) y `FeedMapper.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T18.5 Implementar `FeedRepositoryImpl` desconectando el feed de `StoreRepository.getSampleFriends()` — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [x] T18.6 Actualizar `CommunityContract.kt` y `CommunityViewModel.kt` con paginación, pull-to-refresh y creación de post — verif: `./gradlew.bat testDebugUnitTest`
- [x] T18.7 Crear `CreateFeedPostSheet.kt` (texto, foto moderada y selector de comercio cercano) y actualizar `CommunityScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [x] T18.8 Registrar `FeedApiService`, `FeedRepository` y casos de uso en módulos Hilt dedicados (`PublishingModule.kt` / `CommunityModule.kt`) — verif: `./gradlew.bat compileDebugKotlin`
- [x] T18.9 Añadir tests unitarios, `FeedPostDao` SQLCipher y `MockWebServer` (`nearby` 200, `posts` 201 y 422 moderación) — verif: `./gradlew.bat testDebugUnitTest`
- [x] T18.10 Añadir tests Turbine y Compose UI para pull-to-refresh, modal de publicación y etiqueta de comercio — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [x] T18.11 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_endpoints.py`

## Evidencia local — 2026-11-13

- Cliente implementado para las HU solicitadas, conservando Clean Architecture + MVI, Hilt y SQLCipher.
- Compilación de la app y de las pruebas instrumentadas verificada.
- 146 pruebas unitarias existentes y específicas: 0 fallos, 0 errores, 0 omisiones. Cobertura global Kover: 89.85%.
- Pruebas HTTP con MockWebServer: no se publicaron ofertas ni posts en el servidor real.
- Las pruebas instrumentadas de UI y SQLCipher están escritas y compiladas; su ejecución en dispositivo sigue pendiente.
- La integración con los contratos desplegados del backend y la revisión formal de la slice siguen pendientes.
- Cambios únicamente locales, sin commit, push, tag ni modificación de configuración de escáneres.


## Corrección de integración — 2026-11-14

- [x] T18.12 Contrastar contrato backend 024/026 y despliegue sin cambiar URL base, autenticación ni rutas documentadas; registrar evidencia del 404 y ausencia de endpoints.
- [x] T18.13 Separar fallos de lectura/publicación en CommunityViewState/MVI; conservar borrador/foto y evitar que errores de carga aparezcan en el formulario.
- [x] T18.14 Mostrar estado de servicio no disponible para NotFound y ocultar estado vacío al fallar carga; textos en recursos Android.
- [x] T18.15 Agregar regresiones HTTP 404/autenticación, ViewModel y Compose; ejecutar compilación, tests, Kover, análisis y gates sin APK/AAB.
- [ ] T18.16 Integración E2E con backend implementado/desplegado y contrato de respuesta aprobado (acción del usuario: validar E2E contra backend 024/026 desplegado en AWS; ver plan cierre móvil §5.Z-3).


### Evidencia de corrección local — 2026-11-14

- 188 pruebas unitarias, 0 fallos/errores/omisiones; 6 regresiones nuevas de HTTP/estado. Kover actualizado 81.41% global; CommunityViewModel 59/63 líneas cubiertas (93.65%).
- compileDebugKotlin, compileDebugAndroidTestSources, testDebugUnitTest, koverXmlReportDebug/koverVerifyDebug, detekt, ktlintCheck y lint: BUILD SUCCESSFUL. Los 5 scripts Python PASS.
- 4 pruebas Compose nuevas compiladas, ejecución en dispositivo pendiente. No se generaron APK/AAB.
- SAST disponible: 66 reglas, 0 hallazgos. La configuración completa no puede validarse porque el paquete remoto p/mobile devuelve 404; no se omite, sustituye ni modifica CI, y no se declara suite DevSecOps completa aprobada.
- T18.16 sigue pendiente: backend main 01dad6e no registra feed; specs 024/026 pendientes. Informe en app/build/reports/community-backend-diagnosis.md. Sin commit/push ni despliegue.

## Avance en movil_Rocio — 2026-11-21

- Se verificó backend main 1c27253: FeedEndpoints publica /feed/offers y CommunityEndpoints publica /community/friends-activity. Las rutas del feed geolocalizado y de creación de publicaciones de FT-18 no están registradas allí.
- Una consulta GET sin credenciales con coordenadas públicas a /api/v1/feed/nearby en AWS respondió 503. Esa respuesta no permite validar el contrato desplegado. No se enviaron publicaciones.
- En la revisión posterior, AWS respondió HTTP 404 a `GET /api/v1/feed/nearby`, mientras que ofertas cercanas y ajustes de usuario respondieron HTTP 401 a peticiones sin credenciales. T18.16 sigue pendiente de validar el feed autenticado y su contrato desplegado.
- Se retiró la dependencia residual de datos de amigos de ejemplo en CommunityViewModel. Una falla de caché ya no invalida una página remota válida, y al leer offline se descartan filas con JSON corrupto sin perder las válidas. Se conserva el error de red si la caché no se puede leer.
- Validación local: 453 pruebas unitarias sin fallos, Kover Debug 80.79 %, compilación de la app e instrumentadas, lint, Detekt, ktlint y cinco gates Python en verde.

## Activación del Muro comunitario, cámara/galería moderada y etiqueta de comercio — 2026-11-27

- [x] T18.17 `CommunityScreen`: activar pestañas **Muro** (`CommunityFeedContent`), **Actividad** (`FriendsActivityFeedContent`) y **Amigos y QR**, junto con `CreateFeedPostSheet` (cámara con `rememberSafeMerchantCameraLauncher`, galería, advertencia de moderación obligatoria y etiqueta opcional de comercio cercano `merchantId`) y `PostCommentsSheet`.
- [x] T18.18 Alineación HTTP con Specs 024/026/039: `FeedApiService`/`FeedRepositoryImpl` (`radiusKm` en `/feed/nearby`, lectura de `PagedResult.items`, `SetFeedPostLikeRequest(liked)` explícito en `/like`, `textContext` en `/media/upload` y `202 -> Unprocessable` fail-closed) — verif: `FeedAvailabilityApiTest`, `PublishingApiTest`, `CommunityViewModelInteractionsTest`.
