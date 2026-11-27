# SPEC — FT-43 Endurecimiento de seguridad móvil (G-07, G-09, G-04)

> Feature transversal de cierre de brechas de seguridad móvil del Apéndice F constitucional (nivel fintech).

## 1. Contexto y objetivo

La auditoría de seguridad móvil (`constitution.md` Apéndice F y `docs/asvs-l2-checklist.md`) mantiene abiertas tres brechas de defensa en profundidad del cliente Android:

- **G-07 (Certificate Pinning):** ausencia de fijación de certificados TLS para el API Gateway en AWS (`*.execute-api.us-east-2.amazonaws.com`).
- **G-09 (`FLAG_SECURE` y autenticación biométrica / credencial de dispositivo):** exposición potencial de pantallas sensibles (QR de apartado, perfil, autenticación, recuperación y eliminación de cuenta) en capturas/recientes y ejecución de acciones de alto riesgo (mostrar QR de apartado activo y eliminar cuenta) sin verificación local del dispositivo.
- **G-04 (SBOM + Build Provenance):** ausencia de generación formal de SBOM CycloneDX en `releaseRuntimeClasspath` y de atestación criptográfica de procedencia SLSA (`actions/attest-build-provenance`) en los flujos de CI/release.

## 2. Alcance

- **C1 (G-07):** Configurar `<pin-set>` con expiración explícita en `app/src/main/res/xml/network_security_config.xml` para `execute-api.us-east-2.amazonaws.com` (`includeSubdomains="true"`) usando las huellas SHA-256 SPKI reales de las raíces de Amazon Trust Services (Amazon Root CA 1..4) más raíz de respaldo Starfield (`SFSRootCAG2`). Agregar pruebas unitarias (OkHttp `CertificatePinner` / `MockWebServer`) e instrumentadas sobre el XML publicado.
- **C2 (G-09 `FLAG_SECURE`):** Extraer un efecto reutilizable con conteo de referencias por ventana (`SecureScreenEffect`) y aplicarlo en pantallas/modales de QR de apartado, canje comercial, login/registro/OTP, recuperación de contraseña, perfiles con datos de cuenta y confirmación de eliminación de cuenta.
- **C3 (G-09 Biometría / Credencial de dispositivo):** Integrar `androidx.biometric:biometric` (Apache 2.0, USD $0) con contrato limpio en `domain/security/DeviceAuthenticator` e implementación `AndroidDeviceAuthenticator` en `presentation/common/` (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`), habilitando `FragmentActivity` en `MainActivity` y requiriendo verificación previa al mostrar el QR de apartado activo (`ClaimOfferScreen` / `MyClaimsScreen`) y antes de ejecutar la eliminación de cuenta (`ConfirmDeleteAccountDialog`).
- **C4 (G-04 SBOM + Build Provenance):** Incorporar el plugin Gradle `org.cyclonedx.bom` (Apache 2.0, USD $0, solo tiempo de compilación) para generar `bom.json` en `releaseRuntimeClasspath`, publicar el artefacto SBOM y firmar procedencia SLSA con `actions/attest-build-provenance` (fijado por SHA completo) en `.github/workflows/ci.yml` y `.github/workflows/release-playstore.yml`.
- **Fuera de alcance:** `G-08` (Play Integrity API), que requiere habilitación manual en Google Cloud / Play Console y cuenta de servicio en SSM (`plan §3`).

## 3. Requisitos funcionales

- **RF-43-01 (`G-07`):** Toda comunicación HTTPS hacia `*.execute-api.us-east-2.amazonaws.com` valida la cadena TLS contra las huellas SPKI fijadas en `network_security_config.xml` con fecha de expiración `2027-11-26` (con múltiples pines de respaldo).
- **RF-43-02 (`G-09` `FLAG_SECURE`):** El flag `WindowManager.LayoutParams.FLAG_SECURE` permanece activo mientras haya al menos un composable seguro montado sobre la `Window`, sin apagarse prematuramente al cerrar un diálogo hijo cuando la pantalla padre también es segura.
- **RF-43-03 (`G-09` Autenticación del dispositivo):** Antes de revelar el QR de un apartado activo (`ClaimOfferScreen` o listado `MyClaimsScreen`) o confirmar la eliminación de cuenta (`ConfirmDeleteAccountDialog`), la app solicita verificación biométrica o credencial de pantalla del dispositivo mediante `DeviceAuthenticator`.
- **RF-43-04 (`G-09` Manejo seguro de error/cancelación):** Si el usuario cancela o falla la verificación, el QR no se revela y la eliminación de cuenta no se ejecuta; si el dispositivo carece de biometría/PIN configurado, se aplica manejo explícito y auditable sin omitir silenciosamente el flujo de eliminación ni filtrar el secreto del QR.
- **RF-43-05 (`G-04` SBOM y Procedencia):** `./gradlew :app:cyclonedxDirectBom` genera el inventario CycloneDX (`bom.json`) sin secretos y los flujos de GitHub Actions suben el SBOM y emiten `attest-build-provenance` sobre artefactos de build/release.

## 4. Requisitos no funcionales y restricciones

- **NFR-43-01 (Capas):** `domain/security/` no importa `android.*`, `androidx.*`, ni librerías externas.
- **NFR-43-02 (Costo Free Tier):** Cero costo mensual adicional; las dependencias añadidas usan licencia Apache 2.0 / MIT.
- **NFR-43-03 (Observabilidad segura):** Cero registro de secretos, huellas privadas ni tokens en logs; manejo explícito de excepciones.
- **NFR-43-04 (Cobertura y linters):** `detekt`, `ktlintCheck`, `lint`, `koverVerifyDebug` (`>= 80%`) y `5/5` scripts Python en verde.

## 5. Criterios de aceptación y contratos

- **CA-43-01:** `network_security_config.xml` incluye los 5 pins SHA-256 verificados contra Amazon Trust Services y la cadena observada en TLSv1.3 de `zqqdwovx81.execute-api.us-east-2.amazonaws.com`, validados por prueba unitaria e instrumentada.
- **CA-43-02:** `SecureScreenEffect` posee prueba unitaria que verifica activación de `FLAG_SECURE`, conteo de referencias con múltiples composables anidados y limpieza al desmontar el último.
- **CA-43-03:** Las pruebas unitarias de `ClaimOfferScreen` / `MyClaimsScreen` / `ConfirmDeleteAccount` / `DeviceAuthenticator` comprueban que la acción sensible solo procede tras `DeviceAuthOutcome.Authenticated` y bloquea el avance ante cancelación/error.
- **CA-43-04:** `./gradlew.bat :app:cyclonedxDirectBom` produce `app/build/reports/bom.json` válido en formato CycloneDX JSON y los flujos YAML validan `attest-build-provenance` y publicación del SBOM.

## 6. Riesgos y supuestos

- **Riesgo de rotación de certificados:** Al incluir las 4 raíces Amazon Root CA (1..4) más `SFSRootCAG2` y expiración explícita (`2027-11-26`), AWS puede rotar el certificado hoja o el intermedio sin romper clientes existentes.
- **Dispositivos sin bloqueo de pantalla:** En equipos sin PIN ni biometría (`BIOMETRIC_ERROR_NO_HARDWARE` / `BIOMETRIC_ERROR_NONE_ENROLLED`), se documenta y prueba la respuesta explícita al usuario (`NoLockEnrolled` / `Unavailable`) antes de bloquear o informar el requisito.
- **Entorno CI (`attest-build-provenance`):** Requiere permiso `attestations: write` e `id-token: write` en GitHub Actions cuando el flujo corra en repositorio oficial.

## 7. Trazabilidad

| Requisito | Brecha | Implementación | Verificación |
|---|---|---|---|
| `RF-43-01` | `G-07` | `network_security_config.xml` | `NetworkPinningTest` + `NetworkSecurityConfigTest` (`androidTest`) |
| `RF-43-02` | `G-09` | `SecureScreenEffect.kt` + 11 pantallas/modales | `SecureScreenEffectTest` |
| `RF-43-03`, `RF-43-04` | `G-09` | `DeviceAuthenticator.kt`, `AndroidDeviceAuthenticator.kt`, `MainActivity.kt` | `DeviceAuthenticationGateTest` |
| `RF-43-05` | `G-04` | `build.gradle.kts`, `app/build.gradle.kts`, `ci.yml`, `release-playstore.yml` | `./gradlew.bat :app:cyclonedxDirectBom` + inspección `bom.json` |

## Aprobación

- **Estado:** En implementacion (autorizado por `docs/plan-ejecucion-cierre-movil-2026-11-26.md` §3 y §5 Bloque C).
- **Fecha:** 2026-11-26
