# CHECKLIST — FT-18 Feed Social de Comunidad y Creación de Publicaciones

> Gate de cierre. Alcance: SE-027, SE-028, SE-029 (Pantalla P-24; Componentes C-38, C-40). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [ ] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] `GET /api/v1/feed` con paginación cursor y caché SQLCipher de primera página
- [ ] `POST /api/v1/feed/posts` con compresión de imagen y moderación fail-closed (`422`)
- [ ] `DELETE /api/v1/feed/posts/{id}` habilitado solo para el autor del post
- [ ] Eliminado `MockDataProvider` de `CommunityViewModel.kt`
- [ ] Bloqueo de publicación para invitados con redirección a `P-16`

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

- [ ] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [ ] `Memoria.md` §6 actualizado con decisiones y evidencia
- [ ] `CHECKLIST_PR.md` completo
- [ ] Tag `v1.21.0-ft18`

### Evidencia local de las HU — 2026-11-13

146 pruebas unitarias: 0 fallos, 0 errores, 0 omisiones. Kover global 89.85% (umbral 80%).
La app y las pruebas instrumentadas compilan; detekt, ktlint, lint y los scripts locales pasan.
Las pruebas instrumentadas están pendientes de ejecución, al igual que la validación con el backend desplegado.
No se declara cierre formal de la slice, aprobación, merge ni tag. El alcance ejecutado corresponde a las HU solicitadas;
las condiciones globales sobre eliminación de mocks legacy y otras HU no se modifican ni se declaran cumplidas.
