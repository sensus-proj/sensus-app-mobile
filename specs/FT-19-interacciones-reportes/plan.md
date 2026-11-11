# PLAN — FT-19 Interacciones Sociales y Reportes de Moderación

> Derivado de `spec.md`. Historias/Alcance: SE-030, SE-031, SE-032, SE-033 (Pantalla P-22).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Reacción Like (`POST /api/v1/feed/posts/{id}/like`).** `TogglePostLikeUseCase` aplica actualización optimista sobre `isLikedByMe` y `likesCount` con rollback automático si falla la petición.
2. **Comentarios (`GET/POST /api/v1/feed/posts/{id}/comments`).** `CreatePostCommentUseCase` valida longitud (`1..300` caracteres) y envía a moderación síncrona antes de publicar.
3. **Reportes (`POST /api/v1/reports` y `/feed/posts/{id}/report`).** `SubmitReportUseCase` envía motivo (`FAKE_OFFER`, `OFFENSIVE`, `SPAM`, `WRONG_PRICE`, `OTHER`), oculta preventivamente el ítem en cliente y excluye CSAM redirigiendo a canal legal externo (`constitution.md` §8).

## 2. UI y navegación

- `PostCommentsBottomSheet` muestra lista paginada de comentarios y barra de envío.
- `ReportContentDialog` (`P-22`) presenta motivos tipificados, detalle opcional y confirmación.
- Previews: `CommentsSheetLoaded`, `ReportDialogReasons`, `ReportSuccess`.

## 3. DI

| Binding | Implementación |
|---|---|
| `ReportsApiService` | Retrofit `/api/v1/reports` en `NetworkModule` |
| `TogglePostLikeUseCase` | Inyección por constructor con `CommunityRepository` |
| `SubmitReportUseCase` | Inyección por constructor con `CommunityRepository` |

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

1. Crear `PostComment`, `ReportReason`, `ReportTargetType` y casos de uso en `domain/`.
2. Añadir endpoints de likes, comentarios y reportes en `FeedApiService` y `ReportsApiService`.
3. Implementar `PostCommentsBottomSheet` y `ReportContentDialog` (`P-22`).
4. Conectar reacciones, comentarios y reportes en `CommunityScreen` y `ProductDetailScreen`.
5. Probar rollback de likes, moderación de comentarios y `409 Conflict` en reportes duplicados.

## 6. Prohibiciones

- Prohibido almacenar o procesar reportes de explotación infantil en la app o backend (canalizar por vía legal externa).
- Prohibido permitir likes, comentarios o reportes en modo invitado.

## Tareas

Ver `tasks.md`.
