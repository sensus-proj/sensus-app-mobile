# PLAN — FT-27 Cierre de QA Integral, Hardening ASVS L2 y Evidencia de Defensa

> Derivado de `spec.md`. Historias/Alcance: Sprint 5 (`Sensus_Sprint_Planning_Original.csv` y `FORMATO DE ACEPTACION DEL PROYECTO.docx`).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Cobertura y pruebas E2E.** Ejecutar la suite completa unitaria (JUnit5 + MockK + Turbine), integración SQLCipher y flujos E2E Compose (`Auth`, `Discovery`, `Reserva QR`, `Canje Comercio`, `Comunidad`, `Baja de cuenta`) con Kover `>= 80%`.
2. **Auditoría OWASP MASVS / ASVS L2.** Verificar todos los controles V1..V14 en `docs/asvs-l2-checklist.md` con evidencia trazable.
3. **Acta de aceptación.** Conciliar `specs/README.md`, `docs/endpoints-internos.md`, `docs/rate-matrix.md` y `Memoria.md` §6 para la defensa del proyecto (20/11–04/12/2026).

## 2. UI y navegación

- Verificación visual en modo claro y oscuro, accesibilidad TalkBack y ausencia total de pantallas huérfanas o datos mock.

## 3. DI

| Binding | Implementación |
|---|---|
| — | Sin nuevos bindings requeridos |

## 4. Gates

- `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes.
- `./gradlew.bat detekt ktlintCheck lint` verdes (0 violaciones).
- `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (cobertura `>= 80%`).
- `python scripts/check_sdd.py` PASS.
- `python scripts/check_layers.py` PASS.
- `python scripts/check_endpoints.py` PASS.
- `python scripts/check_secrets.py` PASS.
- `python scripts/check_coverage.py --min 80` PASS.

## 5. Orden de implementación

1. Verificar que `FT-10..FT-23` estén en estado `Cerrada` con todos sus checklists en `[x]`.
2. Ejecutar `./gradlew.bat compileDebugKotlin compileDebugAndroidTestSources detekt ktlintCheck lint testDebugUnitTest koverVerifyDebug`.
3. Ejecutar los 5 scripts Python (`check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 80`).
4. Cerrar `docs/asvs-l2-checklist.md`, `CHECKLIST_PR.md` y `Memoria.md` §6.

## 6. Prohibiciones

- Prohibido ejecutar `assembleDebug`, `assembleRelease` o `bundleRelease` sin autorización explícita del usuario.
- Prohibido bajar el umbral de cobertura del `80%` en Kover o desactivar reglas de Detekt/Ktlint.

## Tareas

Ver `tasks.md`.
