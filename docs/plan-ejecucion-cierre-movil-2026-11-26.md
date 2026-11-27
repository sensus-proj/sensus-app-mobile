# Plan de ejecución — Cierre de todas las features de la app móvil Sensus

| Campo | Valor |
|---|---|
| Fecha | 2026-11-26 |
| Autor del plan | Claude (sesión con Rodrigo Sánchez, responsable del proyecto) |
| Destinatario | Modelo de IA que ejecutará el trabajo sin supervisión |
| Repositorio móvil | `C:\Users\Rodrigo\Sensus\Sensus`, rama `movil_Rodrigo`, base `c215700` |
| Repositorio backend | `C:\Users\Rodrigo\source\repos\sensus-back`, rama `Back_Rodrigo` (**solo en el Bloque F**, al final) |
| Normativa | `.specify/memory/constitution.md` (es la misma que `constitution.md`), `AGENTS.md`, `CHECKLIST_PR.md`, `memoria.md`, `docs/errores-conocidos.md` |
| Estimación | ~25–28 h efectivas en 6 bloques |
| **Ejecución en paralelo** | Este plan corre **al mismo tiempo** que el del backend (`docs/plan-ejecucion-cierre-specs-2026-11-26.md`), que otra sesión está ejecutando. Por eso **no puedes tocar ningún archivo del backend** hasta el Bloque F, y el Bloque F solo empieza cuando exista `C:\Users\Rodrigo\source\repos\sensus-back\docs\avance-cierre-specs-2026-11-26.md` con su RESUMEN EJECUTIVO final. Leer el backend sí está permitido. |

---

## 0. Objetivo y definición de "terminado"

**Objetivo:** que **cada** feature `specs/FT-*` de la app quede con:

1. Todas sus tareas `- [ ]` de `tasks.md` cerradas (`- [x]`) **con evidencia en la misma línea o debajo** (archivo, prueba o comando y su resultado).
2. Su `checklists/checklist.md` sin casillas abiertas, salvo las que dependan de **una acción del usuario** (sección 5.Z). Esas se dejan abiertas y se les agrega al final `(acción del usuario: <qué>; ver plan cierre móvil §5.Z-<n>)`.
3. Su estado en `specs/README.md` coherente: `Cerrada` solo si todo lo anterior se cumple; si queda una acción del usuario, `En revision` con la nota.
4. Código que cumple el requisito, con pruebas en verde y Kover ≥ 80 %.

**Prohibido cerrar algo sin evidencia.** Si una tarea dice algo que el código no hace, es un hueco real: impleméntalo o regístralo en "Hallazgos".

---

## 1. Cómo usar este documento

1. Lee completo este archivo. Después `AGENTS.md`, la constitución (§4 Seguridad, §5 Almacenamiento, §7 Pruebas, §10 SDD), `CHECKLIST_PR.md`, `docs/errores-conocidos.md` (**obligatorio** antes de tocar Room, SQLCipher, `SecurePreferences` o el cierre de sesión) y `memoria.md` §4 (Design System) y §6.
2. **Autorización explícita del usuario:** la regla 5 de `AGENTS.md` ("no ejecutar tareas automáticamente") **queda suspendida para este plan**: el responsable pidió ejecutarlo completo. La regla 6 (no generar APK) también, solo para el punto D3. El resto de `AGENTS.md` sigue vigente.
3. Ejecuta los bloques **en orden: A → B → C → D → E → F**. No avances de punto sin cumplir su "Terminado cuando". Si no puedes, deja el punto como pendiente con la razón exacta y sigue.
4. Al cerrar cada bloque, corre la verificación global (sección 6) y escribe el avance en `docs/avance-cierre-movil-2026-11-26.md` (sección 7).
5. Si algo contradice el plan o la constitución, **detente y pregunta**. No inventes alcance.

---

## 2. Reglas no negociables

### 2.1 Git

- **Backend:** de A a E, **solo lectura**. Otra sesión lo está modificando: no edites archivos, no compiles ni corras sus pruebas, para no chocar con sus carpetas `bin/` y `obj/`. En el Bloque F se permite editar.
- **Móvil y backend:** PROHIBIDO `commit`, `push`, `merge`, `rebase`, `stash`, `reset`, `checkout -- <archivo>`, `clean`, crear o borrar ramas. Todo queda sin commit.
- Excepción: los **renombres de carpetas** del punto A1 se hacen con `git mv` (no crean commit; solo preparan el cambio en el índice). Es la única operación de git que modifica el índice que tienes permitida.
- Antes de empezar, `git status --short` en ambos repos. Móvil: rama `movil_Rodrigo`, con solo `Claude outputs/` sin seguimiento. Backend: lo que dejó el plan del backend, sin commit, que **no debes revertir**. Si ves otra cosa, detente y pregunta.

### 2.2 Producción, AWS, Google y dispositivos

- **Cero peticiones a la API de producción.** Única excepción: el apretón de manos TLS del punto C1 (`openssl s_client`, que no envía ninguna petición HTTP).
- Sin cambios en AWS, GitHub, Firebase, Google Play Console ni SonarCloud.
- Los celulares conectados por USB (`R5CW90LY07N`, Galaxy S23 Ultra; `uwmnofhub6ucq49d`, Xiaomi) **no se usan de noche**: la API está en pausa de 22:30 a 07:00 y todas las pruebas en dispositivo o E2E son de la sección 5.Z.
- No leas ni modifiques `keys.properties`, `local.properties`, `mcp_config.json`, `.agent/mcp_config.json`, `app/google-services.json`, `*.jks` ni `*.keystore`.

### 2.3 Flujo SDD móvil por cada cambio (AGENTS.md §7)

