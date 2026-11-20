# PLAN — FT-14-canje-qr-comercio

1. Registrar tareas pendientes antes de código (tasks.md).
2. Materializar contratos publicados con serialización explícita; fechas UTC a epoch-ms.
3. Implementar repositorio fail closed y casos de uso puros; Hilt ClaimsModule.
4. MVI y UI manual/confirmación explícita, recursos strings_claims.xml.
5. Pruebas nuevas JUnit4 y Compose sin modificar suites compartidas.
6. Integración principal añade claimsApi sin logs, ZXing core y navegación; ejecuta gates secuenciales.

No GET, cancelación, cantidades múltiples, idempotency header inventado ni persistencia plaintext.
Verificación pendiente: compileDebugKotlin, testDebugUnitTest, compileDebugAndroidTestSources, lint,
detekt, ktlintCheck, koverVerify y scripts check_sdd/check_layers/check_endpoints/check_secrets/check_coverage --min 80.

Integración de UI: ClaimOfferScreen(offer, actions = ClaimsScreenActions(onNavigateBack, onNavigateToRoute, onClaimConfirmed)).
Callbacks y Clock agrupados en ClaimsScreenActions (@ComposeUiModel); Screen y Content tienen cuatro parámetros, sin supresiones.

## Captura con cámara
Reutilizar el lector QR y enviar el contenido leído al intent Edit. Verificar que la lectura no dispare Submit.
