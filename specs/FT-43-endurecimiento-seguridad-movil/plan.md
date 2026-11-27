# PLAN — FT-43 Endurecimiento de seguridad móvil (G-07, G-09, G-04)

## 1. Diseño por capa

- **Dominio (`domain/security/`):**
  - `DeviceAuthenticator` y `DeviceAuthOutcome` (`Authenticated`, `Cancelled`, `NoDeviceCredential`, `Error(val message: String)`): contrato puro en Kotlin sin dependencias Android.
- **Red / Recursos (`app/src/main/res/xml/`):**
  - `network_security_config.xml`: `<domain-config cleartextTrafficPermitted="false">` para `execute-api.us-east-2.amazonaws.com` (`includeSubdomains="true"`) con `<pin-set expiration="2027-11-26">` y los 5 pins SHA-256 SPKI verificados (`AmazonRootCA1..4` + `SFSRootCAG2`).
- **Presentación (`presentation/`):**
  - `presentation/common/SecureScreenEffect.kt`: utilitario `@Composable` con mapa por `Window` (`WeakHashMap<Window, Int>`) para llevar conteo de referencias y asegurar que `FLAG_SECURE` nunca se retire mientras quede otra pantalla o diálogo seguro activo. Aplicado en `MerchantSalesScreen`, `ClaimOfferScreen`, `MyClaimsScreen`, `ActiveApartadoScreen`, `ForgotPasswordScreens`, `ConfirmDialogs` (`ConfirmDeleteAccountDialog`), `BuyerProfileScreen`, `MerchantProfileScreen`, `LoginScreen`, `RegisterScreen` y `OtpVerificationScreen`.
  - `MainActivity.kt`: cambia de `ComponentActivity` a `androidx.fragment.app.FragmentActivity` para soportar `BiometricPrompt`.
  - `presentation/common/AndroidDeviceAuthenticator.kt` + composable `rememberDeviceAuthenticator()`: envuelve `BiometricPrompt` y `BiometricManager.Authenticators.BIOMETRIC_STRONG or DEVICE_CREDENTIAL`, con helper Compose y compuerta para revelar QR activo (`ClaimOfferScreen` / `MyClaimsScreen`) y confirmar eliminación de cuenta (`ConfirmDeleteAccountDialog`).
- **Build & CI (`gradle/`, `.github/workflows/`):**
  - Dependencia `androidx.biometric:biometric:1.1.0` (Apache 2.0, USD $0, AndroidX oficial) declarada en `gradle/libs.versions.toml` y `app/build.gradle.kts`.
  - Plugin `org.cyclonedx.bom` versión `3.6.1` (Apache 2.0, USD $0, solo build-time) configurado en `gradle/libs.versions.toml`, `build.gradle.kts` y `app/build.gradle.kts` para `releaseRuntimeClasspath` generando `app/build/reports/bom.json`.
  - Flujos `.github/workflows/ci.yml` y `.github/workflows/release-playstore.yml`: ejecutan `./gradlew :app:cyclonedxDirectBom`, suben `app/build/reports/bom.json` y añaden `actions/attest-build-provenance@4d101475d8b20a2381f78447822ac1eab6504dd8 # v4.2.2`.

## 2. Modelo de amenazas (STRIDE)

| Categoría | Amenaza en scope | Mitigación en `FT-43` |
|---|---|---|
| **Spoofing** | Suplantación del API Gateway mediante CA fraudulenta o proxy local confiando en almacén de usuario | Pinning de raíces Amazon Trust Services + respaldo Starfield en `network_security_config.xml` (`G-07`). |
| **Tampering** | Alteración de artefactos APK/AAB o dependencias no inventariadas entre compilación CI y distribución | Generación CycloneDX SBOM sobre `releaseRuntimeClasspath` + `actions/attest-build-provenance` SLSA en CI/release (`G-04`). |
| **Repudiation** | Confirmación accidental o no autorizada de eliminación de cuenta sin presencia física del dueño | Reautenticación local con biometría/PIN del dispositivo antes de enviar la baja (`G-09`). |
| **Information Disclosure** | Captura de pantalla / miniatura en Recents de folio QR canjeable, credenciales o datos personales | `SecureScreenEffect` (`FLAG_SECURE`) con ref-counting por ventana + bloqueo biométrico previo para revelar QR activo (`G-09`). |
| **Denial of Service** | Bloqueo permanente ante rotación de certificado hoja o intermedio AWS | Inclusión de las 4 raíces Amazon Root CA y raíz Starfield (`SFSRootCAG2`) con expiración `2027-11-26` + manejo explícito de errores de biometría sin crashes. |
| **Elevation of Privilege** | Exposición de QR o eliminación de cuenta si un tercero toma el teléfono desbloqueado | Solicitud de credencial fuerte/dispositivo (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`) en `MainActivity : FragmentActivity`. |

## 3. Verificación y Constitution Check

- **Cumplimiento Free Tier:** USD $0; sin servicios nuevos de nube ni infraestructura pagada.
- **Pruebas unitarias (`app/src/test`):** `NetworkPinningTest`, `SecureScreenEffectTest` (con Robolectric / conteo de flags en `Window`), `DeviceAuthenticationGateTest`.
- **Pruebas instrumentadas (`app/src/androidTest`):** verificación de XML/pines reales y vistas protegidas compiladas con `compileDebugAndroidTestSources`.
- **Constitution Check:** PASS contra principios de arquitectura en capas, seguridad OWASP MASVS L2 + resiliencia, sin secretos y cobertura `>= 80 %`.
