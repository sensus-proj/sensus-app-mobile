# TASKS — FT-14-canje-qr-comercio

- [x] Materializar DTOs exactos y POST sin body con HTTP 200.
- [x] Implementar dominio, repositorio, sesión fail closed y Hilt ClaimsModule.
- [x] Implementar contratos/ViewModels MVI y UI explícita sin éxito simulado.
- [x] Añadir pruebas JUnit4/API/ViewModel y Compose en archivos Claims nuevos.
- [x] Integración principal: claimsApi sanitizada, ZXing core y navegación.
- [x] Ejecutar gates locales Gradle/Python, compilación y cobertura >=80% en integración principal.
- [x] Listado y cancelación: corresponden al cliente (FT-13, "Mis reservas", integrado 2026-11-22). El canje del comercio no depende de ellos.

Contrato auditado: backend main 01dad6e, OfferClaimDtos.cs y OfferEndpoints.cs.
Validación principal ejecutada: 257 pruebas unitarias sin fallos/errores/omisiones, Kover 82.82%, compileDebugKotlin/compileDebugAndroidTestSources/lint/detekt/ktlint PASS. Pruebas Compose compiladas, no ejecutadas.

- [x] Verificar Initialize por offerId sin POST y efecto Confirmed único con callback onClaimConfirmed sin vaciar el carrito antes de confirmación; retirada automática posterior no implementada.

- [x] Revisión: capturar JSON/HTTP inválidos; comparar identidad de autenticación; expirar QR con reloj; limitar payload y probar fallos sin éxito falso.


- [ ] Ejecutar pruebas instrumentadas en dispositivo y E2E con cuenta real sin simulación.
- [x] Retirada de una unidad de bolsa tras confirmar el apartado, cubierta y validada en FT-13.
- Suite remota de seguridad/CI no ejecutada sin push; paquete SAST p/mobile externo devuelve 404. No se cierran estas condiciones.

- [x] Cargar el folio desde el lector QR existente, permitir revisarlo y verificar mediante pruebas unitarias la confirmación explícita.

## Avance registrado 2026-11-21

- La lectura de QR carga el folio en el estado MVI y muestra una confirmación visual de captura. Se mantiene la edición manual y el POST solo ocurre mediante el botón de canje.
- El emisor del backend produce el mismo texto para el folio y el QR. Se agregaron regresiones que comprueban folio literal, envío explícito, bloqueo de duplicados y ausencia de éxito antes de la respuesta.
- Validación local: 448 pruebas unitarias sin fallos y Kover Debug 80.66 %. Compilaron la app y las pruebas instrumentadas. Lint, Detekt, ktlint y los cinco gates Python pasaron.
