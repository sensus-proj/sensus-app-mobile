# PLAN — FT-13-reserva-qr-cliente

1. Registrar tareas pendientes antes de código (tasks.md).
2. Materializar contratos publicados con serialización explícita; fechas UTC a epoch-ms.
3. Implementar repositorio fail closed y casos de uso puros; Hilt ClaimsModule.
4. MVI y UI manual/confirmación explícita, recursos strings_claims.xml.
5. Cubrir el contrato en JUnit4 y Compose, y la retirada de una unidad en las pruebas de CartRepository y ClaimOfferViewModel.
6. Integración principal añade claimsApi sin logs, ZXing core y navegación; ejecuta gates secuenciales.

7. Al confirmar la creación, ClaimOfferViewModel solicita a CartRepository retirar una unidad de la oferta confirmada antes de emitir el efecto de UI. La operación comparte el bloqueo de las demás escrituras de bolsa.

El cliente no usa la consulta individual GET por claimId ni implementa cancelación. El POST continúa apartando una unidad por confirmación y los códigos siguen en memoria.
Verificación pendiente: compileDebugKotlin, testDebugUnitTest, compileDebugAndroidTestSources, lint,
detekt, ktlintCheck, koverVerify y scripts check_sdd/check_layers/check_endpoints/check_secrets/check_coverage --min 80.

Integración de UI: ClaimOfferScreen(offer, actions = ClaimsScreenActions(onNavigateBack, onNavigateToRoute, onClaimConfirmed)).
Callbacks y Clock agrupados en ClaimsScreenActions (@ComposeUiModel); Screen y Content tienen cuatro parámetros, sin supresiones.

Caché por oferta exclusivamente en memoria del ViewModel, borrada solo por cambios de identidad de autenticación.
expectExisting bloquea nuevas mutaciones si el backend anuncia apartado y falta el código local.
onClaim ahora es callback separado en ClaimOfferContent (cinco parámetros); ClaimsScreenActions conserva cinco campos incluyendo expectExisting.
