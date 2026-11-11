# PLAN — FT-15 Creación de Ofertas por Comerciante, Compresión y Moderación

> Derivado de `spec.md`. Historias/Alcance: SE-019, SE-020, SE-021, SE-022 (Pantalla P-32; Componentes C-45, C-46, C-47).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Compresión de imagen en cliente (`SE-020`).** `ImageCompressor` en `data/media/` redimensiona la imagen seleccionada a máx. `1600 px` y comprime en JPEG/WebP `<= 1 MB` en `Dispatchers.IO` antes del envío.
2. **Validación de precios y vigencia (`SE-019`, `SE-021`).** `CreateOfferUseCase` valida en dominio `offerPrice < originalPrice`, `stock >= 1`, `expiresAt > startsAt` e imagen presente.
3. **Envío atómico y moderación (`SE-022`).** `POST /api/v1/offers` envía imagen y texto en una sola solicitud; el backend ejecuta moderación fail-closed antes de Cloudinary.

## 2. UI y navegación

- `CreateOfferScreen` (`P-32`) usa `PhotoPickerSlots` (`C-45`), `DiscountCalculatorInputs` (`C-46`), `QuickDurationChips` (`C-47`) y banner de rechazo de moderación (`422`).
- Previews: `FormEmpty`, `FormValidWithDiscount`, `SubmittingModeration`, `ModerationRejected`.

## 3. DI

| Binding | Implementación |
|---|---|
| `ImageCompressor` | `AndroidImageCompressor` en `DataModule` |
| `CreateOfferUseCase` | Inyección por constructor con `OffersRepository` |

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

1. Crear `CreateOfferCommand`, `ModerationVerdict` y `CreateOfferUseCase` en `domain/`.
2. Implementar `ImageCompressor` (`<= 1 MB`, `<= 1600 px`) y endpoint `POST /api/v1/offers`.
3. Crear `CreateOfferContract` y `CreateOfferViewModel` MVI.
4. Conectar `CreateOfferScreen` (`P-32`) al ViewModel real con Photo Picker nativo.
5. Añadir tests unitarios de reglas de negocio y `MockWebServer` para `201`, `422` y `503`.

## 6. Prohibiciones

- Prohibido subir imágenes directo a Cloudinary desde la app sin pasar por el backend.
- Prohibido publicar una oferta si la moderación devuelve rechazo o timeout (fail-closed).

## Tareas

Ver `tasks.md`.
