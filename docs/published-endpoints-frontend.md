# Endpoints publicados vinculados al frontend

Fecha: 2026-11-14. Validación local aprobada; no se declara E2E contra producción.

## Conexión oficial comprobada en GitHub

- Repositorio móvil: sensus-proj/sensus-app-mobile. Main remoto a122726; develop 87646f7.
- Primer cliente de desarrollo 3a6e99a (2026-10-29): http://localhost:5235/; no es un servidor de producción para teléfono.
- Migración EC2 5fd03fc (2026-11-04): https://18-117-247-171.sslip.io/.
- Migración oficial serverless 78556bb (2026-11-09), anterior a esta conversación: https://zqqdwovx81.execute-api.us-east-2.amazonaws.com/.
- Main/develop y el proyecto local conservan API Gateway. Swagger /swagger/index.html documenta ese mismo servidor; no es otra conexión.
- Se conserva Retrofit/AuthInterceptor, token real y URL. Los clientes nuevos usan las variantes sanitizadas del NetworkClient existente.

## Operaciones incorporadas

| Operación publicada | Evidencia en servidor oficial, sin mutación | Pantalla y recorrido |
|---|---|---|
| GET /api/v1/offers/{id} | GET a GUID sintácticamente válido devuelve 401 (ruta protegida) | Comprador: Mapa o lista -> tocar oferta -> ProductDetailScreen. Refresca stock/fecha/precio/datos del comercio; el resumen no habilita apartado sin confirmación. |
| POST /api/v1/offers/{id}/view | GET sobre la ruta devuelve 405 Allow POST | ProductDetailScreen al cargar detalle válido; no bloquea compra ante fallo ni se reenvía en cada recomposición. |
| POST /api/v1/offers/{id}/claim | GET devuelve 405 Allow POST | Detalle -> Apartar 1 unidad -> ClaimOfferScreen -> confirmar. Solo muestra QR/código devueltos por servidor; bolsa requiere una oferta y una unidad. |
| POST /api/v1/offers/claims/{code}/redeem | GET devuelve 405 Allow POST | Comercio -> Canjes QR -> ingresar código -> validar. Éxito únicamente con respuesta real. |
| POST /api/v1/offers/{id}/republish | GET devuelve 405 Allow POST | Comercio -> Mi Tienda -> De baja (archivadas/vencidas) -> abrir oferta -> stock/nueva vigencia -> Republicar. |
| GET /api/v1/users/settings | GET devuelve 401 | Comprador -> lista de ofertas -> campana Notificaciones: carga preferencias reales. |
| PUT /api/v1/users/settings | OPTIONS oficial 405 Allow GET,PUT; DTO/validator reales en main 01dad6e | Misma pantalla: alertas push/ofertas/social y radio 1..50 km -> Guardar. |
| PUT /api/v1/users/device-token | GET devuelve 405 Allow PUT | Misma pantalla -> Activar notificaciones en este dispositivo: permiso Android 13+, ID/token reales de Firebase y confirmación backend. Requiere configuración Firebase oficial, hoy ausente. |

Para ajustes se confirmó además OPTIONS oficial 405 con Allow GET,PUT; UserEndpoints.cs confirma ambos handlers. No se enviaron PUT ni POST reales para probar negocio.

## Contratos y arquitectura

Contratos leídos en sensus-proj/sensus-back main 01dad6e: OfferDetailDto.cs, OfferClaimDtos.cs, RepublishOfferBody, NotificationSettingsDto, RegisterDeviceCommand y UpdateNotificationSettingsCommand/validators. Sin usar propuestas como contratos aprobados.

