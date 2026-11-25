# PLAN — FT-18 Feed Comunitario y Publicaciones

> Derivado de spec.md. Alcance SE-027/028/029; creación/lectura, sin interacciones de FT-19.

## Diseño vigente

1. CommunityScreen -> CommunityViewModel (MVI) -> GetNearbyFeedUseCase / CreateFeedPostUseCase -> FeedRepository.
2. FeedRepositoryImpl utiliza FeedApiService del NetworkClient existente: GET /api/v1/feed/nearby y POST /api/v1/feed/posts. No cambia URL, interceptor ni autenticación.
3. FeedPostDto/PagedFeedDto y FeedMapper mapean el contrato en data. FeedPostDao conserva páginas en Room + SQLCipher. No se persiste contenido rechazado.
4. Creación: validar texto/coordenadas/sesión, comprimir imagen opcional y subir con moderación; crear post solo al recibir confirmación remota.
5. Hilt en CommunityModule/PublishingModule; dominio sin Android ni datos.

## Corrección de indisponibilidad

- Separar feedError y publishError en CommunityViewState, conservando tipos AppError para que presentación traduzca NotFound con strings.xml.
- Carga y refresh solo modifican feedError; edición/envío/cierre del formulario solo publishError. Los fallos no limpian borrador/foto/etiqueta.
- Solo mostrar vacío tras carga sin error y sin posts. La caché y paginación existentes se conservan; no reintentar automáticamente una página fallida en bucle.
- No cambiar una ruta por otra supuesta ni aprobar contenido localmente para ocultar 404.

## Pruebas y verificación

- JUnit 4 + MockK/Turbine: independencia de errores, conservación de borrador, rechazo y recuperación tras refresh.
- MockWebServer: GET/POST 404 con el mismo cliente y token inyectado por AuthInterceptor; sin caché/éxito falso.
- Compose: servicio no disponible sin falso vacío, aislamiento del error del formulario. Compilar pruebas instrumentadas; ejecución en dispositivo requiere autorización para generar binarios.
- compileDebugKotlin, compileDebugAndroidTestSources, testDebugUnitTest, koverXmlReportDebug/koverVerifyDebug, lint, detekt, ktlintCheck y cinco gates Python.

## Dependencia externa

Backend main 01dad6e contiene contratos borrador y tareas pendientes de specs 024/026; no registra feed en Program.cs. GET oficial devuelve 404 y Swagger JSON 500. La corrección local no implementa ni despliega ese servicio: no se declara E2E resuelto hasta su disponibilidad.

## Tareas

Ver tasks.md T18.12..T18.16.

La caché del feed es accesoria a la respuesta remota. Su lectura y escritura ocurren en Dispatchers.IO, preservan la cancelación y aíslan los fallos de Room y las filas con contenido corrupto.
