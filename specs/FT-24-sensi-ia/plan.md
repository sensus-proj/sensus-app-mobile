# PLAN — FT-24 Asistente Conversacional Sensi IA (Bloqueada por Backend)

> Derivado de `spec.md`. Historias/Alcance: Pantalla P-25, Componente C-39 (Tesina Sensus / Excel Móvil).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Gobernanza SDD (`Borrador`).** El endpoint `POST /api/v1/ai/chat` no existe aún en el catálogo AWS (`Catalogo_Endpoints_AWS.xlsx`) ni en `sensus-back/specs/001..048`. La feature queda documentada y bloqueada hasta que se apruebe la spec de backend o se descarte.
2. **Saneamiento inmediato.** `SensiChatViewModel.kt` no puede importar `MockDataProvider` en `app/src/main/**` (se limpia en `FT-10`).
3. **Contrato previsto (si se aprueba en backend).** `SensiAiApiService` (`POST /api/v1/ai/chat`) recibe `{ message, lat, lng, radiusKm }` y devuelve `{ reply, suggestedOffers: List<OfferDto> }`.

## 2. UI y navegación

- `SensiChatScreen` (`P-25`) muestra burbujas de conversación (`C-39`), chips de prompts rápidos y tarjetas de ofertas sugeridas.
- Mientras el backend no exista, la entrada a `P-25` se mantiene oculta tras feature flag o muestra aviso honesto de disponibilidad.

## 3. DI

| Binding | Implementación |
|---|---|
| `AskSensiUseCase` | Pendiente de spec en `sensus-back` |

## 4. Gates

- `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes.
- `./gradlew.bat detekt ktlintCheck lint` verdes (0 violaciones).
- `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (cobertura `>= 80%`).
- `python scripts/check_sdd.py` PASS.
- `python scripts/check_layers.py` PASS.
- `python scripts/check_endpoints.py` PASS.
- `python scripts/check_secrets.py` PASS.
- `python scripts/check_coverage.py --min 80` PASS.

## 5. Orden de implementación

1. Confirmar con el responsable del proyecto si se creará la spec `POST /api/v1/ai/chat` en `sensus-back` o si se retira `P-25` del alcance.
2. Si se aprueba en backend: crear `SensiAiApiService`, `SensiRepositoryImpl` y `AskSensiUseCase`.
3. Conectar `SensiChatViewModel` al repositorio real y agregar tests `MockWebServer`.

## 6. Prohibiciones

- Prohibido simular respuestas de IA con `MockDataProvider` o `delay()` en código de producción.
- Prohibido invocar APIs de OpenAI/LLM directamente desde el APK con API keys en el cliente.

## Tareas

Ver `tasks.md`.
