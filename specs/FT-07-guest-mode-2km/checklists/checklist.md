# CHECKLIST — FT-07 Modo Invitado y Filtro de 2 km

> Gate de cierre. Historia SE-008. Sin todos los items en `[x]` no hay merge.

## Spec y alcance

- [x] `spec.md` con 7 secciones + Aprobacion resuelta
- [x] SE-008 cubierta por tests de filtro y bloqueo
- [x] Sin login, perfiles ni borrado de cuenta
- [x] `tasks.md` completo con evidencia

## Filtro de 2000 m

- [x] `radiusMeters` es exactamente 2000 en invitado
- [x] El repositorio aplica el filtro, no solo la UI
- [x] `RadiusSelectorModal` bloqueado con texto explicito
- [x] Ofertas fuera de 2000 m no aparecen
- [x] El backend recibe el radio y lo valida

## Bloqueos

- [x] Favoritos bloqueado
- [x] Comunidad bloqueado
- [x] Publicar oferta bloqueado
- [x] Rutas de comercio redirigen a login
- [x] Dialogo unico `LoginRequiredDialog`

## Privacidad

- [x] Sin token ni `userId` en modo invitado
- [x] Solo coordenadas aproximadas
- [x] Sin persistencia de datos de cuenta

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
- [x] `python scripts/check_layers.py` PASS
- [x] `python scripts/check_endpoints.py` PASS

## Cierre SDD

- [x] `Memoria.md` §6 actualizado
- [x] `CHECKLIST_PR.md` verde
- [x] Tag `v1.10.0-ft07`
- [x] 1 revisor humano aprobo el PR
