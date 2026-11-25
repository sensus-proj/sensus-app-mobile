# CHECKLIST — FT-27 Cierre de QA Integral, Hardening ASVS L2 y Evidencia de Defensa

> Gate de cierre. Alcance: Sprint 5 (`Sensus_Sprint_Planning_Original.csv` y `FORMATO DE ACEPTACION DEL PROYECTO.docx`). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [ ] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] 100% de las historias `SE-001..SE-045` trazadas y verificadas con pruebas automatizadas
- [x] Kover `>= 80.00%` en `domain/` y `presentation/` verificado por `check_coverage.py --min 80`
- [x] Cero referencias a `MockDataProvider` en `app/src/main/**`
- [ ] Controles OWASP ASVS L2 (V1..V14) cerrados con evidencia en `docs/asvs-l2-checklist.md`
- [ ] `Memoria.md` §6 y `CHECKLIST_PR.md` actualizados para el comité de defensa

## Arquitectura y seguridad

- [ ] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [ ] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [x] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [x] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [ ] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty` y `Error`
- [ ] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar (`SensusAmber`) ni emojis
- [ ] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos

## Gates automatizados

- [x] `./gradlew.bat compileDebugKotlin` verde
- [x] `./gradlew.bat compileDebugAndroidTestSources` verde
- [x] `./gradlew.bat detekt ktlintCheck lint` verdes
- [x] `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (`>= 80%`)
- [x] `python scripts/check_sdd.py` PASS
- [x] `python scripts/check_layers.py` PASS
- [x] `python scripts/check_endpoints.py` PASS
- [x] `python scripts/check_secrets.py` PASS
- [x] `python scripts/check_coverage.py --min 80` PASS

- [x] Compilación Kotlin Release de la candidata.
- [ ] Revisar y resolver el hallazgo Semgrep de MainActivity exportada.
- [ ] Ejecutar los escáneres remotos con resultado bloqueante sobre el commit de la candidata.
- [ ] Ejecutar la suite instrumentada en dispositivo sobre la candidata.

## Cierre SDD

- [ ] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [ ] `Memoria.md` §6 actualizado con decisiones y evidencia
- [ ] `CHECKLIST_PR.md` completo
- [ ] Tag oficial `v1.4.0` tras el cierre de los controles y la autorización de publicación.

- [ ] Verificar el empaquetado de 16 KB de la candidata y la apertura de una base anterior. El AAR de SQLCipher 4.20.0 ya presenta segmentos ELF `PT_LOAD` de 16 KB en ARM64 y x86_64.
- [x] Verificar la APK Debug generada con `zipalign -c -P 16 4` y la alineación de las bibliotecas de 64 bits incluidas.
