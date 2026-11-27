# Checklist — FT-13-reserva-qr-cliente

- [x] Contrato HTTP verificado con source backend y MockWebServer; disponibilidad 401/405 comprobada sin mutaciones reales.
- [x] CompileDebugKotlin y compilación instrumentada verdes en integración principal.
- [x] JUnit4, Compose compilado, lint, detekt, ktlint y gates de seguridad verdes (`./gradlew.bat compileDebugKotlin compileDebugAndroidTestSources detekt ktlintCheck lint testDebugUnitTest koverXmlReportDebug koverVerifyDebug`: `532/532` pruebas unitarias PASS, Kover `81.94%`, 5 `check_*.py` PASS, `check_endpoints.py` `61 endpoints`).
- [x] Cobertura Kover global medida y validada: 81.94%.
- [x] Navegación principal conectada y cliente sanitizado sin logging del código.
- [x] Sin rutas GET/cancelación inventadas ni éxito simulado (`EncryptedClaimIndex` máx. 50 + `GET /api/v1/offers/claims/{claimId}` en paralelo acotado, máx. 4 en `Dispatchers.IO`).

Evidencia de implementación: `EncryptedClaimIndex.kt`, `ClaimsApiService.kt`, `ClaimsRepositoryImpl.kt`, `GetMyClaimsUseCase.kt`, `MyClaimsViewModel.kt`, `MyClaimsScreen.kt`, `MyClaimsRepositoryTest.kt`, `MyClaimsViewModelTest.kt`, `MyClaimsScreenTest.kt` (verificado el 2026-11-26: `532/532` unitarias PASS, Kover `81.94%`, `compileDebugAndroidTestSources`, `lint`, `detekt`, `ktlintCheck` y los 5 scripts Python PASS).

- [x] Pruebas unitarias de JSON inválido 200, cambios de identidad, restauración y poda 404 ejecutadas (`ClaimsApiTest`, `ClaimsRepositoryTest`, `MyClaimsRepositoryTest`, `ClaimsMemoryRestoreTest`, `ClaimsSessionChangesTest`, `MyClaimsViewModelTest` — `40/40` en suites `*Claim*`).
- [ ] Ejecutar en dispositivo las pruebas Compose de payload UTF-8 excesivo y expiración visible sin reenvío (`ClaimsExpirySafetyTest`) (acción del usuario: ejecutar `connectedDebugAndroidTest` en teléfono físico; ver plan cierre móvil §5.Z-3).

- [x] `ClaimsMemoryRestoreTest` ejecutado en JUnit4; `ClaimsUnavailableCodeTest` compilado en `compileDebugAndroidTestSources`.
- [ ] Ejecutar `ClaimsUnavailableCodeTest` en dispositivo (acción del usuario: ejecutar `connectedDebugAndroidTest` en teléfono físico; ver plan cierre móvil §5.Z-3).

- [x] Regresiones unitarias de JSON inválido, identidad y restauración en memoria ejecutadas.
- [ ] Pruebas Compose en dispositivo y transacciones E2E reales ejecutadas (acción del usuario: ejecutar `connectedDebugAndroidTest` y recorrido E2E en teléfono físico; ver plan cierre móvil §5.Z-3).

- [x] Verificar que el éxito nuevo descuenta una unidad, conserva las demás y evita descuentos por fallo, cambio de sesión o reapertura del QR.
- [ ] Verificar el flujo de punta a punta con una cuenta real y el backend desplegado (acción del usuario: probar reserva, "Mis reservas" y canje QR con cuentas reales en backend desplegado; ver plan cierre móvil §5.Z-3).
