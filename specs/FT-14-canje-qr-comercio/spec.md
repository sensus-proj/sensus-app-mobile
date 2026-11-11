# SPEC — FT-14 Escáner QR y Validación de Canje en Comercio

> Rector: `.specify/memory/constitution.md` §2, §4.3 y §7. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **Sensus_Movil_Componentes_y_CRUD.xlsx** (Pantalla **P-33**, Componente **C-48**); **Tesina Sensus §2.3**.
> Backend ref: Spec `033-apartado-con-qr`.

## 1. Contexto y objetivo

`MerchantSalesScreen` muestra un visor de cámara dibujado en `Canvas` y un botón *"Activar Cámara / Escanear"* que simula el resultado con un string literal (`"Código SNS-A7K9-24 validado..."`), sin permiso de cámara, sin lector óptico ni petición HTTP al backend.

**Objetivo:** implementar el escaneo real de códigos QR mediante **CameraX + ML Kit Barcode Scanning** (on-device) y el respaldo manual de captura de folio, enviando `POST /api/v1/offers/claims/{code}/redeem` para validar y acreditar el canje en tienda.

**Alcance:** permiso runtime `Manifest.permission.CAMERA`, visor CameraX embebido en Compose, analizador ML Kit Barcode (`FORMAT_QR_CODE`), `RedeemClaimUseCase`, `MerchantSalesViewModel` MVI, estados de éxito/código ya usado/comercio incorrecto/expirado y respaldo de captura manual de folio.
**Fuera de alcance:** generación de QR (FT-13), métricas históricas de ventas (FT-16).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-14-01 | Solicitar permiso de cámara (`android.permission.CAMERA`) en tiempo de ejecución únicamente al activar el escáner en `MerchantSalesScreen`, explicando su propósito si el usuario lo deniega. |
| RF-14-02 | Analizar frames con CameraX `ImageAnalysis` + ML Kit `BarcodeScanner` filtrando `FORMAT_QR_CODE`; al detectar un código válido, pausar el escaneo para evitar envíos duplicados y mostrar estado de validación. |
| RF-14-03 | Permitir escribir manualmente el folio corto (`6–8` caracteres alfanuméricos normalizados en mayúsculas) como alternativa cuando la cámara no esté disponible. |
| RF-14-04 | Enviar `POST /api/v1/offers/claims/{code}/redeem` con token de comerciante (`MERCHANT`); manejar `200 OK` (detalle de producto y monto a cobrar en caja), `404` (folio inexistente), `409` (ya canjeado o vencido) y `403` (reserva de otro comercio). |
| RF-14-05 | Eliminar por completo el texto fijo simulado (`"SNS-A7K9-24"`, `"Cobrar $90 MXN"`) de `MerchantSalesScreen.kt`. |

## 3. Requisitos no funcionales

- **Privacidad:** el procesamiento de cámara con ML Kit es 100 % local en el dispositivo; no se graban ni suben frames de video de la cámara al servidor.
- **Debouncing:** una sola invocación HTTP por detección de código hasta que el vendedor confirme `"Escanear siguiente"`.
- **Seguridad:** verificación de rol `MERCHANT` en backend; nunca validar localmente como exitoso un código sin confirmación del servidor.

## 4. Arquitectura y contratos internos

```text
domain/
  model/RedeemResult.kt          # OfferTitle, Quantity, UnitPrice, TotalAmount, RedeemedAt
  repository/ClaimsRepository.kt # suspend fun redeemClaim(code: String): Result<RedeemResult>
  usecase/RedeemClaimUseCase.kt  # Validación de formato de código/folio
data/
  remote/api/ClaimsApiService.kt # POST /api/v1/offers/claims/{code}/redeem
  remote/dto/ClaimDtos.kt        # RedeemResponseDto
presentation/merchant/
  MerchantSalesContract.kt, MerchantSalesViewModel.kt, MerchantSalesScreen.kt
  QrCameraPreview.kt             # AndroidView CameraX + BarcodeAnalyzer
```

## 5. Criterios de aceptacion

- **Given** un vendedor con permiso de cámara **When** apunta al QR del cliente **Then** lee el código automáticamente, pausa el visor y llama a `POST /api/v1/offers/claims/{code}/redeem`.
- **Given** un código ya canjeado previamente **When** se intenta validar **Then** el backend responde `409` y la UI muestra advertencia clara de código ya utilizado.
- **Given** permiso de cámara denegado **When** el vendedor abre la pantalla **Then** puede validar ingresando el folio manualmente sin crashes.
- **Given** un código de otro comercio **When** intenta canjearse **Then** la API responde `403` y la app bloquea el canje mostrando el motivo.

## 6. Plan de pruebas

- **Unitarias:** `RedeemClaimUseCaseTest` (normalización trim/mayúsculas, rechazo de folio vacío o con caracteres inválidos).
- **ViewModel:** `MerchantSalesViewModelTest` con Turbine (estados Escaneando -> Validando -> Canjeado / Error `403`/`404`/`409` -> Reanudar).
- **Integración (`MockWebServer`):** `POST /api/v1/offers/claims/{code}/redeem` en escenarios `200`, `403`, `404`, `409` y timeout.
- **UI (Compose Test Rule):** validación por ingreso manual de folio, manejo de permiso denegado y visualización de tarjeta de cobro en éxito.

## 7. Riesgos, limites y deuda

- Requiere aprobar enmienda menor a `constitution.md` §3 para agregar `androidx.camera:camera-*` y `com.google.mlkit:barcode-scanning` (decisión D-2) y declarar `uses-permission android:name="android.permission.CAMERA"` con `uses-feature android:name="android.hardware.camera" android:required="false"` en `AndroidManifest.xml`.
- La validación de cámara física requiere prueba manual en dispositivo/emulador con cámara virtual.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución y aprobación D-2 |
