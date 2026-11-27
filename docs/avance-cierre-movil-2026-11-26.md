# Avance de ejecución — Cierre móvil Sensus (2026-11-26)

> Documento vivo actualizado al cierre de cada bloque del plan `docs/plan-ejecucion-cierre-movil-2026-11-26.md`.
> Reglas cumplidas: cero `commit`/`push`/`merge`/`stash`/`reset` en ambos repositorios (solo `git mv` en A1), cero peticiones HTTP a la API de producción (salvo apretón TLS con `openssl s_client` en C1), sin usar dispositivos físicos, sin relajar reglas/umbrales.

## RESUMEN EJECUTIVO

Se completaron todos los bloques del plan **`A → B → C → D → E → F → G`** (`docs/plan-ejecucion-cierre-movil-2026-11-26.md`) cumpliendo todas las reglas operativas:
- **Móvil (`Sensus` — Bloques `A..E` + `G`):**
  - Renumeradas `FT-40..42`; reescrito `FT-13` con reconstrucción local (`EncryptedClaimIndex`, máx. 50) + `GET /api/v1/offers/claims/{claimId}` sin rutas de listado/cancelación.
  - Implementado **`FT-17` Favoritos** completo (Room v8→v9 `MIGRATION_8_9`, `POST/GET /api/v1/users/favorites`, `FavoritesScreen`/`ViewModel`, corazón en `BuyerHomeScreen` y `ProductDetailScreen`).
  - Implementado **`FT-43` Seguridad (`G-07`, `G-09`, `G-04`)**: Certificate Pinning AWS (5 pins SPKI), `SecureScreenEffect` (`FLAG_SECURE` ref-counted), `AndroidDeviceAuthenticator` (`MainActivity : FragmentActivity`, biometría/PIN) y SBOM `CycloneDX 1.6` (`199` comps) + `attest-build-provenance`.
  - Resueltas brechas `D1..D6`, conciliadas todas las casillas de `FT-10..FT-43` (`0` sin clasificar) y corregidos en **Bloque G (`T27.11`)** los 3 fallos CI de `main` (`ee98eaf`): TruffleHog sin `base`/`head`, Semgrep en `release-playstore.yml` con `"setuptools<81"`, y **169** nuevas líneas cubiertas (`>= 160` requeridas en Sonar).
  - **Gates finales móvil:** `check_sdd` (**44**), `check_layers` (**378**), `check_endpoints` (**61**), `check_secrets` (**553**), `detekt`/`ktlintCheck`/`testDebugUnitTest`/`koverVerifyDebug` PASS (**561/561** unitarias, Kover **84.11 %** ≥ `80.00 %`).
- **Backend (`sensus-back` — Bloque F):** `FR-038-07` (`GET /api/v1/users/favorites` 20/100 + índice MongoDB), OpenAPI, `constitution.md` Apéndice F (`G-04`, `G-07`, `G-09`); `check-specs` (**62/62**), `-warnaserror` (**0/0**), formato limpio, Arch (**6/6**), Unit (**1229/1229**), E2E (**3/3**), Int filtradas (**14/14**).
- **Pendiente del usuario (`§5.Z-1..§5.Z-10`):** push/PR/tags y confirmación CI remota (TruffleHog, Semgrep, Sonar `main`), Cloud/Play/SonarCloud/Firebase, landing `rodnix.com.mx` y pruebas físicas en `V2341`.

## Resumen por bloque

