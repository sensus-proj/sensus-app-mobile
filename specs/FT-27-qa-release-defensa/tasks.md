# TASKS — FT-27 Estabilización QA, Pruebas E2E Integrales y Release Play Store

> 1 PR = 1 slice de cierre.

- [x] T27.1 Verificar eliminación definitiva de `MockDataProvider.kt` y cualquier mock residual en `app/src/main` — verif: `python scripts/check_secrets.py`
- [x] T27.2 Condicionar `HttpLoggingInterceptor` en `NetworkClient.kt` a `NONE` en builds release para evitar fuga de tokens/PII — verif: `./gradlew.bat compileReleaseKotlin`
- [x] T27.3 Verificar `app/proguard-rules.pro` con SQLCipher, Kotlinx Serialization, Retrofit, OSMDroid, CameraX/ML Kit y ZXing — verif: `./gradlew.bat compileReleaseKotlin`
- [x] T27.4 Ejecutar la suite instrumentada integral en dispositivo — verificado con ADB en vivo V2341, 60/60 casos. Los recorridos autenticados de escritura siguen en las respectivas FT.
- [x] T27.5 Actualizar `versionCode`, `versionName` y `distribution/whatsnew/whatsnew-es-419.txt` — verif: `./gradlew.bat lint detekt ktlintCheck`
- [ ] T27.6 Ejecutar todos los escáneres CI (`ci.yml` y `security-scanners.yml`), verificar Kover `>= 80%`, actualizar `Memoria.md` y preparar tag de release — verif: `python scripts/check_coverage.py --min 80`
- [ ] T27.7 Actualizar SQLCipher para páginas de 16 KB y comprobar la apertura de la base cifrada existente y la alineación del paquete candidato.
- [x] T27.8 Validar el origen, la ruta y el parámetro de los enlaces de restablecimiento recibidos por la actividad pública. El backend genera enlaces HTTPS y `sensus://reset`; ambas variantes pasan las pruebas. La revisión de manifiesto sigue en T27.6.

## Evidencia de ejecución (2026-11-17)

- **T27.1** — `MockDataProvider.kt` eliminado. `OffersRepositoryImpl` ya no cae a datos de muestra sin DAO y `StoreRepositoryImpl` devuelve valores vacíos. 0 referencias en `app/src/main`; `check_secrets.py` PASS.
- **T27.2** — Hallazgo de seguridad: el cliente base usaba `HttpLoggingInterceptor` en nivel `BODY` también en `release`, así que login, registro y perfil enviaban contraseñas y tokens JWT a logcat. Ahora `httpLoggingLevel(isDebug)` devuelve `NONE` en release y `BASIC` en debug (sin cabeceras ni cuerpos) y las cabeceras `Authorization`, `Cookie` y `Set-Cookie` se redactan. Prueba: `NetworkLoggingSecurityTest` (4). `buildConfig` pasó a `true` para usar `BuildConfig.DEBUG`.
- **T27.3** — `minifyReleaseWithR8` termina sin clases faltantes. Se conservan SQLCipher (1052), Room, OSMDroid, ZXing, los DTO de Sensi y sus serializadores. CameraX y ML Kit no son dependencias del proyecto, así que no requieren reglas. Se añadieron `-assumenosideeffects` para `Log.v/d/i` y atributos de depuración para Play Console. Quedan 126 avisos de R8 por metadatos de Kotlin más nuevos que R8 (no bloquean).
- **T27.4** — La suite instrumentada **nunca había podido ejecutarse**: faltaba `testInstrumentationRunner` y las pantallas con `hiltViewModel()` no tenían infraestructura Hilt de pruebas. Se añadieron `HiltTestRunner`, `HiltTestActivity` (solo `debug`), `hilt-android-testing` y `kaptAndroidTest`, y se corrigieron `AuthFlowE2ETest`, `MainScreenTest` y `FT10UiSanitizationTest`. Nuevas: `SensiOfferCardComposeTest` (4) y `MarkdownTextComposeTest` (2). Resultado en emulador Android 15 (`connectedDebugAndroidTest`): **47 de 47 en verde**.
  - **Brecha:** el canje del comerciante por QR no tiene UI (solo API, repositorio y caso de uso), así que no existe `MerchantScanE2ETest`; depende de FT-14.
  - `AuthFlowE2ETest` abre una sesión de invitado real contra el backend.
