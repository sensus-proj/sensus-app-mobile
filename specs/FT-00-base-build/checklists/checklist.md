# CHECKLIST — FT-00 Base Build y Reestructuracion de Paquetes

> Gate de cierre de la feature. Copia de `.specify/templates/checklist-template.md` especializada.
> Sin todos los items en `[x]` no hay merge.

## Spec y alcance

- [x] `spec.md` con 7 secciones + Aprobacion resuelta
- [x] Alcance respetado: sin logica de negocio ni endpoints reales
- [x] `plan.md` §5 (migracion de `app/`) ejecutado sin desviaciones
- [x] `tasks.md` completo; ninguna tarea `[x]` sin evidencia

## Build y estructura

- [x] `./gradlew.bat compileDebugKotlin --no-daemon` verde
- [x] `./gradlew.bat compileDebugAndroidTestSources --no-daemon` verde
- [x] Existen `domain/`, `data/`, `presentation/`, `di/`, `navigation/`, `util/`
- [x] `theme/` vive en `presentation/theme/` y no quedan imports rotos
- [x] `libs.versions.toml` es la unica fuente de versiones
- [x] `SensusApp` registrado en `AndroidManifest.xml`

## Arquitectura

- [x] `presentation -> domain <- data` respetado
- [x] `domain/` sin imports de `android.*`, HTTP ni JSON
- [x] `MockDataProvider` solo en previews/fakes, nunca en produccion
- [x] Sin MVVM, `UiState`, `UiEvent` ni Koin

## Calidad y seguridad

- [x] `lint`, `detekt`, `ktlintCheck` verdes
- [x] `koverVerify` ≥80% obligatorio en pruebas unitarias, de integración y E2E (constitución §7.4)
- [x] R8/ProGuard con keep rules de Retrofit, Serialization y SQLCipher
- [x] Sin secretos, tokens ni PII en codigo o logs
- [x] `python scripts/check_sdd.py` PASS
- [x] `python scripts/check_layers.py` PASS

## Cierre SDD

- [x] `Memoria.md` §6 actualizado con decisiones y evidencia
- [x] `CHECKLIST_PR.md` verde
- [x] Tag `v1.3.0-ft00`
- [x] 1 revisor humano aprobo el PR
