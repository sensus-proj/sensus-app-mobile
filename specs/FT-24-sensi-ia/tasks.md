# TASKS — FT-24 Asistente Sensi IA

> Estado: Aprobada (D-6 resuelta el 2026-11-13; backend especificado en `sensus-back` Spec 046, despliegue pendiente).

- [x] T24.1 Acordar contrato en `sensus-back` para conversaciones y mensajes de Sensi IA — verif: `sensus-back/specs/046-asistente-sensi-ia/contracts/openapi.yaml`, ADR 0011 y `constitution.md` v1.4.0 del backend (2026-11-13)
- [ ] T24.2 Retirar la respuesta fabricada de `SensiChatScreen.sendUserMessage(...)`, `SAMPLE_COMBO_PRICE`, `MockDataProvider.sampleChatMessages` y `StoreRepository.getSampleChatMessages()`; restaurar el flujo MVI en la pantalla — verif: `grep -rn "SAMPLE_COMBO_PRICE\|sampleChatMessages\|Combo Express Inteligente" app/src/main` vacío y `./gradlew.bat compileDebugKotlin`
- [ ] T24.3 Bloquear envío en modo invitado con `GuestPromptDialog` desde `SensiChatScreen` — verif: test Compose en `androidTest`
- [ ] T24.4 Crear `AiConversation`, `AiChatMessage`, `BudgetPackage` y `AiChatRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T24.5 Crear `SendSensiMessageUseCase` y `LoadAiConversationUseCase` — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T24.6 Crear `AiMessageEntity.java`, `AiMessageDao.java`, migración Room, `AiChatApiService.kt` y `AiChatRepositoryImpl.kt` — verif: `./gradlew.bat testDebugUnitTest` y `python scripts/check_endpoints.py`
- [ ] T24.7 Actualizar `SensiChatContract.kt` (`isTyping`, `budgetPackage`, errores `422`/`429`/degradación), `SensiChatViewModel.kt` y `SensiChatScreen.kt` (tarjetas navegables) — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T24.8 Añadir tests `MockWebServer`, Turbine y Compose UI con Kover `>= 80%` y actualizar `Memoria.md` §6 — verif: `python scripts/check_coverage.py --min 80`