1. `spec.md` de la feature: requisito `RF-<FT>-<nn>` (o RNF) y su criterio de aceptación Given/When/Then.
2. `plan.md`: diseño, capa, DI y riesgos.
3. `tasks.md`: tarea nueva `- [ ] T<FT>.<n> … — verif: <comando>`; al terminar, `- [x]` y la evidencia.
4. Código siguiendo la arquitectura (Model → Repository → UseCase → ViewModel MVI → Screen → Route → DI), Kotlin idiomático, sin emojis, textos en `strings.xml`, complejidad ≤ 10 por método y ≤ 15 por clase, y Design System de `memoria.md` §4.
5. Pruebas: JUnit 4, MockK, Turbine, `MockWebServer` para la API, Compose Test Rule para la UI y pruebas de Room con migración.
6. `checklists/checklist.md` con evidencia.
7. Si cambia un endpoint consumido: `docs/endpoints-internos.md` y `docs/rate-matrix.md`, y luego `python scripts/check_endpoints.py`.
8. `memoria.md` §6 con la decisión y la evidencia.
9. `python scripts/check_sdd.py` en PASS.

### 2.4 Pruebas y escáneres

- Kover ≥ 80 % en todo momento.
- detekt, ktlint y lint sin hallazgos.
- Los 5 `scripts/check_*.py` en PASS.
- **Prohibido** relajar `detekt.yml`, agregar `@Suppress` para pasar una regla, bajar umbrales, omitir pruebas o agregar exclusiones a gitleaks, `check_secrets.py`, Semgrep, MobSF o Dependency-Check. Si una regla salta, se corrige el código. Si un escáner da un falso positivo con un dato de prueba, se cambia el dato (lección de hoy: `"token": "<valor de 16 o más caracteres>"` en pruebas lo marca gitleaks; usa valores cortos como `jwt-a`).

### 2.5 Comandos

```powershell
# Móvil (C:\Users\Rodrigo\Sensus\Sensus)
.\gradlew.bat compileDebugKotlin --no-daemon
.\gradlew.bat detekt ktlintCheck lint --no-daemon --continue
.\gradlew.bat testDebugUnitTest koverXmlReportDebug koverVerifyDebug --no-daemon
.\gradlew.bat compileDebugAndroidTestSources --no-daemon
python scripts/check_sdd.py; python scripts/check_layers.py; python scripts/check_endpoints.py; python scripts/check_secrets.py
python scripts/check_coverage.py --min 80 --require-report --report app/build/reports/kover/reportDebug.xml
```

```bash
# Escáner de secretos sobre el árbol (Git Bash, Docker Desktop arriba)
MSYS_NO_PATHCONV=1 docker run --rm -v "C:/Users/Rodrigo/Sensus/Sensus:/repo" zricethezav/gitleaks:v8.24.3 dir /repo/app/src --redact
```

- Gradle completo tarda ~6 min y lint ~9 min. Córrelos en segundo plano y no lances dos Gradle a la vez.
- MobSF local no termina en un tiempo razonable por las carpetas `build/`. No lo corras: lo valida el CI.

---

## 3. Decisiones ya tomadas por el usuario (no las vuelvas a preguntar)

1. **Mis reservas (FT-13):** se conecta **con lo que sí existe en el backend**, sin rutas nuevas. El backend solo tiene `POST /api/v1/offers/{id}/claim`, `GET /api/v1/offers/claims/{claimId}`, `POST /api/v1/offers/claims/{code}/redeem` y `POST /api/v1/offers/claims/expire-check` (solo administrador). La app **no** llama a `GET /api/v1/offers/claims` ni a `PATCH …/cancel`, que no existen y responden 404.
2. **Favoritos (FT-17) se implementa completa**, incluido el listado en el backend (Bloque F). La app se construye primero contra el contrato fijo de la decisión 7.
3. **Seguridad:** se implementan **G-07 (pinning)** y **G-09 (`FLAG_SECURE` + biometría)**. **G-08 (Play Integrity) queda fuera**: sigue `Abierta` en el Apéndice F del backend con su fecha límite.
7. **Contrato fijo de `GET /api/v1/users/favorites`.** Lo implementa el Bloque F; la app lo consume desde el Bloque B y lo prueba con `MockWebServer`.
   - **Parámetros:** `page` (≥ 1, por defecto 1) y `pageSize` (1–100, por defecto 20).
   - **Acceso:** JWT de usuario registrado. Invitado → 403; sin token → 401.
   - **Respuesta 200**, en JSON camelCase como el resto de la API: `{ "items": [FavoriteOfferDto], "totalCount": int, "pageNumber": int, "pageSize": int, "totalPages": int, "hasPreviousPage": bool, "hasNextPage": bool }`.
   - **`FavoriteOfferDto`:** `id` (uuid), `merchantId` (uuid), `merchantName`, `category`, `title`, `description`, `originalPrice` (decimal), `discountPercentage` (int), `discountedPrice` (decimal), `stock` (int), `imageUrl` (string o null), `startsAt` y `endsAt` (ISO-8601 UTC), `latitude` y `longitude` (double o null; la app calcula la distancia para "Cerca de mí"), `likesCount` (int) y `favoritedAtUtc` (ISO-8601 UTC). Es la misma proyección que `NearbyOfferDto` del backend, sin `distanceMeters` ni `isLikedByMe` y con `favoritedAtUtc`.
   - **Contenido:** solo ofertas visibles al público, ordenadas por `favoritedAtUtc` descendente.
   - **No cambies este contrato.** Si en el Bloque F algo no cuadra, se ajusta el backend al contrato, no la app.
