# CHECKLIST — FT-19 Reportes de Moderación en Productos

> Gate de cierre. Alcance: SE-033 (Pantalla P-22).

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [x] Alcance cubierto por pruebas de éxito, error (`409`), invitado y exclusión legal
- [x] `plan.md` y `tasks.md` alineados sin Muro comunitario
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP`

## Funcionalidad y reglas de negocio

- [x] `POST /api/v1/reports` integrado desde `ProductDetailScreen` vía `ReportContentDialog` (`P-22`)
- [x] Manejo de `409 Conflict` cuando el usuario ya reportó el mismo recurso en 24 h

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [x] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [x] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [x] Preview `@Preview` en `ReportContentDialog.kt`
- [x] Paleta morada/roja de marca (`SensusPurple`, `SensusRed`); sin ámbar (`SensusAmber`) ni emojis
- [x] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos

## Gates automatizados y cierre

- [x] `./gradlew.bat compileDebugKotlin compileDebugAndroidTestSources detekt ktlintCheck lint testDebugUnitTest koverVerifyDebug`
- [x] `py scripts/check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 90`
- [ ] PR aprobado hacia `develop`/`main` tras autorización del equipo
