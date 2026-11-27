# TASKS — FT-43 Endurecimiento de seguridad móvil (G-07, G-09, G-04)

## Fase A — C1 Certificate Pinning (`G-07`)

- [x] T43.1 Configurar `<domain-config>` para `execute-api.us-east-2.amazonaws.com` (`includeSubdomains="true"`) y `<pin-set expiration="2027-11-26">` con las 5 huellas SPKI SHA-256 de Amazon Root CA 1..4 y `SFSRootCAG2` en `app/src/main/res/xml/network_security_config.xml` — verif: `NetworkPinningTest` + `NetworkSecurityConfigTest`.
- [x] T43.2 Añadir pruebas unitarias (`OkHttp CertificatePinner`) e instrumentadas validando pines, expiración e igualdad con los certificados verificados por TLSv1.3 — verif: `testDebugUnitTest` (`548/548`) y `compileDebugAndroidTestSources` PASS.

## Fase B — C2 `FLAG_SECURE` transversal (`G-09`)

- [x] T43.3 Crear `presentation/common/SecureScreenEffect.kt` y `WindowSecureFlagRegistry.kt` con conteo de referencias por `Window` y reemplazar el `DisposableEffect` ad-hoc de `MerchantSalesScreen.kt` — verif: `SecureScreenEffectTest` PASS.
- [x] T43.4 Aplicar `SecureScreenEffect` en pantallas/diálogos sensibles (`ClaimOfferScreen`, `MyClaimsScreen`, `ActiveApartadoScreen`, `ForgotPasswordScreens`, `ConfirmDeleteAccountDialog`, `BuyerProfileScreen`, `MerchantProfileScreen`, `LoginScreen`, `RegisterScreen`, `OtpVerificationScreen`) — verif: `compileDebugKotlin`, `detekt` y `ktlintCheck` PASS.

## Fase C — C3 Autenticación biométrica y credencial (`G-09`)

- [x] T43.5 Declarar `androidx.biometric:biometric:1.1.0` en `libs.versions.toml` y `app/build.gradle.kts`, y migrar `MainActivity` a `FragmentActivity` — verif: `compileDebugKotlin` PASS.
- [x] T43.6 Crear `domain/security/DeviceAuthenticator.kt`, `AndroidDeviceAuthenticator.kt` y `DeviceAuthGate.kt` (`SensitiveQrGate` + `LocalDeviceAuthenticator`) para proteger revelación de QR (`ClaimOfferScreen`, `MyClaimsScreen`) y `ConfirmDeleteAccountDialog` — verif: `DeviceAuthenticatorTest`, `DeviceAuthUiTest` (`androidTest` compilada) y `check_layers.py` (`378 archivos`) PASS.

## Fase D — C4 SBOM CycloneDX + Build Provenance (`G-04`)

- [x] T43.7 Configurar plugin `org.cyclonedx.bom` (`3.6.1`) sobre `releaseRuntimeClasspath` para generar `app/build/reports/bom.json` — verif: `./gradlew.bat :app:cyclonedxDirectBom` (`CycloneDX 1.6`, `199` componentes) PASS.
- [x] T43.8 Cablear generación/carga de SBOM y `actions/attest-build-provenance` (fijado por SHA `4d101475d8b20a2381f78447822ac1eab6504dd8`) en `.github/workflows/ci.yml` y `.github/workflows/release-playstore.yml`, actualizar `docs/asvs-l2-checklist.md`, `specs/README.md` y correr gates completos — verif: sección 6 completa PASS (`Kover 80.80%`, `548/548` unitarias, `5/5` Python).
