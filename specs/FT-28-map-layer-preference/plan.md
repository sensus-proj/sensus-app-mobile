# PLAN — FT-28 Persistencia Cifrada de Capa del Mapa en SQLCipher

> Derivado de `spec.md`. Rector: `.specify/memory/constitution.md` §2, §3, §4, §5 y §7.

## 1. Diseño

1. **Esquema Room cifrado v2.** Crear `UserPreferenceEntity.java` y `UserPreferenceDao.java` (siguiendo el estándar Java del paquete `db/` para compatibilidad estricta con Kapt/Room), incrementar `SensusDatabase` a `version = 2` y definir `MIGRATION_1_2` en `DatabaseFactory.kt`.
2. **Capa de dominio pura.** Crear `MapLayer`, `MapPreferencesRepository`, `GetPreferredMapLayerUseCase` y `SavePreferredMapLayerUseCase` sin imports Android/Room.
3. **Repositorio con caché en memoria.** `MapPreferencesRepositoryImpl` guarda la lectura inicial en un campo `@Volatile` y escribe en `Dispatchers.IO` sobre `UserPreferenceDao`, degradando con seguridad a `MapLayer.STANDARD`.
4. **Precarga y MVI.** `AppViewModel` dispara `getPreferredMapLayerUseCase()` en `init` para calentar caché durante Splash; `BuyerMapViewModel` arranca `viewState` desde `peekCached()` y carga/guarda ante `LayerSelected`; `BuyerOsmMapView` aplica la capa desde `factory` y `update`.

## 2. UI y mapeos

- Sin cambios visuales invasivos: el selector **Capas** existente en `BuyerMapComponents.kt` conserva su diseño actual y `OsmMapUtils.kt` expone `toStyle()` / `toDomain()`.

## 3. DI

| Binding | Implementación |
|---|---|
| `UserPreferenceDao` | `SensusDatabase.userPreferenceDao()` en `DatabaseModule` y `SensusContainer` |
| `MapPreferencesRepository` | `MapPreferencesRepositoryImpl` en `RepositoryModule` y `SensusContainer` |
| `GetPreferredMapLayerUseCase` / `SavePreferredMapLayerUseCase` | Proveídos en `UseCaseModule` y `SensusContainer` |

## 4. Gates

- `./gradlew.bat ktlintFormat compileDebugKotlin compileDebugAndroidTestSources detekt ktlintCheck lint testDebugUnitTest koverXmlReportDebug koverVerifyDebug --no-daemon`
- `python scripts/check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 80`

## 5. Orden de implementación

1. Entidad, DAO, `SensusDatabase` v2 y `MIGRATION_1_2`.
2. Modelo/repositorio/casos de uso en `domain/` e implementación en `data/`.
3. Cableado DI (`SensusContainer`, `DatabaseModule`, `RepositoryModule`, `UseCaseModule`).
4. Mapeos `OsmMapUtils`, precarga en `AppViewModel`, estado/intents en `BuyerMapViewModel` y `BuyerOsmMapView`.
5. Pruebas unitarias (`MapPreferencesRepositoryTest`, `BuyerViewModelsTest`, `AppViewModelTest`) e integración SQLCipher (`EncryptedDatabaseIntegrationTest`).
6. Documentación y validación integral de gates.

## 6. Prohibiciones

- Prohibido `fallbackToDestructiveMigration()` en `DatabaseFactory.kt`.
- Prohibido importar `presentation.*` o `data.*` desde `domain/`.
- Prohibido ejecutar `assembleDebug`/`bundleRelease` o comandos Git sin autorización.
