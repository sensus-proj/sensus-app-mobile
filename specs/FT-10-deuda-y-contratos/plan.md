# PLAN — FT-10 Saneamiento de Deuda Técnica, Contratos v1 y Gates Reales

> Derivado de `spec.md`. Historias/Alcance: Constitución §4, §9, §11 y §12.
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Gates estáticos reales.** Reemplazar el stub `StaticQualityCheckTask` en `app/build.gradle.kts` por los plugins oficiales `io.gitlab.arturbosch.detekt` y `org.jlleitschuh.gradle.ktlint`, con `config/detekt/detekt.yml` versionado.
2. **Endurecimiento de manifiesto.** Ajustar `android:allowBackup="false"` y `android:usesCleartextTraffic="false"` en `app/src/main/AndroidManifest.xml`, unificando `app/src/main/res/xml/network_security_config.xml` (solo CAs del sistema y HTTPS obligatorio también en `debug`).
3. **Alineación de rutas `/api/v1/*`.** Migrar `AuthApiService`, `UserApiService` y `MerchantApiService` a `/api/v1/auth/*`, `/api/v1/users/me` y `GET /api/v1/merchants/me` + `PUT /api/v1/merchants/profile` (corrigiendo `api/v1/comercios/me`).
4. **Logout verificable (Spec 004).** Incorporar `POST /api/v1/auth/logout` en `AuthApiService` y ejecutarlo en `LogoutUseCase` antes de purgar `EncryptedSharedPreferences` y `SensusDatabase`.
5. **Acotamiento transicional de `MockDataProvider`.** Mantener `MockDataProvider` restringido al fallback offline de `StoreRepositoryImpl` y `OffersRepositoryImpl` hasta que las olas `FT-11..FT-26` (`RF-11-07`) sustituyan cada agregado, con fixtures dedicados en `src/test/` y `src/androidTest/` y cero usos en `presentation/`, `domain/` ni Auth/Profile/Merchant.
6. **Purga visual y legal.** Retirar `SensusAmber`, el OTP prellenado `"7294"` en `OtpVerificationScreen` y el texto de compartir por WhatsApp en `CommunityScreen`; enlazar `https://rodnix.com.mx/sensus/privacidad.html` en `TermsScreen`.

## 2. UI y navegación

- `OtpVerificationScreen` (`presentation/auth/otp/`) inicia con código vacío y validación real.
- `TermsScreen` (`presentation/legal/`) abre `https://rodnix.com.mx/sensus/privacidad.html` con `UriHandler`.
- `CommunityScreen` (`presentation/community/`) usa `Intent.ACTION_SEND` genérico sin mención a WhatsApp.
- Previews: `OtpEmpty`, `OtpError`, `TermsDefault`.

## 3. DI

| Binding | Implementación |
|---|---|
| `AuthApiService` | Retrofit `/api/v1/auth/*` en `NetworkModule` |
| `UserApiService` | Retrofit `/api/v1/users/*` en `NetworkModule` |
| `MerchantApiService` | Retrofit `/api/v1/merchants/*` en `NetworkModule` |
| `LogoutUseCase` | Inyección por constructor con `AuthRepository` + `SessionRepository` |

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

1. Configurar plugins reales `detekt` y `ktlint` en `gradle/libs.versions.toml` y `app/build.gradle.kts`.
2. Endurecer `AndroidManifest.xml` (`allowBackup=false`, `usesCleartextTraffic=false`).
3. Alinear `AuthApiService`, `UserApiService` y `MerchantApiService` a `/api/v1/*`.
4. Implementar `POST /api/v1/auth/logout` en `LogoutUseCase` y `SessionRepositoryImpl`.
5. Acotar `MockDataProvider` al puente transicional de `StoreRepositoryImpl` y `OffersRepositoryImpl` hasta `FT-11..FT-26`, con cero referencias en `presentation/`, `domain/` ni Auth/Profile/Merchant.
6. Corregir `OtpVerificationScreen`, `TermsScreen` y `CommunityScreen`.
7. Ejecutar `MockWebServer` tests y los 5 gates Python.

## 6. Prohibiciones

- Prohibido mantener `StaticQualityCheckTask` simulado en `app/build.gradle.kts`.
- Prohibido importar `MockDataProvider` desde `presentation/`, `domain/` o repositorios ya conectados al backend (`Auth`, `Profile`, `Merchant`); su alcance en `main/` queda limitado al fallback transicional de `StoreRepositoryImpl` y `OffersRepositoryImpl` hasta `FT-11..FT-26`.
- Prohibido usar colores ámbar/amarillos (`#F59E0B`, `SensusAmber`) o emojis en UI.
- Prohibido generar APK/AAB durante la verificación (`assembleDebug`/`assembleRelease`).

## Tareas

Ver `tasks.md`.
