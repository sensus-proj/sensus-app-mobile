# TASKS — FT-02 Store Skeleton: Persistencia Cifrada y Offline Base

> 1 PR = 1 slice. Orden: Keystore -> DB -> prefs -> sync -> gates.
> **Pendientes, sin ejecutar.**

## Fase A — Keystore y base cifrada

- [x] T02.1 Crear `data/local/prefs/KeystoreManager.kt` (clave AES-256, alias `sensus_db_key`) — verif: `testDebugUnitTest`
- [x] T02.2 Crear `data/local/db/DatabaseFactory.kt` con `SupportFactory` de SQLCipher — verif: `compileDebugKotlin`
- [x] T02.3 Crear `data/local/db/SensusDatabase.kt` (Room, `version = 1`, `exportSchema = true`) — verif: `compileDebugKotlin`
- [x] T02.4 Crear entidad y `data/local/dao/BaseDao.kt` de ejemplo — verif: `compileDebugKotlin`
- [x] T02.5 Test instrumentado: la DB abre con passphrase y falla sin ella — verif: `compileDebugAndroidTestSources`
- [x] T02.6 Verificar que la DB no abre con SQLite estandar — verif: test instrumentado

## Fase B — Preferencias seguras y sesion

- [x] T02.7 Crear `data/local/prefs/SecurePrefs.kt` con `EncryptedSharedPreferences` — verif: `testDebugUnitTest`
- [x] T02.8 Implementar `TokenLocalDataSource` (`sessionFlow`, `saveSession`, `clearSession`, `isSessionValid`) — verif: `testDebugUnitTest`
- [x] T02.9 Verificar que ningun token aparece en logs — verif: `testDebugUnitTest`
- [x] T02.10 Implementar borrado total de DB + prefs en `clearSession()` — verif: test instrumentado

## Fase C — Offline y sincronizacion

- [x] T02.11 Crear `util/NetworkMonitor.kt` con `callbackFlow` — verif: `testDebugUnitTest`
- [x] T02.12 Crear `util/SyncManager.kt` con `syncPending(): Result<SyncReport>` — verif: `testDebugUnitTest`
- [x] T02.13 Crear cola `pending_operations` y su DAO — verif: `compileDebugKotlin`
- [x] T02.14 Implementar resolucion de conflicto por `updatedAt` (`server-wins` en inventarios) — verif: `testDebugUnitTest`
- [x] T02.15 Disparar sincronizacion al recuperar conectividad — verif: `testDebugUnitTest`

## Fase D — DI y manifest

- [x] T02.16 Crear `di/DatabaseModule` (DB, DAOs, prefs, Keystore) — verif: `compileDebugKotlin`
- [x] T02.17 Registrar `SyncManager` y `NetworkMonitor` en DI — verif: `compileDebugKotlin`
- [x] T02.18 Poner `allowBackup=false` y ajustar `data_extraction_rules.xml` — verif: `compileDebugKotlin`
- [x] T02.19 Verificar que `SensusDatabase` no se expone fuera de `data/` — verif: `python scripts/check_layers.py`

## Fase E — Cierre

- [x] T02.20 `compileDebugKotlin` y `compileDebugAndroidTestSources` verdes — verif: Gradle
- [x] T02.21 `testDebugUnitTest` verde — verif: `testDebugUnitTest`
- [x] T02.22 `lint`, `detekt`, `ktlintCheck` verdes — verif: Gradle
- [x] T02.23 Actualizar `docs/asvs-l2-checklist.md` (V6, V10) — verif: revision humana
- [x] T02.24 Actualizar `Memoria.md` §6 con la decision de cifrado — verif: revision humana
- [x] T02.25 Tag `v1.5.0-ft02` — verif: revision humana
