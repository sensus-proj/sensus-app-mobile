# PLAN — FT-07 Modo Invitado y Filtro de 2 km

> Derivado de `spec.md`. Rector: `.specify/memory/constitution.md` §2, §4, §5 y §10.
> Conciliado en **FT-10** (2026-11-11) contra la implementación real en `Movil_Rodrigo`.

## 1. Diseño real implementado

1. **Sesión invitado (`StartGuestSessionUseCase`).** Solicita token efímero en `POST /api/v1/auth/guest` y guarda `SessionType.GUEST` en `SecurePreferences` vía `SessionRepository`.
2. **Política de radio en dominio (`GuestRadiusPolicy`).** Define mínimo de `50 m`, máximo `5000 m` para registrados y límite `2000 m` (`GUEST_MAX_RADIUS_KM = 2.0f`) para invitados.
3. **Reutilización de pantallas (`buyer/`).** En lugar de duplicar pantallas `GuestMapScreen`/`GuestHomeScreen`, `BuyerMapScreen`, `BuyerHomeScreen`, `BuyerMapViewModel` y `BuyerHomeViewModel` observan `SessionRepository.sessionState` y aplican el comportamiento invitado.
4. **Selector de radio (`RadiusSelectorBottomSheet`).** Muestra opciones rápidas y control fino de `50 m` a `5 km`, bloqueando radios `> 2 km` cuando `isGuest = true`.
5. **Bloqueo explícito (`GuestPromptDialog`).** Acciones protegidas (favoritos, comunidad, reservas, chat IA, rutas) disparan el diálogo único hacia Login/Registro.

## 2. UI y navegación

- `LoginScreen` expone `Explorar como invitado` invocando `LoginViewIntent.ContinueAsGuest`.
- `RadiusSelectorModal.kt` (`RadiusSelectorBottomSheet`) muestra aviso del límite de `2 km` para invitados.
- `GuestPromptDialog` centraliza el aviso de cuenta requerida en `presentation/modals/`.

## 3. DI

| Binding | Implementación |
|---|---|
| `SessionRepository` | `SessionRepositoryImpl` (instancia única vía `SensusContainer` y `AppModule`) |
| `StartGuestSessionUseCase` | Inyectado vía Hilt (`AppModule.provideStartGuestSessionUseCase`) |
| `GuestRadiusPolicy` | Objeto puro en `domain/model/` |

## 4. Gates

- `compileDebugKotlin`, `testDebugUnitTest`, `compileDebugAndroidTestSources` en verde.
- `lint`, `detekt`, `ktlintCheck` en verde.
- `koverVerifyDebug` >= 80% obligatorio.
- `python scripts/check_layers.py` PASS.
- `python scripts/check_endpoints.py` PASS.

## 5. Prohibiciones

- No permitir radio mayor a `2000 m` en modo invitado.
- No almacenar PII de usuario en sesión invitado.
- No duplicar pantallas `buyer/` para invitado.

## Tareas

Ver `tasks.md` (conciliado; las pantallas finales se implementaron directamente sobre `BuyerMapScreen`, `BuyerHomeScreen`, `StartGuestSessionUseCase` y `GuestRadiusPolicy`).