4. **IDs duplicados:** se renumeran las features sociales: `FT-31-me-gusta-ofertas` → `FT-40-me-gusta-ofertas`, `FT-32-actividad-amigos-comunidad` → `FT-41-actividad-amigos-comunidad`, `FT-33-moderacion-groserias-ux-perfil` → `FT-42-moderacion-groserias-ux-perfil`. La cadena de rendimiento FT-31 a FT-37 conserva sus números.
5. **Bandeja de notificaciones cifrada (FT-21):** no existe contrato en el backend. Queda **fuera de alcance**; no se inventa una API.
6. **Sin commits ni push.** El usuario revisa con Claude y sube los cambios.

---

## 4. Estado al entregar este plan (verificado el 2026-11-26)

- `movil_Rodrigo` en `c215700`: build, detekt, ktlint, lint, pruebas unitarias, Kover ≥ 80 %, Semgrep (0 hallazgos) y gitleaks en verde en local.
- Inventario de tareas y casillas abiertas (`T` = tareas, `C` = casillas del checklist):
  - FT-00 a FT-09, FT-22, FT-24, FT-26 y FT-29: 0.
  - FT-10: C1. FT-11: C21. FT-12: C22. FT-13: T2, C5. FT-14: T1, C5. FT-15: C21. FT-16: C24.
  - **FT-17: T16/16, C37/37 (sin implementar).**
  - FT-18: T1, C23. FT-19: C1. FT-20: C2. FT-21: T2, C6. FT-23: T1, C2. FT-25: T1, C1. FT-27: T5, C20. FT-28: C1. FT-30: C1.
  - FT-31-sistema-visual-login: T7, C4. FT-32-linea-base: T2, C5. FT-33-visibilidad: T4, C5. FT-34: T3, C5. FT-35: T3, C5. FT-36: T4, C6. FT-37: T3, C5. FT-38: T4, C5. FT-39: T5, C4.
- **Huecos reales confirmados en código:**
  - FT-13 llama a rutas inexistentes.
  - FT-17 sin código.
  - Sin pinning: `network_security_config.xml` solo confía en las CA del sistema.
  - `FLAG_SECURE` solo en `MerchantSalesScreen`; falta en las pantallas del QR del comprador y en las de cuenta.
  - Sin biometría.
  - Sin SBOM móvil: G-04 sigue `Parcial` en el backend por el móvil.
  - IDs de feature duplicados.

---

## 5. Puntos a ejecutar

### BLOQUE A — Trazabilidad y conexiones rotas (~3 h)

#### A1. Renumerar las features duplicadas (decisión §3.4)

1. `git mv specs/FT-31-me-gusta-ofertas specs/FT-40-me-gusta-ofertas`, y lo mismo para FT-41 y FT-42.
2. Dentro de cada una, reemplaza el ID viejo por el nuevo en encabezados, IDs de requisito (`RF-31-` → `RF-40-`, etc.) y tareas (`T31.` → `T40.`, etc.). **Antes de reemplazar, comprueba con grep** que el patrón solo aparece en las carpetas sociales, porque FT-31-sistema-visual-login también usa `FT31.`.
3. Actualiza todas las referencias en `specs/README.md` (filas y columna "Depende de"), `memoria.md`, `docs/`, `CHECKLIST_PR.md`, comentarios KDoc en `app/src` (`grep -rn "FT-31\|FT-32\|FT-33" app/src docs memoria.md specs`) Cambia **solo** las que se refieren a las sociales. Las referencias del **backend** (specs 057, 058 y 059) se cambian en el Bloque F, no aquí. Anota en el avance la lista exacta, que obtienes solo leyendo: `grep -rn "FT-31\|FT-32\|FT-33" C:\Users\Rodrigo\source\repos\sensus-back\specs`.
4. Agrega una nota en `specs/README.md` con la tabla de equivalencias de IDs viejos a nuevos y la fecha.
- **Terminado cuando:** `ls specs` no muestra IDs repetidos, `check_sdd.py` está en PASS y el grep del repo móvil no devuelve referencias sociales con el ID viejo.

#### A2. Mis reservas con lo que existe (FT-13, decisión §3.1)

- **Hoy:** `ClaimsApiService` declara `getMyClaims()` (`GET api/v1/offers/claims`) y `cancelClaim()` (`PATCH …/cancel`). `ClaimsRepositoryImpl` los usa (líneas ~66, 90 y 94), y ambas rutas responden 404. Ya existe `EncryptedClaimIndex` (en `SecurePreferences.claimIndex`), que guarda el `claimId` por cuenta y oferta.
- **Hacer:**
  1. Agrega a `EncryptedClaimIndex` la lectura de **todos** los `claimId` de la cuenta actual. Debe estar cifrado, acotado a la identidad y con un tope (por ejemplo, los 50 más recientes) para no crecer sin límite.
  2. `getMyClaims()` del repositorio arma la lista llamando a `GET api/v1/offers/claims/{claimId}` por cada ID guardado, en paralelo acotado (máximo 4 a la vez), en `Dispatchers.IO`. Si una llamada da 404, se quita ese ID del índice; si un apartado está vencido o canjeado, se muestra con su estado. Sin sesión registrada, falla cerrado igual que hoy.
  3. Quita `cancelClaim` de la API, el repositorio, el caso de uso, el ViewModel (intención y estado) y la UI (el botón). La spec 033 del backend excluye la cancelación.
  4. Quita `getMyClaims()` de `ClaimsApiService`.
  5. Actualiza `docs/endpoints-internos.md` (borra las dos rutas propuestas), la spec y el plan de FT-13 (la cancelación y el listado remoto pasan a "fuera de alcance por decisión del responsable", y `RF-13-xx` describe la reconstrucción local) y la tarea `T13.L7`.
