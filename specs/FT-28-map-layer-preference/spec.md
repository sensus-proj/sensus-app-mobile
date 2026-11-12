# SPEC — FT-28 Persistencia Cifrada de Capa del Mapa en SQLCipher

> Rector: `.specify/memory/constitution.md` §2, §3, §4, §5 y §7. Plantilla: `specs/_template.md`.
> Extensión UX sobre `FT-02` (SQLCipher) y `FT-07`/`FT-11` (Mapa OSMDroid).

## 1. Contexto y objetivo

Actualmente [`BuyerMapViewModel`](app/src/main/java/com/example/sensus/presentation/buyer/BuyerMapViewModel.kt) gestiona el cambio visual entre las cuatro capas disponibles en [`MapLayerStyle`](app/src/main/java/com/example/sensus/presentation/common/OsmMapUtils.kt) (`STANDARD`, `SATELLITE`, `TERRAIN`, `DARK`) únicamente en memoria volátil. Al cerrar y volver a abrir la app, el mapa regresa siempre a `STANDARD`.

**Objetivo:** aprovechar la base cifrada **Room + SQLCipher (AES-256)** existente (`SensusDatabase`) para almacenar de forma local y cifrada la preferencia de vista de capa del mapa elegida por el usuario y precargarla al iniciar la app para mostrarla de inmediato al abrir el mapa.

**Alcance:**
- Nueva entidad Java [`UserPreferenceEntity`](app/src/main/java/com/example/sensus/data/local/db/entity/UserPreferenceEntity.java) (tabla `user_preferences`) y DAO [`UserPreferenceDao`](app/src/main/java/com/example/sensus/data/local/db/dao/UserPreferenceDao.java).
- Actualización de [`SensusDatabase`](app/src/main/java/com/example/sensus/data/local/db/SensusDatabase.java) de versión `1` a `2` con migración explícita no destructiva (`MIGRATION_1_2` en [`SensusDatabaseMigrations`](app/src/main/java/com/example/sensus/data/local/db/SensusDatabaseMigrations.java), expuesta en [`DatabaseFactory`](app/src/main/java/com/example/sensus/data/local/db/DatabaseFactory.kt)) y provisión DI en `DatabaseModule`/`SensusContainer`.
- Contrato puro de dominio (`MapLayer`, `MapPreferencesRepository`), casos de uso (`GetPreferredMapLayerUseCase`, `SavePreferredMapLayerUseCase`), implementación `MapPreferencesRepositoryImpl` con caché en memoria tras lectura inicial en `Dispatchers.IO`, precarga en `AppViewModel` durante Splash y carga/guardado reactivo en `BuyerMapViewModel` + `BuyerOsmMapView`.

**Fuera de alcance:** sincronización remota de preferencias visuales en el backend AWS (`PUT /api/v1/users/settings` queda reservado a push en FT-21).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-28-01 | `SensusDatabase` incorpora la tabla cifrada `user_preferences` (`prefKey`, `prefValue`, `updatedAt`) incrementando `version = 2` con migración explícita `MIGRATION_1_2` sin pérdida de datos existentes. |
| RF-28-02 | Al seleccionar cualquier capa en `BuyerMapScreen` (`STANDARD`, `SATELLITE`, `TERRAIN`, `DARK`), `BuyerMapViewModel` actualiza el estado visual y persiste la preferencia bajo `prefKey = "map_layer"` en SQLCipher mediante `SavePreferredMapLayerUseCase`. |
| RF-28-03 | Al abrir la aplicación, `AppViewModel` precarga en segundo plano la capa almacenada para calentar caché, de modo que `BuyerMapViewModel` y `BuyerOsmMapView` apliquen la capa guardada desde el primer cuadro al entrar al mapa. |
| RF-28-04 | Si no existe preferencia previa, la DB está corrupta/ilegible o el valor guardado no corresponde a un estilo conocido, el sistema degrada de forma segura a `STANDARD` (`MapLayer.STANDARD`). |
| RF-28-05 | Si el usuario cambia manualmente la capa antes de concluir la lectura inicial asíncrona de SQLCipher, prevalece la selección explícita del usuario. |

## 3. Requisitos no funcionales

