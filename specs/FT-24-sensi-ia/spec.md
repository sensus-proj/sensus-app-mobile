# SPEC — FT-24 Asistente Sensi IA (Paquetes de Presupuesto y Recetas)

> Rector: `.specify/memory/constitution.md` §2, §6.3 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **Sensus_Movil_Componentes_y_CRUD.xlsx** (hoja *Sensi IA*, Pantalla **P-25**, Componente **C-39**); **Tesina Sensus §1, §2.3**; **Formato de Aceptación**.
> Backend ref: **`sensus-back` Spec 046 — Asistente conversacional Sensi** (habilitada por ADR 0011 y enmienda `constitution.md` v1.4.0 del backend). Decisión **D-6 resuelta** el 2026-11-13: se aprueba la vía generativa con enmienda.

## 1. Contexto y objetivo

La Tesina, el Formato de Aceptación y la hoja *Sensi IA* del Drive especifican a **Sensi**, un asistente conversacional donde el usuario indica un presupuesto (ej. `"Tengo $100 para la despensa"`) o pide ideas de recetas, y Sensi arma un paquete de ofertas cercanas abiertas y con stock sin rebasar el monto.

**Estado real del cliente (auditado el 2026-11-13):**

- `SensiChatViewModel` siembra el estado con `storeRepository.getSampleChatMessages()`, que resuelve a `MockDataProvider.sampleChatMessages`.
- `SensiChatScreen.sendUserMessage(...)` **fabrica la respuesta de Sensi en código de producción**: inventa un `"Combo Express Inteligente"` con tres artículos y precios en duro y la constante `SAMPLE_COMBO_PRICE`. Esto viola la prohibición de `plan.md` §6 y la regla de retiro de mocks de `Memoria.md`.
- La pantalla **no usa el flujo MVI**: muta una `MutableList<ChatMessage>` local y nunca despacha `SensiChatViewIntent.SendMessage`, por lo que `SensiChatViewModel.processIntent` es código muerto.
- **No hay bloqueo de modo invitado en `SensiChatScreen`**: `GuestPromptDialog` solo se usa en `MainAppMenu.kt` y ahí cubre el apartado (`activeApartadoOrder`), no la entrada a `P-25`. La afirmación en contrario de la bitácora del 2026-11-11 es inexacta para esta pantalla.
- `SensiChatViewState` carece de `isTyping` y de paquete de presupuesto.

**Objetivo:** conectar `SensiChatScreen` y `SensiChatViewModel` con el puente REST del backend (`POST /api/v1/ai/conversations`, `POST /api/v1/ai/messages`, `GET /api/v1/ai/conversations/{id}`) para mostrar paquetes de ofertas por presupuesto, manteniendo historial en Room + SQLCipher, restaurando el flujo MVI y retirando toda respuesta simulada del código de producción.

**Alcance:** `AiChatRepository`, `SendSensiMessageUseCase`, `LoadAiConversationUseCase`, `AiMessageEntity`/`AiMessageDao`, `AiChatApiService`, y tarjetas interactivas de paquete de presupuesto dentro de burbujas de chat (`P-25`) con navegación al detalle/apartado de cada producto.
**Fuera de alcance:** llamadas directas desde Android a OpenAI/Gemini con API keys embebidas (prohibido por seguridad §4/§6.3); cálculo del paquete o de su total en el cliente (lo calcula y valida el backend, Spec 046 `FR-046-06`).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-24-01 | Bloquear el envío en modo invitado mostrando `GuestPromptDialog` desde `SensiChatScreen`, sin depender del gate de `MainAppMenu`. |
| RF-24-02 | Iniciar o recuperar conversación mediante `POST /api/v1/ai/conversations` / `GET /api/v1/ai/conversations/{id}` y persistir caché local en `AiMessageDao`. |
| RF-24-03 | Al enviar mensaje con presupuesto/consulta (`POST /api/v1/ai/messages`), adjuntar ubicación aproximada, radio actual (1.0–5.0 km) y `screenContext`; el backend devuelve texto de Sensi y paquete opcional de ofertas (`recommendedPackage.items`) con su total ya validado y lista de `offersCited`. |
| RF-24-04 | Renderizar dentro de la burbuja de Sensi tarjetas compactas de cada oferta sugerida con botón para abrir su `ProductDetailScreen` o agregarlas al apartado. |
| RF-24-05 | Retirar por completo `MockDataProvider.sampleChatMessages`, `StoreRepository.getSampleChatMessages()` y su implementación en `StoreRepositoryImpl`. |
| RF-24-06 | Eliminar la respuesta fabricada de `SensiChatScreen.sendUserMessage(...)` y la constante `SAMPLE_COMBO_PRICE`. Ninguna respuesta de Sensi puede originarse en el cliente. |
| RF-24-07 | Restaurar el flujo MVI: la pantalla despacha `SensiChatViewIntent.SendMessage` y observa `SensiChatViewState`; sin estado mutable local de mensajes. |
| RF-24-08 | Mostrar indicador `Sensi está pensando...` mientras la solicitud está en vuelo (`isTyping` en `SensiChatViewState`). |
| RF-24-09 | Mostrar el total que devuelve el backend tal cual; el cliente no recalcula ni reordena el paquete. |
| RF-24-10 | Manejar la degradación del backend (`degraded = true`): mostrar la respuesta normalmente con aviso informativo discreto (“Sensi está en modo básico”), sin estado de error. |
| RF-24-11 | Manejar `400` (rechazo por moderación o prompt injection con `title`/`detail`), `403` (invitado), `404` (conversación inexistente con recreación automática), `429` (límite con `Retry-After`) y `5xx`/timeout con mensajes accionables y distintos entre sí. |
| RF-24-12 | Soportar eliminación de conversación mediante `DELETE /api/v1/ai/conversations/{id}`, purgando la caché local en `AiMessageDao`. |

