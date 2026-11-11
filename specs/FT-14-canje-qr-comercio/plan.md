# PLAN — FT-14 Escaneo y Validación de Código QR por Comerciante

> Derivado de `spec.md`. Historias/Alcance: Pantalla P-33, Componente C-48 (Spec 033 Backend).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Canje en servidor (`POST /api/v1/offers/claims/{code}/redeem`).** `RedeemOfferClaimUseCase` valida formato no vacío del código y envía la petición autenticada con rol `MERCHANT`.
2. **Escáner de cámara (`CameraX` + `ML Kit`).** `QrCameraScannerPreview` (`C-48`) analiza frames en tiempo real con debounce de `2000 ms` para evitar lecturas duplicadas.
3. **Fallback manual.** Si el permiso `CAMERA` es denegado o el QR físico no se lee, el comerciante ingresa el código alfanumérico `SNS-XXXX-XX` manualmente.
4. **Retiro de código hardcodeado.** Se elimina la cadena fija `"Código SNS-A7K9-24 validado..."` de `MerchantSalesScreen.kt`.

## 2. UI y navegación

- `MerchantSalesScreen` (`P-33`) integra visor CameraX (`C-48`), campo manual con `ImeAction.Done` y tarjeta de resultado (`SUCCESS`, `ALREADY_REDEEMED`, `EXPIRED`, `FORBIDDEN`).
- Previews: `ScannerReady`, `ManualEntry`, `RedeemSuccess`, `RedeemConflictError`.

## 3. DI

| Binding | Implementación |
|---|---|
| `RedeemOfferClaimUseCase` | Inyección por constructor con `ReservationsRepository` |

## 4. Gates

- `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes.
- `./gradlew.bat detekt ktlintCheck lint` verdes (0 violaciones).
- `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (cobertura `>= 80%`).
- `python scripts/check_sdd.py` PASS.
- `python scripts/check_layers.py` PASS.
- `python scripts/check_endpoints.py` PASS.
- `python scripts/check_secrets.py` PASS.
- `python scripts/check_coverage.py --min 80` PASS.

## 5. Orden de implementación

1. Crear `ClaimRedemptionResult` y `RedeemOfferClaimUseCase` en `domain/`.
2. Añadir `@POST("api/v1/offers/claims/{code}/redeem")` en `ReservationsApiService`.
3. Integrar CameraX + ML Kit Barcode Scanning en `QrCameraScannerPreview` (`C-48`) con permiso `CAMERA`.
4. Refactorizar `MerchantSalesScreen` y crear `MerchantSalesViewModel` MVI eliminando el texto hardcodeado.
5. Probar `200`, `403`, `404`, `409` y `410` con `MockWebServer`.

## 6. Prohibiciones

- Prohibido dejar respuestas de canje hardcodeadas en `MerchantSalesScreen.kt`.
- Prohibido disparar múltiples llamadas concurrentes para el mismo frame QR (aplicar debounce).

## Tareas

Ver `tasks.md`.
