# CHECKLIST — FT-18 Feed Social de Comunidad y Creación de Publicaciones

> Gate de cierre. Alcance: SE-027, SE-028, SE-029 (Pantalla P-24; Componentes C-38, C-40). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta (`7 secciones` PASS en `check_sdd.py`; falta cierre formal de aprobación: (acción del usuario: aprobar tabla en `spec.md`; ver plan cierre móvil §5.Z-5))
- [x] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde (`CommunityFeedViewModelTest`, `FeedAvailabilityApiTest`, `FeedInteractionsAndReportsRepositoryTest`)
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [x] `GET /api/v1/feed/nearby` con paginación por página y caché SQLCipher
- [x] `POST /api/v1/feed/posts` con compresión de imagen y moderación fail-closed (`422`)
- [x] FT-19 (fuera del alcance FT-18): `DELETE /api/v1/feed/posts/{id}` habilitado solo para el autor del post
- [x] Eliminado `MockDataProvider` de `CommunityViewModel.kt`
- [x] Bloqueo de publicación para invitados con redirección a `P-16`

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [x] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>` (`CommunityViewModel`)
- [x] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [x] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [ ] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty` y `Error` (acción del usuario: decidir refactor UI para añadir `@Preview` de 4 estados en `CommunityScreen` heredado; ver plan cierre móvil §5.Z-5)
- [x] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar (`SensusAmber`) ni emojis
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
- [ ] `CHECKLIST_PR.md` completo (acción del usuario: completar checklist manual de PR antes del merge; ver plan cierre móvil §5.Z-5)
- [ ] Tag `v1.21.0-ft18` (acción del usuario: crear commit/PR/tag tras revisión; ver plan cierre móvil §5.Z-5)

### Evidencia local de las HU — 2026-11-13

146 pruebas unitarias: 0 fallos, 0 errores, 0 omisiones. Kover global 89.85% (umbral 80%).
La app y las pruebas instrumentadas compilan; detekt, ktlint, lint y los scripts locales pasan.
Las pruebas instrumentadas están pendientes de ejecución, al igual que la validación con el backend desplegado.
No se declara cierre formal de la slice, aprobación, merge ni tag. El alcance ejecutado corresponde a las HU solicitadas;
las condiciones globales sobre eliminación de mocks legacy y otras HU no se modifican ni se declaran cumplidas.


### Corrección de indisponibilidad — 2026-11-14

- [x] Estado MVI con errores de feed y publicación independientes, sin falso estado vacío tras 404.
- [x] Regresiones HTTP con autenticación existente y conservación del borrador verificadas.
- [x] Pruebas Compose nuevas compiladas.
- [ ] Ejecución de pruebas Compose en dispositivo (acción del usuario: ejecutar `androidTest` en teléfono físico; ver plan cierre móvil §5.Z-3).
- [ ] Backend 024/026 implementado/desplegado y E2E exitoso (acción del usuario: validar E2E contra backend 024/026 desplegado en AWS; ver plan cierre móvil §5.Z-3).
- SAST: paquete remoto p/mobile responde 404; no se modifica configuración, no se declara suite CI completa aprobada. Paquetes disponibles p/default + p/owasp-top-ten: 66 reglas sobre los 4 archivos Kotlin modificados, 0 hallazgos.

- Validación local 2026-11-14: 188 tests sin fallos, Kover 81.41%, compilación/linters/lint y 5 gates Python PASS. No se cierra la slice ni se declara el backend funcional.