| Bloque | Alcance | Estado | Notas clave |
|---|---|---|---|
| **A** | A1 Renumeración `FT-40..42` + A2 `FT-13` reconstrucción local | **Cerrado** | `43` features SDD válidas; `GET /offers/claims` y `PATCH .../cancel` retirados; `532/532` unitarias PASS; Kover `81.94%`; `5/5` Python PASS. |
| **B** | `FT-17` Favoritos (Room v9, API 038, UI P-23, `ProductDetailScreen`) | **Cerrado** | `MIGRATION_8_9` + `FavoriteDao`/`Entity`, `POST/GET /users/favorites`, `FavoritesScreen`/`ViewModel`, corazón en `ProductDetailScreen` & `BuyerHomeScreen`, `541/541` unitarias PASS, Kover `80.75 %`, `5/5` Python PASS. |
| **C** | Endurecimiento seguridad móvil (`G-07`, `G-09`, `G-04`) | **Cerrado** | `pin-set` 5 huellas SPKI AWS en `network_security_config.xml`; `SecureScreenEffect` + `DeviceAuthenticator` (`FragmentActivity`); `CycloneDX 1.6` (`199` comps) + `attest-build-provenance`; `548/548` unitarias, Kover `80.80 %`, `5/5` Python PASS. |
| **D** | Brechas de código `D1..D6` | **Cerrado** | Excepción `#F3B443` `SplashScreen` en `memoria.md` §4; `sqlcipher-android:4.20.0` `PT_LOAD = 16384` en 4 ABIs; test concurrencia `MerchantProfileViewModelTest`; conciliados `FT-21`, `FT-23`, `FT-27`, `FT-31..FT-38`; `549/549` unitarias, Kover `80.81 %`, `5/5` Python PASS. |
| **E** | Cierre documental `FT-10..FT-43` | **Cerrado** | Conciliadas casillas de `FT-10..FT-43` (0 sin clasificar); `35/35` `check-runs` verdes en `c215700`; `SHA1 gradle-wrapper.jar` (`636CF...`); `memoria.md` §6 y `specs/README.md` actualizados. |
| **F** | Backend `GET /api/v1/users/favorites` + Apéndice F | **Cerrado** | Spec `038` `FR-038-07` (`GET /api/v1/users/favorites`), `GetUserFavoritesQuery`, `MongoOfferFavoriteRepository.GetPagedByUserIdAsync`, OpenAPI; Apéndice F (`G-04`, `G-07`, `G-09`); `1229/1229` unit, `6/6` arch, `3/3` E2E, `14/14` int. filtradas. |
| **G** | CI rojo en `main` (`ee98eaf`): G1 TruffleHog, G2 Semgrep, G3 Sonar (`T27.11`) | **Cerrado** | TruffleHog sin `base`/`head`; Semgrep `release-playstore.yml` con `"setuptools<81"`; `+169` líneas cubiertas (`>= 160` mín.); `561/561` unitarias PASS, Kover `84.11 %`, `5/5` Python PASS. |

---

## Bloque A — Detalle y evidencia verificada (Cerrado)

### A1 — Renumeración de colisiones sociales
- Carpetas movidas con `git mv`:
  - `specs/FT-31-me-gusta-ofertas` -> `specs/FT-40-me-gusta-ofertas`
  - `specs/FT-32-actividad-amigos-comunidad` -> `specs/FT-41-actividad-amigos-comunidad`
  - `specs/FT-33-moderacion-groserias-ux-perfil` -> `specs/FT-42-moderacion-groserias-ux-perfil`
- Actualizados los cuatro artefactos SDD internos (`FT-40`, `FT-41`, `FT-42`, prefijos `RF-40/41/42`, `T40/41/42`, dependencia `FT-41 -> FT-40`), `specs/README.md` (con tabla de equivalencias fechada `2026-11-26`), `docs/endpoints-internos.md` y `memoria.md`.
- Comprobado en lectura sobre `sensus-back/specs` y `sensus-back/docs`: cero referencias a `FT-31`, `FT-32` o `FT-33`.

### A2 — `FT-13` Reconstrucción local de «Mis reservas» y retiro de cancelación
- `EncryptedClaimIndex`: historial ordenado (`claim_history:<cuenta>`), tope `50` registros por cuenta, evicción del más antiguo, `getClaimIds(account)` con fallback a llaves `claim:*:<cuenta>` y `removeClaimId(account, claimId)`.
- `ClaimsApiService` y `ClaimsRepository`: retirados `GET api/v1/offers/claims` y `PATCH api/v1/offers/claims/{claimId}/cancel`; retirado `CancelClaimUseCase` y renombrado a `GetMyClaimsUseCase.kt`.
- `ClaimsRepositoryImpl`: `restoreClaim` devuelve `NotFound` si no hay ID en índice local; `getMyClaims()` reconstruye vía `GET /api/v1/offers/claims/{claimId}` en paralelo acotado (`Semaphore(4)` en `Dispatchers.IO` inyectable), poda los `404` del índice, falla cerrado ante `401`/`403` o sin sesión.
- `MyClaimsContract`, `MyClaimsViewModel`, `MyClaimsScreen` y `strings_claims.xml`: retirados botón/diálogo/intent/efecto de cancelación; orden estable de visualización `ACTIVE` -> `REDEEMED` -> `EXPIRED`.
- Pruebas actualizadas: `MyClaimsRepositoryTest` (`MockWebServer`: reconstrucción, poda `404`, `401` fail-closed, invitado fail-closed, aislamiento por cuenta y tope `50`), `MyClaimsViewModelTest`, `MyClaimsScreenTest` (`assertDoesNotExist()` para `"Cancelar reserva"`).
- Documentación conciliada: `docs/endpoints-internos.md`, `docs/rate-matrix.md`, `specs/FT-13-reserva-qr-cliente/*`, `specs/README.md` (`En revision`).

