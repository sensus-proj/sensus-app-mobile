# TASKS — FT-24 Asistente Sensi IA

> Estado: Aprobada (D-6 resuelta el 2026-11-13; backend especificado en `sensus-back` Spec 046, despliegue pendiente).

- [x] T24.1 Acordar contrato en `sensus-back` para conversaciones y mensajes de Sensi IA — verif: `sensus-back/specs/046-asistente-sensi-ia/contracts/openapi.yaml`, ADR 0011, ADR 0012 y `constitution.md` v2.1.0 del backend (`recommendedPackage.items`)
- [x] T24.2 Retirar la respuesta fabricada de `SensiChatScreen.sendUserMessage(...)`, `SAMPLE_COMBO_PRICE`, `MockDataProvider.sampleChatMessages` y `StoreRepository.getSampleChatMessages()`; restaurar el flujo MVI en la pantalla — verif: `grep -rn "SAMPLE_COMBO_PRICE\|sampleChatMessages\|Combo Express Inteligente" app/src/main` vacío y `./gradlew.bat compileDebugKotlin`
- [x] T24.3 Bloquear envío en modo invitado con `GuestPromptDialog` desde `SensiChatScreen` — verif: test Compose en `androidTest` / `test`
- [x] T24.4 Crear `AiConversation`, `AiChatMessage`, `RecommendedPackage`, `CitedOffer`, `ScreenContext` y `AiChatRepository` en `domain/` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T24.5 Crear `SendSensiMessageUseCase` y `LoadAiConversationUseCase` — verif: `./gradlew.bat testDebugUnitTest`
- [x] T24.6 Crear `AiMessageEntity.java`, `AiMessageDao.java`, migración Room v4->v5, `AiChatApiService.kt`, `NetworkClient.aiChatApi` (timeout 30 s sin logs de cuerpo) y `AiChatRepositoryImpl.kt` — verif: `./gradlew.bat testDebugUnitTest` y `python scripts/check_endpoints.py`
- [x] T24.7 Actualizar `SensiChatContract.kt` (`isTyping`, `recommendedPackage`, errores `400`/`403`/`404`/`429`/degradación), `SensiChatViewModel.kt` y `SensiChatScreen.kt` (tarjetas navegables) — verif: `./gradlew.bat compileDebugKotlin`
- [x] T24.8 Añadir tests `MockWebServer`, Turbine y Compose UI con Kover `>= 80%` y actualizar `memoria.md` §6 — verif: `python scripts/check_coverage.py --min 80`
- [x] T24.9 Renderizar Markdown ligero (negritas, cursivas, código, viñetas y listas numeradas) en las respuestas de Sensi con `MarkdownParser` y `MarkdownText` sin dependencias nuevas — verif: `MarkdownParserTest` (12 pruebas) y `./gradlew.bat testDebugUnitTest`
- [x] T24.10 Mostrar las ofertas que Sensi menciona como tarjeta (imagen, descuento, tienda, precios, stock, vigencia, dirección) con botones Apartar, Cómo llegar y Ver detalle conectados al detalle, apartado y ruta existentes; `offersJson` en Room v6 (`MIGRATION_5_6`) — verif: `SensiOfferMapperTest`, `AiOffersCacheTest` y `./gradlew.bat testDebugUnitTest`