- **Pruebas:**
  - `MockWebServer`: 3 IDs guardados → 3 GET y la lista armada; un 404 → el ID se poda; un 401 → falla cerrado.
  - Índice: otra cuenta no ve los IDs; tope de 50; `clearAll()` del cierre de sesión **no** borra `KEY_DB_PASSPHRASE` (`errores-conocidos.md`).
  - ViewModel con Turbine.
  - Compose: la pantalla sin el botón de cancelar.
- **Terminado cuando:** `grep -rn "offers/claims\"\|/cancel\|cancelClaim" app/src` no devuelve nada, `check_endpoints.py` está en PASS y todas las pruebas de FT-13 están en verde.

---

### BLOQUE B — FT-17 Favoritos completa (~6 h)

Sigue las 16 tareas de `specs/FT-17-favoritos/tasks.md` **en orden**, con estos ajustes:

- **T17.3 y T17.4:** Room está hoy en `version = 8`, con `MIGRATION_7_8` como la última. Verifícalo de nuevo. Si sigue igual, la migración es 8 → 9. Lee `docs/errores-conocidos.md` antes de empezar. La migración es solo aditiva (`CREATE TABLE`), **nunca** destructiva (`fallbackToDestructiveMigration` prohibido) y con prueba de migración que abra una base v8 con datos.
- **T17.6:** `GET /api/v1/users/favorites` todavía no existe en el backend; lo crea el Bloque F. Implementa el cliente contra el **contrato fijo de la decisión 7** y pruébalo con `MockWebServer`. Hasta que se despliegue, un 404 de esa ruta **no** debe borrar los favoritos locales: la app muestra los de Room y reintenta la sincronización después.
- **T17.7:** actualización optimista con *rollback*. Ante un error de red sin confirmación, **revierte** y muestra un error; no dejes estados fantasma.
- **T17.11:** reemplaza el "guardar" local de oferta en `ProductDetailScreen` (`onToggleSaved`) por el favorito sincronizado. El guardado de **publicaciones** del feed se conserva. Los invitados ven `GuestPromptDialog` y no se llama a ningún endpoint.
- **T17.12:** el corazón en las tarjetas de `BuyerHomeScreen` va solo si el componente lo soporta sin rediseño; si no, documenta la deuda.
- **T17.14:** límite de tasa: reconcilia `docs/rate-matrix.md` con la política real del backend (`SecurityExtensions.cs`, Spec 038) y cita el número exacto.
- **Seguridad:** los favoritos se guardan en Room con SQLCipher, en `Dispatchers.IO`, se borran al cerrar sesión con la purga por identidad existente y nunca se escriben en logs.
- **Terminado cuando:** las 16 tareas y las 37 casillas están cerradas con evidencia (las que exigen dispositivo, como acción del usuario 5.Z) y FT-17 queda `En revision` en `specs/README.md`, a la espera del E2E en el teléfono.

---

### BLOQUE C — Seguridad móvil (~5 h)

#### C1. Pinning de certificado (G-07)

- **Diseño:** en `app/src/main/res/xml/network_security_config.xml`, un `<domain-config>` para `execute-api.us-east-2.amazonaws.com` con `includeSubdomains="true"` y un `<pin-set expiration="AAAA-MM-DD">` con fecha de **12 meses** desde hoy. Pines de las **raíces** de Amazon Trust Services (Amazon Root CA 1, 2, 3 y 4) más **Starfield Services Root Certificate Authority - G2** como respaldo, con un mínimo de 2 pines. No pines el certificado hoja ni los intermedios, que AWS rota. No pines Cloudinary, Firebase ni Google Maps: documéntalo como decisión en el plan de la feature.
- **Cómo obtener los hashes**, sin copiarlos de internet a ciegas:
  1. Descarga los certificados raíz oficiales de `https://www.amazontrust.com/repository/` (`AmazonRootCA1.pem` a `AmazonRootCA4.pem` y `SFSRootCAG2.pem`) a una carpeta temporal nueva fuera del repo.
  2. Calcula cada pin: `openssl x509 -in <pem> -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl base64`.
  3. Comprueba que la cadena real termina en una de esas raíces: `openssl s_client -connect zqqdwovx81.execute-api.us-east-2.amazonaws.com:443 -servername zqqdwovx81.execute-api.us-east-2.amazonaws.com -showcerts </dev/null`. Solo es un apretón de manos TLS, sin petición HTTP, y funciona aunque la API esté en pausa.
  4. Anota en el plan de la feature cada pin, el nombre de su certificado y el comando con el que se obtuvo.
- **Feature dueña:** crea `specs/FT-43-endurecimiento-seguridad-movil/` con los cuatro artefactos (este punto, C2 y C3), desde `specs/_template.md`, y agrégala a `specs/README.md`.
- **Pruebas:**
  - Prueba instrumentada compilable (`compileDebugAndroidTestSources`) que cargue la configuración.
  - Prueba unitaria con `MockWebServer` y un certificado autofirmado (`okhttp-tls` `HeldCertificate`, si ya está en el catálogo; si no, justifica la dependencia: licencia Apache-2.0, sin costo) que demuestre que un `OkHttpClient` con el mismo `CertificatePinner` rechaza un certificado que no coincide.
  - Registra en el checklist la verificación real en el teléfono como acción del usuario 5.Z.
- **Terminado cuando:** la configuración tiene al menos 2 pines verificados con su origen, las pruebas pasan y anotas en el avance el texto para la fila G-07 del Apéndice F del backend: `Cerrada en código el <fecha> (FT-43; pendiente verificar en teléfono)`. **No edites el backend aquí**: esa fila se cambia en el Bloque F.

#### C2. `FLAG_SECURE` en pantallas sensibles (G-09, parte 1)