- **T27.5** — `versionName` 1.4.0 y `baseVersionCode` 9 (el CI usa `4 + GITHUB_RUN_NUMBER`). `whatsnew-es-419.txt` actualizado (452 de 500 caracteres) con Sensi IA, tarjetas, Markdown y privacidad.
- **T27.6 (parcial)** — Gates locales en verde: `ktlintCheck`, `detekt`, `lint`, pruebas unitarias con Kover, `check_sdd`, `check_layers`, `check_endpoints`, `check_secrets` y `check_coverage --min 80`. Los escáneres en la nube (CodeQL, Semgrep, Trivy, mobsfscan, Sonar y los demás) se ejecutan en CI al subir los cambios y quedan por confirmar. Tag propuesto: `v1.4.0-ft27` (no creado).

## Revisión de la candidata en movil_Rocio — 2026-11-22

- Se actualizó whatsnew-es-419.txt para la candidata 1.4.0 (versionCode base 9). El tag propuesto para el release es v1.4.0, alineado con versionName. Un push de tag puede activar el despliegue a Google Play.
- Se detectó una contradicción con la constitución §4.7: security-scanners.yml usa continue-on-error para varios escáneres y descarta el código de salida de OSV. La candidata queda bloqueada para publicación hasta que el flujo aplique resultados bloqueantes y exista evidencia remota para el mismo commit.
- La ejecución de dispositivo de 2026-11-17 corresponde a una revisión anterior. La cobertura del canje está presente en ClaimsScreensTest y falta repetir la suite instrumentada y el recorrido real de compra y canje sobre la candidata.
- Validación local: 458 pruebas unitarias, cero fallos, Kover Debug 80.10 %. Compilación Debug, Release e instrumentada, lint, Detekt, ktlint y cinco gates Python en verde. Sin generación de APK/AAB.
- Semgrep con p/default, p/owasp-top-ten, p/kotlin y p/java analizó 345 archivos de app/src/main, incluidos los nuevos. Registró un hallazgo bloqueante en AndroidManifest.xml:26 por MainActivity exportada (entrada del launcher y App Links). El escaneo excluyó un archivo por el límite de tamaño de la herramienta. No se suprimió el hallazgo ni se acredita como verde el gate SAST.

## Revisión de dispositivo — 2026-11-22

- Android Studio confirmó la instalación en el vivo V2341 y advirtió incompatibilidad de la APK Debug con páginas de 16 KB. La inspección ELF de los binarios nativos existentes confirmó segmentos PT_LOAD de 4096 bytes en libsqlcipher.so para arm64-v8a y x86_64. libandroidx.graphics.path.so presenta 16384 bytes en ambas. Sigue pendiente resolver SQLCipher y verificar el paquete final de distribución.

- La actualización a SQLCipher 4.20.0 compiló en Debug junto con las pruebas instrumentadas. Los ELF del AAR presentan alineación de 16384 bytes en ARM64 y x86_64. Quedan por comprobar la base cifrada existente en el dispositivo y el empaquetado final.

- Se hicieron bloqueantes los escáneres y la ausencia del XML de cobertura en CI. Se fijaron las acciones por SHA y se corrigieron cuatro interpolaciones directas de metadatos en shell. Actionlint pasó en los tres flujos. Semgrep de repositorio completo pasó de 69 hallazgos a uno, correspondiente a `MainActivity` exportada para launcher y App Links. T27.6 continúa abierta por ese hallazgo y la ejecución remota pendiente.
- La primera suite por Gradle ejecutó 60 pruebas con dos fallos de jerarquía Compose. El reintento detectó una caída de Hilt al iniciar Firebase. Se corrigió la inicialización del servicio y la suite completa pasó por ADB. La APK Debug pasó `zipalign -c -P 16 4`. El vivo usa páginas de 4 KB, por lo que esa ejecución no acredita una prueba de arranque en 16 KB.
- `connectedDebugAndroidTest` retiró la instalación Debug al terminar. La APK nueva se instaló otra vez y abrió correctamente con una base nueva. La base anterior dejó de estar disponible para esta prueba, así que la reapertura de una base existente sigue pendiente en un entorno de prueba preparado para actualización.
