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

## E-03 — Test intermitente en CI: `IllegalStateException` en `TestMainDispatcherJvm` por `Looper`

| Campo | Valor |
|---|---|
| Síntoma | `release-playstore` falla en `pre-flight-gates` con un test de ViewModel en rojo que pasa en local y en el PR |
| Excepción | `IllegalStateException at TestMainDispatcherJvm.kt:45` · `Caused by: RuntimeException at Looper.java` |
| Detectado | 2026-11-16, en `main` (`CommunityFeedViewModelTest.successfulPost_…`) |
| Estado | Corregido |

**Causa raíz.** `CommunityViewModel.openComposer()` hacía `withContext(Dispatchers.IO)`, un salto de hilo real. En
los tests (que usan `StandardTestDispatcher` y `Dispatchers.setMain`) el test podía terminar y ejecutar
`resetMain()` antes de que ese hilo regresara; al reanudarse, la corrutina intentaba usar el `Main` original
(`Looper`, que en JVM no existe) y lanzaba la excepción. `runTest` recoge las excepciones no capturadas de otros
hilos y las **atribuye al siguiente test**, por eso el rojo aparecía en un test que no tenía nada que ver. Es una
carrera: depende de la carga de la máquina, así que casi nunca falla en local.

**Corrección.** `CommunityViewModel.ioDispatcher` (`internal var`, por defecto `Dispatchers.IO`) y los tests lo
sustituyen por el dispatcher de prueba. Así el trabajo de E/S corre en el planificador virtual y termina con
`advanceUntilIdle()`. Prueba: `openComposer_runsItsIoWorkOnTheInjectedDispatcher…`.

**Regla preventiva.**
- En un ViewModel, todo `withContext(Dispatchers.IO/Default)` MUST usar un dispatcher sustituible en tests.
  Si el ViewModel es `@HiltViewModel`, usar una propiedad `internal var` (un parámetro de constructor con
  dispatcher no se puede proveer con Hilt sin un módulo).
- Un test que pasa en local pero falla "al azar" en el test siguiente suele ser una corrutina que sobrevive al
  `resetMain()`. Buscar saltos de hilo reales, `delay` con tiempo real o `viewModelScope` sin cancelar.

---

## E-04 — Semgrep no escanea nada: `Failed to download config from https://semgrep.dev/p/mobile: 404`

| Campo | Valor |
|---|---|
| Síntoma | Semgrep imprime `[ERROR] invalid configuration file found` pero el paso aparece en verde |
| Detectado | 2026-11-16, en `release-playstore` y `security-scanners` |
| Estado | Corregido |

**Causa raíz.** El pack `p/mobile` ya no existe en el registro de Semgrep (404). `semgrep-action@v1` no hace
fallar el paso, así que el escaneo SAST quedaba **sin ejecutarse** sin que el CI lo mostrara.

**Corrección.** `config` usa `p/default`, `p/owasp-top-ten`, `p/kotlin` y `p/java` (verificados: devuelven YAML).

**Regla preventiva.** Un paso verde que imprime `[ERROR]` no es un paso sano. Al añadir un pack, comprobar
`curl -sL https://semgrep.dev/p/<pack>` (debe devolver YAML y código 200).

## E-05 — Contraseñas y tokens JWT en logcat de producción (`HttpLoggingInterceptor` en `BODY`)

| Campo | Valor |
|---|---|
| Síntoma | En `release`, las llamadas de login, registro y perfil imprimían cuerpos y cabeceras en logcat |
| Detectado | 2026-11-17, auditoría de FT-27 (RF-27-03) |
| Estado | Corregido |

**Causa raíz.** `NetworkClient` creaba `HttpLoggingInterceptor` con nivel `BODY` sin condicionarlo al tipo de compilación.
Solo los clientes de publicación (`publishingHttpClient`) quitaban el interceptor; `authApi`, `profileApi`, `merchantApi`,
`mediaApi` y `friendsApi` usaban el cliente base. Contraseñas, tokens y PII viajaban a logcat también en producción.

**Corrección.** `httpLoggingLevel(isDebug)`: `NONE` en release y `BASIC` en debug (método, URL y código; sin cabeceras ni
cuerpos). `Authorization`, `Cookie` y `Set-Cookie` se redactan. Prueba de regresión: `NetworkLoggingSecurityTest`.

**Regla preventiva.** Ningún nivel `BODY` o `HEADERS` en el código de la app. Todo interceptor de logging se crea con
`buildLoggingInterceptor`. Las reglas R8 eliminan además `Log.v/d/i` en release.

---

## E-06 — La suite instrumentada nunca se había ejecutado

| Campo | Valor |
|---|---|
| Síntoma | `connectedDebugAndroidTest` arrancaba 0 pruebas con `Process crashed` |
| Detectado | 2026-11-17, FT-27 (T27.4) |
| Estado | Corregido: 47 de 47 en emulador Android 15 |

**Causa raíz.** (1) `app/build.gradle.kts` no declaraba `testInstrumentationRunner`. (2) Las pantallas con
`hiltViewModel()` se probaban en una `ComponentActivity` común, que Hilt no admite sin `@AndroidEntryPoint` y sin
`HiltTestApplication`. (3) Dos pruebas estaban desactualizadas respecto a la UI ("Compartir", "Explorar"). (4) `MainScreen`
pide ubicación al abrirse y el diálogo del sistema pausaba la actividad. Como el CI solo **compila** las pruebas
instrumentadas, nadie lo había notado.

**Corrección.** `HiltTestRunner`, `HiltTestActivity` (solo `src/debug`), `hilt-android-testing` y `kaptAndroidTest`; las
pruebas con Hilt usan `@HiltAndroidTest` y `HiltAndroidRule`; los permisos se conceden con `UiAutomation` antes de componer.

**Regla preventiva.**
- Compilar una prueba no es ejecutarla: antes de un release, correr `connectedDebugAndroidTest` en un emulador o dispositivo
  de pruebas. Lanzarlo con `ANDROID_SERIAL=<emulador>` para no tocar un teléfono con la app de Play.
- **Nunca** ejecutar `connectedDebugAndroidTest` ni `installDebug` en un dispositivo con la app de producción instalada: la
  firma distinta obliga a desinstalar y borra los datos locales.
- Las pruebas de pantallas con permisos deben concederlos antes de componer.

---

---

## Lista de verificación antes de entregar un cambio de persistencia o sesión

- [ ] Los accesos a Room corren en `Dispatchers.IO` (E-01).
- [ ] Ningún borrado de preferencias arrastra la clave de SQLCipher (E-02).
- [ ] Un fallo de Room no cierra la app (E-02).
- [ ] Probado en el teléfono: abrir Sensi, cerrar sesión, iniciar sesión y abrir Sensi otra vez.
- [ ] `adb logcat -d -b crash` sin `FATAL EXCEPTION` tras esa secuencia.
- [ ] Ningún ViewModel salta a un `Dispatchers.IO` real sin dispatcher sustituible en tests (E-03).
- [ ] Sin `HttpLoggingInterceptor` en `BODY`/`HEADERS`; se usa `buildLoggingInterceptor` (E-05).
- [ ] `connectedDebugAndroidTest` ejecutado en emulador antes de publicar, no solo compilado (E-06).
