# TASKS — FT-08 Baja de Cuenta y GPS Nativo

> 1 PR = 1 slice. Historia: SE-009. **Pendientes, sin ejecutar.**

## Fase A — Dominio y datos

- [x] T08.1 Crear `domain/account/repository/AccountRepository.kt` — verif: `compileDebugKotlin`
- [x] T08.2 Crear `DeleteAccountUseCase` (exito, fallo, idempotencia) — verif: `testDebugUnitTest`
- [x] T08.3 Crear `data/account/remote/AccountApi.kt` con `DELETE /api/users/account` — verif: `compileDebugKotlin`
- [x] T08.4 Crear `AccountRepositoryImpl` (204/404 -> exito, 401 -> limpiar sesion) — verif: `testDebugUnitTest`
- [x] T08.5 Probar 204, 401, 404 y timeout con `MockWebServer` — verif: `testDebugUnitTest`

## Fase B — Borrado local

- [x] T08.6 Implementar secuencia `clearSession()` -> cerrar DB -> borrar archivo — verif: `testDebugUnitTest`
- [x] T08.7 Reintentar una vez si el archivo esta bloqueado — verif: `testDebugUnitTest`
- [x] T08.8 Verificar que no quedan tokens, DB ni cache — verif: test instrumentado
- [x] T08.9 Verificar que un fallo de red no borra nada local — verif: `testDebugUnitTest`

## Fase C — GPS nativo

- [x] T08.10 Migrar `OnboardingLocationScreen` a `presentation/auth/location/` — verif: `compileDebugKotlin`
- [x] T08.11 Solicitar `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` con rationale — verif: `compileDebugKotlin`
- [x] T08.12 Implementar alternativa manual si se niega el permiso — verif: `testDebugUnitTest`
- [x] T08.13 Confirmar que no se pide ubicacion en segundo plano — verif: revision humana

## Fase D — UI y navegacion

- [x] T08.14 Crear `DeleteAccountDialog` con doble confirmacion — verif: `compileDebugKotlin`
- [x] T08.15 Conectar el dialogo desde `BuyerProfileScreen` — verif: `compileDebugKotlin`
- [x] T08.16 Navegar a `Welcome` limpiando el back stack — verif: `testDebugUnitTest`
- [x] T08.17 Enlazar el aviso de privacidad — verif: `compileDebugKotlin`
- [x] T08.18 Agregar previews `PermissionRationale`, `PermissionDenied`, `ConfirmDeletion`, `Deleting`, `Deleted` — verif: `compileDebugKotlin`

## Fase E — Pruebas y cierre

- [x] T08.19 Tests Compose de permisos y doble confirmacion — verif: `compileDebugAndroidTestSources`
- [x] T08.20 Test instrumentado de borrado total — verif: `compileDebugAndroidTestSources`
- [x] T08.21 `compileDebugKotlin`, `testDebugUnitTest`, `lint` verdes — verif: Gradle
- [x] T08.22 `python scripts/check_secrets.py` y `check_endpoints.py` PASS — verif: scripts
- [x] T08.23 Actualizar `docs/endpoints-internos.md` y `docs/asvs-l2-checklist.md` (V8) — verif: revision humana
- [x] T08.24 Actualizar `Memoria.md` §6 y `CHECKLIST_PR.md` — verif: revision humana
- [x] T08.25 Tag `v1.11.0-ft08` — verif: revision humana
