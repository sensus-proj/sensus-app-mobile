# CHECKLIST — FT-17 Favoritos y Guardados de Ofertas

> Gate de cierre. Alcance: SE-036 (Pantalla P-23; Componente C-37). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta (`check_sdd.py PASS`, alcance actualizado por Decisión 7 del plan 2026-11-26)
- [x] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde (`FavoritesUseCasesTest` 4/4, `FavoritesRepositoryImplTest` 3/3, `FavoritesViewModelTest` 2/2; `FavoritesScreenTest` y `EncryptedDatabaseIntegrationTest` compiladas, ejecución física pendiente `§5.Z-3`)
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos (`tasks.md` T17.1..T17.16 cerradas)
- [x] Decisión SavedInterests vs Favoritos documentada: sin dos acciones visibles equivalentes para guardar la misma oferta (`ProductDetailScreen` usa `CircleFavoriteIconButton` con `FavoritesViewModel`; `SavedOffersRepository` queda para publicaciones)
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [x] `POST /api/v1/users/favorites/{offerId}` integrado con actualización optimista y rollback ante error no recuperable (`FavoritesRepositoryImpl` + `MockWebServer` `3/3` PASS; `GET /api/v1/users/favorites` preserva caché Room ante `404` hasta Bloque F)
- [x] `FavoriteEntity`/`FavoriteDao` activos en SQLCipher para lectura offline (aislados por `ownerKey` y purgados en `SessionRepositoryImpl.clearSession()`)
- [x] Migración `SensusDatabase` v8→v9 implementada y registrada (`SensusDatabaseMigrations.MIGRATION_8_9` + `DatabaseFactory`)
- [x] `FavoritesScreen` (`P-23`) con filtros `C-37`: implementados `Todas` (`FavoriteFilter.ALL`) y `Solo disponibles` (`FavoriteFilter.AVAILABLE_ONLY`) conforme a Decisión 7 del plan (`docs/plan-ejecucion-cierre-movil-2026-11-26.md` §3, reemplazando `Todos`/`Cerca de mí`/`Por agotarse`)
- [x] `ProductDetailScreen` usa semántica de Favorito para ofertas (sin doble acción equivalente con pin/interés) y `BuyerHomeScreen` incluye el corazón en `FavoriteProductOfferCard`
- [x] Modo invitado bloqueado con `GuestPromptDialog`; sin llamadas al endpoint de favoritos (`ToggleFavoriteUseCase` y `FavoritesRepositoryImpl` rechazan invitado sin HTTP)
- [x] Sin regresión en guardado de publicaciones de Intereses guardados (`SavedOffersRepositoryImplTest` y suite de `541/541` unitarias PASS)

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py PASS (0 hallazgos, 373 archivos)`)
- [x] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>` (`FavoritesViewModel`)
- [x] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [x] Logger sanitizado, sin PII, JWT, coordenadas exactas ni payloads sensibles en consola (`check_secrets.py PASS (540 archivos)`)
- [x] Acceso a Room/IO en `Dispatchers.IO`

## UI y accesibilidad

- [x] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty` y `Error` (`FavoritesScreen.kt`)
- [x] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar ni emojis (`SensusPurple`/`SensusVioletInk`)
- [x] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos
- [x] Estado vacío con icono vectorial (`Icons.Default.FavoriteBorder`) y CTA a explorar ofertas

## Gates automatizados

- [x] `./gradlew.bat compileDebugKotlin` verde (validado junto con el conjunto completo)
- [x] `./gradlew.bat compileDebugAndroidTestSources` verde (`FavoritesScreenTest` + `EncryptedDatabaseIntegrationTest`)
- [x] `./gradlew.bat lint` verde
- [x] `./gradlew.bat detekt ktlintCheck` verdes
- [x] `./gradlew.bat testDebugUnitTest koverXmlReportDebug koverVerifyDebug` verde (`541/541` pruebas unitarias PASS, Kover `80.75 % >= 80.00 %`)
- [x] `python scripts/check_sdd.py` PASS (`43 features`)
- [x] `python scripts/check_layers.py` PASS (`373 archivos`)
- [x] `python scripts/check_endpoints.py` PASS (`61 endpoints`)
- [x] `python scripts/check_secrets.py` PASS (`540 archivos`)
- [x] `python scripts/check_coverage.py --min 80 --require-report --report app/build/reports/kover/reportDebug.xml` PASS (`80.75 %`)

## Cierre SDD

- [x] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados con contrato final (`120 req/h`, `FavoritesPolicy` Spec `038`)
- [x] `Memoria.md` §6 actualizado con decisiones y evidencia
- [x] `specs/README.md` actualizado (`En revision`)
- [ ] `CHECKLIST_PR.md` completo (acción del usuario: revisión manual de PR en GitHub / dispositivos; ver plan cierre móvil §5.Z-5)
- [ ] Tag de release definido al cerrar (acción del usuario: crear commit/PR/tag tras revisión; ver plan cierre móvil §5.Z-5)