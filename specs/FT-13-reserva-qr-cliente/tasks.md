# TASKS — FT-13 Reserva / Apartado con Folio y Código QR Real (Cliente)

> 1 PR = 1 slice. Orden: Dominio -> DB/API -> Generador QR -> ViewModel/UI -> DI -> Tests -> Gates.

## Fase A — Dominio

- [ ] T13.1 Definir `ClaimRecord`, `ClaimStatus` (`ACTIVE`, `REDEEMED`, `CANCELLED`, `EXPIRED`) y `ClaimsRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T13.2 Crear `CreateClaimUseCase`, `GetMyClaimsUseCase` y `CancelClaimUseCase` (bloqueando modo invitado) — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Persistencia SQLCipher y API

- [ ] T13.3 Crear `ClaimEntity.java` y `ClaimDao.java` y registrar en `SensusDatabase.java` + `DatabaseModule.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T13.4 Crear `ClaimDtos.kt`, `ClaimsApiService.kt` (`POST /api/v1/offers/{id}/claim`, borrador `GET /api/v1/offers/claims`, `PATCH /api/v1/offers/claims/{id}/cancel`) y `ClaimMapper.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T13.5 Implementar `ClaimsRepositoryImpl` con guardado offline-first en `ClaimDao` y mapeo de `409 Conflict` — verif: `./gradlew.bat testDebugUnitTest`

## Fase C — Generador QR y Presentación

- [ ] T13.6 Agregar `com.google.zxing:core` en `gradle/libs.versions.toml` (previa aprobación de D-2) y crear `QrCodeBitmapGenerator.kt` — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T13.7 Actualizar `ActiveApartadoScreen.kt` (`P-16`/`P-18`) para renderizar QR real, folio y cuenta regresiva desde `expiresAt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T13.8 Crear `MyClaimsContract.kt`, `MyClaimsViewModel.kt` y `MyClaimsScreen.kt` (`P-21`) con filtros por estado — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T13.9 Conectar `"Mis apartados y pedidos"` en `BuyerProfileScreen.kt` y el botón cancelar (`P-20`) — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — DI, Pruebas y Cierre

- [ ] T13.10 Registrar `ClaimsApiService`, `ClaimsRepository` y casos de uso en `AppModule.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T13.11 Añadir tests unitarios (`CreateClaimUseCaseTest`, `QrCodeBitmapGeneratorTest`, `MyClaimsViewModelTest`) e integración (`ClaimDao` SQLCipher + `MockWebServer` 201/409) — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T13.12 Añadir tests Compose UI para `ActiveApartadoScreen` (folio/QR real) y `MyClaimsScreen` — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T13.13 Actualizar `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `Memoria.md` §6 y verificar todos los gates — verif: `python scripts/check_endpoints.py`