- **Seguridad:** almacenamiento exclusivo dentro de `sensus_encrypted.db` protegido con SQLCipher AES-256 (`DatabaseKeyManager`), sin PII ni secretos y sin usar `fallbackToDestructiveMigration()`.
- **Rendimiento:** lectura/escritura Room en `Dispatchers.IO` sin bloquear el hilo principal; lectura instantánea en apertura del mapa vía caché en memoria precargada en arranque.
- **Arquitectura Clean + MVI:** modelo `MapLayer` y repositorio/casos de uso en `domain/` sin dependencias Android/Room; conversión explícita `MapLayer` ↔ `MapLayerStyle` en `presentation/`.
- **Calidad:** `detekt`, `ktlintCheck`, `lint`, Kover `>= 80%` y los 5 scripts `scripts/check_*.py` en verde.

## 4. Arquitectura y contratos internos

```text
domain/
  model/MapLayer.kt                       # STANDARD, SATELLITE, TERRAIN, DARK + fromStorageKey()
  repository/MapPreferencesRepository.kt  # peekCachedLayer(), getPreferredLayer(), savePreferredLayer()
  usecase/
    GetPreferredMapLayerUseCase.kt
    SavePreferredMapLayerUseCase.kt
data/
  local/db/
    entity/UserPreferenceEntity.java      # @Entity(tableName = "user_preferences")
    dao/UserPreferenceDao.java            # getByKey, upsert, deleteByKey
    SensusDatabase.java                   # version = 2, userPreferenceDao()
    SensusDatabaseMigrations.java         # MIGRATION_1_2 (CREATE TABLE IF NOT EXISTS user_preferences)
    DatabaseFactory.kt                    # Registra SensusDatabaseMigrations.MIGRATION_1_2
  repository/MapPreferencesRepositoryImpl.kt
di/
  DatabaseModule.kt, RepositoryModule.kt, UseCaseModule.kt, SensusContainer.kt
presentation/
  root/AppViewModel.kt                    # Precalentamiento en arranque
  buyer/BuyerMapViewModel.kt              # Inicialización inmediata + persistencia en LayerSelected
  buyer/BuyerMapScreen.kt                 # Aplicación inicial en BuyerOsmMapView.factory
  common/OsmMapUtils.kt                   # MapLayer.toStyle() y MapLayerStyle.toDomain()
```

## 5. Criterios de aceptacion

- **Given** un usuario elige la capa `SATELLITE` o `DARK` en el mapa **When** cierra por completo y vuelve a abrir la aplicación **Then** `BuyerMapScreen` inicia inmediatamente mostrando esa misma capa sin revertir a `STANDARD`.
- **Given** una instalación previa con `sensus_encrypted.db` versión 1 **When** abre la nueva versión de la app **Then** `MIGRATION_1_2` crea `user_preferences` conservando sesiones, ofertas y cola offline existentes.
- **Given** una base nueva sin preferencia guardada **When** el usuario abre el mapa **Then** muestra `MapLayerStyle.STANDARD` por defecto sin errores.

## 6. Plan de pruebas

- **Unitarias (`JUnit 4` + `MockK` + `Turbine`):**
  - `MapPreferencesRepositoryTest`: parseo `MapLayer.fromStorageKey`, lectura SQLCipher en frío, hit en caché tras escritura, fallback seguro a `STANDARD` ante error/valor desconocido y SQL generado en `DatabaseFactory.MIGRATION_1_2`.
  - `BuyerViewModelsTest`: precarga desde caché en frame inicial, carga asíncrona en frío, resguardo ante selección manual previa y guardado vía `SavePreferredMapLayerUseCase`.
  - `AppViewModelTest`: verificación de precalentamiento de `GetPreferredMapLayerUseCase` en arranque.
- **Integración (`androidTest` Room + SQLCipher):** `EncryptedDatabaseIntegrationTest` validando `UserPreferenceDao` (`upsert`/`getByKey`/`deleteByKey`) sobre `SensusDatabase` cifrada con `SupportFactory`.
- **Cobertura y Gates:** Kover `>= 80%`, `detekt`, `ktlintCheck`, `lint` y los 5 scripts Python en `PASS`.

## 7. Riesgos, limites y deuda

- La preferencia es local al dispositivo (`user_preferences`); si se desinstala la app (con `android:allowBackup="false"`), la preferencia se reinicia a `STANDARD`.
- La ejecución de pruebas instrumentadas en dispositivo físico (`connectedDebugAndroidTest`) depende de entorno ADB/emulador conectado; el entorno headless verifica `compileDebugAndroidTestSources` y pruebas unitarias JVM.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| En revisión (implementación y gates PASS; pendiente autorización Git/PR) | Pendiente revisión PR | 2026-11-11 | `SensusDatabase` v2 + `MIGRATION_1_2`, `detekt`/`ktlintCheck`/`lint` PASS, `108` unit tests, Kover `93.45%`, 5 `check_*.py` PASS |
