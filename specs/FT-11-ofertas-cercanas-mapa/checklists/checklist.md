# CHECKLIST — FT-11 Exploración de Ofertas Cercanas, Mapa OSM y Paginación Cursor

> Gate de cierre. Alcance: SE-010, SE-011, SE-012, SE-013, SE-014 (Pantallas P-07..P-11, Componentes C-01..C-20). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [ ] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] `GET /api/v1/offers/nearby` integrado con `lat`, `lng`, `radiusKm`, `category`, `cursor`, `limit`
- [ ] Paginación cursor (`limit = 20`, máx. `100`) con scroll infinito sin duplicados
- [ ] Invitado restringido a `radiusKm <= 2.0` en UI y caso de uso
- [ ] Toggle Lista (`P-07`) / Mapa OSM (`P-09`) preserva categoría y radio
- [ ] Pin de mapa (`C-15`) despliega mini-card (`C-16`) y navega a detalle (`P-14`)
- [ ] Estado vacío (`P-11` / `C-19`) permite ampliar radio con un toque
- [ ] Caché SQLCipher sirve última página válida en modo offline con aviso

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
- [ ] Tag `v1.14.0-ft11`
