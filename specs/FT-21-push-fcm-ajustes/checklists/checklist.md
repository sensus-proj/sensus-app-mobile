# CHECKLIST — FT-21 Notificaciones Push FCM, Bandeja Local y Preferencias por Categoría

> Gate de cierre. Alcance: SE-039, SE-040, SE-041 (Pantallas P-04, P-26). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [ ] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] `POST` y `DELETE /api/v1/notifications/devices` integrados con login y logout
- [ ] `GET` y `PUT /api/v1/notifications/settings` integrados con switches por categoría
- [ ] Permiso `POST_NOTIFICATIONS` (Android 13+) solicitado con rationale en `P-04`
- [ ] Eliminado `MockDataProvider` de `NotificationsViewModel.kt`
- [ ] Botón `Marcar todo como leído` en `NotificationsScreen.kt` (línea 114) funcional
- [ ] Opción `Preferencias de notificaciones` en `BuyerProfileScreen.kt` (línea 321) conectada

## Arquitectura y seguridad

- [ ] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [ ] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [ ] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [ ] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [ ] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [ ] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty` y `Error`
- [ ] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar (`SensusAmber`) ni emojis
- [ ] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos

## Gates automatizados

- [ ] `./gradlew.bat compileDebugKotlin` verde
- [ ] `./gradlew.bat compileDebugAndroidTestSources` verde
- [ ] `./gradlew.bat detekt ktlintCheck lint` verdes
- [ ] `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (`>= 80%`)
- [ ] `python scripts/check_sdd.py` PASS
- [ ] `python scripts/check_layers.py` PASS
- [ ] `python scripts/check_endpoints.py` PASS
- [ ] `python scripts/check_secrets.py` PASS
- [ ] `python scripts/check_coverage.py --min 80` PASS

## Cierre SDD

- [ ] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [ ] `Memoria.md` §6 actualizado con decisiones y evidencia
- [ ] `CHECKLIST_PR.md` completo
- [ ] Tag `v1.24.0-ft21`
