# specs/ — Indice maestro de features SDD

> Cada feature vive en `specs/FT-XX-<slug>/` con exactamente `spec.md`, `plan.md`, `tasks.md` y `checklists/checklist.md`.
> Rector: `.specify/memory/constitution.md` §10. Plantilla: `.specify/templates/spec-template.md` (copia en `_template.md`).
> Los artefactos raiz `spec.md`, `plan.md` y `task.md` fueron retirados: su contenido vive en FT-00..FT-03.

## Estructura canonica

```text
specs/FT-XX-<slug>/
├── spec.md              # que y por que (7 secciones + Aprobacion)
├── plan.md              # como (diseno, UI, DI, gates)
├── tasks.md             # tareas atomicas con comando de verificacion
└── checklists/
    └── checklist.md     # gate de cierre
```

## Estado de las features

| Feature | Tipo | Historias | Depende de | Estado | Evidencia |
|---|---|---|---|---|---|
| `FT-00-base-build` | Cimentacion | — | — | Cerrada | `spec.md`, `app/build.gradle.kts`, `SensusApp.kt` |
| `FT-01-nucleo` | Cimentacion | — | FT-00 | Cerrada | `spec.md`, `MviViewModel.kt`, `AppModule.kt`, `NetworkClient.kt` |
| `FT-02-store-skeleton` | Cimentacion | — | FT-00, FT-01 | Cerrada | `spec.md`, `SensusDatabase.java`, `DatabaseKeyManager.kt`, `DefaultSyncManager.kt` |
| `FT-03-gates-cierre` | Cimentacion | — | FT-00..FT-02 | Cerrada | `spec.md`, `scripts/check_*.py` (5 gates en PASS) |
| `FT-04-auth-roles` | Negocio | SE-001..SE-004 | FT-01, FT-02 | Cerrada | `spec.md`, `LoginUseCase.kt`, `RegisterUseCase.kt`, `LoginViewModel.kt` |
| `FT-05-password-recovery` | Negocio | SE-005 | FT-04 | Cerrada | `spec.md`, `RequestPasswordResetUseCase.kt`, `ResetPasswordUseCase.kt`, `ForgotPasswordViewModel.kt` |
| `FT-06-profiles-spatial` | Negocio | SE-006, SE-007 | FT-02, FT-04 | Cerrada | `spec.md`, `UpdateProfileUseCase.kt`, `UpdateMerchantProfileUseCase.kt`, `MerchantMapper.kt` |
| `FT-07-guest-mode-2km` | Negocio | SE-008 | FT-01, FT-06 | Cerrada | `spec.md`, `StartGuestSessionUseCase.kt`, `GuestRadiusPolicy`, `RadiusSelectorModal.kt` |
| `FT-08-account-deletion-gps` | Negocio | SE-009 | FT-02, FT-04 | Cerrada | `spec.md`, `DeleteAccountUseCase.kt`, `GetUserLocationUseCase.kt`, `SessionRepositoryImpl.kt` |
| `FT-09-cicd-devsecops` | Infraestructura / CD | — | FT-03 | Cerrada | `spec.md`, `ci.yml`, `security-scanners.yml`, `release-playstore.yml` |
| `FT-10-deuda-y-contratos` | Saneamiento / Contratos | Constitución §4, §9, §11 | FT-09 | En revision | `spec.md`, `LogoutUseCase.kt`, `NavigationKeys.kt`, `detekt`/`ktlintCheck`/`lint` PASS, `100` unit tests / `Kover 93.23%`, 5 `check_*.py` PASS (pendiente PR a `main` y tag `v1.13.0-ft10`) |
| `FT-11-ofertas-cercanas-mapa` | Negocio (Ola 1) | SE-010..SE-014; P-07..P-11 | FT-10 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-12-detalle-filtros-ruteo` | Negocio (Ola 1) | SE-015..SE-018; P-14, P-17, P-19 | FT-11 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-13-reserva-qr-cliente` | Negocio (Ola 1) | P-16, P-18, P-20, P-21 | FT-12 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-14-canje-qr-comercio` | Negocio (Ola 1) | P-33, C-48 | FT-13 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-15-oferta-creacion-comercio` | Negocio (Ola 2) | SE-019..SE-022; P-32 | FT-10 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-16-inventario-ciclo-vida` | Negocio (Ola 2) | SE-023..SE-026, SE-045; P-30, P-31 | FT-15 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-17-favoritos` | Negocio (Ola 3) | SE-036; P-23 | FT-12 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-18-feed-comunidad` | Negocio (Ola 3) | SE-027..SE-029; P-24 | FT-11, FT-15 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-19-interacciones-reportes` | Negocio (Ola 3) | SE-030..SE-033; P-22 | FT-18 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-20-busqueda-recomendaciones-ml` | Negocio (Ola 3) | SE-034, SE-035, SE-037, SE-038; P-12, P-13 | FT-12, FT-17 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-21-push-fcm-ajustes` | Negocio / Push (Ola 3) | SE-039..SE-041; P-04, P-26 | FT-10 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-22-ux-transversal-offline` | UX / Offline (Ola 4) | SE-042..SE-044; P-05, P-06 | FT-11 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-23-soporte-legal-faq` | Soporte / Legal (Ola 4) | SE-042..SE-044; P-35 | FT-10 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-24-sensi-ia` | Negocio (Ola 5) | P-25, C-39, Tesina | FT-12 | Borrador | `spec.md`, `plan.md`, `tasks.md` (bloqueada por backend) |
| `FT-25-amigos-qr` | Negocio (Ola 5) | P-24, C-38, Tesina | FT-13 | Borrador | `spec.md`, `plan.md`, `tasks.md` (bloqueada por backend) |
| `FT-26-resenas-comercio` | Negocio (Ola 5) | P-15, C-28..C-31 | FT-14 | Borrador | `spec.md`, `plan.md`, `tasks.md` (bloqueada por backend) |
| `FT-27-qa-release-defensa` | QA / Release (Ola 6) | Sprint 5 (Drive) | FT-10..FT-26 | Especificada, sin ejecutar | `spec.md`, `plan.md`, `tasks.md` |
| `FT-28-map-layer-preference` | UX / Offline SQLCipher | Personalización Mapa | FT-02, FT-07 | En revision | `SensusDatabase` v2, `MapPreferencesRepositoryImpl.kt`, `108` unit tests / `Kover 93.45%`, 5 `check_*.py` PASS (pendiente commit/PR) |

Estados permitidos: `Borrador`, `Especificada, sin ejecutar`, `En implementacion`, `En revision`, `Cerrada`.
Ninguna feature pasa a `Cerrada` sin `checklists/checklist.md` completo y evidencia en `Memoria.md` §6.

## Orden de ejecucion

```
FT-00 -> FT-01 -> FT-02 -> FT-03 -> FT-04 -> FT-05 -> FT-06 -> FT-07 -> FT-08 -> FT-09
  -> FT-10 (Saneamiento)
  -> Ola 1 (FT-11 -> FT-12 -> FT-13 -> FT-14)
  -> Ola 2 (FT-15 -> FT-16)
  -> Ola 3 (FT-17, FT-18 -> FT-19, FT-20, FT-21)
  -> Ola 4 (FT-22, FT-23)
  -> Ola 5 (FT-24, FT-25, FT-26, sujetas a backend)
  -> Ola 6 (FT-27 QA y Release)
