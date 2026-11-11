# CHECKLIST — FT-13 Reserva / Apartado con Código QR para Comprador

> Gate de cierre. Alcance: Pantallas P-16, P-18, P-20, P-21; Componentes C-33, C-34, C-35, C-36 (Spec 033 Backend). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [ ] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] `POST /api/v1/offers/{id}/claim` integrado con manejo de `201`, `401` y `409 Conflict`
- [ ] Generación de bitmap QR real con ZXing (`512x512 px`) legible por escáner estándar
- [ ] `ReservationEntity` persistida en SQLCipher para visualización offline en tienda
- [ ] Temporizador `C-34` actualiza cada segundo y marca `EXPIRED` al vencer
- [ ] Historial de apartados (`P-21`) filtra `Activos`, `Canjeados` y `Vencidos`

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
- [ ] Tag `v1.16.0-ft13`
