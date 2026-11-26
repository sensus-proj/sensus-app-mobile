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

## E-07 — CI: `Could not find tink-android-1.8.0.jar` y «Configuration cache entry discarded»

| Campo | Valor |
|---|---|
| Síntoma | `unit-test` falla en 17 s, antes de ejecutar pruebas: `hiltAggregateDepsDebug` no puede serializarse porque falta `com.google.crypto.tink:tink-android:1.8.0` (dependencia transitiva de `security-crypto`) |
| Detectado | 2026-11-19, PR #23 (solo cambios de documentación) |
| Estado | Corregido |

**Causa raíz.** La caché de Gradle tenía varios dueños. Los seis jobs de `ci.yml` usaban `setup-java` con
`cache: gradle` y la **misma clave**: el primero en terminar guardaba solo lo que había resuelto (p. ej. el
`.pom` de Tink sin el `.jar`). Además, `static-analysis` y `release-playstore.yml` añadían `actions/cache` sobre
`~/.gradle/caches` con `restore-keys` genérico, que restaura cachés parciales de otras versiones. Con la
metadata en caché, Gradle solo buscó el JAR en Maven Central y un fallo de descarga tumbó el build. El JAR sí
existe en Central (HTTP 200).

**Corrección.** Todos los jobs usan `actions/setup-java@v5` sin caché y `gradle/actions/setup-gradle@v4`, que
guarda entradas por job y solo escribe desde la rama por defecto (en PR solo lee). Se eliminaron los
`actions/cache` manuales.

**Segunda iteración (2026-11-20).** Con la caché ya limpia, `instrumentation-compile` falló al resolver el plugin
`com.google.dagger.hilt.android:2.58` (existe en Maven Central y en el Plugin Portal: HTTP 200). Confirma que el
fondo es **inestabilidad de descargas desde Maven Central** en los runners, agravada porque cada job descargaba
todo de cero (en PR la caché era de solo lectura y nadie la escribía). Corrección: `GRADLE_OPTS` con 5
reintentos, *backoff* de 2 s y timeouts de 60 s, y `cache-read-only` en `false` para `main` y `develop`, de modo
que los PR lean la caché de su rama base.

**Tercera iteración (2026-11-20).** El push a `develop` (run #117) volvió a fallar con `tink-android-1.8.0.jar`
pese a los reintentos: Central respondió **404**, y Gradle no reintenta un 404 (solo errores de red, 5xx y 429).
Corrección: en `settings.gradle.kts` se antepone el mirror oficial de Maven Central operado por Google
(`maven-central.storage-download.googleapis.com/maven2/`) en `pluginManagement` y `dependencyResolutionManagement`;
`mavenCentral()` queda como respaldo. Verificado con `dependencyInsight --refresh-dependencies`: Tink se resuelve
desde el mirror.

**Regla preventiva.** Un solo mecanismo de caché para `~/.gradle`. No combinar `setup-java cache: gradle` con
`actions/cache` ni usar `restore-keys` genéricos. Si reaparece, borrar las cachés en *Actions → Caches* y
relanzar antes de tocar dependencias.

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

## E-08 — El aviso de ofertas cercanas vuelve al iniciar sesión

El cierre de sesión borraba el consentimiento de ubicación y la marca de que el aviso ya se había respondido. El siguiente acceso volvía a mostrarlo. El almacén de instalación conserva ahora la marca del aviso y deja desactivado el envío de ubicación al salir. La activación voluntaria sigue disponible en Preferencias de notificaciones. La prueba de regresión comprueba la limpieza del consentimiento y la conservación de la marca en una nueva instancia del almacén.

## E-09 — Una oferta confirmada podía faltar en la caché de publicación

El POST de creación podía devolver 201 y, aun así, la copia local no guardarse. La revisión de código identificó que el repositorio invocaba el DAO bloqueante de Room desde la corrutina de la pantalla, que normalmente corre en Main, y la protección de caché absorbía el error.

La inserción tras el 201 se ejecuta en Dispatchers.IO. La prueba HTTP comprueba que el DAO reciba la oferta con el stock confirmado y que la escritura ocurra fuera del hilo del llamador. La prueba en dispositivo sigue pendiente.

## E-10 — El inventario del comercio podía incluir ofertas del catálogo público

El panel precargaba la tabla compartida de ofertas y el repositorio devolvía esa misma tabla si fallaba la consulta privada. Además, una consulta privada válida reemplazaba el catálogo público guardado. Ahora el panel muestra resultados de my-offers o un error con reintento. La lectura del inventario no modifica la caché de exploración. Las pruebas de ViewModel y MockWebServer cubren el fallo y verifican que el DAO no se consulte ni se modifique al listar inventario.

## E-11 — Una falla de caché podía impedir mostrar el feed comunitario

El guardado de la página se hacía dentro de la lectura remota y un error de Room convertía una respuesta válida en fallo. En modo offline una lectura fallida o una fila con JSON corrupto podía interrumpir la recuperación.

El cliente usa la página remota aunque no pueda guardarla. Al leer la caché omite las filas dañadas y conserva el error de red si Room no está disponible. Las dos operaciones permanecen en Dispatchers.IO y propagan las cancelaciones.

## E-12 — La activación manual podía intentar registrar el dispositivo con las notificaciones bloqueadas

La pantalla solo comprobaba si debía solicitar el permiso en Android 13 o superior. El registro automático comprobaba además si el sistema permitía notificaciones, pero el caso de uso compartido no aplicaba esa regla.

La activación manual y automática consultan ahora el permiso efectivo antes del SDK y antes de registrar. La UI muestra un mensaje de permiso sin borrar las preferencias. Las pruebas cubren el bloqueo previo y la revocación durante la consulta del token.

## E-13 — SQLCipher legado impide aprobar la compatibilidad con páginas de 16 KB

Android Studio mostró un aviso de incompatibilidad al instalar la APK. Los segmentos `PT_LOAD` de `libsqlcipher.so` del artefacto legado tenían alineación de 4096 bytes en ARM64 y x86_64.

Se migró a `sqlcipher-android` 4.20.0 y se verificó la alineación a 16384 bytes en ambas arquitecturas en el AAR. Para confirmar la corrección en release, verificar el empaquetado y que la candidata abra la base existente al actualizar sobre una instalación anterior.

## E-14 — Firebase puede interrumpir una suite instrumentada con Hilt

En el vivo V2341, Firebase inició `SensusMessagingService` durante la suite. La inyección automática al crear el servicio intentó acceder al componente de Hilt de una prueba desde un servicio de aplicación y abortó el proceso.

El servicio obtiene la sincronización de tokens desde `SensusApp` cuando recibe el token. En el proceso instrumentado, `HiltTestApplication` permite ejecutar las pruebas sin iniciar esa sincronización contra la sesión de la app real.
