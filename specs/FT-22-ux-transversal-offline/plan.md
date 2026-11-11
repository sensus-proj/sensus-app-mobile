# PLAN — FT-22 Estados UX Transversales (Offline, Permisos, Vacíos y Shimmer)

> Derivado de `spec.md`. Historias/Alcance: SE-042, SE-043, SE-044 (Pantallas P-05, P-06; Componentes C-06, C-10, C-19, C-50).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Conectividad reactiva (`NetworkMonitor`).** `SensusMainScaffold` observa `NetworkMonitor.isOnline` y despliega `OfflineBanner` (`C-06`) de forma no intrusiva.
2. **Pantallas de bloqueo y error (`P-05`, `P-06`).** `NoConnectionScreen` (`P-05`) cuando no existe caché local y `LocationBlockedScreen` (`P-06`) con acceso a ajustes o selector manual de colonia.
3. **Catálogo de estados reutilizables.** `SensusLoadingShimmer` (`C-10`), `SensusEmptyState` (`C-19`) y `SensusErrorRetryCard` unificados con semántica TalkBack y touch targets `>= 48x48 dp`.

## 2. UI y navegación

- `NoConnectionScreen` (`P-05`), `LocationBlockedScreen` (`P-06`), `OfflineBanner` (`C-06`) y `SensusLoadingShimmer` (`C-10`) en `presentation/components/states/`.
- Previews: `OfflineBannerVisible`, `NoConnectionFull`, `LocationBlockedFull`, `ErrorRetryCard`.

## 3. DI

| Binding | Implementación |
|---|---|
| `NetworkMonitor` | `ConnectivityNetworkMonitor` vinculado en `AppModule` |

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

1. Crear `NoConnectionScreen` (`P-05`) y `LocationBlockedScreen` (`P-06`).
2. Unificar `SensusLoadingShimmer`, `SensusEmptyState` y `SensusErrorRetryCard`.
3. Integrar `OfflineBanner` (`C-06`) en el scaffold principal.
4. Verificar accesibilidad (`contentDescription`, touch target `>= 48 dp`) y pruebas Compose UI.

## 6. Prohibiciones

- Prohibido mostrar pantallas en blanco o spinners infinitos sin salida cuando falla la red.
- Prohibido exponer nombres de excepciones Kotlin/Java o códigos HTTP crudos al usuario.

## Tareas

Ver `tasks.md`.
