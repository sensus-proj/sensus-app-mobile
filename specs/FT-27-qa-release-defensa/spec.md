# SPEC — FT-27 Estabilización QA, Pruebas E2E Integrales y Release Play Store

> Rector: `.specify/memory/constitution.md` §4, §7, §8 y §10. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **Sensus_Sprint_Planning_Original** (Sprint 5 QA & Cierre: 24/11/2026 – 04/12/2026).

## 1. Contexto y objetivo

Al completar las olas funcionales (`FT-10` a `FT-26`), el Sprint Planning establece un margen de 2 semanas para pruebas integrales de concurrencia/red en dispositivo Android, eliminación absoluta de `MockDataProvider.kt` de `app/src/main`, verificación del pipeline DevSecOps y preparación del release hacia Google Play Console (`internal` track).

**Objetivo:** verificar el sistema móvil completo de extremo a extremo contra el backend AWS Serverless (`https://zqqdwovx81.execute-api.us-east-2.amazonaws.com/`), erradicar todo residuo mock en `app/src/main`, validar rendimiento/memoria/batería en dispositivo real o emulador, completar los flujos E2E críticos en `androidTest/` y preparar las notas de versión (`distribution/whatsnew/`) y el congelamiento de release conforme a la constitución §8.

**Alcance:** eliminación física de `MockDataProvider.kt` de `app/src/main`, suite E2E de flujos críticos (`auth -> mapa -> detalle -> apartado QR -> canje comerciante`, `invitado 2km`, `baja de cuenta`), auditoría de logs sin PII ni tokens, pruebas de estrés/red intermitente, actualización de `versionCode`/`versionName`, notas de release y revisión integral del pipeline CI/CD.
**Fuera de alcance:** generación local de APK/AAB versionados en Git (el `.aab` firmado solo se genera en el runner efímero de GitHub Actions con autorización explícita).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-27-01 | Verificar que `MockDataProvider.kt` y cualquier clase puente de datos falsos hayan sido eliminados de `app/src/main`, quedando los fakes estrictamente dentro de `app/src/test` y `app/src/androidTest`. |
| RF-27-02 | Ejecutar pruebas E2E (`androidTest`) cubriendo los flujos principales de comprador, modo invitado 2 km, publicación/canje QR de comerciante y baja de cuenta. |
| RF-27-03 | Auditar Logcat y `HttpLoggingInterceptor` asegurando que en compilaciones `release` el nivel de logging HTTP no imprima encabezados `Authorization` ni cuerpos con contraseñas o PII. |
| RF-27-04 | Validar reglas R8/ProGuard (`proguard-rules.pro`) sobre Retrofit, Kotlinx Serialization, SQLCipher, Room, Hilt y OSMDroid compilando en modo Release sin generación ni versionamiento de binarios locales (`compileReleaseKotlin` / verificación de reglas). |
| RF-27-05 | Actualizar `versionCode`, `versionName` SemVer y `distribution/whatsnew/whatsnew-es-419.txt` para la pista `internal` de Google Play Console. |

## 3. Requisitos no funcionales

- **Cobertura:** `koverVerifyDebug` y `check_coverage.py --min 80` en verde sin exclusiones indebidas.
- **DevSecOps:** 100 % de los escáneres en `.github/workflows/ci.yml` y `.github/workflows/security-scanners.yml` en PASS.
- **Estabilidad:** pruebas de estrés de rotación, cambio de tema, pérdida/recuperación de red y background/foreground sin fugas ni `ANR`.

## 4. Arquitectura y contratos internos

```text
app/src/main/java/com/example/sensus/data/remote/NetworkClient.kt # Logging NONE en release
app/proguard-rules.pro                                            # Reglas keep SQLCipher/Serialization/Hilt
app/src/androidTest/java/com/example/sensus/                      # Suites E2E integrales
distribution/whatsnew/whatsnew-es-419.txt
specs/README.md, Memoria.md, CHECKLIST_PR.md
```

## 5. Criterios de aceptacion

- **Given** búsqueda de mocks en `app/src/main` **When** se inspecciona el código fuente **Then** existen 0 referencias a `MockDataProvider`.
- **Given** una compilación Release (`isMinifyEnabled = true`) **When** se validan llamadas HTTP **Then** `HttpLoggingInterceptor` no registra tokens JWT ni cuerpos PII.
- **Given** la suite completa local y en GitHub Actions **When** se ejecuta CI **Then** los 5 gates Python, `detekt`, `ktlintCheck`, `lint`, CodeQL, Semgrep, pruebas unitarias (`>= 80%` Kover) y `compileDebugAndroidTestSources` terminan en verde.

## 6. Plan de pruebas

- **Unitarias y Seguridad:** verificación de logging HTTP en release, reglas R8 y cobertura global Kover `>= 80%`.
- **Instrumentadas (`androidTest`):** suite completa ejecutada en emulador/dispositivo (`EncryptedDatabaseIntegrationTest`, `AuthFlowE2ETest`, `ApartadoFlowE2ETest`, `MerchantScanE2ETest`, `GuestModeAndAccountDialogsE2ETest`).
- **QA manual / Estrés:** prueba de red intermitente, rotación y consumo de cuota AWS/Cloudinary dentro de capa gratuita.

## 7. Riesgos, limites y deuda

- El disparo del workflow `.github/workflows/release-playstore.yml` depende de que el responsable de proyecto configure `KEYSTORE_BASE64`, `SIGNING_*` y `PLAY_STORE_SERVICE_ACCOUNT_JSON` en GitHub Secrets y otorgue autorización explícita para publicar a pista `internal`.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de cierre de Olas 1–4 |
