# TASKS — FT-21 Preferencias y activación

## Slice autorizada 2026-11-14 — backend main 01dad6e

Registradas como pendientes antes de implementar; validación local principal completada, cierre/E2E pendientes.
Las tareas históricas de rutas no publicadas se sustituyen por este alcance ratificado.

- [x] T21.S1 Implementar GET/PUT /api/v1/users/settings con DTO exacto, dominio, repositorio y DI dedicada.
  Fuentes creadas; pendiente compileDebugKotlin y pruebas.
- [x] T21.S2 Sustituir muestras por preferencias MVI conservando firma/callbacks de NotificationsScreen.
  Fuente creada; pendiente validación Compose.
- [x] T21.S3 Crear pruebas nuevas JUnit4 de dominio, API, ViewModel y fuente Compose.
  Fuentes creadas; pendiente ejecución y cobertura >=80%.
- [x] T21.S4 Conciliar spec/plan/checklist y documentar bloqueo Firebase.
  Artefactos actualizados; pendiente revisión de integración.
- [x] T21.S5 Main: integrar networkClient.notificationsApi, adaptar BuyerViewModelsTest y ejecutar gates secuenciales.
  API propiedad observada integrada por main; manifest, tests compartidos y gates pendientes.
- [x] T21.S6 Implementar activación explícita con permiso Android13+, proveedor Firebase real y PUT device-token.
  Fuente creada; pendiente declarar POST_NOTIFICATIONS por main y verificar comportamiento en dispositivo.

## Prerrequisitos / deuda posterior (sin cierre total)

- [x] Aprovisionar recursos Android oficiales de Firebase por equipo de despliegue; `app/google-services.json` presente en local (ignorado por git, 2026-11-22). CI compila sin él (el plugin solo se aplica si el archivo existe); el release a Play necesita aportarlo.
- [x] Servicio receptor, canales, renovación y ciclo login/logout con contratos publicados (implementado en FT-39; E2E en teléfono se sigue en FT-39). Se atiende en `FT-39-push-recepcion-ubicacion` (2026-11-19); RF-21-06 cambia: el `deviceId` ahora es un UUID por instalación, no el ID de Firebase Installations.
- [ ] Bandeja local cifrada cuando exista alcance/contrato acordado; no inventar API inbox ni marcado remoto.
- [ ] Main concilia memoria y documentación compartida; ejecuta CHECKLIST_PR, seguridad y cobertura antes de merge.
## Corrección posterior al gate inicial

- [x] T21.S7 Corregir ReturnCount del mapper y previews sin suppressions; serializar intenciones MVI y revisar errores Firebase.
  Validación de detekt, ktlint, compilación y pruebas pendiente por main.
- [x] T21.S8 Bloquear invitados/sesiones inactivas antes de REST/SDK, descartar resultados al cambiar identidad y limpiar preferencias MVI; validar pruebas de seguridad por main.


## Evidencia de integración principal — 2026-11-14

257 pruebas unitarias globales sin fallos, Kover regenerado 82.82%; compileDebugKotlin, compileDebugAndroidTestSources, lint, detekt, ktlintCheck y cinco scripts Python PASS. Las frases «pendiente por main» anteriores describen el traspaso histórico y quedan resueltas para estas validaciones locales. POST_NOTIFICATIONS declarado y pruebas compartidas adaptadas. GET/PUT settings publicados confirmados con OPTIONS 405 Allow GET,PUT; device-token con GET 405 Allow PUT. Fuente Firebase SDK real con guardas de sesión y sin registros de tokens. Firebase Messaging 24.1.2 oficial compatible con Room 2.6.1; 26.0.2 produjo incompatibilidad de metadata y no se utiliza. Sin cambios de Kotlin, Room, umbrales o CI. La configuración Firebase oficial sigue ausente; no se inventa ni se declara recepción/renovación completa. Compose solo compilado.

## Avance en movil_Rocio — 2026-11-21

- El caso de uso compartido comprueba si Android permite notificaciones antes de pedir el token al SDK y después de obtenerlo, antes del PUT. La activación manual, la renovación y los reintentos quedan sujetos a la misma comprobación.
- La UI muestra el mensaje de permiso si el sistema bloquea las notificaciones y conserva las preferencias ya cargadas. Se añadieron regresiones de bloqueo previo, revocación durante la obtención del token y mapeo de error en el ViewModel.
- El archivo local app/google-services.json existe y está ignorado por Git. No se leyó ni se modificó su contenido. Aún falta verificar registro y entrega en un dispositivo con el proyecto y backend de despliegue.
- Validación local del 2026-11-22 con 456 pruebas unitarias y 0 fallos, Kover Debug 80.80%, compilación de la app e instrumentadas, lint, Detekt, ktlint y cinco gates Python en verde.