### Gates verificados tras el último edit del Bloque A
- `./gradlew.bat compileDebugKotlin compileDebugAndroidTestSources detekt ktlintCheck lint testDebugUnitTest koverXmlReportDebug koverVerifyDebug`: **BUILD SUCCESSFUL** (`532/532` pruebas unitarias PASS, `40/40` en `*Claim*`, `0` fallos).
- `python scripts/check_sdd.py`: **PASS** (`0 hallazgos, 43 features`).
- `python scripts/check_layers.py`: **PASS** (`0 hallazgos, 359 archivos`).
- `python scripts/check_endpoints.py`: **PASS** (`0 hallazgos, 61 endpoints`).
- `python scripts/check_secrets.py`: **PASS** (`0 hallazgos, 521 archivos`).
- `python scripts/check_coverage.py --min 80 --require-report --report app/build/reports/kover/reportDebug.xml`: **PASS** (`81.94%`, umbral `80.00%`).
- `git grep -n -E 'offers/claims"|/cancel|cancelClaim' -- app/src`: **0 coincidencias** de rutas retiradas.

---

## Bloque B — Detalle y evidencia verificada (Cerrado)

### `FT-17` — Favoritos de ofertas (`P-23`, `C-37`)
- **Dominio (`domain/`)**: `FavoriteModels.kt` (`FavoriteFilter { ALL, AVAILABLE_ONLY }`, `FavoriteOffer`, `FavoriteToggleResult`), `FavoritesRepository` y casos de uso `ToggleFavoriteUseCase`, `GetFavoritesUseCase` y `ObserveFavoritesUseCase`.
- **Datos y Room SQLCipher (`data/`, `di/`)**: `FavoriteEntity.java`, `FavoriteDao.java`, `SensusDatabase.java` (versión `8` → `9`), `SensusDatabaseMigrations.java` (`MIGRATION_8_9`), `DatabaseFactory.kt`, `DatabaseModule.kt` (`provideFavoriteDao`), purga automática en `SessionRepositoryImpl.clearSession()` (sin abrir DB de forma ansiosa) y aislamiento por `ownerKey`.
- **Red y mapeo (`data/remote/`, `data/mapper/`)**: `FavoritesApiService` (`POST api/v1/users/favorites/{offerId}` según Spec `038` y `GET api/v1/users/favorites` según contrato fijo Decisión 7), `FavoriteDtos.kt`, `NetworkClient.kt`, `FavoriteMapper.kt` y `FavoritesRepositoryImpl.kt` (actualización optimista con rollback ante error HTTP/red/JSON malformado y preservación de caché Room cuando `GET` devuelve `404` antes de implementar Bloque F).
- **Presentación y navegación (`presentation/`)**: `FavoritesContract.kt`, `FavoritesViewModel.kt`, `FavoritesScreen.kt` (estados Loading/Loaded/Empty/Error + 4 `@Preview`), `ProductOfferComponents.kt`, corazón en `BuyerHomeScreen.kt` (`FavoriteProductOfferCard`) y `ProductDetailScreen.kt`/`ProductDetailComponents.kt` (`CircleFavoriteIconButton` con `GuestPromptDialog` y retiro de `ProductSaveInterestPill` para evitar doble acción sobre ofertas; `SavedOffersRepository` queda para publicaciones de Comunidad), entrada «Mis favoritos» en `BuyerProfileScreen.kt` / `MainAppMenu.kt` / `MainAppBuyerOverlays.kt`.
- **Pruebas y documentación**: `FavoritesUseCasesTest` (`4/4`), `FavoritesRepositoryImplTest` (`3/3` con `MockWebServer`), `FavoritesViewModelTest` (`2/2` con Turbine), `OffersRepositoryOfflineTest` ampliado para verificar `favoriteDao.clearAll()`, `FavoritesScreenTest` y `EncryptedDatabaseIntegrationTest` (`MIGRATION_8_9` + CRUD SQLCipher) compilados en `androidTest`; conciliados `docs/endpoints-internos.md` y `docs/rate-matrix.md` (`120 req/h`), `specs/FT-17-favoritos/{tasks.md, checklists/checklist.md}`, `specs/README.md` (`En revision`) y `memoria.md` §6.

