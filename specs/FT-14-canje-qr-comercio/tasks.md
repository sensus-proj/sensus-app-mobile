# TASKS — FT-14 Escáner QR y Validación de Canje en Comercio

> 1 PR = 1 slice. Orden: Permisos/Deps -> Dominio -> Datos -> CameraX/UI -> Tests -> Gates.

## Fase A — Configuración y Dominio

- [ ] T14.1 Declarar permiso `CAMERA` (con `required="false"`) en `AndroidManifest.xml` y librerías CameraX + ML Kit en `libs.versions.toml` (previa aprobación D-2) — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T14.2 Crear `RedeemResult` y ampliar `ClaimsRepository` con `redeemClaim(code)` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T14.3 Crear `RedeemClaimUseCase` con sanitización y validación de formato de folio/código — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos y ViewModel MVI

- [ ] T14.4 Añadir `@POST("api/v1/offers/claims/{code}/redeem")` en `ClaimsApiService`, DTOs y `ClaimsRepositoryImpl` (manejo `200`, `403`, `404`, `409`) — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T14.5 Crear `MerchantSalesContract.kt` y `MerchantSalesViewModel.kt` con bloqueo de lecturas duplicadas — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — CameraX y UI

- [ ] T14.6 Implementar `QrCameraPreview.kt` con CameraX `ImageAnalysis` + ML Kit `BarcodeScanner` y ciclo de vida seguro — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T14.7 Actualizar `MerchantSalesScreen.kt` conectándolo a `MerchantSalesViewModel`, solicitud de permiso runtime y retirando el string simulado `"SNS-A7K9-24"` — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — Pruebas y Cierre

- [ ] T14.8 Añadir `RedeemClaimUseCaseTest`, `MerchantSalesViewModelTest` (Turbine) y tests `MockWebServer` (`200`, `403`, `404`, `409`) — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T14.9 Añadir tests Compose UI para entrada manual de folio, tarjeta de cobro y estado de permiso denegado — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T14.10 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `docs/asvs-l2-checklist.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_endpoints.py`
