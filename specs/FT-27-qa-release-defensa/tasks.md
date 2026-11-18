# TASKS — FT-27 Estabilización QA, Pruebas E2E Integrales y Release Play Store

> 1 PR = 1 slice de cierre.

- [x] T27.1 Verificar eliminación definitiva de `MockDataProvider.kt` y cualquier mock residual en `app/src/main` — verif: `python scripts/check_secrets.py`
- [x] T27.2 Condicionar `HttpLoggingInterceptor` en `NetworkClient.kt` a `NONE` en builds release para evitar fuga de tokens/PII — verif: `./gradlew.bat compileReleaseKotlin`
- [x] T27.3 Verificar `app/proguard-rules.pro` con SQLCipher, Kotlinx Serialization, Retrofit, OSMDroid, CameraX/ML Kit y ZXing — verif: `./gradlew.bat compileReleaseKotlin`
- [x] T27.4 Completar y ejecutar suite E2E integral en emulador/dispositivo (`connectedDebugAndroidTest`) — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [x] T27.5 Actualizar `versionCode`, `versionName` y `distribution/whatsnew/whatsnew-es-419.txt` — verif: `./gradlew.bat lint detekt ktlintCheck`
- [ ] T27.6 Ejecutar todos los escáneres CI (`ci.yml` y `security-scanners.yml`), verificar Kover `>= 80%`, actualizar `Memoria.md` y preparar tag de release — verif: `python scripts/check_coverage.py --min 80`

## Evidencia de ejecución (2026-11-17)

- **T27.1** — `MockDataProvider.kt` eliminado. `OffersRepositoryImpl` ya no cae a datos de muestra sin DAO y `StoreRepositoryImpl` devuelve valores vacíos. 0 referencias en `app/src/main`; `check_secrets.py` PASS.
- **T27.2** — Hallazgo de seguridad: el cliente base usaba `HttpLoggingInterceptor` en nivel `BODY` también en `release`, así que login, registro y perfil enviaban contraseñas y tokens JWT a logcat. Ahora `httpLoggingLevel(isDebug)` devuelve `NONE` en release y `BASIC` en debug (sin cabeceras ni cuerpos) y las cabeceras `Authorization`, `Cookie` y `Set-Cookie` se redactan. Prueba: `NetworkLoggingSecurityTest` (4). `buildConfig` pasó a `true` para usar `BuildConfig.DEBUG`.
- **T27.3** — `minifyReleaseWithR8` termina sin clases faltantes. Se conservan SQLCipher (1052), Room, OSMDroid, ZXing, los DTO de Sensi y sus serializadores. CameraX y ML Kit no son dependencias del proyecto, así que no requieren reglas. Se añadieron `-assumenosideeffects` para `Log.v/d/i` y atributos de depuración para Play Console. Quedan 126 avisos de R8 por metadatos de Kotlin más nuevos que R8 (no bloquean).
- **T27.4** — La suite instrumentada **nunca había podido ejecutarse**: faltaba `testInstrumentationRunner` y las pantallas con `hiltViewModel()` no tenían infraestructura Hilt de pruebas. Se añadieron `HiltTestRunner`, `HiltTestActivity` (solo `debug`), `hilt-android-testing` y `kaptAndroidTest`, y se corrigieron `AuthFlowE2ETest`, `MainScreenTest` y `FT10UiSanitizationTest`. Nuevas: `SensiOfferCardComposeTest` (4) y `MarkdownTextComposeTest` (2). Resultado en emulador Android 15 (`connectedDebugAndroidTest`): **47 de 47 en verde**.
  - **Brecha:** el canje del comerciante por QR no tiene UI (solo API, repositorio y caso de uso), así que no existe `MerchantScanE2ETest`; depende de FT-14.
  - `AuthFlowE2ETest` abre una sesión de invitado real contra el backend.
- **T27.5** — `versionName` 1.4.0 y `baseVersionCode` 9 (el CI usa `4 + GITHUB_RUN_NUMBER`). `whatsnew-es-419.txt` actualizado (452 de 500 caracteres) con Sensi IA, tarjetas, Markdown y privacidad.
- **T27.6 (parcial)** — Gates locales en verde: `ktlintCheck`, `detekt`, `lint`, pruebas unitarias con Kover, `check_sdd`, `check_layers`, `check_endpoints`, `check_secrets` y `check_coverage --min 80`. Los escáneres en la nube (CodeQL, Semgrep, Trivy, mobsfscan, Sonar y los demás) se ejecutan en CI al subir los cambios y quedan por confirmar. Tag propuesto: `v1.4.0-ft27` (no creado).
