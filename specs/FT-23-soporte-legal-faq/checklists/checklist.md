# CHECKLIST — FT-23 Centro de Ayuda, FAQ, Soporte y Términos Legales

> Gate de cierre. Alcance: SE-042, SE-043, SE-044 (Pantalla P-35; Componentes C-29, C-49). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [x] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [x] Pantalla `HelpFaqScreen` (`P-35`) con acordeones FAQ y `GuestModeBanner` (`C-49`) disponibles offline
- [x] Opción `Centro de ayuda y soporte` en `BuyerProfileScreen.kt` navega a `HelpFaqScreen`
- [x] Enlace `https://rodnix.com.mx/sensus/privacidad.html` accesible desde `HelpFaqScreen` y `TermsScreen`
- [x] Intent `mailto:` protegido con `try/catch (ActivityNotFoundException)`

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [x] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [x] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [x] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [x] Previews `@Preview` en `HelpFaqScreen.kt` (`FaqCollapsed`, `FaqExpanded`) y `GuestModeBanner.kt`
- [x] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar (`SensusAmber`) ni emojis
- [x] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos

## Gates automatizados

- [x] `./gradlew.bat compileDebugKotlin` verde
- [x] `./gradlew.bat compileDebugAndroidTestSources` verde
- [x] `./gradlew.bat detekt ktlintCheck lint` verdes
- [x] `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (`376/376`, `80.87%`)
- [x] `python scripts/check_sdd.py` PASS
- [x] `python scripts/check_layers.py` PASS
- [x] `python scripts/check_endpoints.py` PASS
- [x] `python scripts/check_secrets.py` PASS
- [x] `python scripts/check_coverage.py --min 80` PASS

## Cierre SDD

- [x] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados (sin endpoints nuevos en FT-23)
- [x] `Memoria.md` §6 actualizado con decisiones y evidencia
- [ ] `CHECKLIST_PR.md` completo (acción del usuario: completar checklist manual de PR antes del merge; ver plan cierre móvil §5.Z-5)
- [ ] Tag `v1.26.0-ft23` (acción del usuario: crear commit/PR/tag tras revisión; ver plan cierre móvil §5.Z-5)
