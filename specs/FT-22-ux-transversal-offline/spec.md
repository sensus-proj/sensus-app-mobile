# SPEC — FT-22 UX Transversal: Conectividad Offline, Tema Oscuro y Shimmers

> Rector: `.specify/memory/constitution.md` §2, §5 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-042 (criterio resiliencia de red), SE-043 (criterio modo claro/oscuro), SE-044 (criterio carga progresiva/shimmers)**; Pantallas **P-05, P-06**.

## 1. Contexto y objetivo

Aunque `ConnectivityNetworkMonitor` y `DefaultSyncManager` ya existen en la capa de datos/dominio, la interfaz móvil todavía no muestra un `Snackbar` global reactivo ni pantalla de error `'Sin conexión'` (`P-06`) con botón `'Reintentar'`, carece de estado `'Ubicación desactivada'` (`P-05`) dedicado, `Theme.kt` necesita paleta completa para modo oscuro de alto contraste y las listas muestran textos simples en vez de esqueletos (*shimmers*) durante peticiones de red.

**Objetivo:** exponer de forma transversal en la UI el estado de conectividad (`NetworkMonitor`) y cola de sincronización (`SyncManager`), implementar estado sin conexión (`P-06`), estado GPS apagado (`P-05`), placeholders tipo *shimmer* durante carga asíncrona y soporte de tema claro/oscuro conforme a la configuración del sistema operativo.

**Alcance:** banner/`Snackbar` global de conectividad en `MainAppContainer`/`AppViewModel`, componentes reutilizables `OfflineErrorScreen` (`P-06`) y `LocationDisabledScreen` (`P-05`), componente `ShimmerOfferCard`, adaptación de `Color.kt`/`Theme.kt` para modo claro y oscuro sin tonos amarillos, y manejo centralizado de errores asíncronos evitando crashes.
**Fuera de alcance:** endpoints nuevos de backend.

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-22-01 | Observar `NetworkMonitor.isOnline` en `AppViewModel` / contenedor principal para mostrar `Snackbar` no intrusivo al perder conexión y aviso de sincronización al recuperarla. |
| RF-22-02 | Cuando no exista red ni datos cacheados en `OfferDao`, renderizar vista `P-06` (`OfflineErrorScreen`) con botón `'Reintentar'` que dispare recarga del `ViewModel` actual. |
| RF-22-03 | Cuando los servicios de ubicación del dispositivo estén desactivados (`P-05`), ofrecer pantalla/tarjeta informativa con botón para abrir ajustes de ubicación del sistema o elegir ubicación manualmente (`LocationPickerSheet`). |
| RF-22-04 | Reemplazar indicadores vacíos de carga por componentes `Shimmer` animados en `BuyerHomeScreen`, `SearchOffersScreen` y `CommunityScreen`. |
| RF-22-05 | Soportar paleta clara y oscura en `Theme.kt` respetando `isSystemInDarkTheme()`, contraste AA y regla estricta de cero tonos amarillos en UI interna. |

## 3. Requisitos no funcionales

- **Estabilidad:** cero excepciones no capturadas por red caída, timeouts o cambios de configuración.
- **Accesibilidad:** contraste mínimo WCAG AA tanto en modo claro como en modo oscuro; iconos vectoriales con `contentDescription`.
- **Rendimiento:** animación shimmer con `rememberInfiniteTransition` ligera sin recomposiciones innecesarias en listas.

## 4. Arquitectura y contratos internos

```text
presentation/root/AppContract.kt, AppViewModel.kt   # Expone isOnline y pendingSyncCount
presentation/common/
  OfflineErrorState.kt            # P-06 Reintentar
  LocationDisabledState.kt        # P-05 Ubicación apagada
  ShimmerPlaceholders.kt          # C-15 estado cargando (esqueleto)
theme/Color.kt, Theme.kt          # DarkColorScheme + LightColorScheme con SensusPurple
```

## 5. Criterios de aceptacion

- **Given** el dispositivo pierde conectividad **When** el usuario navega por la app **Then** aparece `Snackbar` informativo sin crashes y conserva los datos cacheados en Room.
- **Given** una pantalla sin datos previos y sin internet **When** intenta cargar **Then** muestra `OfflineErrorState` (`P-06`) y al tocar `'Reintentar'` vuelve a consultar.
- **Given** el sistema operativo cambia a Modo Oscuro **When** se abre la app **Then** la UI adopta `DarkColorScheme` legible y mantiene el morado de marca sin tonos amarillos.
- **Given** una carga de ofertas en curso (`isLoading = true`) **When** no hay caché previo **Then** renderiza tarjetas shimmer animadas.

## 6. Plan de pruebas

- **Unitarias / ViewModel:** `AppViewModelTest` con Turbine probando transiciones `isOnline = true -> false -> true` y disparo de `SyncManager`.
- **UI (Compose Test Rule):** pruebas de `OfflineErrorState` (clic en `'Reintentar'`), `LocationDisabledState`, `ShimmerPlaceholders` y renderizado en `darkTheme = true` y `false`.

## 7. Riesgos, limites y deuda

- Algunos colores hardcodeados (`Color.White`, `Color(0xFF...)`) en pantallas migradas en Sprint 1 deben reemplazarse gradualmente por tokens de `MaterialTheme.colorScheme` para que el modo oscuro sea uniforme en toda la app.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
