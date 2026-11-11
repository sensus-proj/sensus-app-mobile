# TASKS — FT-22 UX Transversal: Conectividad Offline, Tema Oscuro y Shimmers

> 1 PR = 1 slice. Orden: Estado/Tema -> Componentes comunes -> Integración pantallas -> Tests -> Gates.

## Fase A — Tema y Estado Global

- [ ] T22.1 Definir `SensusDarkColorScheme` en `theme/Color.kt` y activar `darkTheme = isSystemInDarkTheme()` en `theme/Theme.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T22.2 Inyectar `NetworkMonitor` y `SyncManager` en `AppViewModel.kt` exponiendo estado offline en `AppViewState` — verif: `./gradlew.bat testDebugUnitTest`

## Fase B — Componentes Transversales UI

- [ ] T22.3 Crear `OfflineErrorState.kt` (`P-06`) con botón `'Reintentar'` y `LocationDisabledState.kt` (`P-05`) — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T22.4 Crear `ShimmerPlaceholders.kt` para tarjetas de ofertas, pines y feed — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T22.5 Integrar `SnackbarHost` de conectividad en `MainAppContainer.kt` y reemplazar colores fijos críticos por `MaterialTheme.colorScheme` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T22.6 Conectar `ShimmerPlaceholders` y `OfflineErrorState` en `BuyerHomeScreen.kt` y pantallas de listado — verif: `./gradlew.bat compileDebugKotlin`

## Fase C — Pruebas y Cierre

- [ ] T22.7 Ampliar `AppViewModelTest` con Turbine para transiciones `NetworkMonitor` y `SyncManager` — verif: `./gradlew.bat testDebugUnitTest`
- [ ] T22.8 Añadir tests Compose UI para `OfflineErrorState`, `LocationDisabledState`, `ShimmerPlaceholders` y tema oscuro — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T22.9 Actualizar `Memoria.md` §6 y validar todos los gates — verif: `python scripts/check_coverage.py --min 80`
