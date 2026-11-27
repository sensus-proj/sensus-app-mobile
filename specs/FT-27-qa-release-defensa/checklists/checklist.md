# CHECKLIST — FT-27 Cierre de QA Integral, Hardening ASVS L2 y Evidencia de Defensa

> Gate de cierre. Alcance: Sprint 5 (`Sensus_Sprint_Planning_Original.csv` y `FORMATO DE ACEPTACION DEL PROYECTO.docx`). Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta (`7 secciones` PASS en `check_sdd.py`; falta cierre formal de aprobación: (acción del usuario: aprobar tabla en `spec.md`; ver plan cierre móvil §5.Z-5))
- [x] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde (`549/549` unitarias y `androidTest` compiladas)
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] 100% de las historias `SE-001..SE-045` trazadas y verificadas con pruebas automatizadas (`549/549` unitarias y tests locales cubren `SE-001..SE-045`; falta validación E2E física/backend desplegado: (acción del usuario: pruebas en dispositivo y AWS; ver plan cierre móvil §5.Z-3))
- [x] Kover `>= 80.00%` en `domain/` y `presentation/` verificado por `check_coverage.py --min 80` (`80.81 %`)
- [x] Cero referencias a `MockDataProvider` en `app/src/main/**`
- [ ] Controles OWASP ASVS L2 (V1..V14) cerrados con evidencia en `docs/asvs-l2-checklist.md` (`G-07`, `G-09` y `G-04` cerrados en `FT-43`; falta `G-08` Play Integrity y validación remota: (acción del usuario: aprovisionar `G-08` si aplica y ejecutar CI remoto; ver plan cierre móvil §5.Z-1 y §5.Z-4))
- [ ] `Memoria.md` §6 y `CHECKLIST_PR.md` actualizados para el comité de defensa (`Memoria.md` §6 actualizado; falta (acción del usuario: completar `CHECKLIST_PR.md`; ver plan cierre móvil §5.Z-5))

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON) (`check_layers.py` PASS)
- [x] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [ ] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>` (`31/32` ViewModels heredan `MviViewModel`; excepción legacy `MainScreenViewModel`: (acción del usuario: decidir migración opcional o cierre; ver plan cierre móvil §5.Z-5))
- [x] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [x] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [ ] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty` y `Error` (`FT-17`/`20`/`24`/`30` completos; acción del usuario: decidir refactor UI en pantallas heredadas; ver plan cierre móvil §5.Z-5)
- [ ] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar (`SensusAmber`) ni emojis (`SensusAmber` retirado, 0 emojis en UI interna y excepción `#F3B443` `SplashScreen` documentada; falta inspección visual física: (acción del usuario: revisar UI en teléfono; ver plan cierre móvil §5.Z-3))
- [ ] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos (`android:min` verificados en componentes nuevos; falta verificación física con accesibilidad: (acción del usuario: recorrer app con lector/escala de fuente en teléfono; ver plan cierre móvil §5.Z-3))

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
- [ ] Revisar y resolver el hallazgo Semgrep de MainActivity exportada (acción del usuario: confirmar/suprimir `exported_activity` necesario para launcher/App Links; ver plan cierre móvil §5.Z-4).
- [ ] Ejecutar los escáneres remotos con resultado bloqueante sobre el commit de la candidata (`35/35` `check-runs` verdes en `c215700`; falta (acción del usuario: ejecutar escáneres en CI tras push del cierre; ver plan cierre móvil §5.Z-4)).
- [ ] Ejecutar la suite instrumentada en dispositivo sobre la candidata (`compileDebugAndroidTestSources` verde; falta (acción del usuario: ejecutar en dispositivo físico; ver plan cierre móvil §5.Z-3)).

## Cierre SDD

- [x] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [x] `Memoria.md` §6 actualizado con decisiones y evidencia
- [ ] `CHECKLIST_PR.md` completo (acción del usuario: completar checklist manual de PR antes del merge; ver plan cierre móvil §5.Z-5)
- [ ] Tag oficial `v1.4.0` tras el cierre de los controles y la autorización de publicación (acción del usuario: crear commit/PR/tag tras revisión; ver plan cierre móvil §5.Z-5).

- [ ] Verificar el empaquetado de 16 KB de la candidata y la apertura de una base anterior. El AAR de SQLCipher 4.20.0 ya presenta segmentos ELF `PT_LOAD` de `16384` bytes en las 4 ABI (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) (`T27.7`; falta (acción del usuario: comprobar APK/AAB candidato y reapertura de DB previa en dispositivo de 16 KB; ver plan cierre móvil §5.Z-3)).
- [x] Verificar la APK Debug generada con `zipalign -c -P 16 4` y la alineación de las bibliotecas de 64 bits incluidas.
