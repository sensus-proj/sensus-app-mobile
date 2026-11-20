# PLAN — FT-32 Filtro local de groserías y ajustes de UX

> Derivado de `spec.md` y alineado a `sensus-back/specs/057-moderacion-texto-filtro-groserias`.
> Rector: `.specify/memory/constitution.md` §2, §4, §5 y §10.

## 1. Diseño

1. Objeto puro `ProfanityFilter` en `domain/moderation/ProfanityFilter.kt` invocado desde casos de uso (`RegisterUseCase`, `UpdateProfileUseCase`, `ActivateMerchantUseCase`, `UpdateMerchantProfileUseCase`, `CreateOfferUseCase`, `UpdateOfferUseCase`, `StoreReviewUseCases`).
2. `SplashScreen.kt` refactorizado con `TextureView` + `MediaPlayer` sin sonido, escalado proporcional y `DisposableEffect` seguro.
3. Ajuste de `statusBarsPadding()` en `BuyerProfileScreen.kt` y `MerchantProfileScreen.kt`.

## 2. UI y navegación

- Sin rutas nuevas; mejora la transición de arranque y la ergonomía superior de perfiles.

## 3. DI

| Binding | Implementación |
|---|---|
| `ProfanityFilter` | Objeto Kotlin puro en `domain/moderation` (sin estado ni DI requerida) |

## 4. Gates

- `compileDebugKotlin`, `detekt`, `ktlintCheck`, `lint`, `testDebugUnitTest koverVerifyDebug`
- `check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 80`

## 5. Orden de implementación

1. Implementar `ProfanityFilter` en `domain/moderation/` e integrarlo en casos de uso.
2. Ajustar `SplashScreen` e *insets* de perfil.
3. Añadir cobertura unitaria completa en `ProfanityFilterTest`.

## 6. Prohibiciones

- No modificar `OfferCategory` ni lógica de categorías al añadir validación de texto.
