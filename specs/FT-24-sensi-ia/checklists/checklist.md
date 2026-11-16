# CHECKLIST — FT-24 Asistente Conversacional Sensi IA

> Gate de cierre. Alcance: Pantalla P-25, Componente C-39 (Tesina Sensus / Excel Móvil). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [x] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [x] Decisión arquitectónica tomada sobre el contrato de Sensi IA en `sensus-back` (D-6: Spec 046, ADR 0011, enmienda v1.4.0)
- [x] Eliminada la respuesta fabricada de `SensiChatScreen` y `SAMPLE_COMBO_PRICE`
- [x] Eliminado `MockDataProvider.sampleChatMessages` y `getSampleChatMessages()`
- [x] Flujo MVI restaurado: la pantalla despacha intents y no muta mensajes localmente
- [x] Bloqueo de invitado en `SensiChatScreen` con `GuestPromptDialog`
- [x] El cliente no calcula ni reordena el paquete; muestra el total del backend
- [x] Degradación (`degraded = true`), `400` (moderación/inyección), `403`, `404` y `429` manejados con mensajes distintos
- [x] Ninguna API key de LLM/OpenAI presente en el cliente móvil

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [x] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [x] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [x] Logger sanitizado, sin PII, JWT, mensajes del chat ni payloads sensibles en consola

## UI y accesibilidad

- [x] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty`, `Error` y `Degraded`
- [x] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar (`SensusAmber`) ni emojis
- [x] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos

## Gates automatizados

- [x] `./gradlew.bat compileDebugKotlin` verde
- [x] `./gradlew.bat testDebugUnitTest` verde
- [x] `python scripts/check_sdd.py` PASS
- [x] `python scripts/check_layers.py` PASS
- [x] `python scripts/check_endpoints.py` PASS
- [x] `python scripts/check_secrets.py` PASS
- [x] `python scripts/check_coverage.py --min 80` PASS (84.01%)

## Cierre SDD

- [x] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [x] `Memoria.md` §6 actualizado con decisiones y evidencia
