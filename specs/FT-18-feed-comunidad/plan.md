# PLAN — FT-18 Feed Social de Comunidad y Creación de Publicaciones

> Derivado de `spec.md`. Historias/Alcance: SE-027, SE-028, SE-029 (Pantalla P-24; Componentes C-38, C-40).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Feed paginado por cursor (`GET /api/v1/feed`).** `GetCommunityFeedUseCase` consulta páginas de 20 posts ordenadas por cercanía y recencia, cacheando la primera página en `CommunityPostDao` (SQLCipher).
2. **Creación de post moderada (`POST /api/v1/feed/posts`).** `CreateCommunityPostUseCase` valida texto (`1..500` caracteres), comprime foto opcional (`<= 1 MB`) y envía al backend para moderación previa a Cloudinary.
3. **Baja de post propio (`DELETE /api/v1/feed/posts/{id}`).** `DeleteCommunityPostUseCase` verifica autoría y elimina el post remota y localmente.

## 2. UI y navegación

- `CommunityScreen` (`P-24`) renderiza `CommunityPostCard` (`C-40`), shimmer de carga y pull-to-refresh.
- `CreatePostBottomSheet` permite redactar publicación, adjuntar foto y vincular oferta/comercio.
- Previews: `FeedLoaded`, `CreatePostSheet`, `FeedEmpty`, `ModerationError`.

## 3. DI

| Binding | Implementación |
|---|---|
| `FeedApiService` | Retrofit `/api/v1/feed/*` en `NetworkModule` |
| `CommunityRepository` | `CommunityRepositoryImpl` en `RepositoryModule` |

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

1. Crear `CommunityPost`, `CommunityRepository` y casos de uso (`Get`, `Create`, `Delete`) en `domain/`.
2. Crear `FeedApiService`, `CommunityPostEntity` y `CommunityPostDao` en `data/`.
3. Implementar `CommunityRepositoryImpl` y retirar `MockDataProvider` de `CommunityViewModel`.
4. Crear `CreatePostBottomSheet` y conectar `CommunityScreen` (`P-24`).
5. Añadir tests unitarios y `MockWebServer` (`200`, `201`, `204`, `422`).

## 6. Prohibiciones

- Prohibido publicar posts sin pasar por la moderación del backend.
- Prohibido permitir crear posts en modo invitado.

## Tareas

Ver `tasks.md`.
