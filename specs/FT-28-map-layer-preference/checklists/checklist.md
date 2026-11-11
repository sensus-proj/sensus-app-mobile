# CHECKLIST — FT-28 Persistencia Cifrada de Capa del Mapa en SQLCipher

> Gate de cierre. Alcance: Constitución §2, §4, §5 y §7.

## Spec y alcance

- [x] `spec.md` con 7 secciones canónicas + `## Aprobación` resuelta
- [x] `plan.md` y `tasks.md` alineados al cambio implementado

## Persistencia y seguridad

- [x] `UserPreferenceEntity` + `UserPreferenceDao` incluidos en `SensusDatabase` `version = 2`
- [x] Migración explícita `MIGRATION_1_2` sin `fallbackToDestructiveMigration()`
- [x] Lectura/escritura SQLCipher fuera del hilo principal (`Dispatchers.IO`) con degradación segura a `MapLayer.STANDARD`
- [x] Precarga en arranque (`AppViewModel`) e inicialización instantánea en `BuyerMapViewModel` + `BuyerOsmMapView`

## Arquitectura y calidad

- [x] `domain/` puro (`check_layers.py` en PASS)
- [x] `./gradlew.bat compileDebugKotlin compileDebugAndroidTestSources` verde (`EncryptedDatabaseIntegrationTest` compilado; ejecución física requiere dispositivo/emulador)
- [x] `./gradlew.bat detekt ktlintCheck lint` verdes (`0` hallazgos)
- [x] `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (`108` tests, `93.45% >= 80%`)
- [x] Los 5 scripts `python scripts/check_*.py` en PASS

## Documentación y cierre

- [x] `Memoria.md` y `specs/README.md` actualizados con evidencia observada
- [ ] Commit/PR en rama `Movil_Rodrigo` pendiente de autorización explícita del usuario
