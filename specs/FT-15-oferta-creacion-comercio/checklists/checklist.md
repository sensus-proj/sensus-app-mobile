# CHECKLIST — FT-15 Creación de Ofertas por Comerciante, Compresión y Moderación

> Gate de cierre. Alcance: SE-019, SE-020, SE-021, SE-022 (Pantalla P-32; Componentes C-45, C-46, C-47). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [ ] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] Validación en dominio de `offerPrice < originalPrice`, `stock >= 1` y `expiresAt > startsAt`
- [ ] Compresión de imagen en `Dispatchers.IO` (`<= 1 MB`, máx. `1600 px`) antes de enviar
- [ ] `POST /api/v1/offers` envía imagen y texto juntos para moderación fail-closed
- [ ] Manejo de rechazo de moderación (`422`) mostrando motivo claro sin perder el formulario
- [ ] `CreateOfferScreen` (`P-32`) gobernada por `CreateOfferViewModel` MVI

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
- [ ] Tag `v1.18.0-ft15`

### Evidencia local de las HU — 2026-11-13

146 pruebas unitarias: 0 fallos, 0 errores, 0 omisiones. Kover global 89.85% (umbral 80%).
La app y las pruebas instrumentadas compilan; detekt, ktlint, lint y los scripts locales pasan.
Las pruebas instrumentadas están pendientes de ejecución, al igual que la validación con el backend desplegado.
No se declara cierre formal de la slice, aprobación, merge ni tag. El alcance ejecutado corresponde a las HU solicitadas;
las condiciones globales sobre eliminación de mocks legacy y otras HU no se modifican ni se declaran cumplidas.
