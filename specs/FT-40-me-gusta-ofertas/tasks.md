# TASKS — FT-40 Me gusta en publicaciones de producto

- [x] T40.1 Extender modelos de oferta con `likesCount`/`isLikedByMe` y crear `ToggleOfferLikeUseCase.kt` — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T40.2 Añadir `POST /api/v1/offers/{id}/like` en `OfferDetailsApiService` y repositorio — verif: `py scripts/check_endpoints.py`
- [x] T40.3 Conectar `ProductDetailViewModel`, `ProductSocialLikeRow` y tarjetas de catálogo — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T40.4 Añadir pruebas unitarias de `ToggleOfferLikeUseCase` y `ProductDetailViewModel` para Kover >= 80 % — verif: `.\gradlew.bat testDebugUnitTest koverVerifyDebug`
