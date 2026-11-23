# PLAN — FT-22 UX Transversal: Conectividad Offline, Tema Oscuro y Shimmers

> Derivado de `spec.md`. Historias/Alcance: SE-042, SE-043, SE-044 (Pantallas P-05, P-06; Componente C-15).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Conectividad reactiva (`NetworkMonitor` y `SyncManager`).** `AppViewModel` observa `NetworkMonitor.isOnline` y `SyncManager.isSyncing` / pendientes, exponiendo `isOnline` y `pendingSyncCount` en `AppViewState`. `MainAppContainer` despliega `Snackbar` no intrusivo y `OfflineBanner` reutilizable cuando corresponda.
2. **Pantallas de bloqueo y error (`P-05`, `P-06`).** `LocationDisabledState` (`P-05`) cuando el GPS está apagado (con botón a `Settings.ACTION_LOCATION_SOURCE_SETTINGS` y selección manual de ubicación reutilizando el patrón de `OnboardingLocationScreen` / `MerchantLocationPickerModal`), y `OfflineErrorState` (`P-06`) cuando no existe red ni caché local en `OfferDao`, ofreciendo botón `Reintentar`.
3. **Shimmers y tema oscuro.** `ShimmerPlaceholders` (`C-15`) para tarjetas y listas con `rememberInfiniteTransition` durante `isLoading` sin caché. Paleta clara y oscura en `Color.kt` / `Theme.kt` con contraste WCAG AA, sin tonos amarillos (`SensusAmber` eliminado).

## 2. UI y navegación

- Componentes en `presentation/components/states/`:
  - `OfflineErrorState.kt` (`P-06`, sin conexión con botón Reintentar)
  - `LocationDisabledState.kt` (`P-05`, ubicación desactivada con ajustes y selección manual)
  - `ShimmerPlaceholders.kt` (`C-15`, estado cargando esqueleto)
  - `OfflineBanner.kt` (banner informativo reutilizable)
- Previews en claro y oscuro: `OfflineErrorState`, `LocationDisabledState`, `OfflineBanner`, `ShimmerPlaceholders`.

## 3. DI

| Binding | Implementación |
|---|---|
| `NetworkMonitor` | `ConnectivityNetworkMonitor` vinculado en `AppModule` |
| `SyncManager` | `DefaultSyncManager` vinculado en `AppModule` |

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

1. Adaptar `Color.kt` y `Theme.kt` (`DarkColorScheme` + `LightColorScheme` con `isSystemInDarkTheme()`, eliminar `SensusAmber`).
2. Actualizar `AppContract.kt` y `AppViewModel.kt` observando `NetworkMonitor` y `SyncManager`.
3. Crear `OfflineErrorState.kt` (`P-06`), `LocationDisabledState.kt` (`P-05`), `ShimmerPlaceholders.kt` (`C-15`) y mover `OfflineBanner.kt` a `presentation/components/states/`.
4. Integrar banner/snackbar en `MainAppContainer.kt` y conectar shimmers/estados en `BuyerHomeScreen.kt`, `SearchOffersScreen.kt` y `CommunityScreen.kt`.
5. Pruebas unitarias (`AppViewModelTest`, ViewModels de pantalla), pruebas Compose UI y validación de gates.

## 6. Prohibiciones

- Prohibido mostrar pantallas en blanco o spinners infinitos sin salida cuando falla la red.
- Prohibido exponer nombres de excepciones Kotlin/Java o códigos HTTP crudos al usuario.

## Tareas

Ver `tasks.md`.
