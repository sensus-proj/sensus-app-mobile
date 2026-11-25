# CHECKLIST — FT-12 Detalle de Oferta, Filtros Avanzados y Ruteo Externo

> Gate de cierre. Alcance: SE-015, SE-016, SE-017, SE-018 (Pantallas P-14, P-17, P-19; Componentes C-21..C-27, C-32). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [ ] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] `GET /api/v1/offers/{id}` integrado con caché SQLCipher de respaldo
- [ ] Semáforo de stock (`C-24`) cambia entre `Disponible`, `Últimas unidades`, `Agotada` y `Expirada`
- [ ] Bottom sheet `P-19` (`C-32`) filtra y ordena por distancia, precio, descuento y vigencia
- [ ] CTA `Cómo llegar` (`C-26`) lanza intent `geo:` con fallback seguro si no hay app de mapas
- [ ] CTA `Apartar con QR` (`C-27`) redirige a modal invitado (`P-16`) si la sesión es guest

## Arquitectura y seguridad

- [ ] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [ ] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [ ] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [ ] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [ ] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty` y `Error`
- [ ] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar (`SensusAmber`) ni emojis
- [ ] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos

## Gates automatizados

- [x] `./gradlew.bat compileDebugKotlin` verde
- [x] `./gradlew.bat compileDebugAndroidTestSources` verde
- [ ] `./gradlew.bat detekt ktlintCheck lint` verdes
- [x] `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (`>= 80%`)
- [x] `python scripts/check_sdd.py` PASS
- [x] `python scripts/check_layers.py` PASS
- [x] `python scripts/check_endpoints.py` PASS
- [x] `python scripts/check_secrets.py` PASS
- [x] `python scripts/check_coverage.py --min 80` PASS

## Cierre SDD

- [ ] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [ ] `Memoria.md` §6 actualizado con decisiones y evidencia
- [ ] `CHECKLIST_PR.md` completo
- [ ] Tag `v1.15.0-ft12`

> Auditoría 2026-11-15 (código contra tareas): `compileDebugKotlin`, `compileDebugAndroidTestSources`, `testDebugUnitTest` (257 pruebas, 0 fallos), `koverVerifyDebug`, `detekt` y `ktlintCheck` verdes; `check_sdd`, `check_layers`, `check_endpoints`, `check_secrets` y `check_coverage --min 80` PASS. `lint` no se ejecutó. Brechas funcionales registradas en `tasks.md`.


- [x] Integración del cliente Android. El detalle muestra los días con horario publicado y los métodos de pago confirmados por el perfil. El filtro Abierto ahora excluye horarios desconocidos y respeta el cierre y los turnos nocturnos.
- [ ] Verificar los datos de la extensión opcional con el backend desplegado.
