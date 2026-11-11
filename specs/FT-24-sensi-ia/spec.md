# SPEC — FT-24 Asistente Sensi IA (Paquetes de Presupuesto y Recetas) [BORRADOR]

> Rector: `.specify/memory/constitution.md` §2, §6.3 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **Sensus_Movil_Componentes_y_CRUD.xlsx** (hoja *Sensi IA*, Pantalla **P-25**, Componente **C-39**); **Tesina Sensus §1, §2.3**; **Formato de Aceptación**.
> Backend ref: **Sin spec aún en `sensus-back` (bloqueada hasta abrir spec equivalente en backend, decisión D-6)**.

## 1. Contexto y objetivo

La Tesina, el Formato de Aceptación y la hoja *Sensi IA* del Drive especifican a **Sensi**, un asistente conversacional donde el usuario indica un presupuesto (ej. `"Tengo $100 para la despensa"`) o pide ideas de recetas, y Sensi arma un paquete de ofertas cercanas abiertas y con stock sin rebasar el monto. Actualmente `SensiChatViewModel` solo agrega mensajes del usuario sobre una lista estática de `MockDataProvider`.

**Objetivo:** conectar `SensiChatScreen` y `SensiChatViewModel` con un puente REST seguro en el backend (`POST /api/v1/ai/conversations`, `POST /api/v1/ai/messages`, `GET /api/v1/ai/conversations/{id}`) para generar paquetes de ofertas por presupuesto, manteniendo historial en Room + SQLCipher y retirando `MockDataProvider.sampleChatMessages`.

**Alcance:** `AiChatRepository`, `SendSensiMessageUseCase`, `AiChatEntity`/`AiChatDao`, tarjetas interactivas de paquete de presupuesto dentro de burbujas de chat (`P-25`) con navegación al detalle/apartado de cada producto.
**Fuera de alcance:** llamadas directas desde Android a OpenAI/Gemini con API keys embebidas (prohibido por seguridad §4/§6.3).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-24-01 | Bloquear acceso o envío en modo invitado mostrando `GuestPromptDialog` (o permitir solo vista informativa según política). |
| RF-24-02 | Iniciar o recuperar conversación mediante `POST /api/v1/ai/conversations` / `GET /api/v1/ai/conversations/{id}` y persistir caché local en `AiChatDao`. |
| RF-24-03 | Al enviar mensaje con presupuesto/consulta (`POST /api/v1/ai/messages`), adjuntar ubicación aproximada y radio actual; el backend devuelve texto de Sensi y lista opcional de ofertas (`budgetPackageItems`) cuyo total no exceda el presupuesto indicado. |
| RF-24-04 | Renderizar dentro de la burbuja de Sensi tarjetas compactas de cada oferta sugerida con botón para abrir su `ProductDetailScreen` o agregarlas directamente al apartado. |
| RF-24-05 | Retirar por completo `MockDataProvider.sampleChatMessages` de `StoreRepositoryImpl` y `SensiChatViewModel`. |

## 3. Requisitos no funcionales

- **Seguridad:** cero API keys de LLM en el cliente Android; toda orquestación de prompts, moderación de entrada/salida y cruce con ofertas vigentes ocurre en `sensus-back`.
- **Resiliencia:** timeout de hasta 30 s en generación de respuesta con indicador de escritura (`Sensi está pensando...`) y mensaje claro de reintento ante fallo.

## 4. Arquitectura y contratos internos

```text
domain/
  model/AiChatModels.kt         # AiConversation, AiChatMessage, BudgetPackage
  repository/AiChatRepository.kt
  usecase/SendSensiMessageUseCase.kt, LoadAiConversationUseCase.kt
data/
  local/db/entity/AiMessageEntity.java, dao/AiMessageDao.java
  remote/api/AiChatApiService.kt # POST /api/v1/ai/conversations, /messages, GET /{id}
  repository/AiChatRepositoryImpl.kt
presentation/buyer/
  SensiChatContract.kt, SensiChatViewModel.kt, SensiChatScreen.kt
```

## 5. Criterios de aceptacion

- **Given** un comprador autenticado escribe `"Tengo $100 para comida"` **When** envía en `SensiChatScreen` **Then** llama a `POST /api/v1/ai/messages` y muestra respuesta más tarjetas de ofertas cercanas cuya suma es `<= $100`.
- **Given** una tarjeta sugerida dentro del chat **When** el usuario la toca **Then** navega a `ProductDetailScreen` de esa oferta.
- **Given** usuario sin red **When** abre `SensiChatScreen` **Then** consulta últimos mensajes desde `AiMessageDao` y deshabilita envío hasta recuperar conexión.

## 6. Plan de pruebas

- **Unitarias:** `SendSensiMessageUseCaseTest` (validación mensaje no vacío, bloqueo invitado).
- **ViewModel:** `SensiChatViewModelTest` con Turbine (estado enviando -> paquete recibido -> error timeout).
- **Integración (`MockWebServer` + `AiMessageDao`):** parseo de respuesta con `budgetPackageItems` y persistencia local.
- **UI (Compose Test Rule):** burbujas de usuario/Sensi, tarjetas de paquete de presupuesto y clic hacia detalle.

## 7. Riesgos, limites y deuda

- **BLOQUEANTE:** estos endpoints aún **no existen** en `sensus-back` (actualmente ADR 0009 limita recomendaciones a lógica determinista). Esta spec permanece en estado `Borrador` hasta acordar y especificar el servicio equivalente en el backend (decisión D-6).

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Bloqueada por falta de spec/contrato en backend (D-6) |
