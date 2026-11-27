# CHECKLIST — FT-43 Endurecimiento de seguridad móvil (G-07, G-09, G-04)

## Spec, diseño y trazabilidad

- [x] `spec.md`, `plan.md` (con modelo de amenazas STRIDE) y `tasks.md` completos sin cambios de alcance ocultos (`check_sdd.py PASS (44 features)`)
- [x] Dependencias añadidas (`androidx.biometric:biometric:1.1.0`, `org.cyclonedx.bom:3.6.1`) documentadas con versión, licencia Apache 2.0 y costo USD $0

## Controles de seguridad

- [x] `G-07`: `network_security_config.xml` fija los 5 pins SHA-256 verificados (Amazon Root CA 1..4 + `SFSRootCAG2`) con `expiration="2027-11-26"` sobre `execute-api.us-east-2.amazonaws.com` (`includeSubdomains="true"`)
- [x] `G-07`: Prueba unitaria (`NetworkPinningTest`) e instrumentada (`NetworkSecurityConfigTest`) cubren `pin-set` y falla cerrada ante certificado distinto (`SSLPeerUnverifiedException`)
- [x] `G-09`: `SecureScreenEffect` + `WindowSecureFlagRegistry` con conteo de referencias por `Window` aplicado en todas las pantallas/diálogos sensibles definidos (`SecureScreenEffectTest` PASS)
- [x] `G-09`: `MainActivity` hereda de `FragmentActivity`; QR activo (`ClaimOfferScreen`/`MyClaimsScreen`) y eliminación de cuenta (`ConfirmDeleteAccountDialog`) protegidos con `DeviceAuthenticator` (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`, `DeviceAuthenticatorTest` + `DeviceAuthUiTest`)
- [x] `G-04`: `./gradlew.bat :app:cyclonedxDirectBom` genera `app/build/reports/bom.json` en `releaseRuntimeClasspath` (`CycloneDX 1.6`, `199` componentes)
- [x] `G-04`: Workflows `.github/workflows/ci.yml` y `release-playstore.yml` ejecutan SBOM y `actions/attest-build-provenance@4d101475d8b20a2381f78447822ac1eab6504dd8 # v4.2.2`

## Gates automatizados

- [x] `./gradlew.bat compileDebugKotlin compileDebugAndroidTestSources detekt ktlintCheck lint testDebugUnitTest koverXmlReportDebug koverVerifyDebug` verdes (`548/548` unitarias, `0` fallos, `0` omisiones)
- [x] `python scripts/check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 80` (`80.80 %`) en PASS
- [x] `docs/asvs-l2-checklist.md`, `specs/README.md` y `docs/avance-cierre-movil-2026-11-26.md` actualizados con textos propuestos para Apéndice F (`G-07`, `G-09`, `G-04`)
- [ ] Verificación física en dispositivo y corrida de GitHub Actions en remoto (acción del usuario: probar biometría/captura de pantalla en teléfono y validar workflow en GitHub; ver plan cierre móvil §5.Z-3 y §5.Z-4)
