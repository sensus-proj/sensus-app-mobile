# TASKS — FT-26 Reseñas de comercio estilo Play Store y calificación real

- [x] T26.1 Alinear spec `FT-26` a `sensus-back/specs/056-resenas-comercios` y actualizar `docs/endpoints-internos.md` — verif: `py scripts/check_endpoints.py`
- [x] T26.2 Crear modelos (`StoreReviewModels.kt`), puerto (`StoreReviewRepository.kt`) y casos de uso (`StoreReviewUseCases.kt`) — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T26.3 Implementar `StoreReviewsApiService.kt` y `StoreReviewRepositoryImpl.kt` con DI en Hilt — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T26.4 Implementar `StoreReviewsScreen`, `StoreReviewsViewModel` (MVI) y apertura desde `StoreCatalogHeaderCard` — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T26.5 Conectar calificación real en `MerchantProfileScreen` y reemplazar cualquier color amarillo/ámbar de estrellas por `SensusPurple` (Constitución §9.2) — verif: `.\gradlew.bat lint`
- [x] T26.6 Ampliar pruebas unitarias en `StoreReviewsViewModelTest` y `MerchantProfileViewModelTest` para alcanzar Kover `>= 80%` — verif: `.\gradlew.bat testDebugUnitTest koverVerifyDebug`
