# PLAN — FT-17 Favoritos y Guardados de Ofertas

> Derivado de `spec.md`. Historias/Alcance: SE-036 (Pantalla P-23; Componente C-37).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Toggle optimista con rollback (`POST /api/v1/users/favorites/{id}`).** `ToggleFavoriteUseCase` actualiza `FavoriteDao` inmediatamente y, si la llamada HTTP falla, revierte el registro local y emite `ViewEffect.ShowError`.
2. **Persistencia cifrada (`FavoriteDao`).** Tabla `favorites` en SQLCipher asociada con `offers` para lectura instantánea offline.
3. **Pantalla Mis Guardados (`P-23`).** `FavoritesViewModel` filtra por `ALL` (`Todos`), `NEARBY` (`Cerca de mí <= 2 km`) y `LOW_STOCK` (`Por agotarse <= 3 uds.`, `C-37`).

## 2. UI y navegación

- `FavoritesScreen` (`P-23`) con chips `C-37`, grilla de tarjetas e ilustración de estado vacío.
- Corazón de favorito conectado en `BuyerHomeScreen` y `ProductDetailScreen` (reemplazando el `Toast` placeholder).
- Previews: `FavoritesLoaded`, `FavoritesLowStock`, `FavoritesEmpty`.

## 3. DI

| Binding | Implementación |
|---|---|
| `FavoriteDao` | Provisión en `DatabaseModule` (`SensusDatabase`) |
| `FavoritesApiService` | Retrofit `/api/v1/users/favorites/*` en `NetworkModule` |
| `FavoritesRepository` | `FavoritesRepositoryImpl` en `RepositoryModule` |

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

1. Crear `FavoriteFilter`, `FavoritesRepository`, `ToggleFavoriteUseCase` y `GetFavoritesUseCase`.
2. Crear `FavoriteEntity`, `FavoriteDao` y `FavoritesApiService`.
3. Implementar `FavoritesRepositoryImpl` con actualización optimista y rollback.
4. Crear `FavoritesContract`, `FavoritesViewModel` y `FavoritesScreen` (`P-23`).
5. Conectar el icono de corazón en `ProductDetailScreen` y `BuyerHomeScreen`.

## 6. Prohibiciones

- Prohibido dejar `Toast.makeText` como única acción del botón de favorito.
- Prohibido permitir guardar favoritos en sesión de invitado.

## Tareas

Ver `tasks.md`.
