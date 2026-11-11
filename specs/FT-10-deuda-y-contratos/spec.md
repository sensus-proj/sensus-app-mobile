# SPEC — FT-10 Saneamiento de Deuda Técnica y Alineación de Contratos

> Rector: `.specify/memory/constitution.md` §2, §3, §4, §6, §9 y §11. Plantilla: `.specify/templates/spec-template.md`.
> Origen: Auditoría móvil vs. Drive (`Sensus_Movil_Componentes_y_CRUD.xlsx`, `Catalogo_Endpoints_AWS.xlsx`) y `sensus-back`.

## 1. Contexto y objetivo

Las features `FT-00` a `FT-09` cerraron la cimentación y el Sprint 1, pero la auditoría contra el código real y los documentos del Drive detectó deuda técnica bloqueante:
1. `app/build.gradle.kts` ejecuta una tarea simulada (`StaticQualityCheckTask`) para `detekt` y `ktlintCheck` en lugar de los analizadores reales, lo que contraviene la constitución §4.7.
2. `AndroidManifest.xml` mantiene `android:allowBackup="true"` y `usesCleartextTraffic="true"` global, en contradicción con `CHECKLIST_PR.md` y OWASP Mobile Top 10.
3. `MerchantApiService` usa la ruta mixta `GET /api/v1/comercios/me` en vez de `GET /api/v1/merchants/me` (spec 006 del backend).
4. `docs/endpoints-internos.md` sigue listando rutas sin `/v1` mientras Retrofit ya consume `/api/v1/...`, y falta conectar `POST /api/v1/auth/logout` (spec 004 del backend) en el cierre de sesión explícito.
5. `OtpVerificationScreen` y `TermsScreen` importan `SensusAmber` (prohibido en UI interna), `OtpVerificationScreen` mantiene código OTP fijo (`"7294"`), `TermsScreen` carece de enlace al aviso oficial (`https://rodnix.com.mx/sensus/privacidad.html`), `CommunityScreen` ofrece invitación por WhatsApp (excluida en `Sensus_Movil_Componentes_y_CRUD.xlsx`), `NavigationKeys.kt` solo declara `Main` usando transición manual por `Crossfade`, y `Componentes_Movil_Sensus.xlsx` en raíz está desactualizado respecto al Drive.

**Objetivo:** sanear la deuda técnica, seguridad, Design System, navegación Navigation 3 y contratos `/api/v1` en inglés antes de implementar las olas del marketplace.

**Alcance:** plugins reales de Detekt y Ktlint, endurecimiento de `AndroidManifest.xml`, conexión real de `POST /api/v1/auth/logout`, alineación de rutas `/api/v1`, limpieza de UI legacy (`SensusAmber`, OTP fijo, WhatsApp), enlace al aviso de privacidad, rutas `@Serializable` en Navigation 3, conciliación documental de `FT-07` y actualización de docs vivos.
**Fuera de alcance:** nuevas pantallas de negocio de ofertas/social (FT-11..FT-26).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-10-01 | Reemplazar `StaticQualityCheckTask` en `app/build.gradle.kts` por los plugins oficiales de Detekt y Ktlint configurados contra `detekt.yml` y `.editorconfig`. |
| RF-10-02 | Establecer `android:allowBackup="false"` en `AndroidManifest.xml` y restringir `usesCleartextTraffic` en release a tráfico HTTPS TLS 1.2+. |
| RF-10-03 | Unificar todos los servicios Retrofit bajo prefijo `/api/v1/` en inglés: corregir `GET /api/v1/comercios/me` a `GET /api/v1/merchants/me` y auditar `POST /api/v1/auth/guest`, `POST /api/v1/auth/reset-password`, `POST /api/v1/users/photo-signature` y `POST /api/v1/media/moderate`. |
| RF-10-04 | Conectar `SessionRepository` / `AuthRepository` al endpoint `POST /api/v1/auth/logout` en el cierre de sesión explícito, limpiando token y Room + SQLCipher solo tras intentar invalidación remota. |
| RF-10-05 | Retirar `SensusAmber` de `OtpVerificationScreen` y `TermsScreen`, eliminar el OTP fijo `"7294"` delegando validación al flujo backend o deshabilitando la pantalla si no aplica en v1, y agregar enlace clicable a `https://rodnix.com.mx/sensus/privacidad.html` en `TermsScreen`. |
| RF-10-06 | Retirar la invitación y texto de compartir por WhatsApp en `CommunityScreen` conforme a `Sensus_Movil_Componentes_y_CRUD.xlsx` (hoja Índice). |
| RF-10-07 | Migrar las rutas globales de `AppScreen` a claves `@Serializable` `NavKey` en `NavigationKeys.kt` con `NavDisplay` / stack de Navigation 3 centralizado. |
| RF-10-08 | Conciliar la spec de `FT-07` (documentando la reutilización real de `BuyerMapScreen`/`BuyerHomeScreen` + `GuestRadiusPolicy`) y sincronizar el inventario local de componentes con el Excel del Drive. |

