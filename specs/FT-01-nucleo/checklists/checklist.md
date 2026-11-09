# CHECKLIST — FT-01 Nucleo: Contratos Base, Red y DI

> Gate de cierre. Sin todos los items en `[x]` no hay merge.

## Spec y alcance

- [x] `spec.md` con 7 secciones + Aprobacion resuelta
- [x] Sin logica de negocio ni endpoints reales de FT-04..FT-08
- [x] `plan.md` §5 (migracion de `app/`) ejecutado
- [x] `tasks.md` completo con evidencia por tarea

## Contratos

- [x] `Result`/`AppError` cubren red, HTTP, serializacion y desconocido
- [x] `UseCase` y `NoInputUseCase` son `suspend` y no lanzan excepciones
- [x] `MviViewModel` expone `StateFlow` y `SharedFlow` sin fugas
- [x] `Routes.kt` usa `@Serializable`; sin strings `{id}` manuales

## Red y seguridad

- [x] Timeouts solo en `NetworkModule`, nunca literales en `*.kt`
- [x] `AuthInterceptor` lee token cifrado; nunca lo loguea
- [x] `SanitizedLogger` activo solo en `debug`
- [x] `RateLimitInterceptor` respeta `Retry-After` y solo reintenta idempotentes
- [x] `usesCleartextTraffic=false` y TLS 1.3

## Arquitectura y DI

- [x] `presentation/` no importa `data/`
- [x] `domain/` sin `android.*`, HTTP ni JSON
- [x] Todo provider vive en un modulo Hilt; sin singletons estaticos
- [x] `NetworkMonitor` registrado y probado

## Gates

- [x] `compileDebugKotlin` verde
- [x] `testDebugUnitTest` verde (`MockWebServer`, Turbine)
- [x] `lint`, `detekt`, `ktlintCheck` verdes
- [x] `koverVerify` ≥80% obligatorio en unitarias, integración y E2E (constitución §7.4)
- [x] `python scripts/check_layers.py` PASS
- [x] `python scripts/check_endpoints.py` PASS
- [x] `docs/endpoints-internos.md` y `docs/rate-matrix.md` al dia

## Cierre SDD

- [x] `Memoria.md` §6 actualizado
- [x] `CHECKLIST_PR.md` verde
- [x] Tag `v1.4.0-ft01`
- [x] 1 revisor humano aprobo el PR
