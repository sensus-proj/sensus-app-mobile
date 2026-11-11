# CHECKLIST — FT-10 Saneamiento de Deuda Técnica, Contratos v1 y Gates Reales

> Gate de cierre. Alcance: Constitución §4, §9, §11 y §12. Sin todos los ítems en `[x]` no hay merge ni cierre de slice.

## Spec y alcance

- [ ] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación` resuelta
- [ ] Historias y pantallas del alcance cubiertas por pruebas de éxito, error y borde
- [ ] `plan.md` y `tasks.md` alineados sin cambios de alcance ocultos
- [ ] Sin `TODO`, `FIXME` ni `STOPSHIP` sin issue trazado

## Funcionalidad y reglas de negocio

- [ ] `StaticQualityCheckTask` eliminado y sustituido por plugins reales `detekt` y `ktlint`
- [ ] `android:allowBackup="false"` y `android:usesCleartextTraffic="false"` en `AndroidManifest.xml`
- [ ] `MerchantApiService` apunta a `GET /api/v1/merchants/me` y `PUT /api/v1/merchants/me`
- [ ] `POST /api/v1/auth/logout` invocado en cierre de sesión antes de limpiar almacenamiento local
- [ ] Cero referencias a `MockDataProvider` en `app/src/main/java/**`
- [ ] `OtpVerificationScreen` sin `"7294"` prellenado y sin `SensusAmber`
- [ ] `TermsScreen` abre `https://rodnix.com.mx/sensus/privacidad.html`
- [ ] `CommunityScreen` sin referencias a WhatsApp

## Arquitectura y seguridad

- [ ] `domain/` puro (sin `android.*`, Retrofit, Room ni serializadores JSON)
- [ ] `presentation/` no importa `data/` (`check_layers.py` en PASS)
- [ ] ViewModels heredan de `MviViewModel<ViewState, ViewIntent, ViewEffect>`
- [ ] Sin `MockDataProvider` ni datos fake en `app/src/main/java/**`
- [ ] Logger sanitizado, sin PII, JWT ni payloads sensibles en consola

## UI y accesibilidad

- [ ] Previews `@Preview` para estados `Loading`, `Loaded`, `Empty` y `Error`
- [ ] Paleta morada de marca (`#7C3AED` / `#8B5CF6`); sin ámbar (`SensusAmber`) ni emojis
- [ ] Touch targets `>= 48x48 dp` y `contentDescription` en controles interactivos

## Gates automatizados

- [ ] `./gradlew.bat compileDebugKotlin` verde
- [ ] `./gradlew.bat compileDebugAndroidTestSources` verde
- [ ] `./gradlew.bat detekt ktlintCheck lint` verdes
- [ ] `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (`>= 80%`)
- [ ] `python scripts/check_sdd.py` PASS
- [ ] `python scripts/check_layers.py` PASS
- [ ] `python scripts/check_endpoints.py` PASS
- [ ] `python scripts/check_secrets.py` PASS
- [ ] `python scripts/check_coverage.py --min 80` PASS

## Cierre SDD

- [ ] `docs/endpoints-internos.md` y `docs/rate-matrix.md` conciliados
- [ ] `Memoria.md` §6 actualizado con decisiones y evidencia
- [ ] `CHECKLIST_PR.md` completo
- [ ] Tag `v1.13.0-ft10`
