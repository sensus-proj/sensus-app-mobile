# CHECKLIST — FT-10 Saneamiento de Deuda Técnica, Contratos v1 y Gates Reales

> Gate de cierre. Alcance: Constitución §4, §9, §11 y §12. Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [x] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde (`LogoutUseCaseTest`, `RetrofitRepositoriesTest`, `FT10UiSanitizationTest`)
- [x] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos (`MockDataProvider` acotado al puente transicional `FT-11..FT-26`)
- [x] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [x] `StaticQualityCheckTask` eliminado y sustituido por plugins reales `detekt` (`1.23.8`) y `ktlint` (`12.2.0`)
- [x] `android:allowBackup="false"` y `android:usesCleartextTraffic="false"` en `AndroidManifest.xml` (excepciones locales restringidas a `debug`)
- [x] `MerchantApiService` apunta a `GET /api/v1/merchants/me` (`T10.5`) y `PUT /api/v1/merchants/profile`
- [x] `POST /api/v1/auth/logout` invocado en cierre de sesión antes de limpiar almacenamiento local (`LogoutUseCase` + `SessionRepositoryImpl`)
- [x] Referencias a `MockDataProvider` en `app/src/main/java/**` acotadas exclusivamente al fallback transicional de `StoreRepositoryImpl` y `OffersRepositoryImpl` hasta `FT-11..FT-26` (`RF-11-07`, `docs/endpoints-internos.md`); 0 referencias en `presentation/`, `domain/` ni Auth/Profile/Merchant
- [x] `OtpVerificationScreen` sin `"7294"` prellenado y sin `SensusAmber`
- [x] `TermsScreen` abre `https://rodnix.com.mx/sensus/privacidad.html` y sin `SensusAmber`
- [x] `CommunityScreen` sin referencias a WhatsApp

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [x] `presentation/` no importa `data/` (`check_layers.py` PASS: `0 hallazgos, 139 archivos`)
- [x] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [x] Sin uso de `MockDataProvider` en `presentation/`, `domain/` ni Auth/Profile/Merchant; aislamiento offline documentado en `StoreRepositoryImpl`/`OffersRepositoryImpl` hasta `FT-11..FT-26`
- [x] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola (`check_secrets.py` PASS: `186 archivos`)

## UI y accesibilidad

- [x] Previews `@Preview` en pantallas del alcance sin tonos amarillos ni regresiones
- [x] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); cero `SensusAmber` en UI interna y sin emojis en pantallas FT-10
- [x] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos

## Gates automatizados

- [x] `./gradlew.bat compileDebugKotlin` verde
- [x] `./gradlew.bat compileDebugAndroidTestSources` verde (`FT10UiSanitizationTest.kt` compilado; ejecución en dispositivo/emulador `connectedDebugAndroidTest` fuera de run headless)
- [x] `./gradlew.bat detekt ktlintCheck lint` verdes (`BUILD SUCCESSFUL`, 0 hallazgos)
- [x] `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (`100` unit tests, 0 fallos; Kover `93.23% >= 80%`)
- [x] `python scripts/check_sdd.py` PASS (`0 hallazgos, 28 features`)
- [x] `python scripts/check_layers.py` PASS (`0 hallazgos, 139 archivos`)
- [x] `python scripts/check_endpoints.py` PASS (`0 hallazgos, 54 endpoints`)
- [x] `python scripts/check_secrets.py` PASS (`0 hallazgos, 186 archivos`)
- [x] `python scripts/check_coverage.py --min 80` PASS (`93.23%`)

## Cierre SDD

- [x] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [x] `Memoria.md` §6 actualizado con decisiones y evidencia
- [x] `CHECKLIST_PR.md` verificado y alineado para PR a `main`
- [ ] Tag `v1.13.0-ft10` (pendiente autorización explícita del responsable para `git commit`, `push`, PR a `main` y creación de tag Git)