- **Hoy:** solo `MerchantSalesScreen.kt` lo aplica, con un `DisposableEffect`.
- **Hacer:** extrae ese patrón a un componente reutilizable (`SecureScreenEffect()` en `presentation/common`), con su prueba, y aplícalo en:
  - `ClaimOfferScreen` (QR del apartado).
  - `MyClaimsScreen` y su detalle o QR.
  - La pantalla o diálogo de canje del comercio.
  - Eliminación de cuenta.
  - Cambio y recuperación de contraseña.
  - Cualquier pantalla que muestre el correo o el token.

  Encuéntralas con grep (`QrCode`, `DeleteAccount`, `Password`). `MerchantSalesScreen` pasa a usar el componente nuevo.
- **Pruebas:** prueba de Compose o Robolectric que verifique que la bandera se agrega al entrar y se quita al salir, y que no se quita si ya estaba antes.

#### C3. Biometría o PIN del dispositivo en acciones sensibles (G-09, parte 2)

- **Dependencia:** `androidx.biometric:biometric` en `gradle/libs.versions.toml`, con la última versión estable publicada hace al menos 2 semanas. Justifica en el plan su licencia (Apache-2.0), costo (0) y tamaño. Dependency-Check y Snyk deben quedar limpios.
- **Acciones protegidas:** (a) mostrar el QR de un apartado (`ClaimOfferScreen` y la recuperación en Mis reservas) y (b) confirmar la eliminación de la cuenta.
- **Política:** `BiometricPrompt` con `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`. Si `canAuthenticate` devuelve `BIOMETRIC_ERROR_NONE_ENROLLED`, `NO_HARDWARE` o `HW_UNAVAILABLE`, el dispositivo no tiene bloqueo seguro: **se permite continuar** con el diálogo de confirmación que ya existe y un aviso que recomiende configurar el bloqueo de pantalla. Esta degradación se registra como **riesgo aceptado** en el `spec.md` de FT-43, para que la app no quede inutilizable en la tienda. Si el usuario cancela o falla, la acción **no** se ejecuta.
- **Arquitectura:** interfaz `DeviceAuthenticator` en `domain` e implementación en `data` o `presentation` con la `FragmentActivity`, para poder probar los ViewModels con un doble.
- **Pruebas:** ViewModel con Turbine en los casos éxito, cancelación, error y dispositivo sin bloqueo (continúa con aviso).
- **Terminado cuando:** C2 y C3 están en verde y anotas en el avance el texto para la fila G-09: `Cerrada en código el <fecha> (FT-43; pendiente verificar en teléfono)`. Se aplica en el Bloque F.

#### C4. SBOM móvil (G-04, parte móvil)

- **Hacer:**
  - Plugin Gradle oficial de CycloneDX (`org.cyclonedx.bom`), con una versión estable publicada hace al menos 2 semanas y licencia Apache-2.0, en `app/build.gradle.kts`.
  - Tarea `cyclonedxBom` que genere `app/build/reports/bom.json` solo con la configuración de *release runtime*.
  - En `ci.yml`, un paso que la ejecute y suba el artefacto `sbom-android-cyclonedx` con `actions/upload-artifact` fijado por SHA (copia el SHA que ya usa el repo).
  - En `release-playstore.yml`, el mismo paso antes de firmar, más `actions/attest-build-provenance` fijado por SHA (usa el mismo SHA que el backend: `deploy-serverless.yml` del backend) sobre el `.aab`, con `permissions: id-token: write, attestations: write` solo en ese job.
- **Verificar:** `.\gradlew.bat cyclonedxBom --no-daemon` genera un JSON válido con `bomFormat = CycloneDX`.
- **Terminado cuando:** el BOM se genera y anotas en el avance el texto para la fila G-04: `Cerrada en código el <fecha> (backend y móvil; pendiente primera corrida de CI)`. Se aplica en el Bloque F.

---

### BLOQUE D — Huecos de código en features abiertas (~6 h)

Usa el método de cierre del Bloque E para decidir qué es hueco real. Lo siguiente ya se sabe que es código pendiente:

