# TASKS — FT-13-reserva-qr-cliente

- [x] Materializar DTOs exactos y POST sin body con HTTP 200.
- [x] Implementar dominio, repositorio, sesión fail closed y Hilt ClaimsModule.
- [x] Implementar contratos/ViewModels MVI y UI explícita sin éxito simulado.
- [x] Añadir pruebas JUnit4/API/ViewModel y Compose en archivos Claims nuevos.
- [x] Integración principal: claimsApi sanitizada, ZXing core y navegación.
- [x] Ejecutar gates locales Gradle/Python, compilación y cobertura >=80% en integración principal.
- [ ] Listado y cancelación pendientes de publicación backend, sin rutas inventadas.

Contrato auditado: backend main 01dad6e, OfferClaimDtos.cs y OfferEndpoints.cs.
Validación principal ejecutada: 257 pruebas unitarias sin fallos/errores/omisiones, Kover 82.82%, compileDebugKotlin/compileDebugAndroidTestSources/lint/detekt/ktlint PASS. Pruebas Compose compiladas, no ejecutadas.

- [x] Verificar Initialize por offerId sin POST y efecto Confirmed único con callback onClaimConfirmed sin vaciar el carrito antes de confirmación; retirada automática posterior no implementada.

- [x] Revisión: capturar JSON/HTTP inválidos; comparar identidad de autenticación; expirar QR con reloj; limitar payload y probar fallos sin éxito falso.

- [x] Agrupar callbacks/reloj en ClaimsScreenActions sin supresiones; migrar pruebas propias y comunicar firma de navegación a MAIN.

- [x] Simplificar igualdad de identidad privada sin condición compleja ni exposición de token; verificar contrato equals/hashCode.

- [x] Retener confirmaciones solo en memoria por oferta; Initialize(expectExisting) restaura o bloquea POST sin código; probar cambios de oferta y borrado por identidad.


- [ ] Ejecutar pruebas instrumentadas en dispositivo y E2E con cuenta real sin simulación.
- [ ] Retirada automática de la unidad de bolsa tras confirmación (la bolsa se conserva; no se afirma vaciarla).
- Suite remota de seguridad/CI no ejecutada sin push; paquete SAST p/mobile externo devuelve 404. No se cierran estas condiciones.