- FT-12: OfferDetailsApiService -> OfferDetailsRepositoryImpl (OfferDao/SQLCipher para oferta pública consultada) -> casos de uso -> ProductDetailViewModel -> ProductDetailScreen.
- FT-13/14: ClaimsApiService -> ClaimsRepositoryImpl -> CreateClaimUseCase/RedeemClaimUseCase -> MVI -> ClaimOfferScreen/MerchantSalesScreen.
- FT-16: OffersApiService -> OffersRepositoryImpl -> RepublishOfferUseCase -> MerchantEditOfferViewModel/Screen.
- FT-21: NotificationsApiService -> NotificationsRepositoryImpl / proveedor Firebase real -> casos de uso -> NotificationsViewModel/Screen.
- Hilt en módulos por feature. Dominio puro, errores sin falso éxito, sesiones invitadas bloqueadas, sin persistencia manual de códigos/tokens ni cuerpos sensibles en logs de estos clientes.
- Un apartado reserva una unidad; no se inventa un apartado múltiple ni se usa un QR dibujado estático. No se descuenta bolsa antes de confirmación.

## Exclusiones por no estar publicados o no corresponder a los roles de la app

- Favoritos, historial/cancelación de apartados, interacciones de comunidad, recomendaciones/ML, bandeja/marcar leídas, amigos, Sensi y reseñas: no se agregan rutas basándose solo en propuestas. Comunidad se conserva como estaba tras la corrección previa y no se amplía en esta tarea.
- Mantenimiento automático de expiración y liberación de media por administrador: son rutas de operación/administración del backend; no se exponen como acciones de comprador/comercio ni se eluden permisos.
- Alias de recuperación/autenticación no necesitan pantallas duplicadas: el flujo existente de perfil/recuperación sigue usando sus rutas oficiales vigentes.

## Firebase y QR

- Se verificaron artefactos en Maven Central/Google: ZXing core 3.5.3, Firebase Messaging 24.1.2 y desugar_jdk_libs 2.1.5 (compatibilidad java.time en minSdk24).
- No existe google-services.json ni recursos oficiales de Firebase en esta copia. No se crean secretos/configuración ficticia; la activación informa indisponibilidad hasta configurar el proyecto Firebase del equipo.
- El canje implementado es manual por código; no se declara escáner de cámara en funcionamiento.

## Verificación

257 pruebas unitarias, 0 fallos/errores/omisiones. Kover XML nuevo: 82.82%. compileDebugKotlin, compileDebugAndroidTestSources, testDebugUnitTest, koverXmlReportDebug/koverVerifyDebug, detekt, ktlintCheck y lint: BUILD SUCCESSFUL. Cinco scripts Python PASS. Semgrep disponible: 67 reglas aplicables sobre 64 archivos Kotlin modificados, 0 hallazgos. Pruebas Compose compiladas, no ejecutadas en dispositivo. Pruebas HTTP solo con MockWebServer. No se genera APK/AAB ni se hace commit/push/despliegue.

- Compatibilidad: Firebase Messaging 26.0.2 añade metadata Kotlin 2.1 que el procesador Room 2.6.1 actual no puede leer; se verifica la versión oficial 24.1.2 para conservar la arquitectura y el procesador existentes. No se desactiva Kapt/Room ni se cambia Kotlin.

- QR confirmado: se conserva solo en memoria por oferta y sesión, se borra al cambiar de cuenta y se oculta al vencer. «Ver apartado activo» no crea otro apartado. Tras reiniciar el proceso, recuperar un código remoto anterior requiere un endpoint de consulta que el backend aún no publica; se informa esa limitación y no se inventa un código ni se manda otro POST automáticamente.

- OSV (consulta 2026-11-14) no reportó vulnerabilidades conocidas para los tres artefactos nuevos y sus versiones exactas. Esta consulta no sustituye el escaneo completo de dependencias transitivas del CI.

- La configuración SAST completa de CI sigue sin acreditarse: el paquete externo p/mobile devuelve 404. Se conservaron controles y configuración, y se ejecutaron por separado los paquetes disponibles p/default y p/owasp-top-ten. No se declara CI remoto ni merge aprobado.