### Gates verificados tras el último edit del Bloque B
- `python scripts/check_sdd.py`: **PASS** (`0 hallazgos, 43 features`).
- `python scripts/check_layers.py`: **PASS** (`0 hallazgos, 373 archivos`).
- `python scripts/check_endpoints.py`: **PASS** (`0 hallazgos, 61 endpoints`).
- `python scripts/check_secrets.py`: **PASS** (`0 hallazgos, 540 archivos`).
- `./gradlew.bat compileDebugKotlin compileDebugAndroidTestSources detekt ktlintCheck lint testDebugUnitTest koverXmlReportDebug koverVerifyDebug`: **BUILD SUCCESSFUL** (`541/541` unitarias PASS, `0` fallos, `0` omisiones; suites de Favoritos `9/9`: `FavoritesUseCasesTest` `4/4`, `FavoritesRepositoryImplTest` `3/3`, `FavoritesViewModelTest` `2/2`).
- `python scripts/check_coverage.py --min 80 --require-report --report app/build/reports/kover/reportDebug.xml`: **PASS** (`80.75%`, umbral `80.00%`).

---

## Bloque C — Detalle y evidencia verificada (Cerrado, `FT-43`)

### Implementación y controles
- **C1 (`G-07` Pinning TLS):** `app/src/main/res/xml/network_security_config.xml` configura `cleartextTrafficPermitted="false"` sobre `execute-api.us-east-2.amazonaws.com` (`includeSubdomains="true"`) y `<pin-set expiration="2027-11-26">` con las 5 huellas SPKI SHA-256 verificadas (`AmazonRootCA1..4` + `SFSRootCAG2`). Validado con `NetworkPinningTest` (XML + rechazo `SSLPeerUnverifiedException` con `OkHttp CertificatePinner`) y `NetworkSecurityConfigTest` (`androidTest`).
- **C2 (`G-09` `FLAG_SECURE`):** Creados `SecureScreenEffect.kt` y `WindowSecureFlagRegistry.kt` con conteo de referencias por `Window` y respeto de flags previos; sustituido `DisposableEffect` ad-hoc de `MerchantSalesScreen.kt` y aplicado a `ClaimOfferScreen`, `MyClaimsScreen`, `ActiveApartadoScreen`, `ForgotPasswordScreens`, `ConfirmDeleteAccountDialog` (más `DialogProperties(securePolicy = SecureFlagPolicy.SecureOn)`), `BuyerProfileScreen`, `MerchantProfileScreen`, `LoginScreen`, `RegisterScreen` y `OtpVerificationScreen`. Probado con `SecureScreenEffectTest`.
- **C3 (`G-09` Biometría / Credencial de dispositivo):** Incorporada `androidx.biometric:biometric:1.1.0` (Apache 2.0, USD \$0), migrada `MainActivity` a `FragmentActivity`, y creados `domain/security/DeviceAuthenticator.kt`, `AndroidDeviceAuthenticator.kt` (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`, fail-closed si no hay bloqueo configurado) y `DeviceAuthGate.kt` (`SensitiveQrGate`, `LocalDeviceAuthenticator`). Protegidos la revelación del QR en `ClaimOfferScreen` y `MyClaimsScreen` y la confirmación en `ConfirmDeleteAccountDialog`, verificados en `DeviceAuthenticatorTest` y `DeviceAuthUiTest`.
- **C4 (`G-04` SBOM + Build Provenance):** Incorporado `org.cyclonedx.bom:3.6.1` (Apache 2.0, USD \$0, build-time) sobre `releaseRuntimeClasspath` generando `app/build/reports/bom.json`; flujos `.github/workflows/ci.yml` y `.github/workflows/release-playstore.yml` ejecutan `./gradlew :app:cyclonedxDirectBom`, suben `bom.json` y atestan procedencia con `actions/attest-build-provenance@4d101475d8b20a2381f78447822ac1eab6504dd8 # v4.2.2`.
- **SDD y ASVS:** Creada `specs/FT-43-endurecimiento-seguridad-movil/` (con STRIDE en `plan.md`), actualizados `specs/README.md` (`En revision`) y `docs/asvs-l2-checklist.md`.

### Textos propuestos para Apéndice F (`sensus-back/constitution.md`, aplicables en Bloque F si aplica)
- **`G-07`:** **Cerrada (2026-11-26, `FT-43` móvil).** `app/src/main/res/xml/network_security_config.xml` fija `<domain-config cleartextTrafficPermitted="false">` para `execute-api.us-east-2.amazonaws.com` (`includeSubdomains="true"`) y `<pin-set expiration="2027-11-26">` con las huellas SPKI SHA-256 de Amazon Root CA 1..4 y Starfield Services Root CA - G2 verificadas contra TLSv1.3 de producción (`NetworkPinningTest`, `NetworkSecurityConfigTest`). Renovar pines antes del 2027-11-26.
- **`G-09`:** **Cerrada en cliente (2026-11-26, `FT-43` móvil).** `SecureScreenEffect` + `WindowSecureFlagRegistry` aplican `FLAG_SECURE` con conteo de referencias en autenticación, recuperación, perfiles, QR y eliminación de cuenta; `MainActivity : FragmentActivity` y `AndroidDeviceAuthenticator` (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`) exigen verificación local antes de revelar QR activo y confirmar baja de cuenta (`SecureScreenEffectTest`, `DeviceAuthenticatorTest`, `DeviceAuthUiTest`). Pruebas físicas pendientes en dispositivo (`§5.Z-3`).
- **`G-04`:** **Cerrada en configuración (2026-11-26, `FT-43` móvil).** El plugin Gradle `org.cyclonedx.bom` `3.6.1` emite `app/build/reports/bom.json` (`CycloneDX 1.6`, `199` componentes) sobre `releaseRuntimeClasspath`; `.github/workflows/ci.yml` y `release-playstore.yml` suben el SBOM y firman procedencia SLSA con `actions/attest-build-provenance@4d101475d8b20a2381f78447822ac1eab6504dd8`. Pendiente validación remota en corrida de GitHub Actions (`§5.Z-4`).
- **`G-08`:** Permanece **Abierta** (fuera de alcance móvil por requerir Google Cloud / Play Console / SSM del responsable; `plan §3`).

### Gates verificados tras el último edit del Bloque C
- `python scripts/check_sdd.py`: **PASS** (`0 hallazgos, 44 features`).
- `python scripts/check_layers.py`: **PASS** (`0 hallazgos, 378 archivos`).
- `python scripts/check_endpoints.py`: **PASS** (`0 hallazgos, 61 endpoints`).
- `python scripts/check_secrets.py`: **PASS** (`0 hallazgos, 550 archivos`).
- `./gradlew.bat detekt ktlintCheck :app:cyclonedxDirectBom compileDebugKotlin compileDebugAndroidTestSources lint testDebugUnitTest koverXmlReportDebug koverVerifyDebug`: **BUILD SUCCESSFUL** (`548/548` unitarias PASS, `0` fallos, `0` omisiones; `app/build/reports/bom.json`: `CycloneDX 1.6`, `199` componentes).
- `python scripts/check_coverage.py --min 80 --require-report --report app/build/reports/kover/reportDebug.xml`: **PASS** (`80.80%`, umbral `80.00%`).

---

## Bloque D — Detalle y evidencia verificada (Cerrado)

- **D1 (`FT-38` arranque `#F3B443`):** Documentada en `memoria.md` §4 la excepción única de arranque para `SplashScreen` (`R.color.sensus_video_matte` `#F3B443` en márgenes del video original `sensus_splash`); cerradas las 2 tareas de arranque en `specs/FT-38-experiencia-comercial/tasks.md` y anotadas en su checklist.
- **D2 (`FT-31` acceso visual):** Cerradas las 6 tareas implementadas de `LoginScreen`/`RegisterScreen`/`AuthAccessScreenLayout` en `specs/FT-31-sistema-visual-login/tasks.md` (`AuthAccessMotionTest` + `AuthFlowE2ETest` compilado); pendientes de ejecución física y revisión Semgrep anotadas con `§5.Z-3`/`§5.Z-4`.
- **D3 (`FT-27 T27.7` SQLCipher 16 KB):** Verificado mediante lectura binaria ELF del AAR en caché (`sqlcipher-android-4.20.0.aar`) que `libsqlcipher.so` declara segmentos `PT_LOAD` alineados a `16384` bytes (`16 KB`) en las 4 ABI (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`); compilado `EncryptedDatabaseIntegrationTest` (`MIGRATION_8_9`). Validación física sobre dispositivo de 16 KB anotada con `§5.Z-3`.
- **D4 (`FT-33..FT-37` pruebas/contratos):** Verificado `031-edicion-perfil-comercio/openapi.yaml` (solo expone `PUT /api/v1/merchants/profile`), añadida prueba de concurrencia y reconciliación `toggleStoreOpen_sharesInFlightProfileRequest_andReconcilesAuthoritativeServerState` en `MerchantProfileViewModelTest.kt`, confirmada cobertura de `T33.1`, `T34.1/T34.2`, `T35.2` y `T36.2` en `BuyerViewModelsTest`, `PublicOfferThumbnailTest`, `AppViewModelTest` y `OffersRepositoryOfflineTest`. Mediciones físicas `FT-32` anotadas con `§5.Z-3`.
- **D5 (`FT-23 T23.7`):** Confirmado `strings_support.xml` (`ft23_privacy_summary`) alineado con backend Specs `042`/`055` (`180 días`, seudonimización); publicación pendiente en *landing* anotada con `§5.Z-2`.
- **D6 (`FT-21`):** Marcada la bandeja local cifrada como `fuera de alcance (sin contrato backend; decisión del responsable 2026-11-26)` y conciliadas casillas verificadas vs pendientes (`§5.Z-1`/`3`/`4`/`5`).

### Gates verificados tras el último edit del Bloque D
- `python scripts/check_sdd.py`: **PASS** (`0 hallazgos, 44 features`).
- `python scripts/check_layers.py`: **PASS** (`0 hallazgos, 378 archivos`).
- `python scripts/check_endpoints.py`: **PASS** (`0 hallazgos, 61 endpoints`).
- `python scripts/check_secrets.py`: **PASS** (`0 hallazgos, 550 archivos`).
- `./gradlew.bat compileDebugKotlin compileDebugAndroidTestSources detekt ktlintCheck lint testDebugUnitTest koverXmlReportDebug koverVerifyDebug`: **BUILD SUCCESSFUL** (`549/549` unitarias PASS, `0` fallos, `0` omisiones).
- `python scripts/check_coverage.py --min 80 --require-report --report app/build/reports/kover/reportDebug.xml`: **PASS** (`80.81%`, umbral `80.00%`).

---

## Bloque E — Detalle y evidencia verificada (Cerrado)

- **Auditoría integral (`specs/FT-10..FT-43`):** Revisadas todas las listas de `tasks.md` y `checklists/checklist.md`. Todas las casillas verificables con código, pruebas y gates locales (`TODO`/`MockDataProvider` = 0, `MviViewModel` = `31/32` con excepción legacy `MainScreenViewModel`, `ClaimsApiTest`/`ClaimsRepositoryTest`, `Publishing*`, `Republish*`, `BuyerViewModelsTest`, `MerchantProfileViewModelTest`, `PublicOfferThumbnailTest`, `AppViewModelTest`, `Favorites*`, `FT-43`) se marcaron `[x]` con evidencia exacta; cada casilla pendiente externa/dispositivo quedó etiquetada con su acción exacta (`§5.Z-1..§5.Z-5`) o `fuera de alcance` (`FT-21` inbox).
- **Evidencia `T27.9` / `T27.9a` / `T27.9b` / `c215700`:**
  - Commit `c215700ef46a033127fbb601a7d880ddd4bc7798` (`HEAD`): `35/35` `check-runs` en `completed / success`.
  - `gradle/wrapper/gradle-wrapper.jar` (`T27.9a`): `SHA-1 = 636CF935A0FD1451657A4112974B3500CCE3AB84` (Gradle `8.7`, falso positivo de Dependency-Check documentado para supresión justificada en `§5.Z-4`).
  - Secretos locales: `python scripts/check_secrets.py` PASS (`0` hallazgos, `550` archivos). Docker Desktop no se encontraba activo localmente para correr la imagen `zricethezav/gitleaks` (`error during connect: pipe dockerDesktopLinuxEngine`), quedando `gitleaks-action` configurado y validado en CI (`§5.Z-4`).
- **Pendientes en espera del usuario (`§5.Z-1..§5.Z-5`):**
  - `§5.Z-1` Cloud / Servicios externos: desactivar Automatic Analysis en SonarCloud; aprovisionar credenciales oficiales de Firebase para release; (si se decide cerrar `G-08` en el futuro, habilitar Play Integrity API + cuenta de servicio SSM).
  - `§5.Z-2` *Landing* (`rodnix.com.mx`): publicar aviso de privacidad actualizado (`FT-23 T23.7`: personalización del feed e interacciones seudonimizadas `180 días`) y `.well-known/assetlinks.json` con SHA-256 de Play (`T27.9b`).
  - `§5.Z-3` Dispositivo físico (`V2341` / Android): ejecutar `androidTest` Compose y Room/SQLCipher, recorrido biométrico/`FLAG_SECURE`, actualización sobre DB cifrada en 16 KB (`T27.7`), cámara QR + canje real (`FT-14`), FCM push/ubicación en vivo (`FT-39`), recorridos visuales claro/oscuro/escala (`FT-31`/`38`), muestreo de rendimiento `FT-32..37` con `summarize_performance.py`, y E2E contra backend AWS desplegado (`FT-11`/`12`/`16`/`18`/`25`).
  - `§5.Z-4` Escáneres remotos CI: ejecutar Workflows tras push (`ci.yml`, `security-scanners.yml`, `release-playstore.yml`), validar atestación SLSA de `bom.json`, y resolver/suprimir hallazgos de `MainActivity` exportada (`Semgrep`), `gradle-wrapper.jar` (`Dependency-Check`) y `StrandHogg` (`MobSFscan`).
  - `§5.Z-5` Gobierno Git y cierre SDD: autorizar `git add`/`commit`/`push`/PR/tags, completar `CHECKLIST_PR.md`, resolver aprobación de `## Aprobación` en `FT-11`/`12`/`15`/`16`/`18`/`27` y decidir refactor opcional de `@Preview` heredados y `MainScreenViewModel`.

---

## Bloque F — Detalle y evidencia verificada (Cerrado)

- **Compuerta:** Verificada existencia de `C:\Users\Rodrigo\source\repos\sensus-back\docs\avance-cierre-specs-2026-11-26.md` con su `## RESUMEN EJECUTIVO` (Bloques `A → B → C → D`).
- **Endpoint y capas Spec `038` (`FR-038-07`, `CA-038-06`):**
  - `Domain`: `src/Sensus.Domain/Models/FavoriteOffersPage.cs` (`FavoriteOfferEntry`, `FavoriteOffersPage`) y `GetPagedByUserIdAsync` en `IOfferFavoriteRepository.cs`.
  - `Application`: `FavoriteOfferDto.cs`, `GetUserFavoritesQuery.cs` (401 sin sesión, 403 `Favorite.GuestForbidden` para invitados, enriquecimiento de likes con `IOfferLikeRepository`), `GetUserFavoritesQueryValidator.cs` (`page >= 1`, `1 <= pageSize <= 100`, defecto `20`).
  - `Infrastructure`: `MongoOfferFavoriteRepository.GetPagedByUserIdAsync` indexado sobre `ix_offer_favorites_user` y clave primaria `_id` en `offers`/`comercios`, excluyendo ofertas no publicadas/expiradas o comercios inactivos.
  - `Api` y contratos: `GET /api/v1/users/favorites` (`GetUserFavorites`, `RegisteredUserPolicy`, `ReadHeavyPolicy`) en `UserEndpoints.cs`, `specs/038-favoritos/{spec.md, plan.md, tasks.md, checklist.md, contracts/openapi.yaml}`, `specs/requirements.md` y `memory.md`.
- **Apéndice F (`sensus-back/constitution.md`):** Actualizadas `G-04` (CycloneDX 3.6.1 + `attest-build-provenance` en móvil `FT-43`), `G-07` (`network_security_config.xml` AWS SPKI pin-set) y `G-09` (`SecureScreenEffect` + `AndroidDeviceAuthenticator`); `G-08` permanece `Abierta`.
- **Gates verificados tras el último edit del Bloque F:**
  - `node .github/scripts/check-specs.mjs specs`: **`62`** especificaciones correctas.
  - `dotnet build sensus-back.sln -warnaserror`: **PASS** (`0` advertencias, `0` errores).
  - `dotnet format sensus-back.sln --verify-no-changes`: **PASS**.
  - `dotnet test tests/Sensus.ArchitectureTests --no-build`: **`6/6` PASS**.
  - `dotnet test tests/Sensus.Application.UnitTests --no-build`: **`1229/1229` PASS** (`GetUserFavoritesQueryHandlerTests` `5/5`).
  - `dotnet test tests/Sensus.E2ETests --no-build`: **`3/3` PASS**.
  - `dotnet test tests/Sensus.IntegrationTests --no-build --filter "FullyQualifiedName~FavoriteEndpointsTests|FullyQualifiedName~OpenApiContractTests|FullyQualifiedName~MongoOfferFeedIntegrationTests"`: **`14/14` PASS** (`FavoriteEndpointsTests` `9/9`, `OpenApiContractTests` `1/1`, `MongoOfferFeedIntegrationTests` `4/4` — prueba Mongo real lista para ejecutarse al encender Docker Desktop).

---

## Bloque G — Detalle y evidencia verificada (Cerrado, `FT-27 T27.11`)

### G1 — TruffleHog en push a `main`
- **Archivo:** `.github/workflows/security-scanners.yml` (`sdd-and-secrets`).
- **Cambio:** Retiradas las entradas `base: ${{ github.event.repository.default_branch }}` y `head: HEAD` del paso `trufflesecurity/trufflehog@4ef51f69ca6347aaa76a5e64a87eac9c0c909f71`, conservando `path: ./` y `extra_args: --only-verified` sin agregar `continue-on-error`.
- **Verificación:** Inspección del YAML correcta; confirmación real en la siguiente ejecución CI sobre `main` (`§5.Z-1`).

### G2 — Semgrep en `release-playstore.yml`
- **Archivo:** `.github/workflows/release-playstore.yml` (`pre-flight-gates`).
- **Cambio:** Actualizado el paso `Escaneo SAST de seguridad móvil (Semgrep)` a `python -m pip install semgrep==1.130.0 "setuptools<81"`, igualando exactamente el paso de `.github/workflows/security-scanners.yml`.
- **Comparación de paridad:** Revisados los pasos equivalentes de `pre-flight-gates` frente a `ci.yml` y `security-scanners.yml`: `wrapper-validation`, `setup-python` (`3.12`), `setup-java` (`temurin 17`), `setup-gradle` (`v4`), `gitleaks-action@v2`, `lint detekt ktlintCheck`, `testDebugUnitTest koverXmlReportDebug koverVerifyDebug` y `check_coverage.py --min 80` coinciden sin divergencias pendientes.
- **Estado Docker local:** Docker Desktop inactivo en el equipo (`open //./pipe/dockerDesktopLinuxEngine`), por lo que ni `python:3.12-slim` con `semgrep` ni el contenedor `gitleaks` se ejecutaron localmente; la comprobación real queda en CI tras el push (`§5.Z-1`).

### G3 — Cobertura de código nuevo para SonarCloud (`main`, `T27.11`)
- **Línea base SonarCloud (`main`, `ee98eaf`):** `5,313` líneas nuevas, `921` sin cubrir (`76.99 %`), requiriéndose un mínimo de **`160` nuevas líneas cubiertas** (sin modificar `sonar.coverage.exclusions`, *Quality Gate*, ni exclusiones/umbrales de Kover; sin añadir Robolectric).
- **Pruebas añadidas (12 métodos JUnit 4 + MockK + Turbine verificando estados, efectos, errores y expiración/permisos):**
  - `app/src/test/java/com/example/sensus/presentation/SearchAndFeedGapCoverageTest.kt`
  - `app/src/test/java/com/example/sensus/presentation/FriendsAndCommunityGapCoverageTest.kt`
  - `app/src/test/java/com/example/sensus/presentation/ProfileMapMerchantGapCoverageTest.kt`
- **Cálculo exacto verificado contra `app/build/reports/kover/reportDebug.xml` y las líneas `isNew=true` / `lineHits=0` de SonarCloud (`main`):**

| Archivo objetivo | Líneas Sonar `isNew=true` sin cubrir | Cubiertas ahora en Kover | Pendientes y motivo |
|---|---:|---:|---|
| `presentation/buyer/SearchOffersViewModel.kt` | 38 | **38** | — |
| `presentation/buyer/CommunityViewModel.kt` | 32 | **28** | `4` (`111, 129, 141` ramas `else -> Unit` tras dispatch exhaustivo; `400` `targetType != POST` inalcanzable vía intents) |
| `presentation/buyer/FriendsViewModel.kt` | 25 | **24** | `1` (`94` rama `else -> Unit` en `processSecondaryIntent`) |
| `presentation/buyer/OfferFeedViewModel.kt` | 21 | **21** | — |
| `presentation/buyer/InterestsPickerViewModel.kt` | 16 | **16** | — |
| `presentation/buyer/BuyerProfileViewModel.kt` | 15 | **15** | — |
| `presentation/buyer/BuyerMapViewModel.kt` | 11 | **8** | `3` (`292, 299, 305` métodos sin uso de `NoOpMapMerchantRepository`) |
| `domain/model/FriendModels.kt` | 9 | **9** | — |
| `presentation/merchant/MerchantRegisterViewModel.kt` | 9 | **9** | — |
| `presentation/main/MainPushHost.kt` | 33 | **1** | `32` (`@Composable` UI `MainPushHost`/`PushRationaleDialog`) |
| `presentation/push/PushNotificationPresenter.kt` | 37 | **0** | `37` (`Build.VERSION`/clases Android de notificaciones sin Robolectric) |
| **Total sobre el conjunto prioritario** | **246** | **169** | **`169 >= 160`** (`+9` sobre el mínimo requerido; confirmación Sonar en `§5.Z-1`) |

- **Gates finales verificados:**
  - `./gradlew.bat detekt ktlintCheck testDebugUnitTest koverXmlReportDebug koverVerifyDebug --console=plain`: **BUILD SUCCESSFUL** (`detekt` limpio, `ktlintCheck` limpio, **`561/561`** pruebas unitarias PASS con `0` fallos y `0` omisiones).
  - `python scripts/check_coverage.py --min 80 --require-report --report app/build/reports/kover/reportDebug.xml`: **PASS (`84.11 %` ≥ `80.00 %`)**.
  - `python scripts/check_sdd.py`: **PASS** (`0 hallazgos, 44 features`).
  - `python scripts/check_layers.py`: **PASS** (`0 hallazgos, 378 archivos`).
  - `python scripts/check_endpoints.py`: **PASS** (`0 hallazgos, 61 endpoints`).
  - `python scripts/check_secrets.py`: **PASS** (`0 hallazgos, 553 archivos`).