- **D1. FT-38 Experiencia comercial:** "Retirar la capa de color del arranque y conservar el video original del gato" y "Rellenar con amarillo los márgenes del video original". Busca `SplashScreen.kt` y el recurso del video. El amarillo es el del Design System para el arranque; **revisa `memoria.md` §4**, porque el amarillo está prohibido en la UI interna pero el arranque es la excepción documentada. Si no hay excepción documentada, **detente y pregunta**. Pruebas de Compose del arranque.
- **D2. FT-31-sistema-visual-login:** 7 tareas de diseño (ola, cápsulas, fondo animado, Inter, tarjetas enmarcadas). Primero verifica si ya están hechas en `LoginScreen.kt` y `RegisterScreen.kt` (grep de `Wave`, `Capsule`, `animate`) y en `AuthFlowE2ETest.kt`. Lo hecho se cierra con evidencia; lo que falte se implementa según el prototipo descrito en su `spec.md`. Si falta la referencia visual, consulta Figma solo si el MCP funciona sin llaves nuevas; si no, regístralo como hallazgo.
- **D3. FT-27 `T27.7` SQLCipher y páginas de 16 KB:** hoy está en `sqlcipher = "4.20.0"`.
  1. Verifica en las notas de versión de Zetetic que esa versión trae las `.so` alineadas a 16 KB.
  2. Genera un APK de depuración (autorizado solo para este punto): `.\gradlew.bat assembleDebug --no-daemon`.
  3. Comprueba la alineación con `zipalign -c -P 16 -v 4 <apk>`, usando el `zipalign` de `C:\Users\Rodrigo\AppData\Local\Android\Sdk\build-tools\<última>\`, y que todas las `lib/arm64-v8a/*.so` salgan `(OK)`.
  4. Prueba de Room que abra una base cifrada creada con el esquema actual.
  - Si alguna `.so` no está alineada, regístralo con su nombre y origen.
  - No dejes el APK dentro del repo: bórralo de `app/build/outputs` al terminar o déjalo, porque `build/` está ignorado; verifícalo con `git status`.
- **D4. Rendimiento (FT-33 a FT-37), solo la parte de código.** Las mediciones en el dispositivo son de 5.Z.
  - FT-33 `T33.1`: lista vacía autoritativa y errores de red.
  - FT-33 `T33.2`: invalidación por ID y protección contra respuestas obsoletas.
  - FT-33 `T33.3`: revisa el OpenAPI del backend (`specs/*/contracts/openapi.yaml`) para ver si existe una operación que cambie solo el estado del comercio. Si no existe, implementa la lectura de perfil compartida y cierra con esa evidencia.
  - FT-34 `T34.1`: disparadores únicos y cancelación de resultados obsoletos.
  - FT-34 `T34.2`: caché en Room con vigencia, E/S fuera del hilo principal y migración aditiva si hace falta. Coordina el número de versión con FT-17: si FT-17 usó la 9, esta es la 10.
  - FT-35 `T35.2`: reutilización de Coil y variantes de imagen.
  - FT-36 `T36.4`: apertura diferida de Room. **Lee `errores-conocidos.md` antes**: un fallo de Room no debe cerrar la app.
  - FT-37 `T37.1`: inventario de recorridos en una tabla dentro de su `plan.md`.

  Cada cambio lleva pruebas unitarias con escenarios rápidos y concurrentes (Turbine, `runTest`, `advanceUntilIdle`). Las tareas de medición (`T32.2`, `T33.4`, `T34.3`, `T35.1`, `T35.3`, `T36.1`, `T36.3`, `T37.2`, `T37.3`) son de 5.Z: deja el protocolo y la tabla vacía para llenarlas.
- **D5. FT-23 `T23.7` Aviso de privacidad embebido:** agrega al recurso del aviso dentro de la app (búscalo en `res/raw` o en `strings`) la finalidad de personalización del feed y el registro seudonimizado de interacciones, alineado con lo que hacen las specs 042 y 055 del backend (léelas y no prometas más de lo que hacen). La publicación en la landing es de 5.Z.
- **D6. FT-21 bandeja cifrada:** márcala como `fuera de alcance (sin contrato backend; decisión del responsable 2026-11-26)`. No implementes nada.

---

### BLOQUE E — Cierre documental con evidencia (~5 h)

**Método por cada tarea o casilla abierta**, una por una:

1. Lee qué pide.
2. Busca la evidencia con grep en `app/src` y en las pruebas. Anota archivo y prueba.
3. Si es una prueba, **córrela** (`testDebugUnitTest --tests "*Clase*"`) y anota el resultado.
4. Si existe y pasa, márcala `- [x]` y agrega la evidencia.
5. Si no existe y es pequeña (menos de 1 h), impleméntala con §2.3. Si es grande, déjala abierta y regístrala en "Hallazgos".
6. Si exige dispositivo, E2E contra el backend desplegado, Play Console, SonarCloud, la landing o el PR, déjala abierta con la marca de acción del usuario.

Features por cerrar (con el conteo de §4): FT-10, FT-11, FT-12, FT-13, FT-14, FT-15, FT-16, FT-18, FT-19, FT-20, FT-21, FT-25, FT-27, FT-28, FT-30, FT-38 y FT-39, además de lo que quede de los bloques anteriores. Específicos:

- **FT-11, FT-12, FT-15 y FT-16** (más de 20 casillas cada una): suelen ser DoD y Constitution Check. Comprueba cada principio contra el código y las pruebas.
- **FT-27 `T27.6`:** ejecuta en local todo lo que replique el CI (sección 6). La parte de la corrida remota es de 5.Z.
- **FT-27 `T27.9`:** revisa `security-scanners.yml`. Hoy Semgrep, gitleaks y SDD pasan. Comprueba en la API pública de GitHub (`curl -s "https://api.github.com/repos/sensus-proj/sensus-app-mobile/commits/<sha>/check-runs"`) qué jobs fallan en el último commit de `movil_Rodrigo` y corrige los que sean de código.
- **FT-27 `T27.9a`:** falso positivo de Dependency-Check en `gradle-wrapper.jar`. **No** agregues la supresión. Prepara en el avance el bloque XML exacto para `dependency-check-suppressions.xml` (con `<sha1>` del jar, CVE y justificación) y deja la decisión al usuario (5.Z).
- **FT-39:** los puntos de teléfono físico (`T39.7` a `T39.9`) y "conservar el aviso al cerrar sesión" son de 5.Z, salvo que exista una prueba unitaria que cubra la lógica. En ese caso, ciérralos con evidencia parcial y conserva la marca del teléfono.

---

### BLOQUE F — Backend, solo cuando el plan del backend haya terminado (~2.5 h)

**Puerta:** comprueba que `C:\Users\Rodrigo\source\repos\sensus-back\docs\avance-cierre-specs-2026-11-26.md` existe y que su RESUMEN EJECUTIVO dice que el plan terminó. Si no, **no toques el backend**: deja F como `Bloqueado (esperando al plan del backend)` en el avance y cierra tu reporte. Cuando el usuario te avise, retomas desde aquí.

Antes de editar, corre `git status --short` en el backend y **no reviertas** nada de lo que dejó la otra sesión. Recalcula los IDs libres, porque esa sesión pudo haber agregado requisitos.

#### F1. Listado de favoritos (Spec 038)


Se trabaja en `C:\Users\Rodrigo\source\repos\sensus-back`, con las reglas SDD del backend (`AGENTS.md` del backend y §2.3 del plan del backend). Último FR: `FR-038-06`. Última tarea: `T038-09`. Recalcula antes de usar.

- **Requisito nuevo** (siguiente FR libre de 038), que implementa **exactamente** el contrato de la decisión 7: `GET /api/v1/users/favorites?page=1&pageSize=20`, con `RegisteredUserPolicy`. Devuelve los favoritos del usuario autenticado, paginados (20 por defecto y 100 como máximo, con la política de la Spec 010), ordenados por `createdAtUtc` descendente. **Solo incluye ofertas visibles al público**: el mismo filtro que el detalle público, que excluye `PendingModeration`, `Rejected`, `Quarantined`, comercios cerrados o dados de baja y ofertas eliminadas. Reutiliza la proyección pública de oferta que ya existe (busca el DTO de ofertas cercanas o del detalle) para no exponer campos nuevos, con `favoritedAtUtc` agregado.
- **Datos:** usa el índice existente `ix_offer_favorites_user` (`UserId` asc, `CreatedAtUtc` desc) para la página y una sola consulta `$in` por `_id` sobre `offers` para las ofertas, sin N+1. Justifícalo en `plan.md`.
- **Pruebas:**
  - Unitarias del handler: paginación, filtro de no visibles y página vacía.
  - Integración: 200, 401 sin token, 403 para invitado, `pageSize=101` → 400, y que una oferta en cuarentena no aparezca.
  - Testcontainers: verificar que el plan de consulta usa `ix_offer_favorites_user`.
  - Actualizar el OpenAPI de 038 y pasar `OpenApiContractTests`.
- **Terminado cuando:** las pruebas pasan, `check-specs` está en verde y la fila del requisito está en `specs/requirements.md`.

- **Además:** el JSON de ejemplo de la prueba de la app (`FavoritesRepositoryImpl` con `MockWebServer`) y la respuesta real de una prueba de integración del backend deben coincidir campo por campo.

#### F2. Filas del Apéndice F y referencias de IDs

1. En `constitution.md` del backend, aplica a las filas G-07, G-09 y G-04 el texto que anotaste en C1, C3 y C4. G-08 se queda `Abierta`.
2. En `specs/057-*`, `058-*` y `059-*` del backend, cambia las referencias de las features sociales a FT-40, FT-41 y FT-42, según la lista que anotaste en A1.
3. Verificación del backend: `dotnet build sensus-back.sln -warnaserror`, pruebas unitarias, de arquitectura, la clase de integración de favoritos, `OpenApiContractTests` y `node .github/scripts/check-specs.mjs specs`.

- **Terminado cuando:** todo lo anterior está en verde y el avance lo registra.

---

### BLOQUE G — CI rojo en `main` de la app (agregado el 2026-11-26 a las 12:40, ~1.5 h)

El push a `main` (`ee98eaf`) falló en tres checks que en la rama y en `develop` pasaron. Ninguno es un secreto ni un error de código: son de configuración y de cobertura. Corrígelos en `movil_Rodrigo` para que entren en el mismo PR. Puedes hacerlo en cualquier momento después del bloque E, sin esperar al F.

#### G1. TruffleHog aborta en todo push a `main`

- **Causa:** en `.github/workflows/security-scanners.yml`, el paso de TruffleHog (job `sdd-and-secrets`) usa `base: ${{ github.event.repository.default_branch }}` y `head: HEAD`. En un push a `main`, las dos referencias apuntan al mismo commit y la acción sale con `BASE and HEAD commits are the same`.
- **Arreglo:** quita las entradas `base` y `head` y conserva `path: ./` y `extra_args: --only-verified`. Sin esas entradas, la acción toma los commits del evento (`push` o `pull_request`) por sí misma. No cambies el SHA fijado de la acción ni agregues `continue-on-error`.
- **Verificación:** revisa el YAML. La prueba real es el siguiente push, en 5.Z-1.

#### G2. Semgrep falla en el pre-lanzamiento de Play Store

- **Causa probable:** en `.github/workflows/release-playstore.yml`, job "Validación estricta pre-lanzamiento", el paso de Semgrep hace `python -m pip install semgrep==1.130.0` **sin** `"setuptools<81"`. `security-scanners.yml` sí lo incluye y pasa.
- **Arreglo:** usa la misma línea que en `security-scanners.yml`: `python -m pip install semgrep==1.130.0 "setuptools<81"`. Si hay otro paso de ese job que difiera de su equivalente en `security-scanners.yml` o `ci.yml` (versiones, banderas), iguálalo y documéntalo.
- **Verificación:** revisión del YAML. Si Docker funciona, corre `python:3.12-slim` con `pip install semgrep==1.130.0 "setuptools<81" && semgrep --version` para confirmar que arranca.

#### G3. SonarCloud: cobertura de código nuevo en `main` del 77 % (mínimo 80 %)

- **Causa:** en `main`, Sonar mide como código nuevo todo lo acumulado desde su línea base: 5,313 líneas, 921 sin cubrir (76.99 %). Kover pasa porque mide la cobertura total. Hacen falta **al menos 160 líneas nuevas cubiertas**; apunta a más de 250 para dejar margen, porque los bloques B a D también agregan código nuevo.
- **Prohibido:** agregar archivos a `sonar.coverage.exclusions`, cambiar el *Quality Gate* o la línea base, o excluir clases en Kover para pasar.
- **Arreglo:** pruebas unitarias (JUnit 4, MockK y Turbine; sin UI) de las ramas sin cubrir de estos archivos, ordenados por líneas nuevas sin cubrir:
  - `presentation/buyer/SearchOffersViewModel.kt` (38)
  - `presentation/push/PushNotificationPresenter.kt` (37)
  - `presentation/main/MainPushHost.kt` (33; solo la lógica que no sea `@Composable`)
  - `presentation/buyer/CommunityViewModel.kt` (32)
  - `presentation/buyer/FriendsViewModel.kt` (25)
  - `presentation/buyer/OfferFeedViewModel.kt` (21)
  - `presentation/buyer/InterestsPickerViewModel.kt` (16)
  - `presentation/buyer/BuyerProfileViewModel.kt` (15)
  - `presentation/buyer/BuyerMapViewModel.kt` (11)
  - `domain/model/FriendModels.kt` (9)
  - `presentation/merchant/MerchantRegisterViewModel.kt` (9)

  Para encontrar las líneas exactas, consulta la API pública de SonarCloud (sin token):
  `https://sonarcloud.io/api/sources/lines?key=sensus-proj_sensus-app-mobile:app/src/main/java/com/example/sensus/<ruta>&branch=main` (las líneas con `lineHits: 0` e `isNew: true`).
  Cruza el resultado con el reporte de Kover (`app/build/reports/kover/reportDebug.xml`). Las pruebas deben verificar comportamiento real (estados, efectos, errores), no solo ejecutar líneas.
