# PLAN — FT-26 Perfil Público de Comercio y Reseñas Verificadas (Bloqueada por Backend)

> Derivado de `spec.md`. Historias/Alcance: Pantalla P-15; Componentes C-28, C-29, C-30, C-31 (Excel Móvil).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Gobernanza SDD (`Borrador`).** Los endpoints `GET /api/v1/merchants/{id}`, `GET /api/v1/merchants/{id}/reviews` y `POST /api/v1/merchants/{id}/reviews` están documentados en la hoja `CRUD · Reseñas Comercio` del Excel del Drive pero aún no tienen spec en `sensus-back`.
2. **Regla de reseña verificada.** Solo un comprador con al menos un apartado en estado `REDEEMED` en ese comercio puede enviar calificación (`1..5` estrellas) y comentario.

## 2. UI y navegación

- `MerchantPublicProfileScreen` (`P-15`) con cabecera `C-28`, horarios `C-29`, ofertas activas del local y lista de reseñas `C-30`/`C-31`.

## 3. DI

| Binding | Implementación |
|---|---|
| `MerchantReviewsRepository` | Pendiente de spec en `sensus-back` |

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

1. Aprobar e implementar spec de perfil público y reseñas de comercio en `sensus-back`.
2. Añadir endpoints en `MerchantApiService` y crear casos de uso en `domain/`.
3. Implementar `MerchantPublicProfileScreen` (`P-15`) y formulario de reseña verificada.

## 6. Prohibiciones

- Prohibido permitir publicar reseñas a usuarios sin canje verificado (`REDEEMED`) o en modo invitado.

## Tareas

Ver `tasks.md`.
