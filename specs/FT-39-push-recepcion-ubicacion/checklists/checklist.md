# CHECKLIST — FT-39 Recepción de notificaciones push, ciclo de sesión y ubicación para ofertas cercanas

> Gate de cierre. Alcance: Constitución §2, §3, §4, §5, §6 y §9.

## Spec y trazabilidad

- [x] `spec.md` con 7 secciones canónicas + `## Aprobación`
- [x] `PUT`/`DELETE /api/v1/users/notification-location` y `logout?deviceId=` en `docs/endpoints-internos.md` y `docs/rate-matrix.md`
- [x] Deuda «servicio receptor, canales, renovación y ciclo login/logout» de FT-21 enlazada a esta slice

## Arquitectura, UI y seguridad

- [x] `domain/` puro (`check_layers.py` PASS)
- [x] Token FCM y JWT fuera de logs; JWT solo en `SecurePreferences`
- [x] `deviceId` = UUID por instalación que sobrevive a `clearAll()`; nunca el token
- [x] Notificación sin datos del apartado ni QR; extras limitados a `type`, `offerId`, `claimId`, `requestId`
- [x] Explicación previa antes de pedir `POST_NOTIFICATIONS` y ubicación; consentimiento explícito para la ubicación
- [x] Invitados sin permisos de push ni llamadas a device-token o notification-location
- [x] Icono vectorial monocromo, sin emojis ni amarillos

## Calidad y gates

- [x] `./gradlew.bat compileDebugKotlin --no-daemon` PASS
- [x] `./gradlew.bat testDebugUnitTest --no-daemon` PASS (413 pruebas, 0 fallos)
- [x] `./gradlew.bat compileDebugAndroidTestSources lint detekt ktlintCheck --no-daemon` PASS
- [x] Kover ≥ 80 % (`koverVerify` y `py scripts/check_coverage.py --min 80`) — 80.46 %
- [x] Los 5 `scripts/check_*.py` PASS (incluye `check_coverage.py --min 80`)

## Dispositivo

- [ ] Permiso y registro 200 en teléfono físico (acción del usuario: validar permiso y `PUT /users/device-token` en dispositivo físico; ver plan cierre móvil §5.Z-3)
- [ ] Apertura en primer plano, en segundo plano y con la app cerrada (acción del usuario: validar recepción FCM en teléfono físico; ver plan cierre móvil §5.Z-3)
- [ ] Oferta cercana, apartado (comprador y comercio) y logout verificados con el backend desplegado (acción del usuario: validar E2E con backend AWS y teléfono físico; ver plan cierre móvil §5.Z-3)

- [ ] Comprobar en el dispositivo que cerrar sesión y volver a entrar no repite el aviso de ubicación (acción del usuario: comprobar en dispositivo físico; ver plan cierre móvil §5.Z-3).
