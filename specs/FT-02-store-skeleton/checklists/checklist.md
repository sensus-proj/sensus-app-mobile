# CHECKLIST — FT-02 Store Skeleton: Persistencia Cifrada y Offline Base

> Gate de cierre. Sin todos los items en `[x]` no hay merge.

## Spec y alcance

- [x] `spec.md` con 7 secciones + Aprobacion resuelta
- [x] Sin entidades de negocio (solo esqueleto)
- [x] `tasks.md` completo con evidencia

## Cifrado y secretos

- [x] DB abre solo con `SupportFactory` de SQLCipher
- [x] Passphrase AES-256 derivada de Android Keystore
- [x] `SecurePrefs` usa `EncryptedSharedPreferences`
- [x] Sin tokens, passphrase ni PII en texto plano o logs
- [x] `allowBackup=false` y reglas de extraccion coherentes
- [x] `clearSession()` purga DB, prefs y estado en memoria

## Offline

- [x] `NetworkMonitor` emite `Flow<Boolean>` real
- [x] `SyncManager.syncPending()` encola y reintenta al reconectar
- [x] Conflicto resuelto por `updatedAt` con regla documentada
- [x] Cola persistida en DB cifrada

## Arquitectura

- [x] `SensusDatabase` no se expone fuera de `data/`
- [x] DAOs solo se usan desde `data/repository/`
- [x] Todo provider en `DatabaseModule`; sin `Context` de Activity
- [x] Migraciones versionadas; sin `fallbackToDestructiveMigration()` sin spec

## Gates

- [x] `compileDebugKotlin` verde
- [x] `compileDebugAndroidTestSources` verde
- [x] `testDebugUnitTest` verde
- [x] `lint`, `detekt`, `ktlintCheck` verdes
- [x] `koverVerify` ≥80% obligatorio en unitarias, integración y E2E (constitución §7.4)
- [x] `python scripts/check_layers.py` PASS
- [x] `docs/asvs-l2-checklist.md` V6 y V10 actualizados

## Cierre SDD

- [x] `Memoria.md` §6 actualizado
- [x] `CHECKLIST_PR.md` verde
- [x] Tag `v1.5.0-ft02`
- [x] 1 revisor humano aprobo el PR
