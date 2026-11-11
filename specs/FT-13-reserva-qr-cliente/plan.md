# PLAN — FT-13 Reserva / Apartado con Código QR para Comprador

> Derivado de `spec.md`. Historias/Alcance: Pantallas P-16, P-18, P-20, P-21; Componentes C-33, C-34, C-35, C-36 (Spec 033 Backend).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Reclamación de oferta (`POST /api/v1/offers/{id}/claim`).** `ClaimOfferUseCase` bloquea invitados (`AppError.Unauthorized`), invoca el endpoint con clave de idempotencia y persiste `ReservationEntity` en SQLCipher.
2. **Generador QR real (`QrBitmapGenerator`).** Utiliza `com.google.zxing:core` para codificar el token/código firmado en una matriz QR de `512x512 px` sin depender de red.
3. **Cuenta regresiva reactiva (`C-34`).** `ActiveApartadoViewModel` calcula el tiempo restante frente a `expiresAtMillis` cada segundo y transiciona a `EXPIRED` al llegar a `00:00`.
4. **Historial de apartados (`P-21`).** `ReservationsHistoryScreen` lista reservas filtradas por `ACTIVE`, `REDEEMED`, `EXPIRED` y `CANCELLED` desde SQLCipher + API.

## 2. UI y navegación

- `ConfirmReservationSheet` (`P-18` / `C-33`) confirma unidades y tiempo límite antes del POST.
- `ActiveApartadoScreen` (`P-20`) renderiza el `ImageBitmap` ZXing real (`C-35`), código alfanumérico (`SNS-XXXX-XX`) y contador (`C-34`).
- `ReservationsHistoryScreen` (`P-21`) con tabs por estado y navegación al QR activo.
- Previews: `ConfirmSheet`, `QrActive`, `QrExpired`, `HistoryList`.

## 3. DI

| Binding | Implementación |
|---|---|
| `ReservationsApiService` | Retrofit `/api/v1/offers/{id}/claim` en `NetworkModule` |
| `ReservationsRepository` | `ReservationsRepositoryImpl` con `ReservationDao` en `RepositoryModule` |
| `QrBitmapGenerator` | Implementación ZXing en `presentation/util/` |

## 4. Gates

- `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes.
- `./gradlew.bat detekt ktlintCheck lint` verdes (0 violaciones).
- `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (cobertura `>= 80%`).
- `python scripts/check_sdd.py` PASS.
- `python scripts/check_layers.py` PASS.
- `python scripts/check_endpoints.py` PASS.
- `python scripts/check_secrets.py` PASS.
- `python scripts/check_coverage.py --min 80` PASS.

## 5. Orden de implementación

1. Crear modelo `OfferReservation`, `ReservationStatus` y `ReservationsRepository` en `domain/`.
2. Implementar `ClaimOfferUseCase`, `GetActiveReservationsUseCase` y `CancelReservationUseCase`.
3. Añadir `com.google.zxing:core` en `libs.versions.toml` e implementar `QrBitmapGenerator`.
4. Crear `ReservationEntity`, `ReservationDao` y `ReservationsApiService`.
5. Reemplazar el QR simulado en `ActiveApartadoScreen` por el QR ZXing real y crear `P-18` y `P-21`.
6. Ejecutar pruebas unitarias, SQLCipher y Compose UI.

## 6. Prohibiciones

- Prohibido dibujar códigos QR falsos/decorativos en Canvas en producción.
- Prohibido permitir crear apartados en modo invitado.

## Tareas

Ver `tasks.md`.