- **Si te sobra tiempo**, después de lo anterior: prueba de Compose con Robolectric de `SelectedStoreBottomCard.kt` (248 líneas sin cubrir), solo si Robolectric ya está en el catálogo. No agregues dependencias para esto.
- **Verificación:** las pruebas nuevas pasan, Kover sigue ≥ 80 % y en el avance anotas el cálculo: líneas cubiertas nuevas contra las 160 que faltan. La confirmación real es el análisis de Sonar del siguiente push a `main` (5.Z-1).
- **Feature dueña:** FT-27 (QA y release). Agrega la tarea `T27.11` con su evidencia.

---

### 5.Z Acciones del usuario (el ejecutor escribe un runbook para cada una)

| # | Acción | Cierra |
|---|---|---|
| 1 | Revisar con Claude, hacer commit, push y PR de `movil_Rodrigo` y del backend (Bloque F), y verificar el CI verde | FT-27 `T27.6`, `T27.10` de FT-39 y los PR de cada feature |
| 2 | De día (07:00–22:30), con el backend desplegado: E2E en los 2 celulares de Mis reservas, Favoritos, canje QR, feed, amigos y push (`T39.7`–`T39.9`) | FT-13, FT-14, FT-17, FT-18 `T18.16`, FT-25 `T25.8`, FT-39 |
| 3 | En el teléfono: pinning (la app funciona contra producción) y biometría (QR y eliminación de cuenta) | G-07 y G-09 "verificado en teléfono" |
| 4 | Mediciones de rendimiento con el protocolo FT-32 (`scripts/summarize_performance.py`) | FT-32 a FT-37, tareas de medición |
| 5 | Pruebas instrumentadas en el dispositivo: `.\gradlew.bat connectedDebugAndroidTest` con un teléfono conectado | FT-13, FT-14, FT-31 `FT31.04`, FT-38 |
| 6 | Aprobar o rechazar la supresión de Dependency-Check que preparó el ejecutor | FT-27 `T27.9a` |
| 7 | Un administrador desactiva Automatic Analysis en SonarCloud | FT-27 `T27.9b` |
| 8 | Publicar en la landing `/.well-known/assetlinks.json` y el aviso de privacidad actualizado | FT-27 `T27.9b`, FT-23 `T23.7` (landing) |
| 9 | Rotar la llave de Figma (G-12) y guardarla en la variable de entorno `FIGMA_API_KEY` | G-12 |
| 10 | Decidir más adelante si se aborda G-08 (Play Integrity) | G-08 sigue `Abierta` |

