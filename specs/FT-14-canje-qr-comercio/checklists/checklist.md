# Checklist — FT-14-canje-qr-comercio

- [x] Contrato HTTP verificado con source backend y MockWebServer; disponibilidad 401/405 comprobada sin mutaciones reales.
- [x] CompileDebugKotlin y compilación instrumentada verdes en integración principal.
- [ ] JUnit4, Compose, lint, detekt, ktlint y gates de seguridad verdes.
- [x] Cobertura Kover global medida y validada: 82.82%.
- [x] Navegación principal conectada y cliente sanitizado sin logging del código.
- [x] Sin rutas GET/cancelación inventadas ni éxito simulado.

Evidencia de implementación: archivos Claims, ClaimOffer y MerchantSales de esta slice.
Integración principal: 257 tests unitarios sin fallos; compilación principal/instrumentada, lint, detekt, ktlint y cinco scripts Python PASS. Las condiciones Compose ejecutado/E2E/seguridad remota siguen pendientes.

- [ ] Ejecutar pruebas nuevas de JSON inválido 200, payload UTF-8 excesivo, cambios de identidad, sync inocuo y expiración visible sin reenvío.

- [x] Regresiones unitarias de JSON inválido, identidad y restauración en memoria ejecutadas.
- [ ] Pruebas Compose y transacciones E2E reales ejecutadas.

- [ ] Verificar con un QR real que se rellena el folio y que la solicitud se envía solo al confirmar.

- [x] Folio escaneado visible y editable para confirmación explícita; pruebas unitarias de envío único en verde.
- [ ] Lectura por cámara y canje real del comercio propietario probados en dispositivo.
