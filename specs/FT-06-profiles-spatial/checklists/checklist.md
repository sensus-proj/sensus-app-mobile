# CHECKLIST — FT-06 Perfiles y Recálculo Espacial

> Gate de cierre. Historias SE-006 y SE-007. Sin todos los items en `[x]` no hay merge.

## Spec y alcance

- [x] `spec.md` con 7 secciones + Aprobacion resuelta
- [x] SE-006 y SE-007 cubiertas por tests
- [x] Sin invitado, borrado de cuenta ni carga de imagenes
- [x] `tasks.md` completo con evidencia

## Espacial

- [x] El cliente envia coordenadas como sugerencia
- [x] `GEOGRAPHY(POINT, 4326)` se calcula en backend
- [x] La respuesta del backend sobrescribe el punto
- [x] Coordenadas sin truncar
- [x] El radio de FT-07 usa la ubicacion actualizada

## Datos y seguridad

- [x] Solo el dueno edita su perfil
- [x] Perfil cacheado en DB cifrada
- [x] Escritura optimista con rollback
- [x] Sin PII en logs

## Arquitectura

- [x] `domain/` sin `android.*`, HTTP ni JSON
- [x] `presentation/` no importa `data/`
- [x] ViewModel por pantalla con `ViewState`/`ViewIntent`/`ViewEffect`
- [x] Sin MVVM, `UiState`, `UiEvent` ni Koin

## UI

- [x] Previews `View`, `Edit`, `Saving`, `Error`, `Offline`
- [x] Errores por campo anunciados
- [x] Morado de marca; sin amarillos ni emojis

## Gates

- [x] `compileDebugKotlin` verde
- [x] `testDebugUnitTest` verde
- [x] `compileDebugAndroidTestSources` verde
- [x] `lint`, `detekt`, `ktlintCheck` verdes
- [x] `koverVerify` ≥45% (propuesto)
- [x] `python scripts/check_layers.py` PASS
- [x] `python scripts/check_endpoints.py` PASS
- [x] `docs/endpoints-internos.md` con ambos `PUT`

## Cierre SDD

- [x] `Memoria.md` §6 actualizado
- [x] `CHECKLIST_PR.md` verde
- [x] Tag `v1.9.0-ft06`
- [x] 1 revisor humano aprobo el PR
