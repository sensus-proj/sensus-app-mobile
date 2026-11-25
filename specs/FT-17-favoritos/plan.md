# PLAN — FT-17 Favoritos y Guardados de Ofertas

> Derivado de `spec.md`. Historias/Alcance: SE-036 (Pantalla P-23; Componentes C-36, C-37).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Favoritos como capacidad separada de Intereses guardados.** FT-17 implementa favoritos de ofertas sincronizados con backend (`POST /api/v1/users/favorites/{offerId}`). No reemplaza `SavedInterests` para publicaciones. La UI de detalle/tarjetas no debe mostrar dos acciones equivalentes para guardar la misma oferta; el control de oferta pasa a semántica de Favorito.
2. **Offline-first con Room + SQLCipher.** `FavoriteEntity`/`FavoriteDao` almacenan el estado local de favoritos. `SensusDatabase` sube de v8 a v9 con migración explícita `MIGRATION_8_9` en `SensusDatabaseMigrations.java`. La lectura de P-23 funciona sin red.
3. **Toggle optimista con rollback.** `ToggleFavoriteUseCase` actualiza estado local/Room antes de la llamada remota. Si la API falla con error no recuperable (`401`, `404`, error servidor/red no confirmada según política), revierte el cambio local y emite efecto de error no sensible.
4. **Sincronización remota.** El toggle usa `POST /api/v1/users/favorites/{offerId}` (Spec 038). `GET /api/v1/users/favorites` se integra solo si backend lo confirma; mientras tanto se documenta como contrato propuesto y la fuente de verdad local es Room.
5. **Filtros P-23.** `FavoritesViewModel` expone `ALL`, `NEARBY`, `LOW_STOCK`. `LOW_STOCK` = `1 <= stock <= 3`. `NEARBY` usa distancia calculable dentro del radio efectivo del comprador; si no hay ubicación/distancia disponible, la UI muestra estado informativo y no aplica filtro falso.

## 2. Reconciliación con código existente

- `ProductDetailScreen` actualmente cablea guardado a `SavedInterestsViewModel.ToggleOffer` y usa pin (`ProductSaveInterestPill`/`CirclePinIconButton`). FT-17 debe migrar la acción de oferta a Favorito (corazón, semántica y estado reactivo desde `FavoritesRepository`/VM de detalle).
- `SavedOffersRepository`/`SavedInterestsScreen` siguen soportando publicaciones guardadas. Cualquier cambio sobre ofertas guardadas debe preservar posts y evitar regresión en FT-30.
- `BuyerProfileScreen` no tiene entrada a Favoritos hoy; se agrega acceso a `FavoritesScreen`.
- `BuyerHomeScreen`/tarjetas: solo agregar corazón donde el componente de tarjeta tenga contrato claro; si no existe, limitar a ProductDetail + FavoritesScreen en la primera slice y documentar extensión.

## 3. UI y navegación

- `FavoritesScreen` (`P-23`): top bar, chips `C-37`, lista/grilla de favoritos, estado vacío, estado error/retry.
- Estados Compose: `Loading`, `Loaded`, `Empty`, `Error`, `Offline` (si aplica).
- Previews: `FavoritesLoadedPreview`, `FavoritesLowStockPreview`, `FavoritesEmptyPreview`, `FavoritesErrorPreview`.
- Navegación: agregar ruta/entrada en el grafo de buyer/profile según patrón actual (`MainApp*`, `NavigationKeys` si aplica). El acceso mínimo es desde perfil comprador.
- Invitado: `GuestPromptDialog` existente, sin llamada a API.

## 4. Datos y migración

