# CHECKLIST — FT-04 Auth y Roles

> Gate de cierre. Historias SE-001..SE-004. Sin todos los items en `[x]` no hay merge.

## Spec y alcance

- [x] `spec.md` con 7 secciones + Aprobacion resuelta
- [x] SE-001..SE-004 cubiertas por al menos un test cada una
- [x] Sin recuperacion de contrasena, perfiles, invitado ni borrado de cuenta
- [x] `tasks.md` completo con evidencia

## Seguridad

- [x] Contrasena nunca se persiste ni se loguea
- [x] Mensaje generico `Credenciales invalidas` (sin enumeracion)
- [x] Tokens solo en `TokenLocalDataSource` cifrado
- [x] `expiresAt` viene del backend, no hardcodeado
- [x] Registro sin documentos oficiales (INE, RFC, acta)

## Arquitectura

- [x] `domain/auth/` sin `android.*`, HTTP ni JSON
- [x] `presentation/` no importa `data/`
- [x] `ValidatePasswordUseCase` es puro y testeable
- [x] Un ViewModel por pantalla con `ViewState`/`ViewIntent`/`ViewEffect`
- [x] Sin MVVM, `UiState`, `UiEvent` ni Koin

## UI

- [x] Login, registro y switch de rol con preview por estado
- [x] `contentDescription` en iconos interactivos
- [x] Morado de marca; sin amarillos ni emojis
- [x] `TermsScreen` enlaza el aviso de privacidad

## Gates

- [x] `compileDebugKotlin` verde
- [x] `testDebugUnitTest` verde
- [x] `compileDebugAndroidTestSources` verde
- [x] `lint`, `detekt`, `ktlintCheck` verdes
- [x] `koverVerify` ≥45% (propuesto)
- [x] `python scripts/check_layers.py` PASS
- [x] `python scripts/check_endpoints.py` PASS
- [x] `docs/endpoints-internos.md` con `POST /api/auth/login` y `POST /api/auth/register`

## Cierre SDD

- [x] `Memoria.md` §6 actualizado
- [x] `CHECKLIST_PR.md` verde
- [x] Tag `v1.7.0-ft04`
- [x] 1 revisor humano aprobo el PR
