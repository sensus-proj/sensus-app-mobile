# PLAN — FT-31 Actividad de amigos en Comunidad (`FriendsActivity`)

> Derivado de `spec.md` y alineado a `sensus-back/specs/056-actividad-amigos-comunidad`.
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §9.2 y §10.

## 1. Diseño

1. Interfaz `FriendsActivityRepository` en `domain/repository/` y modelos de actividad (`CommunityActivityItem`, `CommunityActivityType`).
2. `FriendsApiService.getFriendsActivity` + `FriendsActivityRepositoryImpl` en `data/repository/`.
3. `FriendsActivityContract`, `FriendsActivityViewModel` (MVI) y `FriendsActivityContent` integrado en `CommunityScreen` manteniendo la pestaña Amigos/QR (`FT-25`). Sustituir `SensusRating` por `SensusPurple` para íconos de estrella (§9.2).

## 2. UI y navegación

- Pestaña principal en `CommunityScreen` alternando entre «Actividad» y «Amigos / QR».

## 3. DI

| Binding | Implementación |
|---|---|
| `FriendsActivityRepository` | `FriendsActivityRepositoryImpl` en `RepositoryModule` |

## 4. Gates

- `compileDebugKotlin`, `detekt`, `ktlintCheck`, `lint`, `testDebugUnitTest koverVerifyDebug`
- `check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 80`

## 5. Orden de implementación

1. Implementar contrato y repositorio de actividad de amigos.
2. Conectar `FriendsActivityViewModel` y `FriendsActivityContent`.
3. Corregir color de estrella a `SensusPurple` (§9.2) y completar `FriendsActivityViewModelTest`.

## 6. Prohibiciones

- No usar tonos amarillos o ámbar en íconos de calificación (`SensusRating` prohibido en UI interna).