- **Entidad/DAO (Java, patrón Room existente):** `FavoriteEntity.java`, `FavoriteDao.java`.
- **DB:** `SensusDatabase.java` incorpora entidad y DAO; `version = 9`.
- **Migración:** agregar `MIGRATION_8_9` en `SensusDatabaseMigrations.java` y registrarla en `DatabaseFactory`/builder donde se registran migraciones.
- **Módulos DI:** `DatabaseModule` provee `FavoriteDao`; módulo de red/DI equivalente provee `FavoritesApiService`; `RepositoryModule` o módulo equivalente bindea `FavoritesRepository`.
- **Modelo local:** incluir campos mínimos para render P-23 offline (offerId, nombre, comercio, imagen, precio, stock, estado abierto/cerrado si disponible, distancia/cacheada o inputs para calcularla, timestamps). Evitar duplicar innecesariamente datos grandes; definir estrategia snapshot vs join en implementación.

## 5. Contratos y docs vivos

- Confirmar y registrar en `docs/endpoints-internos.md`:
  - `POST /api/v1/users/favorites/{offerId}` — requerido, auth Bearer, estado según implementación.
  - `GET /api/v1/users/favorites` — propuesto/confirmar backend.
- Reconciliar `docs/rate-matrix.md` con el límite real de Spec 038. La spec actual apunta a 120/hora; si backend define otro valor, actualizar spec y rate-matrix en la misma PR.
- No agregar secretos ni PII a logs.

## 6. Gates

- `./gradlew.bat compileDebugKotlin --no-daemon`
- `./gradlew.bat testDebugUnitTest --no-daemon`
- `./gradlew.bat compileDebugAndroidTestSources --no-daemon`
- `./gradlew.bat lint --no-daemon`
- `./gradlew.bat detekt ktlintCheck --no-daemon` (cuando estén cableados; si no, documentar estado real)
- `./gradlew.bat testDebugUnitTest koverVerify --no-daemon` / `koverVerifyDebug` según configuración real
- `python scripts/check_sdd.py`
- `python scripts/check_layers.py`
- `python scripts/check_endpoints.py`
- `python scripts/check_secrets.py`
- `python scripts/check_coverage.py --min 80`

## 7. Orden de implementación

1. **Contratos y dominio:** `FavoriteFilter`, modelo dominio, `FavoritesRepository`, casos de uso.
2. **Persistencia local:** `FavoriteEntity`, `FavoriteDao`, migración v8→v9, `SensusDatabase`, `DatabaseModule`.
3. **Remoto y repositorio:** `FavoritesApiService`, DTOs/mappers, `FavoritesRepositoryImpl` con optimista/rollback.
4. **Presentación:** `FavoritesContract/ViewModel/Screen`, navegación y entrada desde perfil.
5. **Integración UI existente:** reemplazar pin/guardado de oferta en `ProductDetailScreen`; evaluar corazón en tarjetas BuyerHome solo si el componente lo soporta sin ampliar alcance.
6. **Pruebas y docs:** unitarias, VM, Room, MockWebServer, Compose; actualizar docs vivos, Memoria y checklist.

## 8. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Duplicidad con SavedInterests | Definir frontera: Favoritos=ofertas backend-synced; Intereses=posts/ofertas locales según FT-30. Una sola acción visible por oferta. |
| Backend sin `GET /favorites` | P-23 opera con Room; sync remota diferida y contrato marcado propuesto. |
| Filtro `Cerca de mí` sin ubicación | Estado informativo/resultado vacío explícito; no usar distancia falsa. |
| Regresión en posts guardados | No tocar semántica de posts en SavedInterests; pruebas de regresión. |
| Migración DB | Test de migración 8→9 y registro en factory. |

## 9. Prohibiciones

- Prohibido dejar dos acciones visibles equivalentes para guardar la misma oferta.
- Prohibido llamar endpoint de favoritos en modo invitado.
- Prohibido guardar PII/JWT en logs o Room sin cifrado.
- Prohibido romper guardado de publicaciones de Intereses guardados.
- Prohibido `assembleDebug`/`bundleRelease` salvo instrucción explícita.

## Tareas

Ver `tasks.md`.