# TASKS — FT-10 Saneamiento de Deuda Técnica y Alineación de Contratos

> 1 PR = 1 slice. Orden: Build/Seguridad -> Contratos/Dominio -> UI/Navegación -> Pruebas -> Docs/Gates.

## Fase A — Build, Seguridad y Análisis Estático Real

- [x] T10.1 Declarar versiones de plugins Detekt y Ktlint en `gradle/libs.versions.toml` — verif: `./gradlew.bat help` (`detekt = "1.23.8"`, `ktlint = "12.2.0"`)
- [x] T10.2 Reemplazar `StaticQualityCheckTask` en `app/build.gradle.kts` por los plugins oficiales `detekt` y `ktlintCheck` — verif: `./gradlew.bat detekt ktlintCheck`
- [x] T10.3 Resolver todas las violaciones reportadas por `detekt` y `ktlintCheck` en `app/src/` sin bajar reglas — verif: `./gradlew.bat detekt ktlintCheck` (`BUILD SUCCESSFUL`, 0 violaciones)
- [x] T10.4 Cambiar `android:allowBackup="false"` y configurar `usesCleartextTraffic="false"` en `AndroidManifest.xml` (moviendo excepciones locales a `debug`) — verif: `./gradlew.bat lint` (`BUILD SUCCESSFUL`)

## Fase B — Contratos `/api/v1` y Logout Real

- [x] T10.5 Actualizar `MerchantApiService.getMyComercio()` de `api/v1/comercios/me` a `api/v1/merchants/me` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T10.6 Añadir `POST /api/v1/auth/logout` en `AuthApiService`, `AuthRepository` y `RetrofitAuthRepository` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T10.7 Crear `LogoutUseCase` en `domain/usecase/` y conectarlo a `AppViewModel` / `BuyerProfileViewModel` — verif: `./gradlew.bat testDebugUnitTest`
- [x] T10.8 Auditar endpoints divergentes (`auth/guest`, `auth/reset-password`, `users/photo-signature`, `media/moderate`) y registrar resolución en `docs/endpoints-internos.md` — verif: `python scripts/check_endpoints.py` (`PASS (0 hallazgos, 54 endpoints)`)

## Fase C — Saneamiento UI y Navegación Tipada

- [x] T10.9 Retirar `SensusAmber` de `OtpVerificationScreen.kt` y `TermsScreen.kt` y eliminar `"7294"` prellenado — verif: `./gradlew.bat compileDebugKotlin`
- [x] T10.10 Añadir enlace clicable a `https://rodnix.com.mx/sensus/privacidad.html` en `TermsScreen.kt` — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [x] T10.11 Retirar invitación/texto de WhatsApp en `CommunityScreen.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T10.12 Declarar claves `@Serializable` `NavKey` en `NavigationKeys.kt` y migrar `Navigation.kt` a Navigation 3 centralizado — verif: `./gradlew.bat compileDebugKotlin`

## Fase D — Pruebas, Conciliación Documental y Gates

- [x] T10.13 Añadir `LogoutUseCaseTest` y tests `MockWebServer` para `POST /api/v1/auth/logout` y `GET /api/v1/merchants/me` — verif: `./gradlew.bat testDebugUnitTest` (`100` unit tests, `0` fallos)
- [x] T10.14 Añadir tests Compose UI para enlace de privacidad en `TermsScreen` y limpieza en `CommunityScreen` / `OtpVerificationScreen` — verif: `./gradlew.bat compileDebugAndroidTestSources` (`FT10UiSanitizationTest.kt`)
- [x] T10.15 Actualizar `specs/FT-07-guest-mode-2km/` para conciliar arquitectura real y actualizar `Componentes_Movil_Sensus.xlsx` con `Sensus_Movil_Componentes_y_CRUD.xlsx` del Drive — verif: `python scripts/check_sdd.py` (`PASS (0 hallazgos, 28 features)`)
- [x] T10.16 Ejecutar suite completa (unitarias, Kover `>= 80%`, `lint`, `detekt`, `ktlintCheck`, 5 scripts Python) y actualizar `Memoria.md` §6 — verif: `python scripts/check_coverage.py --min 80` (`PASS 93.23%`)
