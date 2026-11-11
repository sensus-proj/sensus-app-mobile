# PLAN — FT-25 Red de Amigos mediante Código QR (Bloqueada por Backend)

> Derivado de `spec.md`. Historias/Alcance: Pantalla P-24 (tab Amigos), Componente C-38 (Tesina Sensus / Excel Móvil).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Gobernanza SDD (`Borrador`).** Los endpoints `GET /api/v1/users/me/friend-qr`, `POST /api/v1/users/friends/add-by-qr` y `GET /api/v1/users/friends` no existen en `sensus-back`.
2. **Saneamiento de QR falso.** El código QR decorativo dibujado a mano en `CommunityScreen.kt` debe sustituirse por ZXing real (`QrBitmapGenerator` de `FT-13`) cuando el backend emita el token firmado, u ocultarse mientras no exista el endpoint.

## 2. UI y navegación

- Modal `FriendQrModal` (`C-38`) con tab `Mi código QR` (ZXing) y tab `Escanear amigo` (CameraX).

## 3. DI

| Binding | Implementación |
|---|---|
| `FriendsRepository` | Pendiente de spec en `sensus-back` |

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

1. Definir y aprobar spec de red de amigos en `sensus-back` (o retirar tab Amigos de `P-24`).
2. Crear `FriendsApiService`, `FriendsRepositoryImpl` y casos de uso en `domain/`.
3. Integrar `QrBitmapGenerator` (ZXing) y `QrCameraScannerPreview` en `C-38`.

## 6. Prohibiciones

- Prohibido renderizar un QR falso/decorativo en Canvas que no codifique un payload real.

## Tareas

Ver `tasks.md`.
