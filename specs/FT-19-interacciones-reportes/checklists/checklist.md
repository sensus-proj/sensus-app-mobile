# CHECKLIST — FT-19 Interacciones Sociales y Reportes de Moderación

> Gate de cierre. Alcance: SE-030, SE-031, SE-032, SE-033 (Pantalla P-22).

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [x] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP`

## Funcionalidad y reglas de negocio

- [x] `POST /api/v1/feed/posts/{id}/like` con actualización optimista y rollback
- [x] `GET` y `POST /api/v1/feed/posts/{id}/comments` con moderación de texto (`ProfanityFilter` + `<= 300`)
- [x] `POST /api/v1/reports` integrado desde `CommunityScreen` y `ProductDetailScreen` vía `ReportContentDialog` (`P-22`)
- [x] Ocultamiento preventivo local del post reportado tras éxito
- [x] Manejo de `409 Conflict` cuando el usuario ya reportó el mismo recurso en 24 h

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [x] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [x] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [x] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [x] Previews `@Preview` en `PostCommentsSheet.kt` y `ReportContentDialog.kt`
- [x] Paleta morada/roja de marca (`SensusPurple`, `SensusRed`); sin ámbar (`SensusAmber`) ni emojis
- [x] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos

## Gates automatizados y cierre

- [x] `./gradlew.bat compileDebugKotlin compileDebugAndroidTestSources detekt ktlintCheck lint testDebugUnitTest koverVerifyDebug`
- [x] `py scripts/check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 80`
- [ ] PR aprobado hacia `develop`/`main` y tag `v1.22.0-ft19` tras autorización del equipo
