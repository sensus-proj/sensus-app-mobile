# CHECKLIST — FT-30 Feed de Promociones por Intereses

> Gate de cierre. Backend: specs 042, 054 y 055. Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] Selector de intereses opcional tras el registro y editable en el perfil
- [ ] Pestaña `Promociones` separada de `Comunidad` con `GET /api/v1/feed/offers`
- [ ] Paginación con `snapshotId` y deduplicación cuando es nulo
- [ ] Invitado sin llamada al feed y con `GuestPromptDialog`
- [ ] Estados de carga, vacío, error y sin conexión

## Arquitectura y seguridad

- [ ] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [ ] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [ ] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [ ] Sin coordenadas, intereses ni identificadores en logs
- [ ] Caché de la página 1 en Room + SQLCipher

## UI y accesibilidad

- [ ] Previews para `Loading`, `Loaded`, `Empty`, `Offline` y selector
- [ ] Paleta morada de marca; sin ámbar ni emojis
- [ ] Touch targets `>= 48x48 dp` y `contentDescription` en controles

## Gates automatizados

- [ ] `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes
- [ ] `./gradlew.bat detekt ktlintCheck lint` verdes
- [ ] `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (`>= 80%`)
- [ ] `python scripts/check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py` y `check_coverage.py --min 80` PASS

## Cierre SDD

- [ ] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [ ] `Memoria.md` §6 actualizado con decisiones y evidencia
- [ ] `CHECKLIST_PR.md` completo
