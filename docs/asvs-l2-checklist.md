# asvs-l2-checklist.md — vivo

> Baseline aplicable a Sensus Mobile (constitucion §4 y §7). Alineado a FT-00..FT-08.
> SAST: CodeQL bloqueante en CI; Detekt/Ktlint/Lint locales.
> Estado: `[ ]` pendiente, `[~]` parcial, `[x]` cumplido.

## V1 — Arquitectura, diseno y amenazas

- [~] Capas Clean con dominio puro; sin I/O en `domain/` (gate `scripts/check_layers.py`).
- [ ] Modelo de amenazas documentado por feature critica (FT-04, FT-08).
- [ ] `specs/FT-XX/` con 7 secciones y Aprobacion (gate `scripts/check_sdd.py`).

## V2 — Autenticacion

- [ ] Login y registro contra `POST /api/auth/login` y `POST /api/auth/register` (FT-04).
- [ ] Politica de contrasena: 8+, mayuscula, minuscula, digito y simbolo (FT-04).
- [ ] Mensajes genericos (`Credenciales invalidas`); sin enumeracion de usuarios (FT-04, FT-05).
- [ ] Recuperacion con token de 15 min y un solo uso (FT-05).
- [ ] Registro sin documentos oficiales; rol `CONSUMER`/`MERCHANT` (FT-04).
- [ ] Segundo factor delegado al backend, nunca al cliente.

## V3 — Sesion

- [ ] Expiracion server-driven (`expiresAtMillis`), sin 24 h hardcodeadas (FT-04).
- [ ] Token solo en `EncryptedSharedPreferences` (FT-02).
- [ ] `sessionFlow` reactivo; el arranque decide la ruta inicial (FT-04).
- [ ] `clearSession()` en logout, expiracion y eliminacion de cuenta (FT-02, FT-08).
- [ ] Token de recuperacion descartado al primer uso y al salir de la pantalla (FT-05).

## V4 — Control de acceso

- [ ] Denegar por defecto; sin ramas abiertas.
- [ ] Separacion de roles validada en backend, no solo en UI (FT-04).
- [ ] Modo invitado sin acceso a favoritos, comunidad, publicacion ni rutas de comercio (FT-07).
- [ ] Solo el dueno edita su perfil (FT-06).

## V5 — Validacion de entrada

- [ ] Validacion de formularios antes de enviar (FT-04, FT-06).
- [ ] Validacion de coordenadas en rango y sin truncar (FT-06).
- [ ] Moderacion previa al guardado (texto e imagen).

## V6 — Criptografia

- [ ] SQLCipher AES-256 para SQLite (FT-02).
- [ ] Passphrase de 256 bits desde Android Keystore (FT-02).
- [ ] `allowBackup=false` (hoy `true` en `AndroidManifest.xml`; se corrige en FT-02).
- [ ] Sin llaves literales en codigo ni `BuildConfig`.

## V7 — Errores y logs

- [ ] `Result`/`AppError` uniforme; sin stack traces al usuario (FT-01).
- [ ] `Logger` sanitizado + `CorrelationId`, sin PII ni tokens (FT-01).
- [ ] Mensaje de red generico (`No logramos conectarnos`).
- [ ] Token de recuperacion nunca en logs ni en `ViewState` (FT-05).

## V8 — Proteccion de datos

- [ ] Retencion definida por tipo de dato.
- [ ] Eliminacion de cuenta purga datos remotos y locales (FT-08).
- [ ] Tras la baja no quedan tokens, DB SQLCipher ni cache (FT-08).
- [ ] Aviso de privacidad enlazado: `https://rodnix.com.mx/sensus/privacidad.html`.

## V9 — Comunicaciones

- [x] TLS 1.3 obligatorio; `usesCleartextTraffic=false` (`network_security_config.xml`, FT-01/FT-43).
- [x] Certificate pinning en `execute-api.us-east-2.amazonaws.com` (`includeSubdomains="true"`, `pin-set expiration="2027-11-26"`, Amazon Root CA 1..4 + `SFSRootCAG2`; `NetworkPinningTest` + `NetworkSecurityConfigTest`, `FT-43` / `G-07`).
- [ ] Timeouts y reintentos segun `docs/rate-matrix.md` (FT-01).

## V10 — Configuracion maliciosa

