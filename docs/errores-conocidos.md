# Errores conocidos y cómo evitarlos

> Registro de fallos que ya nos costaron tiempo. Antes de tocar persistencia local, sesión o el chat de Sensi,
> lee este archivo. Cada entrada tiene síntoma, causa raíz, corrección, prueba de regresión y regla preventiva.
> Al cerrar un fallo nuevo, agrega una entrada `E-nn` aquí y una línea en `memoria.md`.

## Cómo diagnosticar un cierre inesperado de la app

Con el teléfono conectado por USB y depuración activa:

```bash
adb logcat -c                       # limpia el log
# reproducir el fallo en el teléfono
adb logcat -d -b crash | tail -60   # excepción fatal y pila
adb shell dumpsys package sensus.com | grep -E "lastUpdateTime|firstInstallTime"
```

La primera línea `FATAL EXCEPTION` y el `Caused by` dicen la causa. Para entender **cuándo** empezó, compara la
hora del fallo con `lastUpdateTime` (si coincide con una instalación nueva, sospecha de migración o clave).

---

## E-01 — La app se cierra al entrar a Sensi: «Cannot access database on the main thread»

| Campo | Valor |
|---|---|
| Síntoma | Al abrir la pestaña de Sensi la app se cierra de inmediato |
| Excepción | `java.lang.IllegalStateException: Cannot access database on the main thread` |
| Detectado | 2026-11-15, en el dispositivo (la suite de pruebas estaba en verde) |
| Estado | Corregido |

**Causa raíz.** `AiChatRepositoryImpl` llamaba a los DAO bloqueantes de Room (`insertMessage`,
`getMessagesByConversationId`, etc.) desde una corrutina del `viewModelScope`, que corre en
`Dispatchers.Main.immediate`. Room lo prohíbe en el hilo principal. Las pruebas no lo vieron porque usan DAO
simulados con MockK, que aceptan cualquier hilo.

**Corrección.** Todo acceso a Room del repositorio va dentro de `withContext(Dispatchers.IO)`
(`getCachedMessages`, `cacheWrite`).

**Regla preventiva.**
- Toda función de repositorio `suspend` que toque un DAO bloqueante (los de Java, sin `suspend`) MUST envolver
  esa llamada en `withContext(Dispatchers.IO)`.
- Un DAO simulado no prueba el hilo. Cualquier cambio de persistencia local MUST verificarse **en el
  dispositivo** abriendo la pantalla afectada, no solo con pruebas unitarias.

---

## E-02 — La app se cierra al entrar a Sensi: «file is not a database» (clave de SQLCipher perdida)

| Campo | Valor |
|---|---|
| Síntoma | Tras cerrar sesión, volver a iniciar o expirar el token, la app se cierra al entrar a Sensi |
| Excepción | `android.database.sqlite.SQLiteException: file is not a database: , while compiling: select count(*) from sqlite_master;` (pila: `net.sqlcipher… keyDatabase`) |
| Detectado | 2026-11-16, en el dispositivo (se repitió aunque E-01 ya estaba corregido) |
| Estado | Corregido y verificado en el teléfono (2026-11-16): `DatabaseFactory` recreó la base ilegible y Sensi abrió sin `FATAL EXCEPTION` |

**Causa raíz.** La frase de paso de SQLCipher se guarda en `SecurePreferences` bajo
`sensus_db_encrypted_passphrase`. `SecurePreferences.clearAll()` hacía `prefs.edit().clear()`, y se invoca al
cerrar sesión, al borrar la cuenta y cuando `AuthInterceptor` recibe un 401 (token vencido). Eso borraba
también la clave de la base. En el siguiente arranque `DatabaseKeyManager` generaba una clave **nueva**, pero
el archivo `sensus_encrypted.db` seguía cifrado con la **anterior**, y SQLCipher no podía abrirlo. La
excepción salía de un `catch` del repositorio (que intentaba leer la caché) y llegaba sin capturar al hilo
principal.

**Corrección (tres capas).**
1. `SecurePreferences.clearAll()` conserva `KEY_DB_PASSPHRASE`. La constante vive en `SecurePreferences` y
   `DatabaseKeyManager` la reutiliza, para que no puedan divergir.
2. `DatabaseFactory` abre y valida la base al crearla. Si no se puede descifrar, la elimina y la recrea vacía
   (es solo una caché local; la fuente de verdad es el backend) en lugar de dejar la app inutilizable. Esto
   también repara teléfonos que ya quedaron con una base ilegible.
3. `AiChatRepositoryImpl` trata cualquier fallo de Room como caché vacía (`getCachedMessages`,
   `cacheWrite` y `observeCachedMessages` con `.catch`): el chat nunca falla por la caché.

**Pruebas de regresión.**
- `SecurePreferencesClearAllTest`: `clearAll()` conserva la clave de la base y sí borra sesión y datos de
  usuario.
- `AiChatLocalCacheFailureTest`: si Room falla al leer, escribir, borrar u observar, no se lanza la
  excepción y el turno de Sensi sigue.

**Regla preventiva.**
- NUNCA borrar con `clear()` un almacén que contenga la clave de cifrado de la base. Los datos de sesión y
  los secretos de infraestructura locales se separan: `clearAll()` solo limpia sesión y usuario.
- Cualquier clave nueva que cifre datos persistentes MUST declararse como constante en
  `SecurePreferences` y excluirse de `clearAll()`, con su prueba en `SecurePreferencesClearAllTest`.
- La caché local es accesoria: ningún fallo de Room MUST propagarse como excepción a la capa de
  presentación. Si una función de repositorio lee o escribe Room, captura `Exception` (sin tragarse
  `CancellationException`) y degrada.
- Tras cualquier cambio en sesión, cierre de sesión o base de datos: instalar en el dispositivo, **cerrar
  sesión, volver a entrar y abrir Sensi**.

---

## Lista de verificación antes de entregar un cambio de persistencia o sesión

- [ ] Los accesos a Room corren en `Dispatchers.IO` (E-01).
- [ ] Ningún borrado de preferencias arrastra la clave de SQLCipher (E-02).
- [ ] Un fallo de Room no cierra la app (E-02).
- [ ] Probado en el teléfono: abrir Sensi, cerrar sesión, iniciar sesión y abrir Sensi otra vez.
- [ ] `adb logcat -d -b crash` sin `FATAL EXCEPTION` tras esa secuencia.