## 3. Requisitos no funcionales

- **Seguridad:** cero secretos ni llaves embebidas, `allowBackup="false"`, token revocado vía `POST /api/v1/auth/logout` antes de purgar `SecurePreferences` y `SensusDatabase`.
- **Calidad estática:** `detekt` y `ktlintCheck` reales ejecutando reglas de complejidad (`<= 10` por método, `<= 15` por clase) y longitud (`max_line_length = 160`) sin falsos positivos ni stubs.
- **Design System:** cero referencias a amarillo/ámbar en UI interna; iconos 100 % vectoriales con `contentDescription`.
- **Cobertura:** Kover `>= 80%` bloqueante tras los cambios.

## 4. Arquitectura y contratos internos

```text
NavigationKeys.kt             # Splash, Onboarding, Login, Register, ForgotPassword, Terms, Main (NavKey)
domain/repository/
  AuthRepository.kt           # suspend fun logout(): Result<Unit>
domain/usecase/
  LogoutUseCase.kt            # Invoca AuthRepository.logout() y purga SessionRepository
data/remote/api/
  AuthApiService.kt           # POST /api/v1/auth/logout + contratos auditados
  MerchantApiService.kt       # GET /api/v1/merchants/me
docs/endpoints-internos.md    # Tabla canonica alineada a /api/v1
```

## 5. Criterios de aceptacion

- **Given** el comando `./gradlew.bat detekt ktlintCheck` **When** se ejecuta en local o CI **Then** analiza los archivos `.kt` con los plugins oficiales y falla si se inyecta una violación real de formato o complejidad.
- **Given** un usuario autenticado **When** pulsa Cerrar sesión **Then** se llama a `POST /api/v1/auth/logout`, se limpia `SecurePreferences` y se purga `SensusDatabase`.
- **Given** `MerchantApiService` **When** consulta el comercio del usuario **Then** envía `GET /api/v1/merchants/me`.
- **Given** `TermsScreen` **When** el usuario toca el enlace de Aviso de Privacidad **Then** abre `https://rodnix.com.mx/sensus/privacidad.html`.
- **Given** `AndroidManifest.xml` **When** se inspecciona **Then** contiene `android:allowBackup="false"`.

## 6. Plan de pruebas

- **Unitarias:** `LogoutUseCaseTest` (éxito, error de red con degradación segura local), validación de `PasswordPolicy` y mapeos.
- **Integración (`MockWebServer`):** verificación de headers `Authorization` y ruta `POST /api/v1/auth/logout` y `GET /api/v1/merchants/me`.
- **UI (Compose Test Rule):** verificar ausencia de texto WhatsApp en `CommunityScreen`, enlace de privacidad en `TermsScreen` y ausencia de código OTP prellenado en `OtpVerificationScreen`.
- **Gates:** los 5 scripts Python en PASS + `detekt` + `ktlintCheck` + `lint` + Kover `>= 80%`.

## 7. Riesgos, limites y deuda

- Si el backend de AWS aún no expone `POST /api/v1/auth/logout` o usa rutas divergentes en `guest`/`moderate`, el cliente manejará respuestas `404`/`501` de logout sin impedir el cierre local de sesión mientras el backend publica el contrato definitivo.
- Habilitar los plugins reales de Detekt/Ktlint puede revelar violaciones preexistentes en pantallas grandes (e.g. `MainAppContainer`, `BuyerProfileScreen`); se resolverán dentro de `FT-10` sin relajar umbrales.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| En revisión (código y gates PASS; pendiente autorización Git/PR/tag `v1.13.0-ft10`) | Rodrigo (pendiente aprobación PR a `main`) | 2026-11-11 | `detekt`, `ktlintCheck`, `lint`, `compileDebugKotlin`, `compileDebugAndroidTestSources`, 100 tests unitarios (0 fallos), Kover `93.23%` y 5 `scripts/check_*.py` en PASS |
