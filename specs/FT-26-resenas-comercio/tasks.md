# TASKS — FT-26 Detalle Público del Comercio y Reseñas Post-Canje [BORRADOR]

> Estado: Borrador (bloqueada hasta aprobación de contrato en `sensus-back`, D-6).

- [ ] T26.1 Acordar contrato OpenAPI en `sensus-back` para `/api/v1/merchants/{id}`, `/reviews` y `POST /api/v1/reviews` — verif: revisión de contrato
- [ ] T26.2 Crear modelos públicos de comercio/reseña y `ReviewsRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T26.3 Crear `GetMerchantPublicProfileUseCase`, `GetMerchantReviewsUseCase` y `SubmitReviewUseCase` (rango `1..5`, solo `REDEEMED`) — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T26.4 Crear `ReviewsApiService.kt` y `ReviewsRepositoryImpl.kt` — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T26.5 Crear `MerchantPublicDetailScreen.kt` (`P-15`, `C-28..C-31`) y `SubmitReviewDialog.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T26.6 Añadir tests unitarios, `MockWebServer` y Compose UI con Kover `>= 80%` — verif: `python scripts/check_coverage.py --min 80`
