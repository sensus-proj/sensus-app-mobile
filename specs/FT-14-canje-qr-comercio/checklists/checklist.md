# CHECKLIST — FT-14 Escaneo y Validación de Código QR por Comerciante

> Gate de cierre. Alcance: Pantalla P-33, Componente C-48 (Spec 033 Backend). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [ ] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] `POST /api/v1/offers/claims/{code}/redeem` integrado con mapeo de `200`, `403`, `404`, `409`, `410`
- [ ] Visor CameraX + ML Kit (`C-48`) con solicitud de permiso `CAMERA` y rationale
- [ ] Entrada manual de código `SNS-XXXX-XX` funcional cuando se niega cámara
- [ ] Eliminada validación hardcodeada `SNS-A7K9-24` de `MerchantSalesScreen.kt`
- [ ] Debounce de 2 segundos activo tras cada lectura QR

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
- [ ] Tag `v1.17.0-ft14`
