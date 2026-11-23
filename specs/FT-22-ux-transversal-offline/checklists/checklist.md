# CHECKLIST — FT-22 UX Transversal: Conectividad Offline, Tema Oscuro y Shimmers

> Gate de cierre. Alcance: SE-042, SE-043, SE-044 (Pantallas P-05, P-06; Componente C-15). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [x] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [x] Banner offline y Snackbar de conectividad reactivos a `NetworkMonitor.isOnline` en el contenedor principal (`MainAppContainer.kt`)
- [x] Pantalla `P-05` (`LocationDisabledState`) con acceso a ajustes del sistema (`Settings.ACTION_LOCATION_SOURCE_SETTINGS`) y ubicación manual
- [x] Pantalla `P-06` (`OfflineErrorState`) con botón `Reintentar` funcional
- [x] Componente `ShimmerPlaceholders` (`C-15`) adoptado en pantallas de listado durante carga sin caché
- [x] Touch targets `>= 48x48 dp` y `contentDescription` verificados en estados transversales

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [x] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [x] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [x] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [x] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty` y `Error`
- [x] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar (`SensusAmber`) ni emojis
- [x] Soporte completo de modo claro y oscuro en `Theme.kt` con contraste WCAG AA
- [x] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos

## Gates automatizados

- [x] `./gradlew.bat compileDebugKotlin` verde
- [x] `./gradlew.bat compileDebugAndroidTestSources` verde
- [x] `./gradlew.bat detekt ktlintCheck lint` verdes
- [x] `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (`>= 80%`)
- [x] `python scripts/check_sdd.py` PASS
- [x] `python scripts/check_layers.py` PASS
- [x] `python scripts/check_endpoints.py` PASS
- [x] `python scripts/check_secrets.py` PASS
- [x] `python scripts/check_coverage.py --min 80` PASS

## Cierre SDD

- [x] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [x] `Memoria.md` §6 actualizado con decisiones y evidencia
- [x] `CHECKLIST_PR.md` completo
- [x] Tag `v1.25.0-ft22` (N/A — operación de Git delegada al responsable)
