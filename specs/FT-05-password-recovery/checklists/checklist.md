# CHECKLIST — FT-05 Recuperacion de Contrasena

> Gate de cierre. Historia SE-005. Sin todos los items en `[x]` no hay merge.

## Spec y alcance

- [x] `spec.md` con 7 secciones + Aprobacion resuelta
- [x] SE-005 cubierta por tests de exito, expirado y reuso
- [x] Sin MFA ni cambio de contrasena autenticado
- [x] `tasks.md` completo con evidencia

## Seguridad

- [x] Solicitud responde igual exista o no el correo
- [x] Token de 15 min validado por backend; cliente no lo genera
- [x] Token no aparece en UI, logs ni `ViewState`
- [x] Token invalidado al primer uso
- [x] Rate-limit de reenvio (60 s cliente, 5/hora backend)
- [x] Nueva contrasena cumple `ValidatePasswordUseCase`

## Arquitectura

- [x] `domain/` sin `android.*`, HTTP ni JSON
- [x] `presentation/` no importa `data/`
- [x] ViewModel por pantalla con `ViewState`/`ViewIntent`/`ViewEffect`
- [x] Sin MVVM, `UiState`, `UiEvent` ni Koin

## UI

- [x] Mensaje neutro de envio
- [x] Mensaje de token expirado/usado
- [x] Previews por estado
- [x] Morado de marca; sin amarillos ni emojis

## Gates

- [x] `compileDebugKotlin` verde
- [x] `testDebugUnitTest` verde
- [x] `compileDebugAndroidTestSources` verde
- [x] `lint`, `detekt`, `ktlintCheck` verdes
- [x] `koverVerify` ≥45% (propuesto)
- [x] `python scripts/check_endpoints.py` PASS
- [x] `python scripts/check_secrets.py` PASS
- [x] `docs/endpoints-internos.md` y `docs/rate-matrix.md` al dia

## Cierre SDD

- [x] `Memoria.md` §6 actualizado
- [x] `CHECKLIST_PR.md` verde
- [x] Tag `v1.8.0-ft05`
- [x] 1 revisor humano aprobo el PR
