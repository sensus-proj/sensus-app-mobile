# CHECKLIST — FT-16 Inventario de Ofertas del Comerciante, Ciclo de Vida y Métricas

> Gate de cierre. Alcance: SE-023, SE-024, SE-025, SE-026, SE-045 (Pantallas P-30, P-31; Componentes C-41..C-44). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta (`7 secciones` PASS en `check_sdd.py`; falta cierre formal de aprobación: (acción del usuario: aprobar tabla en `spec.md`; ver plan cierre móvil §5.Z-5))
- [x] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde (`MerchantViewModelsTest`, `MerchantEditOfferViewModelTest`, `RepublishRepositoryTest`, `RepublishViewModelTest`, `MerchantEditOfferContentTest`)
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] Confirmar el contador de vistas por oferta con una respuesta real de inventario que incluya `viewCount` (acción del usuario: validar contra backend desplegado con `viewCount`; ver plan cierre móvil §5.Z-3).

- [x] `GET /api/v1/merchants/me/stats` alimenta KPIs reales en `MerchantDashboardScreen` (`P-30`)
- [x] `GET /api/v1/merchants/me/offers` lista inventario paginado por estado en `P-31`
- [x] `PATCH /api/v1/offers/{id}/pause`, `/activate`, `/sold-out` y `DELETE` integrados
- [x] Manejo de `409 Conflict` (`rowVersion`) refrescando la oferta en conflicto (`RepublishRepositoryTest`)
- [x] Eliminado `MockDataProvider` de `MerchantDashboardViewModel`

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [x] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>` (`MerchantDashboardViewModel`, `MerchantEditOfferViewModel`)
- [x] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [x] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [ ] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty` y `Error` (acción del usuario: decidir refactor UI para añadir `@Preview` de 4 estados en `MerchantDashboardScreen` heredado; ver plan cierre móvil §5.Z-5)
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
- [ ] Tag `v1.19.0-ft16` (acción del usuario: crear commit/PR/tag tras revisión; ver plan cierre móvil §5.Z-5)

- [x] REPUBLISH: validar compileDebugKotlin, testDebugUnitTest, compileDebugAndroidTestSources, lint/detekt/ktlint, Kover >=80%, gates SDD y seguridad en corrida coordinada main (`549/549`, Kover `80.81 %`, `5/5` Python PASS).
- [ ] Ejecutar Compose y E2E autenticado contra backend oficial; confirmar 409 sin caché ni reintento (acción del usuario: probar E2E contra backend AWS en dispositivo; ver plan cierre móvil §5.Z-3).
- [x] Ejecutar en teléfono `MerchantEditOfferContentTest` con una compilación que incluya el diálogo de confirmación de pausa (2026-11-22, 2/2 en V2341).

> Auditoría 2026-11-15 (código contra tareas): `compileDebugKotlin`, `compileDebugAndroidTestSources`, `testDebugUnitTest` (257 pruebas, 0 fallos), `koverVerifyDebug`, `detekt` y `ktlintCheck` verdes; `check_sdd`, `check_layers`, `check_endpoints`, `check_secrets` y `check_coverage --min 80` PASS. `lint` no se ejecutó. Brechas funcionales registradas en `tasks.md`.
