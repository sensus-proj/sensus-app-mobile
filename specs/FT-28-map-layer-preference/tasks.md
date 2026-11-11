# TASKS — FT-28 Persistencia Cifrada de Capa del Mapa en SQLCipher

> Orden: SQLCipher v2 -> Dominio -> Repositorio/DI -> ViewModels/Mapa -> Pruebas -> Gates/Docs.

- [x] T28.1 Crear `UserPreferenceEntity.java`, `UserPreferenceDao.java`, actualizar `SensusDatabase` a `version = 2` y definir `MIGRATION_1_2` en `DatabaseFactory.kt` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T28.2 Crear `MapLayer.kt`, `MapPreferencesRepository.kt`, `GetPreferredMapLayerUseCase.kt` y `SavePreferredMapLayerUseCase.kt` en `domain/` — verif: `python scripts/check_layers.py` (`PASS`)
- [x] T28.3 Implementar `MapPreferencesRepositoryImpl.kt` y registrar `UserPreferenceDao`, repositorio y casos de uso en `SensusContainer`, `DatabaseModule`, `RepositoryModule` y `PreferencesModule` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T28.4 Conectar `OsmMapUtils` (`toStyle`/`toDomain`), precarga en `AppViewModel`, persistencia/inicialización en `BuyerMapViewModel` y aplicación inmediata en `BuyerOsmMapView` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T28.5 Añadir pruebas unitarias (`MapPreferencesRepositoryTest`, `BuyerViewModelsTest`, `AppViewModelTest`) y prueba de integración SQLCipher (`EncryptedDatabaseIntegrationTest`) — verif: `./gradlew.bat testDebugUnitTest compileDebugAndroidTestSources` (`108` unit tests, `0` fallos)
- [x] T28.6 Ejecutar todos los gates Gradle/Python y actualizar `spec.md`, `checklists/checklist.md`, `specs/README.md` y `Memoria.md` — verif: `python scripts/check_coverage.py --min 80` (`PASS 93.45%`)
