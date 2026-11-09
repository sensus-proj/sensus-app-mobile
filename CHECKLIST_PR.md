# CHECKLIST_PR.md — Gate obligatorio (1 revisor, `main` protegida)

> Constitucion §8 y §10. Un PR = una slice = una feature. Sin verde no hay merge.

## Pre-vuelo

- [x] `Memoria.md` actualizado (estado, decisiones con porque, errores a evitar)
- [x] Spec en `specs/FT-XX/` conciliada con lo implementado (Aprobacion resuelta)
- [x] `plan.md` y `tasks.md` de la feature al dia
- [x] Sin secretos en codigo, logs ni mensajes de error
- [x] `keys.properties` gitignored; solo `keys.properties.example` versionado

## Build y analisis

- [x] `./gradlew.bat compileDebugKotlin --no-daemon` verde
- [x] `./gradlew.bat lint --no-daemon` verde
- [x] `./gradlew.bat testDebugUnitTest --no-daemon` verde
- [x] `./gradlew.bat compileDebugAndroidTestSources --no-daemon` verde
- [x] `detekt` + `ktlintCheck` verdes (cuando el plugin este cableado; deuda de FT-00)
- [x] `koverVerify` >= 80% (umbral obligatorio para todo código nuevo en unitarias, integración y E2E; constitución §7.4. Deuda de FT-00)

## Gates SDD

- [x] `python scripts/check_sdd.py` PASS
- [x] `python scripts/check_layers.py` PASS
- [x] `python scripts/check_endpoints.py` PASS
- [x] `python scripts/check_secrets.py` PASS
- [x] `python scripts/check_coverage.py --min 80` PASS o SKIP documentado

## Arquitectura

- [x] `presentation -> domain <- data` respetado; sin `android.*` en `domain/`
- [x] ViewModel por pantalla con `ViewState`/`ViewIntent`/`ViewEffect`
- [x] Dependencias solo por Hilt; sin singletons estaticos
- [x] Rutas `@Serializable` y `NavHost` centralizado
- [x] Sin MVVM legacy, `UiState`/`UiEvent` ni Koin

## Seguridad y SDD

- [x] `security-reviewer` PASS: store cifrado, sin tokens en logs, `allowBackup=false`
- [x] `layer-guardian` PASS: complejidad y estilo
- [x] CodeQL 0 hallazgos nuevos en CI
- [x] TOML y YAML validados
- [x] `docs/` vivos actualizados si cambio un contrato interno
- [x] `constitution.md` y `.specify/memory/constitution.md` siguen siendo el mismo inodo

## Cierre

- [x] Version SemVer y `versionCode` actualizados si hay release
- [x] Tag `vX.Y.Z-ftN` sobre `main`
- [x] 1 revisor humano aprobo el PR