---

## 6. Verificación global al cerrar cada bloque

```powershell
cd C:\Users\Rodrigo\Sensus\Sensus
.\gradlew.bat compileDebugKotlin compileDebugAndroidTestSources --no-daemon
.\gradlew.bat detekt ktlintCheck lint --no-daemon --continue
.\gradlew.bat testDebugUnitTest koverXmlReportDebug koverVerifyDebug --no-daemon
python scripts/check_sdd.py; python scripts/check_layers.py; python scripts/check_endpoints.py; python scripts/check_secrets.py
python scripts/check_coverage.py --min 80 --require-report --report app/build/reports/kover/reportDebug.xml
git status --short
```

Más gitleaks en Docker (§2.5), que debe decir `no leaks found`. En el Bloque F, corre también la verificación del backend: build `-warnaserror`, pruebas unitarias, de arquitectura, la clase de integración afectada y `check-specs`.

Al final de todo, el recuento debe quedar solo con las marcas de acción del usuario o fuera de alcance:

```bash
for d in specs/FT-*/; do grep -HnE "^\s*- \[ \]" $d/tasks.md $d/checklists/checklist.md; done
```

---

## 7. Reporte (en `docs/avance-cierre-movil-2026-11-26.md`, en español)

Al inicio, un **RESUMEN EJECUTIVO** de 15 líneas como máximo. Después:

1. Tabla por punto: `Punto | Estado (Hecho / Parcial / Bloqueado) | Evidencia (pruebas y conteos) | Qué falta`.
2. Tabla por feature: `FT | Tareas cerradas / total | Casillas cerradas / total | Pendiente y por qué`.
3. Verificación global con números exactos: pruebas, Kover, detekt, lint y gates.
4. **Hallazgos:** huecos reales no cerrados, con archivo y propuesta.
5. Runbooks de 5.Z.
6. Preguntas para el usuario, cada una concreta.

Actualiza también `memoria.md` §6 y `specs/README.md`.

Nunca digas "verificado en dispositivo" ni "en producción" sin haberlo comprobado. Nunca reportes conteos de pruebas que no corriste. Nunca marques una casilla sin evidencia.
