# SPEC — FT-13 Reserva / Apartado con Folio y Código QR Real (Cliente)

> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6 y §9. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **Sensus_Movil_Componentes_y_CRUD.xlsx** (hoja *Reservas*, Pantallas **P-16, P-18, P-20, P-21**; Componentes **C-26, C-32, C-33**); **Catalogo_Endpoints_AWS.xlsx**.
> Backend ref: Spec `033-apartado-con-qr`.

## 1. Contexto y objetivo

Actualmente `CartRepositoryImpl` y `ActiveApartadoScreen` generan apartados únicamente en memoria local (`ApartadoOrder`), dibujando un QR simulado en `Canvas` (`QrCodeGraphic`) con temporizador estático local (`44:58`), sin pantalla real de listado `Mis reservas` (`P-21`) ni sincronización con el servidor ni devolución atómica de stock.

**Objetivo:** conectar el flujo de apartado del comprador con `POST /api/v1/offers/{id}/claim`, renderizar un código QR estándar ISO/IEC 18004 escaneable con el folio/código firmado del servidor, persistir reservas activas en Room + SQLCipher para consulta offline en tienda (`P-18`, `P-21`) y soportar el cálculo de expiración basado en `expiresAt` devuelto por el backend.

**Alcance:** `ClaimsRepository`, `CreateClaimUseCase`, `GetMyClaimsUseCase`, entidad Room `ClaimEntity` / `ClaimDao`, generación de bitmap QR real (ZXing `core`), pantallas `ActiveApartadoScreen` (`P-16`/`P-18`), `MyClaimsScreen` (`P-21`) y confirmación de cancelación (`P-20`, sujeta a contrato backend).
**Fuera de alcance:** escaneo y canje por parte del vendedor (FT-14), pagos en línea, envío a domicilio.

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-13-01 | Al confirmar apartado, enviar `POST /api/v1/offers/{id}/claim` con token JWT válido (bloqueado en invitado mostrando `GuestPromptDialog`). |
| RF-13-02 | Ante respuesta `201 Created`, persistir en `ClaimDao` (Room + SQLCipher) el `claimId`, `offerId`, `code`/folio, contenido QR, `expiresAt` UTC y estado (`ACTIVE`). |
| RF-13-03 | Ante `409 Conflict` (stock agotado concurrente o límite de apartado activo alcanzado), mostrar mensaje claro al comprador sin crear orden falsa y refrescar el stock local. |
| RF-13-04 | Renderizar un código QR real legible por lectores estándar usando el código/payload emitido por el backend y mostrar el folio corto debajo para captura manual (`C-32`). |
| RF-13-05 | Calcular la cuenta regresiva en pantalla a partir de `expiresAt` del servidor menos el reloj actual (`Clock`), mostrando estado `Vencida` al llegar a cero. |
| RF-13-06 | Pantalla `Mis reservas` (`P-21`, `C-33`) filtrable por estado (`Activa`, `Canjeada`, `Cancelada`, `Vencida`), alimentada desde `ClaimDao` y sincronizada cuando el backend exponga `GET /api/v1/offers/claims` *(contrato por confirmar)*. |
| RF-13-07 | Cancelación (`P-20`): flujo de confirmación en UI conectado a `PATCH /api/v1/offers/claims/{id}/cancel` cuando el backend habilite cancelación manual *(actualmente fuera de alcance v1 en backend Spec 033)*. |

## 3. Requisitos no funcionales

- **Offline:** la tarjeta/QR de una reserva ya confirmada (`ClaimEntity`) DEBE abrirse sin internet al llegar al mostrador de la tienda.
- **Seguridad:** prohibido apartar en modo invitado; el folio/token de canje solo se almacena cifrado con SQLCipher.
- **Concurrencia:** botón de apartar deshabilitado durante la petición para evitar doble envío.

## 4. Arquitectura y contratos internos

```text
domain/
  model/StoreModels.kt          # ApartadoOrder -> ClaimRecord (claimId, folio, qrPayload, expiresAtEpochMs, status)
  repository/ClaimsRepository.kt
  usecase/CreateClaimUseCase.kt, GetMyClaimsUseCase.kt, CancelClaimUseCase.kt
data/
  local/db/entity/ClaimEntity.java, dao/ClaimDao.java
  remote/api/ClaimsApiService.kt # POST /api/v1/offers/{id}/claim
  remote/dto/ClaimDtos.kt
  repository/ClaimsRepositoryImpl.kt
presentation/buyer/
  ActiveApartadoScreen.kt       # P-16 / P-18 con QR real y timer server-driven
  MyClaimsScreen.kt, MyClaimsViewModel.kt, MyClaimsContract.kt  # P-21
presentation/common/
  QrCodeBitmapGenerator.kt      # Generador puro vía ZXing core
```

## 5. Criterios de aceptacion

- **Given** un comprador autenticado y oferta con stock **When** pulsa Apartar **Then** recibe `201` de `POST /api/v1/offers/{id}/claim`, guarda en `ClaimDao` y muestra el QR real del folio.
- **Given** un conflicto `409` en concurrencia **When** otro usuario tomó la última unidad **Then** muestra aviso de stock insuficiente sin guardar reserva falsa.
- **Given** el usuario entra al local sin datos móviles **When** abre `Mis reservas` **Then** visualiza su código QR y folio cacheados en SQLCipher.
- **Given** un usuario en modo invitado **When** pulsa Apartar **Then** no llama al API y abre `GuestPromptDialog`.

## 6. Plan de pruebas

- **Unitarias:** `CreateClaimUseCaseTest`, `QrCodeBitmapGeneratorTest` (generación no nula para folio válido y rechazo de string vacío), cálculo de temporizador con `Clock` inyectable.
- **Integración (`MockWebServer` + Room SQLCipher):** `201 Created`, `409 Conflict`, persistencia y consulta en `ClaimDao`.
- **UI (Compose Test Rule):** pantalla `ActiveApartadoScreen` con QR, temporizador desde `expiresAt` y pantalla `MyClaimsScreen` con filtros por estado.

## 7. Riesgos, limites y deuda

- **Brecha detectada entre Drive y Backend Spec 033:** el Drive plantea reserva multi-unidad (carrito), expiración de 45 min y cancelación manual (`P-20`); la Spec 033 del backend fija 1 unidad por oferta/usuario, 24 horas y excluye cancelación manual y carrito multi-oferta en v1. `FT-13` se implementa sobre contrato de unidad por oferta y deja preparado el soporte multi-ítem/cancelación como pendiente de acuerdo backend (D-7).
- Requiere añadir `com.google.zxing:core` a `gradle/libs.versions.toml` (enmienda menor a `constitution.md` §3, D-2).

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
