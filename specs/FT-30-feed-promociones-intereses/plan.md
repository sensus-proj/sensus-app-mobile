# PLAN — FT-30 Feed de Promociones por Intereses

> Derivado de `spec.md`. Backend: specs `042` (v2.0.0), `054` y `055`.
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11 (v1.3.0).

## 1. Diseño

1. **Feed paginado (`GET /api/v1/feed/offers`).** `GetOfferFeedUseCase` mantiene el `snapshotId` de la página 1 y lo envía en las siguientes. Se reinicia al refrescar o si la ubicación cambia más de 500 m. Si el backend devuelve `snapshotId` nulo, deduplica por `offerId`.
2. **Intereses (`GET`/`PUT /api/v1/users/me/interests`).** `UpdateMyInterestsUseCase` valida 0–5 categorías del catálogo sin `Otros` antes de llamar al backend; el backend vuelve a validar.
3. **Sin telemetría en el cliente.** El backend registra impresiones, aperturas y apartados (spec 055). El móvil no envía eventos.
4. **Invitado.** La pestaña se muestra, pero abre `GuestPromptDialog`; el caso de uso no llama al endpoint si la sesión es de invitado.

## 2. UI y navegación

- Pestaña `Promociones` junto a `Inicio`, `Mapa` y `Comunidad`.
- `InterestsPickerScreen` como paso opcional al final del registro y desde `BuyerProfileScreen`.
- Tarjeta de oferta reutilizada de `BuyerHomeScreen`; las posiciones `slot = exploration` no llevan marca visual en esta versión.
- Previews: `FeedLoading`, `FeedLoaded`, `FeedEmpty`, `FeedOffline`, `InterestsPicker`.

## 3. DI

| Binding | Implementación |
|---|---|
| `OfferFeedApiService` | Retrofit en `NetworkModule` |
| `InterestsApiService` | Retrofit en `NetworkModule` |
| `FeedOfferDao` | Provisión en `DatabaseModule` (SQLCipher) |
| `OfferFeedRepository`, `InterestsRepository` | `RepositoryModule` |

## 4. Gates

- `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes.
- `./gradlew.bat detekt ktlintCheck lint` verdes.
- `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (cobertura `>= 80%`).
- `python scripts/check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py` y `check_coverage.py --min 80` PASS.

## 5. Orden de implementación

1. Modelos y casos de uso de dominio.
2. Servicios Retrofit, DTOs y repositorios con caché cifrada de la página 1.
3. `InterestsPickerScreen` y su integración tras el registro y en el perfil.
4. `OfferFeedScreen` con paginación y estados.
5. Pruebas unitarias, de integración y de UI.

## 6. Prohibiciones

- Prohibido ordenar o filtrar el feed en el cliente: el orden es el del backend.
- Prohibido registrar coordenadas, intereses o identificadores en logs.
- Prohibido llamar al feed en sesión de invitado.

## Tareas

Ver `tasks.md`.
