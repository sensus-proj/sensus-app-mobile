# SPEC — FT-13-reserva-qr-cliente

## 1. Contexto y objetivo
Integración real sobre backend main 01dad6e, OfferEndpoints.cs y OfferClaimDtos.cs.

## 2. Requisitos funcionales
Solo POST /api/v1/offers/{id}/claim, GET /api/v1/offers/claims/{claimId} y POST /api/v1/offers/claims/{code}/redeem, sin rutas de listado remoto ni cancelación.
RF-13-07 (reconstrucción local de Mis reservas): `EncryptedClaimIndex` conserva hasta 50 `claimId` recientes por cuenta autenticada (cifrado en `EncryptedSharedPreferences`, aislado por cuenta). `GetMyClaimsUseCase` reconstruye la lista llamando a `GET /api/v1/offers/claims/{claimId}` en paralelo acotado (máx. 4 en `Dispatchers.IO`), poda los 404 del índice, muestra estados activos/vencidos/canjeados y falla cerrado sin sesión o ante 401.
Fuera de alcance por decisión del responsable (2026-11-26): listado remoto de apartados (`offers/claims`) y cancelación remota (`offers/claims/{id}/cancel`; Spec 033 del backend no incluye cancelación).
Claim aparta exactamente una unidad por pulsación explícita; nunca al componer o reentrar.
ClaimOfferResultDto: claimId, offerId, code, qrPayload, expiresAtUtc.
RedeemClaimResultDto: claimId, offerId, merchantId, redeemedAtUtc.
El QR codifica exclusivamente qrPayload confirmado. Código y fecha proceden del backend.
Canje manual sin normalizar mayúsculas, sin formato corto inventado, sin montos ni éxito local.

## 3. Requisitos no funcionales
Clean + MVI + Hilt; sesión activa no invitada y token no vacío. Canje requiere comercio registrado.
Sin persistencia ni logs de código/token; respuesta de sesión anterior descartada.
Sin reintentos automáticos de escrituras; sin duplicados durante carga.
Identidad de sesión limitada a email, token, sesión activa, invitado y comercio; ubicación/términos no invalidan confirmación.
JSON inválido o HttpException producen Result.Failure, conservando propagación de CancellationException.
Payload QR máximo 2048 bytes UTF-8 y encoder defensivo sin QR falso al fallar.
Expiración visible con Clock y tick: al vencer oculta QR/código, conserva resultado y no permite repetir POST.

## 4. Arquitectura y contratos internos
ClaimsApiService -> ClaimsRepositoryImpl -> ClaimsRepository -> CreateClaimUseCase/RedeemClaimUseCase.
OfferClaim/ClaimRedemption puros; ClaimsModule usa container.networkClient.claimsApi sanitizada por integración principal.
ClaimOfferScreen y MerchantSalesScreen con contratos y ViewModels MVI separados.

## 5. Criterios de aceptación
Solo HTTP 200 con DTO completo permite éxito. Error o sesión ausente no producen código ni confirmación.
El comprador confirma una unidad. Tras la primera confirmación válida del servidor, se retira una unidad de esa oferta si está en la bolsa y se conservan las demás. Una carga fallida o la reapertura de un QR confirmado dejan la bolsa sin cambios.
Comercio captura código literal y ve confirmación solo del servidor.

## 6. Plan de pruebas
JUnit4 dominio/API/repositorio/ViewModels y Compose en archivos Claims nuevos.
Integración principal ejecuta compilación, pruebas, lint, detekt, ktlint y cobertura >=80%, sin Gradle concurrente.

Confirmaciones y ofertas conocidas con apartado activo se retienen solo en memoria del ClaimOfferViewModel por identidad de autenticación.
Initialize(offerId, expectExisting) restaura el código confirmado sin POST ni efecto repetido.
Con expectExisting y sin código local se bloquean botón y Submit, incluso tras cambiar de oferta.
No se utilizan SecurePreferences, disco, SavedStateHandle ni nuevas rutas para recuperar códigos.

## 7. Riesgos, límites y deuda
Listado remoto y cancelación fuera de alcance por decisión del responsable (2026-11-26); la pantalla Mis reservas reconstruye el estado mediante `GET /api/v1/offers/claims/{claimId}` desde `EncryptedClaimIndex`.
Sin cámara porque esta slice no añade dependencias. ZXing core y cliente sanitizado a cargo de integración principal.
La bolsa se actualiza en memoria al confirmar la reserva. Sin persistencia offline del código en esta slice.
Ante timeout la operación puede haberse aplicado: no hay reenvío automático ni certeza local.

## Aprobación
Implementación local autorizada; validación integral pendiente de integración principal.

Tras reiniciar el proceso o recrear el ViewModel se pierde la caché en memoria. El backend main 1c27253 incluye una consulta de reserva por claimId, pero el detalle de oferta solo devuelve hasActiveClaim. El detalle no permite descubrir un ID que tampoco esté en el índice local.
MAIN debe pasar ClaimsScreenActions(expectExisting = true) cuando el detalle indique hasActiveClaim.
