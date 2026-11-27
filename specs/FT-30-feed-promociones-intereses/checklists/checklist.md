# CHECKLIST — FT-30 Feed de Promociones por Intereses

> Gate de cierre. Backend: specs 042, 054 y 055. Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [x] Selector de intereses opcional tras el registro y editable en el perfil
- [x] Pestaña `Promociones` separada de `Comunidad` con `GET /api/v1/feed/offers`
- [x] Paginación con `snapshotId` y deduplicación cuando es nulo
- [x] Invitado sin llamada al feed y con `GuestPromptDialog`
- [x] Estados de carga, vacío, error y sin conexión

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [x] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [x] Sin coordenadas, intereses ni identificadores en logs
- [x] Caché de la página 1 en Room + SQLCipher (`FeedOfferEntity` / `FeedOfferDao`)

## UI y accesibilidad

- [x] Previews para `Loading`, `Loaded`, `Empty`, `Offline` y selector
- [x] Paleta morada de marca; sin ámbar ni emojis
- [x] Touch targets `>= 48x48 dp` y `contentDescription` en controles

## Gates automatizados

- [x] `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes
- [x] `./gradlew.bat detekt ktlintCheck lint` verdes
- [x] `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (`376/376`, `80.87%`)
- [x] `python scripts/check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py` y `check_coverage.py --min 80` PASS

## Cierre SDD

- [x] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [x] `Memoria.md` §6 actualizado con decisiones y evidencia
- [ ] `CHECKLIST_PR.md` completo (acción del usuario: completar checklist manual de PR antes del merge; ver plan cierre móvil §5.Z-5)
