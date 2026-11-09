# TASKS — FT-07 Modo Invitado y Filtro de 2 km

> 1 PR = 1 slice. Historia: SE-008. **Pendientes, sin ejecutar.**

## Fase A — Dominio

- [x] T07.1 Crear `domain/guest/model/GuestSession.kt` con `radiusMeters = 2000` — verif: `compileDebugKotlin`
- [x] T07.2 Crear `EnterGuestModeUseCase` — verif: `testDebugUnitTest`
- [x] T07.3 Crear `GetNearbyOffersUseCase` que normaliza el radio a 2000 en invitado — verif: `testDebugUnitTest`
- [x] T07.4 Crear `GuestOfferRepository` (interfaz) — verif: `compileDebugKotlin`

## Fase B — Datos

- [x] T07.5 Crear `GuestOfferApi` con consulta por centro y radio — verif: `compileDebugKotlin`
- [x] T07.6 Crear `GuestOfferRepositoryImpl` que envia `radiusMeters` al backend — verif: `testDebugUnitTest`
- [x] T07.7 Probar ofertas dentro y fuera de 2000 m con `MockWebServer` — verif: `testDebugUnitTest`
- [x] T07.8 Verificar que en invitado no se envia token ni `userId` — verif: `testDebugUnitTest`

## Fase C — Presentacion

- [x] T07.9 Crear `GuestModeViewModel` — verif: `testDebugUnitTest`
- [x] T07.10 Crear `GuestMapScreen` reutilizando componentes de `buyer/` — verif: `compileDebugKotlin`
- [x] T07.11 Crear `GuestHomeScreen` con feed filtrado — verif: `compileDebugKotlin`
- [x] T07.12 Bloquear `RadiusSelectorModal` con `isLocked` y texto `Radio fijo en modo invitado` — verif: `compileDebugKotlin`
- [x] T07.13 Crear `LoginRequiredDialog` en `presentation/common/` — verif: `compileDebugKotlin`
- [x] T07.14 Bloquear Favoritos, Comunidad y Publicar oferta — verif: `testDebugUnitTest`
- [x] T07.15 Agregar previews `GuestMap`, `GuestHome`, `LoginRequired` — verif: `compileDebugKotlin`

## Fase D — Navegacion y DI

- [x] T07.16 Agregar `Continuar como invitado` en `OnboardingLocationScreen` — verif: `compileDebugKotlin`
- [x] T07.17 Agregar guardia `GuestRoute` (rutas de comercio -> login) — verif: `testDebugUnitTest`
- [x] T07.18 Restablecer radio al autenticarse — verif: `testDebugUnitTest`
- [x] T07.19 Binding `GuestOfferRepository -> GuestOfferRepositoryImpl` en DI — verif: `compileDebugKotlin`

## Fase E — Pruebas y cierre

- [x] T07.20 Tests Compose de selector fijo, dialogo y redireccion — verif: `compileDebugAndroidTestSources`
- [x] T07.21 `compileDebugKotlin`, `testDebugUnitTest`, `lint` verdes — verif: Gradle
- [x] T07.22 `python scripts/check_layers.py` y `check_endpoints.py` PASS — verif: scripts
- [x] T07.23 Actualizar `docs/endpoints-internos.md` — verif: revision humana
- [x] T07.24 Actualizar `Memoria.md` §6 y `CHECKLIST_PR.md` — verif: revision humana
- [x] T07.25 Tag `v1.10.0-ft07` — verif: revision humana
