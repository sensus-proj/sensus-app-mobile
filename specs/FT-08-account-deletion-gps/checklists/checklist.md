# CHECKLIST — FT-08 Baja de Cuenta y GPS Nativo

> Gate de cierre. Historia SE-009. Sin todos los items en `[x]` no hay merge.

## Spec y alcance

- [x] `spec.md` con 7 secciones + Aprobacion resuelta
- [x] SE-009 cubierta por tests de exito, fallo e idempotencia
- [x] Sin periodo de gracia ni exportacion de datos
- [x] `tasks.md` completo con evidencia

## Baja de cuenta

- [x] `DELETE /api/users/account` con token valido
- [x] 204 y 404 tratados como exito
- [x] 401 limpia sesion y redirige a login
- [x] Fallo de red no borra nada local
- [x] Doble confirmacion antes de borrar
- [x] Back stack limpiado tras la baja

## Borrado local

- [x] `clearSession()` purga tokens
- [x] DB SQLCipher cerrada y eliminada
- [x] Cache y estado en memoria limpios
- [x] Sin PII en logs ni archivos tras la baja
- [x] Reintento si el archivo esta bloqueado

## GPS y permisos

- [x] `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` en runtime
- [x] Rationale antes de solicitar
- [x] Alternativa manual si se niega
- [x] Sin ubicacion en segundo plano

## Arquitectura

- [x] `domain/` sin `android.*`, HTTP ni JSON
- [x] `presentation/` no importa `data/`
- [x] ViewModel con `ViewState`/`ViewIntent`/`ViewEffect`
- [x] Sin MVVM, `UiState`, `UiEvent` ni Koin

## Gates

- [x] `compileDebugKotlin` verde
- [x] `testDebugUnitTest` verde
- [x] `compileDebugAndroidTestSources` verde
- [x] `lint`, `detekt`, `ktlintCheck` verdes
- [x] `koverVerify` ≥45% (propuesto)
- [x] `python scripts/check_secrets.py` PASS
- [x] `python scripts/check_endpoints.py` PASS
- [x] `docs/asvs-l2-checklist.md` V8 actualizado

## Cierre SDD

- [x] `Memoria.md` §6 actualizado
- [x] `CHECKLIST_PR.md` verde
- [x] Tag `v1.11.0-ft08`
- [x] 1 revisor humano aprobo el PR
