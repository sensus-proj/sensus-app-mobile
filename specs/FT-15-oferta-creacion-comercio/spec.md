# SPEC — FT-15 Publicación de Ofertas del Comerciante y Carga Multipart

> Rector: `.specify/memory/constitution.md` §2, §4.5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-019, SE-020, SE-021, SE-022**; Pantalla **P-32**; Componentes **C-43..C-46**.
> Backend ref: Specs `017-moderacion-multimodal`, `022-carga-multimedia`, `034-creacion-oferta`.

## 1. Contexto y objetivo

`MerchantNewOfferScreen` y `MerchantNewOfferViewModel` actualmente crean objetos `ProductOffer` únicamente locales en `OfferDao`/memoria, llamando de forma aislada a `ImageModerationService` (el cual aprueba por defecto si `mediaApi == null`) sin enviar la oferta ni la imagen multipart real al backend (`POST /api/v1/media/upload`, `POST /api/v1/offers`).

**Objetivo:** conectar el formulario de publicación del comerciante con carga multipart de imagen comprimida (`<= 4.5 MB`, moderada en backend fail-closed antes de almacenarse en Cloudinary) y creación real de la oferta en `POST /api/v1/offers` con validación estricta de precios, descuento calculado, stock `>= 1` y fecha/hora límite de vigencia.

**Alcance:** `CreateOfferUseCase`, `UploadModerationMediaUseCase`, compresión de imagen (JPEG/WebP `<= 4.5 MB`), `MediaApiService` multipart (`POST /api/v1/media/upload`), `POST /api/v1/offers`, validación de texto+imagen previa al guardado, actualización de `MerchantNewOfferScreen` (`P-32`).
**Fuera de alcance:** edición o pausa de ofertas existentes (FT-16).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-15-01 | Validar en `domain/usecase` que título no sea vacío, `discountPrice > 0`, `originalPrice > discountPrice`, `stock >= 1` y `expiresAt` sea futura respecto al reloj actual. |
| RF-15-02 | Calcular y mostrar en tiempo real el porcentaje de descuento `(original - oferta) / original * 100` y el ahorro neto sin guardar inconsistencias. |
| RF-15-03 | Al seleccionar foto de cámara o galería, comprimirla en `data/` a JPEG/WebP verificando tamaño `<= 4.5 MB` antes de enviarla por `MultipartBody.Part` a `POST /api/v1/media/upload`. |
| RF-15-04 | `ImageModerationService` DEBE operar estrictamente *fail-closed* (sin aprobar por defecto en producción si no hay red o falla el cliente) y el backend modera texto e imagen antes de persistir o publicar. |
| RF-15-05 | Enviar `POST /api/v1/offers` con `mediaId`/URL devuelto, precios, stock, categoría y `expiresAt` UTC; al recibir `201 Created`, insertar la nueva oferta en `OfferDao` y notificar éxito. |
| RF-15-06 | Si la imagen o texto es rechazado por moderación (`400`/`422`), bloquear la publicación y mostrar mensaje claro sin crear la oferta. |

## 3. Requisitos no funcionales

- **Seguridad:** nunca incluir API keys de OpenAI o Sightengine en Android; toda moderación e ingesta a Cloudinary se ejecuta exclusivamente en el servidor.
- **Tamaño de archivo:** rechazar en cliente archivos que superen 4,5 MB tras compresión antes de consumir ancho de banda (`NFR-022-01`).
- **Offline:** si no hay red al intentar publicar, bloquear con mensaje claro o encolar borrador en `PendingSyncDao` sin publicar falsamente como activo.

## 4. Arquitectura y contratos internos

```text
domain/
  model/StoreModels.kt          # CreateOfferInput, UploadedMedia
  repository/MediaRepository.kt # uploadAndModerate(bytes, mimeType): Result<UploadedMedia>
  repository/OffersRepository.kt# createOffer(input): Result<ProductOffer>
  usecase/CreateOfferUseCase.kt # Validación pura de precios, stock y fecha futura
data/
  remote/api/MediaApiService.kt # @Multipart @POST("api/v1/media/upload")
  remote/api/OffersApiService.kt# @POST("api/v1/offers")
  moderation/ImageModerationService.kt # Fail-closed estricto (retirar fallback Approved)
  media/ImageCompressor.kt
presentation/merchant/
  MerchantNewOfferContract.kt, MerchantNewOfferViewModel.kt, MerchantNewOfferScreen.kt
```

## 5. Criterios de aceptacion

- **Given** un comerciante ingresa precio de oferta mayor o igual al original **When** intenta publicar **Then** el caso de uso bloquea el envío con error de validación.
- **Given** una imagen marcada como inapropiada por el backend **When** se sube a `POST /api/v1/media/upload` **Then** se rechaza `fail-closed` y no se invoca `POST /api/v1/offers`.
- **Given** datos válidos e imagen aprobada **When** pulsa Publicar **Then** se sube la imagen, se crea la oferta en `POST /api/v1/offers` y se guarda en `OfferDao`.
- **Given** caída de red durante la moderación **When** intenta subir imagen **Then** nunca se aprueba localmente (`ModerationResult.Rejected`).

## 6. Plan de pruebas

- **Unitarias:** `CreateOfferUseCaseTest` (tabla de precios inválidos, stock `<= 0`, fechas pasadas, cálculo de porcentaje), `ImageCompressorTest`.
- **ViewModel:** `MerchantNewOfferViewModelTest` con Turbine (subiendo imagen -> creando oferta -> error moderación -> error red).
- **Integración (`MockWebServer`):** subida `@Multipart` a `POST /api/v1/media/upload` (200, 413, 422, timeout fail-closed) y `POST /api/v1/offers` (201).
- **UI (Compose Test Rule):** validación de precios en formulario, selección de fecha/hora límite y alerta de moderación.

## 7. Riesgos, limites y deuda

- Depende de la publicación en backend de `POST /api/v1/media/upload` (Spec 022) y `POST /api/v1/offers` (Spec 034).
- Revisar en `FT-10` el endpoint transitorio `POST /api/v1/media/moderate` para consolidarlo con `POST /api/v1/media/upload`.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
