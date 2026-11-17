# CHECKLIST — FT-25 Red de Amigos por QR, Invitación y Solicitudes Pendientes

> Gate de cierre. Alcance: Pantalla P-24 (tab Amigos), Componente C-38.

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas y `## Aprobación` actualizado
- [x] `plan.md`, `tasks.md` y `docs/endpoints-internos.md` alineados con `/api/v1/friends*`

## Funcionalidad y reglas de negocio

- [x] Retirados `MockDataProvider.sampleFriends` y QR falso en `Canvas`
- [x] Generación real de código `SNS-XXXXXX` y QR ZXing escaneable
- [x] Compartir invitación (`https://rodnix.com.mx/sensus?code=...&name=...`) + App Link / Deep Link
- [x] Sección `"Solicitudes pendientes"` con aceptar (`✓`) y cancelar (`✕`) + notificación local

## Arquitectura y seguridad

- [x] `domain/` puro (sin `android.*`, Retrofit, Room ni `kotlinx.serialization`)
- [x] `presentation/` no importa `data/`
- [x] `FriendsViewModel` hereda de `MviViewModel`

## Gates automatizados

- [x] `.\gradlew.bat ktlintCheck detekt compileDebugKotlin compileDebugAndroidTestKotlin testDebugUnitTest` verde (`344` pruebas, `0` fallos)
- [ ] Prueba E2E en dos teléfonos reales tras despliegue de `sensus-back` y `sensus-landing`
