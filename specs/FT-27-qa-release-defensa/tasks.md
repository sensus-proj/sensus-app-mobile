# TASKS — FT-27 Estabilización QA, Pruebas E2E Integrales y Release Play Store

> 1 PR = 1 slice de cierre.

- [ ] T27.1 Verificar eliminación definitiva de `MockDataProvider.kt` y cualquier mock residual en `app/src/main` — verif: `python scripts/check_secrets.py`
- [ ] T27.2 Condicionar `HttpLoggingInterceptor` en `NetworkClient.kt` a `NONE` en builds release para evitar fuga de tokens/PII — verif: `./gradlew.bat compileReleaseKotlin`
- [ ] T27.3 Verificar `app/proguard-rules.pro` con SQLCipher, Kotlinx Serialization, Retrofit, OSMDroid, CameraX/ML Kit y ZXing — verif: `./gradlew.bat compileReleaseKotlin`
- [ ] T27.4 Completar y ejecutar suite E2E integral en emulador/dispositivo (`connectedDebugAndroidTest`) — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T27.5 Actualizar `versionCode`, `versionName` y `distribution/whatsnew/whatsnew-es-419.txt` — verif: `./gradlew.bat lint detekt ktlintCheck`
- [ ] T27.6 Ejecutar todos los escáneres CI (`ci.yml` y `security-scanners.yml`), verificar Kover `>= 80%`, actualizar `Memoria.md` y preparar tag de release — verif: `python scripts/check_coverage.py --min 80`
