# TASKS — FT-24 Asistente Sensi IA [BORRADOR]

> Estado: Borrador (bloqueada hasta aprobación de contrato en `sensus-back`, D-6).

- [ ] T24.1 Acordar contrato OpenAPI en `sensus-back` para conversaciones y mensajes de Sensi IA — verif: revisión de contrato
- [ ] T24.2 Crear `AiConversation`, `AiChatMessage`, `BudgetPackage` y `AiChatRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T24.3 Crear `SendSensiMessageUseCase` y `LoadAiConversationUseCase` — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T24.4 Crear `AiMessageEntity.java`, `AiMessageDao.java`, `AiChatApiService.kt` y `AiChatRepositoryImpl.kt` retirando `MockDataProvider.sampleChatMessages` — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T24.5 Actualizar `SensiChatViewModel.kt` (`isTyping`, respuesta real, paquetes) y `SensiChatScreen.kt` (tarjetas navegables de ofertas) — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T24.6 Añadir tests `MockWebServer`, Turbine y Compose UI con Kover `>= 80%` y actualizar `Memoria.md` §6 — verif: `python scripts/check_coverage.py --min 80`
