# SPEC — FT-14-canje-qr-comercio

## 1. Contexto y objetivo
Integración real sobre backend main 01dad6e, OfferEndpoints.cs y OfferClaimDtos.cs.

## 2. Requisitos funcionales
Solo POST /api/v1/offers/{id}/claim y POST /api/v1/offers/claims/{code}/redeem, sin body, respuesta 200.
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
El comprador confirma una unidad. Comercio captura código literal y ve confirmación solo del servidor.

## 6. Plan de pruebas
JUnit4 dominio/API/repositorio/ViewModels y Compose en archivos Claims nuevos.
Integración principal ejecuta compilación, pruebas, lint, detekt, ktlint y cobertura >=80%, sin Gradle concurrente.

## 7. Riesgos, límites y deuda
Listado y cancelación pendientes de publicación backend; no se declaran rutas para ellos.
El lector reutiliza Camera2 y ZXing. El resultado de la lectura se puede revisar en el campo de folio antes de enviarlo.
Sin cambio de MainAppContainer, ActiveApartadoScreen ni carrito. Sin persistencia offline del código en esta slice.
Ante timeout la operación puede haberse aplicado: no hay reenvío automático ni certeza local.

## Aprobación
Implementación local autorizada; validación integral pendiente de integración principal.

## Captura con cámara
El comercio puede leer el QR para revisar y editar el folio antes de confirmar explícitamente el canje. El backend codifica el mismo folio firmado en el QR. La lectura muestra un aviso de revisión y no realiza ninguna petición. Una lectura vacía descarta el folio anterior. La captura manual sigue disponible.