- [x] `debuggable=false` en release (valor por defecto de AGP).
- [x] `allowBackup=false` (`AndroidManifest.xml`, FT-27).
- [x] `minifyEnabled` + `shrinkResources` en release.
- [x] `network_security_config` explicito, `usesCleartextTraffic=false` y `FLAG_SECURE` transversal con ref-counting (`SecureScreenEffect` + `DeviceAuthenticator`, `FT-43` / `G-09`).
- [ ] Permisos runtime de ubicacion (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) solo cuando se necesiten (FT-08).
- [ ] Sin ubicacion en segundo plano (FT-08).
- [ ] Permiso runtime `CAMERA` (con `required="false"`) solicitado solo al abrir escaner QR y sin subir video/frames (FT-14).
- [ ] Permiso runtime `POST_NOTIFICATIONS` en Android 13+ sin bloquear app si se niega (FT-21).
- [x] `HttpLoggingInterceptor` en nivel `NONE` para compilaciones Release (FT-27); `BASIC` con cabeceras redactadas en debug. Prueba: `NetworkLoggingSecurityTest`.
- [ ] Validacion de subida multipart (`<= 4.5 MB`, JPEG/PNG/WebP) y moderacion estricta `fail-closed` antes de Cloudinary (FT-15).

## V11 — Logica de negocio

- [ ] Reglas criticas validadas en backend, no solo en cliente (FT-04..FT-08).
- [ ] Operaciones sensibles idempotentes: baja de cuenta, recuperacion (FT-05, FT-08).
- [ ] Filtro de invitado `radiusMeters = 2000` validado en backend (FT-07).
- [ ] Limites y cuotas definidos por feature en su spec y en `docs/rate-matrix.md`.

## V13 — API y consumo

- [ ] Contract-first con OpenAPI; DTOs con `@SerialName` (FT-04..FT-08).
- [ ] Endpoints declarados en `docs/endpoints-internos.md` antes de implementarse.
- [ ] Gate `python scripts/check_endpoints.py` PASS.
- [ ] Timeouts centralizados y documentados en `docs/rate-matrix.md`.

## V14 — Configuracion de build

- [ ] Dependencias con version catalog y BOM; sin versiones sueltas (FT-00).
- [ ] Dependabot activo.
- [ ] `keys.properties` gitignored; `SIGNING_*` solo en CI.
- [ ] Gate `python scripts/check_secrets.py` PASS.

### Evidencia acotada FT-15 / FT-18 — 2026-11-13

- Moderación de imágenes fail-closed: sin cliente, aprobación explícita o respuesta válida, se rechaza la publicación.
- Un rechazo 400/422 de publicación no crea contenido local ni dispara navegación de éxito.
- Validación de precios finitos, stock positivo y vencimiento futuro en dominio.
- Nuevos clientes de publicación sin registro de cuerpos HTTP; autenticación compartida conservada.
- Caché de lectura del feed dentro de la base SQLCipher existente; migración 2 a 3 sin DROP/DELETE de datos existentes.
- Pendiente: validación de moderación y cuotas en backend real, y ejecución instrumentada en dispositivo.

### Evidencia Bloque C — 2026-11-26 (`FT-43`: `G-07`, `G-09`, `G-04`)

- **G-07 (Pinning):** `network_security_config.xml` fija las 4 raíces Amazon Root CA y `SFSRootCAG2` (`expiration="2027-11-26"`) para `execute-api.us-east-2.amazonaws.com`, validado contra TLSv1.3 de `zqqdwovx81.execute-api.us-east-2.amazonaws.com:443` y pruebas `NetworkPinningTest` + `NetworkSecurityConfigTest`.
- **G-09 (`FLAG_SECURE` + Biometría/Credencial):** `SecureScreenEffect` + `WindowSecureFlagRegistry` ref-counted aplicado en pantallas sensibles; `MainActivity : FragmentActivity` y `DeviceAuthenticator` (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`) exigidos antes de revelar QR activo y confirmar eliminación de cuenta (`SecureScreenEffectTest`, `DeviceAuthenticatorTest`, `DeviceAuthUiTest`).
- **G-04 (SBOM + Provenance):** `org.cyclonedx.bom:3.6.1` genera `app/build/reports/bom.json` (`CycloneDX 1.6`, `199` componentes) y `ci.yml` / `release-playstore.yml` ejecutan carga de SBOM y `actions/attest-build-provenance@4d101475d8b20a2381f78447822ac1eab6504dd8`.
- **Pendientes en Apéndice F:** `G-08` (Play Integrity API) queda abierta hasta aprovisionamiento en Google Cloud / Play Console / SSM (`plan §3`).
