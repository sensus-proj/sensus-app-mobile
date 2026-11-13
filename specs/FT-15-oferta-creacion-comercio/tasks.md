# TASKS — FT-15 Publicación de Ofertas del Comerciante y Carga Multipart

> 1 PR = 1 slice. Orden: Dominio -> Compresión/API -> ViewModel -> UI -> Tests -> Gates.

## Fase A — Dominio

- [x] T15.1 Definir `CreateOfferInput`, `UploadedMedia` y `MediaRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T15.2 Ampliar `OffersRepository` con `createOffer(input)` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T15.3 Crear `CreateOfferUseCase` con validaciones puras de título, precios (`original > discount > 0`), `stock >= 1` y expiración futura — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Datos, Compresión y Moderación Fail-Closed

- [x] T15.4 Implementar `ImageCompressor.kt` en `data/media/` garantizando salida JPEG/WebP `<= 4.5 MB` — verif: `./gradlew.bat testDebugUnitTest`
- [x] T15.5 Añadir `@Multipart @POST("api/v1/media/upload")` en `MediaApiService` y `MediaRepositoryImpl` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T15.6 Eliminar el retorno `ModerationResult.Approved` por defecto en `ImageModerationService.kt` asegurando comportamiento *fail-closed* — verif: `./gradlew.bat testDebugUnitTest`
- [x] T15.7 Añadir `@POST("api/v1/offers")` en `OffersApiService` e implementarlo en `OffersRepositoryImpl` sincronizando con `OfferDao` — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Presentación

- [x] T15.8 Actualizar `MerchantNewOfferContract.kt` y `MerchantNewOfferViewModel.kt` para validar campos, calcular `%` ahorro y ejecutar flujo upload + create — verif: `./gradlew.bat testDebugUnitTest`
- [x] T15.9 Añadir selector de fecha/hora límite (`DatePicker`/`TimePicker` M3) en `MerchantNewOfferScreen.kt` y conectar al ViewModel — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [x] T15.10 Registrar `MediaRepository` y `CreateOfferUseCase` en módulos Hilt dedicados (`PublishingModule.kt` / `CommunityModule.kt`) — verif: `./gradlew.bat compileDebugKotlin`
- [x] T15.11 Añadir `CreateOfferUseCaseTest`, tests de `MockWebServer` para subida multipart (`200`, `413`, `422`) y `POST /api/v1/offers` (`201`) — verif: `./gradlew.bat testDebugUnitTest`
- [x] T15.12 Añadir tests Compose UI para formulario de oferta nueva y bloqueo por moderación/precios — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [x] T15.13 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `docs/asvs-l2-checklist.md`, `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_endpoints.py`

## Evidencia local — 2026-11-13

- Cliente implementado para las HU solicitadas, conservando Clean Architecture + MVI, Hilt y SQLCipher.
- Compilación de la app y de las pruebas instrumentadas verificada.
- 146 pruebas unitarias existentes y específicas: 0 fallos, 0 errores, 0 omisiones. Cobertura global Kover: 89.85%.
- Pruebas HTTP con MockWebServer: no se publicaron ofertas ni posts en el servidor real.
- Las pruebas instrumentadas de UI y SQLCipher están escritas y compiladas; su ejecución en dispositivo sigue pendiente.
- La integración con los contratos desplegados del backend y la revisión formal de la slice siguen pendientes.
- Cambios únicamente locales, sin commit, push, tag ni modificación de configuración de escáneres.
