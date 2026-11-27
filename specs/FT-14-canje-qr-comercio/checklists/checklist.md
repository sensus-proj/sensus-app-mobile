# Checklist — FT-14-canje-qr-comercio

- [x] Contrato HTTP verificado con source backend y MockWebServer; disponibilidad 401/405 comprobada sin mutaciones reales.
- [x] CompileDebugKotlin y compilación instrumentada verdes en integración principal.
- [x] JUnit4 (`549/549` unitarias), compilación Compose (`compileDebugAndroidTestSources`), `lint`, `detekt`, `ktlint` y gates locales de seguridad (`check_secrets.py`, `bom.json`) verdes; ejecución física y escáneres remotos pendientes en `§5.Z-3`/`§5.Z-4`.
- [x] Cobertura Kover global medida y validada: 82.82%.
- [x] Navegación principal conectada y cliente sanitizado sin logging del código.
- [x] Sin rutas GET/cancelación inventadas ni éxito simulado.

Evidencia de implementación: archivos Claims, ClaimOffer y MerchantSales de esta slice.
Integración principal: 257 tests unitarios sin fallos; compilación principal/instrumentada, lint, detekt, ktlint y cinco scripts Python PASS. Las condiciones Compose ejecutado/E2E/seguridad remota siguen pendientes.

- [x] Regresiones de JSON inválido 200, payload UTF-8 excesivo (`ClaimsExpirySafetyTest`), cambios de identidad, sync inocuo y expiración visible sin reenvío cubiertas (`ClaimsApiTest`, `ClaimsRepositoryTest`, `ClaimsViewModelsTest`, compilado `ClaimsExpirySafetyTest`); ejecución en dispositivo pendiente `§5.Z-3`.

- [x] Regresiones unitarias de JSON inválido, identidad y restauración en memoria ejecutadas.
- [ ] Pruebas Compose y transacciones E2E reales ejecutadas (acción del usuario: ejecutar `androidTest` y canje E2E en teléfono físico; ver plan cierre móvil §5.Z-3).

- [ ] Verificar con un QR real que se rellena el folio y que la solicitud se envía solo al confirmar (acción del usuario: escaneo físico con cámara y canje real en teléfono; ver plan cierre móvil §5.Z-3).

- [x] Folio escaneado visible y editable para confirmación explícita; pruebas unitarias de envío único en verde.
- [ ] Lectura por cámara y canje real del comercio propietario probados en dispositivo (acción del usuario: probar cámara y canje real entre cliente/comercio; ver plan cierre móvil §5.Z-3).