## 3. Requisitos no funcionales

- **Seguridad:** cero API keys de LLM en el cliente Android; toda orquestación de prompts, moderación de entrada/salida y cruce con ofertas vigentes ocurre en `sensus-back` (Spec 046).
- **Resiliencia:** timeout de hasta 30 s en generación de respuesta con indicador de escritura (`Sensi está pensando...`) y mensaje claro de reintento ante fallo.
- **Privacidad:** no registrar en logcat el contenido de los mensajes ni la respuesta del asistente.

## 4. Arquitectura y contratos internos

```text
domain/
  model/AiChatModels.kt         # AiConversation, AiChatMessage, RecommendedPackage, CitedOffer, ScreenContext
  repository/AiChatRepository.kt
  usecase/SendSensiMessageUseCase.kt, LoadAiConversationUseCase.kt
data/
  local/db/entity/AiMessageEntity.java, dao/AiMessageDao.java
  remote/api/AiChatApiService.kt # POST /api/v1/ai/conversations, POST /api/v1/ai/messages, GET /api/v1/ai/conversations/{id}, DELETE /api/v1/ai/conversations/{id}
  remote/dto/AiDtos.kt
  repository/AiChatRepositoryImpl.kt
presentation/buyer/
  SensiChatContract.kt, SensiChatViewModel.kt, SensiChatScreen.kt
```

Nombres canónicos de esta feature. Quedan retirados los nombres alternos que circulaban en `plan.md`: `SensiAiApiService`, `SensiRepositoryImpl` y `AskSensiUseCase`.

## 5. Criterios de aceptacion

- **Given** un comprador autenticado escribe `"Tengo $100 para comida"` **When** envía en `SensiChatScreen` **Then** llama a `POST /api/v1/ai/messages` y muestra respuesta más tarjetas de ofertas cuyo total recibido es `<= $100`.
- **Given** una tarjeta sugerida dentro del chat **When** el usuario la toca **Then** navega a `ProductDetailScreen` de esa oferta.
- **Given** usuario sin red **When** abre `SensiChatScreen` **Then** consulta últimos mensajes desde `AiMessageDao` y deshabilita envío hasta recuperar conexión.
- **Given** una sesión invitada **When** intenta enviar un mensaje **Then** se muestra `GuestPromptDialog` y no se emite la solicitud.
- **Given** el backend responde `429` **When** se envía un mensaje **Then** se informa el límite alcanzado sin perder el texto escrito con cuenta regresiva `Retry-After`.
- **Given** el backend degrada (`degraded = true`) **When** llega la respuesta **Then** se muestra la respuesta normalmente con aviso de modo básico y sin estado de error.
- **Given** la app compilada **When** se busca en `app/src/main/**` **Then** no existe ninguna respuesta de Sensi generada en el cliente.

## 6. Plan de pruebas

- **Unitarias:** `SendSensiMessageUseCaseTest` (validación mensaje no vacío, límite 1,500 chars, bloqueo invitado).
- **ViewModel:** `SensiChatViewModelTest` con Turbine (estado enviando -> paquete recibido -> error timeout -> degradación modo básico -> 429 rate limit).
- **Integración (`MockWebServer` + `AiMessageDao`):** parseo de respuesta con `recommendedPackage.items`, códigos `400` (moderación/inyección), `403`, `404` (recreación), `429` (`Retry-After`), `200` degraded y persistencia local en Room.
- **UI (Compose Test Rule):** burbujas de usuario/Sensi, tarjetas de paquete de presupuesto, clic hacia detalle, `GuestPromptDialog` en invitado e indicador `isTyping`.

## 7. Riesgos, limites y deuda

- **Dependencia de despliegue:** los tres endpoints están especificados en `sensus-back` Spec 046 pero **aún no desplegados**. El cliente se desarrolla y prueba contra `MockWebServer`; la integración real queda sujeta al despliegue.
- **Deuda documental saneada por esta spec:** la bitácora del 2026-11-11 afirma que `SensiChatScreen` bloquea invitados con `GuestPromptDialog`; el código no lo hace. Se corrige con `RF-24-01`.
- **Deuda de código detectada:** respuesta de IA fabricada en `app/src/main/**` y flujo MVI puenteado (`RF-24-06`, `RF-24-07`). Es la deuda más grave de la feature: incumple una prohibición explícita del propio `plan.md`.
- **Costo:** el proveedor generativo del backend opera bajo la excepción `EX-001` del Apéndice E de la constitución del backend, con techo de 1.00 USD mensuales. Agotado el techo, el backend degrada y el cliente debe seguir respondiendo (`RF-24-10`).
- **Alcance del cliente:** el cliente nunca decide el contenido del paquete; si el backend no responde, no hay paquete.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Aprobada | Rodrigo (decisión D-6 en sesión) | 2026-11-13 | Decisión D-6 resuelta: vía generativa con enmienda. Backend: ADR 0011, `constitution.md` v1.4.0 y Spec 046 (`specs/046-asistente-sensi-ia/`, 4 artefactos + contrato OpenAPI) |
