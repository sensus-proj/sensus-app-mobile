# CHECKLIST — FT-29 Activación de Comercio con Ubicación Interactiva y Persistencia en Perfil

> Gate de cierre. Alcance: Constitución §2, §3, §4, §5, §6 y §9.

## Spec y trazabilidad

- [x] `spec.md` con 7 secciones canónicas + `## Aprobación`
- [x] Endpoints `POST /api/v1/merchants/activate`, `GET /api/v1/merchants/me` y `PUT /api/v1/merchants/profile` alineados en `docs/endpoints-internos.md` y `docs/rate-matrix.md`

## Arquitectura, UI y seguridad

- [x] `domain/` puro (`check_layers.py` en PASS)
- [x] Formulario de comercio conserva los 3 campos (`nombreComercial`, `direccion`, `codigoPostal`) con opción adherida del gato Sensus en blanco y `SensusPurple` (sin amarillo/ámbar)
- [x] Modal `osmdroid` permite ajustar pin manualmente, previsualiza dirección y resuelve fallback manual ante denegación o error GPS
- [x] JWT actualizado, estado de comercio activo y nombre comercial cifrados en `SecurePreferences` y visibles en la casilla activa de `BuyerProfileScreen`

## Calidad y gates

- [x] `./gradlew.bat compileDebugKotlin compileDebugAndroidTestSources --no-daemon` en PASS
- [x] `./gradlew.bat lint detekt ktlintCheck --no-daemon` en PASS (`0` hallazgos)
- [x] `./gradlew.bat testDebugUnitTest koverXmlReportDebug koverVerifyDebug --no-daemon` (158 tests, 0 fallos) y `py scripts/check_coverage.py --min 80` (`90.72%`) en PASS
- [x] Scripts `check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py` en PASS

## Cierre

- [x] `Memoria.md`, `specs/README.md`, `tasks.md` y `spec.md` actualizados con evidencia observada
- [x] Cambios locales listos en rama `movil_Beto` sin `git push` hasta autorización del usuario
