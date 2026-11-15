# PLAN — FT-24 Asistente Conversacional Sensi IA

> Derivado de `spec.md`. Historias/Alcance: Pantalla P-25, Componente C-39 (Tesina Sensus / Excel Móvil).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.
> Contraparte backend: `sensus-back` Spec 046 (ADR 0011, `constitution.md` v1.4.0 del backend).

## 1. Diseño

1. **Gobernanza SDD (`Aprobada`).** La decisión D-6 quedó resuelta el 2026-11-13 por la vía generativa con enmienda en el backend. Los tres endpoints están especificados en `sensus-back/specs/046-asistente-sensi-ia/` y registrados en `docs/endpoints-internos.md`. Pendiente únicamente su despliegue.
2. **División de responsabilidades.** El backend recupera candidatos de forma determinista, calcula y valida el paquete, modera entrada y salida, y redacta con el proveedor generativo. El cliente **solo presenta**: no calcula totales, no reordena el paquete y no genera texto.
3. **Saneamiento previo obligatorio.** Antes de conectar red se retira la respuesta fabricada en `SensiChatScreen.sendUserMessage(...)`, la constante `SAMPLE_COMBO_PRICE`, `MockDataProvider.sampleChatMessages` y `StoreRepository.getSampleChatMessages()`; y se restaura el flujo MVI para que la pantalla despache intents en lugar de mutar una lista local.
4. **Contrato.** `AiChatApiService` expone:
   - `POST /api/v1/ai/conversations` → `AiConversationDto { id, createdAt, updatedAt }`
   - `POST /api/v1/ai/messages` con `{ conversationId, text, lat, lng, radiusMeters }` → `AiMessageResponseDto { message: AiChatMessageDto, budgetPackage: BudgetPackageDto? , degraded: Boolean }`
   - `GET /api/v1/ai/conversations/{id}?page&size` → historial paginado (`PagedResponse<AiChatMessageDto>`)
   - `BudgetPackageDto { budget, total, items: List<BudgetPackageItemDto { offerId, title, merchantName, price, quantity }] }`
   - Errores: `422` moderación, `429` tasa o límite diario, `403` invitado.
5. **Caché local.** `AiMessageEntity` en Room + SQLCipher con migración no destructiva; `LoadAiConversationUseCase` lee caché primero y sincroniza con `GET` cuando hay red.

## 2. UI y navegación

- `SensiChatScreen` (`P-25`) muestra burbujas de conversación (`C-39`), chips de prompts rápidos, indicador `isTyping` y tarjetas de ofertas sugeridas dentro de la burbuja de Sensi.
- Cada tarjeta navega a `ProductDetailScreen` o agrega al apartado; el total mostrado es el del backend.
- En modo invitado, el envío abre `GuestPromptDialog` desde la propia pantalla.
- Mientras el backend no esté desplegado, la pantalla opera contra `MockWebServer` en pruebas; en `app/src/main/**` no existe ninguna respuesta simulada.

## 3. DI

| Binding | Implementación |
|---|---|
| `AiChatRepository` | `AiChatRepositoryImpl` (`AiChatApiService` + `AiMessageDao`) en `RepositoryModule` |
| `AiChatApiService` | Retrofit en `NetworkModule` |
| `AiMessageDao` | `DatabaseModule` |
| `SendSensiMessageUseCase`, `LoadAiConversationUseCase` | `@Inject constructor` |

## 4. Gates

- `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes.
- `./gradlew.bat detekt ktlintCheck lint` verdes (0 violaciones).
- `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (cobertura `>= 80%`).
- `python scripts/check_sdd.py` PASS.
- `python scripts/check_layers.py` PASS.
- `python scripts/check_endpoints.py` PASS.
- `python scripts/check_secrets.py` PASS.
- `python scripts/check_coverage.py --min 80` PASS.
- `grep -rn "SAMPLE_COMBO_PRICE\|sampleChatMessages\|Combo Express Inteligente" app/src/main` sin resultados.

## 5. Orden de implementación

1. Saneamiento: retirar respuesta fabricada, `SAMPLE_COMBO_PRICE`, `sampleChatMessages` y `getSampleChatMessages()`; restaurar MVI en `SensiChatScreen` (`RF-24-05`, `RF-24-06`, `RF-24-07`).
2. Bloqueo de invitado en `SensiChatScreen` (`RF-24-01`).
3. Dominio: `AiChatModels.kt`, `AiChatRepository`, `SendSensiMessageUseCase`, `LoadAiConversationUseCase`.
4. Datos: `AiMessageEntity`, `AiMessageDao`, migración Room, `AiChatApiService`, `AiChatRepositoryImpl`.
5. Presentación: `SensiChatViewState` con `isTyping`, `budgetPackage`, errores tipificados; tarjetas navegables.
6. Pruebas `MockWebServer`, Turbine y Compose UI; cobertura `>= 80%`; actualización de `Memoria.md` §6.

## 6. Prohibiciones

- Prohibido simular respuestas de IA con `MockDataProvider`, `delay()` o texto en duro en código de producción.
- Prohibido invocar APIs de OpenAI/LLM directamente desde el APK con API keys en el cliente.
- Prohibido calcular, corregir o reordenar el paquete de presupuesto en el cliente.
- Prohibido mutar el estado de mensajes fuera de `SensiChatViewModel`.

## Tareas

Ver `tasks.md`.
