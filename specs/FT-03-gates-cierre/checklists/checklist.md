# CHECKLIST — FT-03 Gates de Cierre: Scripts Ejecutables y CI

> Gate de cierre. Sin todos los items en `[x]` no hay merge.

## Spec y alcance

- [x] `spec.md` con 7 secciones + Aprobacion resuelta
- [x] `scripts/README.md` ya no es stub
- [x] `tasks.md` completo con evidencia

## Scripts

- [x] `check_sdd.py` valida estructura, secciones y raiz limpia
- [x] `check_layers.py` valida capas y prohibe MVVM/UiState/UiEvent/Koin
- [x] `check_endpoints.py` valida columnas y endpoints de specs
- [x] `check_secrets.py` detecta patrones sin imprimir valores
- [x] `check_coverage.py` lee Kover XML y respeta `--min`
- [x] Los cinco scripts salen 0/1 y imprimen `PASS`/`FAIL`
- [x] Sin dependencias Python externas
- [x] Sin rutas absolutas de Windows

## CI / CD

- [x] Job `sdd-gates` existe y es bloqueante en `ci.yml`
- [x] Orden: `validate-wrapper -> sdd-gates -> security-scan -> static-analysis -> unit-test -> instrumentation-compile -> build(main)`
- [x] Evidencia subida como artefacto
- [x] `ci.yml` no emite APK ni AAB en PRs rutinarios
- [x] Pipeline CD `.github/workflows/release-playstore.yml` configurado con firma efímera, subida de AAB y mapping ProGuard
- [x] `distribution/whatsnew/` presente con notas de versión y credenciales documentadas en `keys.properties.example`

## Documentacion

- [x] `AGENTS.md` §8 actualizado
- [x] `CHECKLIST_PR.md` actualizado
- [x] `docs/asvs-l2-checklist.md` alineado

## Gates

- [x] `python scripts/check_sdd.py` PASS
- [x] `python scripts/check_layers.py` PASS
- [x] `python scripts/check_endpoints.py` PASS
- [x] `python scripts/check_secrets.py` PASS
- [x] `python scripts/check_coverage.py --min 80` PASS o SKIP documentado

## Cierre SDD

- [x] `Memoria.md` §6 con la salida de los gates
- [x] Tag `v1.6.0-ft03`
- [x] 1 revisor humano aprobo el PR
