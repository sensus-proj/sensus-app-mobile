# CHECKLIST — FT-15 Creación de Ofertas por Comerciante, Compresión y Moderación

> Gate de cierre. Alcance: SE-019, SE-020, SE-021, SE-022 (Pantalla P-32; Componentes C-45, C-46, C-47). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta (`7 secciones` PASS en `check_sdd.py`; falta cierre formal de aprobación: (acción del usuario: aprobar tabla en `spec.md`; ver plan cierre móvil §5.Z-5))
- [x] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde (`PublishingUseCasesTest`, `PublishingBoundariesTest`, `PublishingApiTest`, `OfferPublishingViewModelTest`, `OfferPublishingE2ETest`)
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [x] Validación en dominio de `offerPrice < originalPrice`, `stock >= 1` y `expiresAt > startsAt`
- [x] Compresión de imagen en `Dispatchers.IO` (`<= 1 MB`, máx. `1600 px`) antes de enviar
- [x] `POST /api/v1/offers` envía imagen y texto juntos para moderación fail-closed
- [x] Manejo de rechazo de moderación (`422`) mostrando motivo claro sin perder el formulario
- [x] `CreateOfferScreen` (`P-32`) gobernada por `CreateOfferViewModel` MVI (`MerchantNewOfferViewModel`)

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [x] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>` (`MerchantNewOfferViewModel`)
- [x] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [x] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [ ] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty` y `Error` (acción del usuario: decidir refactor UI para añadir `@Preview` de 4 estados en formulario `MerchantNewOfferScreen` heredado; ver plan cierre móvil §5.Z-5)
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
- [ ] Tag `v1.18.0-ft15` (acción del usuario: crear commit/PR/tag tras revisión; ver plan cierre móvil §5.Z-5)

### Evidencia local de las HU — 2026-11-13

146 pruebas unitarias: 0 fallos, 0 errores, 0 omisiones. Kover global 89.85% (umbral 80%).
La app y las pruebas instrumentadas compilan; detekt, ktlint, lint y los scripts locales pasan.
Las pruebas instrumentadas están pendientes de ejecución, al igual que la validación con el backend desplegado.
No se declara cierre formal de la slice, aprobación, merge ni tag. El alcance ejecutado corresponde a las HU solicitadas;
las condiciones globales sobre eliminación de mocks legacy y otras HU no se modifican ni se declaran cumplidas.

### Evidencia T15.15 — 2026-11-20

- `StoreCategory.apiCode`, `fromApiCode`, `displayNameFor` añadidos en `StoreModels.kt`; `OfferPublishingPolicy.validate` rechaza `StoreCategory.ALL` al crear oferta (`OfferField.CATEGORY`), conservando `validateUpdate` compatible.
- `RemoteOfferMapper`, `OfferMapper`, `AiDtos` y `FriendModels` envían el código `OfferCategory` en `CreateOfferRequest.category` y mapean de vuelta código → `displayName` al leer ofertas; `MapStoreClusterBuilder` y `BuyerHomeScreen` reconocen ambos.
- Verificado: `compileDebugKotlin`, `compileDebugAndroidTestSources`, `detekt`, `ktlintCheck`, `lint`, `testDebugUnitTest` (364 pruebas, 0 fallos/omisiones), `koverVerifyDebug` (80.90 %) y los 5 scripts `scripts/check_*.py` en PASS.
