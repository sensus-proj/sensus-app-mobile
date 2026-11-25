# CHECKLIST — FT-17 Favoritos y Guardados de Ofertas

> Gate de cierre. Alcance: SE-036 (Pantalla P-23; Componente C-37). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [ ] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Decisión SavedInterests vs Favoritos documentada: sin dos acciones visibles equivalentes para guardar la misma oferta
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] `POST /api/v1/users/favorites/{offerId}` integrado con actualización optimista y rollback ante error no recuperable
- [ ] `FavoriteEntity`/`FavoriteDao` activos en SQLCipher para lectura offline
- [ ] Migración `SensusDatabase` v8→v9 implementada y registrada (`SensusDatabaseMigrations`/factory)
- [ ] `FavoritesScreen` (`P-23`) con filtros `Todos`, `Cerca de mí` y `Por agotarse` (`C-37`)
- [ ] `ProductDetailScreen` usa semántica de Favorito para ofertas (sin doble acción equivalente con pin/interés)
- [ ] Modo invitado bloqueado con `GuestPromptDialog`; sin llamadas al endpoint de favoritos
- [ ] Sin regresión en guardado de publicaciones de Intereses guardados

## Arquitectura y seguridad

- [ ] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [ ] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [ ] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [ ] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [ ] Logger sanitizado, sin PII, JWT, coordenadas exactas ni payloads sensibles en consola
- [ ] Acceso a Room/IO en `Dispatchers.IO`

## UI y accesibilidad

- [ ] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty` y `Error`
- [ ] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar ni emojis
- [ ] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos
- [ ] Estado vacío con icono vectorial y CTA a explorar ofertas

## Gates automatizados

- [ ] `./gradlew.bat compileDebugKotlin --no-daemon` verde
- [ ] `./gradlew.bat compileDebugAndroidTestSources --no-daemon` verde
- [ ] `./gradlew.bat lint --no-daemon` verde
- [ ] `./gradlew.bat detekt ktlintCheck --no-daemon` verdes (o estado real documentado si no cableados)
- [ ] `./gradlew.bat testDebugUnitTest koverVerify --no-daemon` verde (`>= 80%`) o comando equivalente vigente
- [ ] `python scripts/check_sdd.py` PASS
- [ ] `python scripts/check_layers.py` PASS
- [ ] `python scripts/check_endpoints.py` PASS
- [ ] `python scripts/check_secrets.py` PASS
- [ ] `python scripts/check_coverage.py --min 80` PASS

## Cierre SDD

- [ ] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados con contrato final (incluido rate limit)
- [ ] `Memoria.md` §6 actualizado con decisiones y evidencia
- [ ] `specs/README.md` actualizado si cambia el estado de FT-17
- [ ] `CHECKLIST_PR.md` completo
- [ ] Tag de release definido al cerrar (no fijar antes de la implementación)