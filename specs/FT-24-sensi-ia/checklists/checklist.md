# CHECKLIST — FT-24 Asistente Conversacional Sensi IA

> Gate de cierre. Alcance: Pantalla P-25, Componente C-39 (Tesina Sensus / Excel Móvil). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [x] Decisión arquitectónica tomada sobre el contrato de Sensi IA en `sensus-back` (D-6: Spec 046, ADR 0011, enmienda v1.4.0)
- [ ] Eliminada la respuesta fabricada de `SensiChatScreen` y `SAMPLE_COMBO_PRICE`
- [ ] Eliminado `MockDataProvider.sampleChatMessages` y `getSampleChatMessages()`
- [ ] Flujo MVI restaurado: la pantalla despacha intents y no muta mensajes localmente
- [ ] Bloqueo de invitado en `SensiChatScreen` con `GuestPromptDialog`
- [ ] El cliente no calcula ni reordena el paquete; muestra el total del backend
- [ ] Degradación (`degraded = true`), `422` y `429` manejados con mensajes distintos
- [ ] Ninguna API key de LLM/OpenAI presente en el cliente móvil

## Arquitectura y seguridad

- [ ] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [ ] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [ ] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [ ] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [ ] Logger sanitizado, sin PII, JWT, mensajes del chat ni payloads sensibles en consola

## UI y accesibilidad

- [ ] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty`, `Error` y `Degraded`
- [ ] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar (`SensusAmber`) ni emojis
- [ ] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos

## Gates automatizados

- [ ] `./gradlew.bat compileDebugKotlin` verde
- [ ] `./gradlew.bat compileDebugAndroidTestSources` verde
- [ ] `./gradlew.bat detekt ktlintCheck lint` verdes
- [ ] `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (`>= 80%`)
- [ ] `python scripts/check_sdd.py` PASS
- [ ] `python scripts/check_layers.py` PASS
- [ ] `python scripts/check_endpoints.py` PASS
- [ ] `python scripts/check_secrets.py` PASS
- [ ] `python scripts/check_coverage.py --min 80` PASS

## Cierre SDD

- [ ] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [ ] `Memoria.md` §6 actualizado con decisiones y evidencia
- [ ] `CHECKLIST_PR.md` completo
- [ ] Tag `v1.27.0-ft24`