```

- **FT-00..FT-03** son cimentacion: build, contratos, persistencia y gates.
- **FT-04..FT-08** son los vertical slices del Sprint 1.
- **FT-10..FT-27** completan Sprints 2–5, deuda técnica y cierre del Drive.

## Convenciones

- `FT-00` = linea base de build y estructura de paquetes.
- `FT-01` = nucleo: `Result`/`AppError`, MVI base, red y Hilt.
- `FT-02` = store local cifrado (Room + SQLCipher) y offline base.
- `FT-03` = gates automatizados y CI.
- `FT-04` en adelante = funcionalidad de negocio, una historia o grupo cerrado por feature.
- Nombres en minusculas con guiones: `FT-04-auth-roles`.
- Un PR = una slice = una feature.
- Toda feature declara sus endpoints y los registra en `docs/endpoints-internos.md`.

## Trazabilidad

```
constitution.md
  -> specs/FT-XX/spec.md
  -> specs/FT-XX/plan.md
  -> specs/FT-XX/tasks.md
  -> specs/FT-XX/checklists/checklist.md
  -> app/src/main/java/com/example/sensus/
  -> scripts/check_*.py
  -> Memoria.md §6
```

El gate `python scripts/check_sdd.py` verifica esta estructura; `check_endpoints.py` verifica los contratos vivos.
