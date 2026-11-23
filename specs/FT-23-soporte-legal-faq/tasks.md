# TASKS — FT-23 Centro de Ayuda (FAQs), Legal Offline y Banner de Invitado

> 1 PR = 1 slice. Orden: Componentes -> Pantallas -> Integración -> Tests -> Gates.

## Fase A — Pantallas y Componentes

- [x] T23.1 Crear `HelpFaqScreen.kt` con acordeones expandibles offline cubriendo radio, comercios, QR y LFPDPPP — verif: compilación y expansión de una respuesta en vivo V2341.
- [x] T23.2 Conectar `"Centro de ayuda y soporte"` en `BuyerProfileScreen.kt` a `HelpFaqScreen` — verif: navegación desde Perfil en vivo V2341.
- [x] T23.3 Completar textos offline de Términos, Cancelación y Privacidad en `TermsScreen.kt` junto a enlace oficial `rodnix.com.mx` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T23.4 Crear `GuestModeBanner.kt` (`C-49`) y mostrarlo en `MainAppContainer.kt` exclusivamente cuando `isGuest == true` — verif: compilación del componente y de sus casos Compose. Sigue pendiente la ejecución en el dispositivo.

## Fase B — Pruebas y Cierre

- [x] T23.5 Verificar expansión de acordeones, términos y visibilidad del banner con `SupportScreensTest` — aprobado en vivo V2341.
- [x] T23.6 Ejecutar suite unitaria, Kover `>= 80%`, análisis estático y actualizar `Memoria.md` §6 — verificado con 80.10% de cobertura de líneas Debug y gates locales en verde.
- [ ] T23.7 Añadir al Aviso de Privacidad (recurso embebido y landing) la finalidad de personalización del feed y el registro seudonimizado de interacciones (RF-23-04) — verif: revisión de texto en `TermsScreen`

## Avance en movil_Rocio — 2026-11-22

- Se integraron el catálogo de preguntas frecuentes y el banner de invitado desde main. El centro de ayuda abre desde Perfil y permite consultar los términos y el aviso oficial.
- La FAQ refleja el radio disponible en FT-11 y evita prometer cancelación de apartados o purga inmediata. Los términos incluyen vigencia del apartado y un resumen del tratamiento de interacciones para personalizar el feed.
- Pruebas Compose para apertura de una respuesta, visibilidad del banner, acción de acceso y lectura offline del resumen.
- Pendientes: publicar la actualización del aviso en la landing, verificar el texto legal completo y ejecutar las pruebas en dispositivo.
- El aviso externo agotó el tiempo de conexión en una consulta HTTPS. Los casos instrumentados de ayuda, términos embebidos y banner de invitado pasaron en el vivo V2341.
- Validación local: 456 pruebas unitarias con 0 fallos, Kover Debug 80.05%, compilación de la app e instrumentadas, lint, Detekt, ktlint y cinco gates Python en verde.

## Cierre local en movil_Rocio — 2026-11-22

- T23.3: `TermsScreen` agrega sin conexión los derechos ARCO (LFPDPPP) y la eliminación de cuenta desde Perfil, junto al enlace oficial. `SupportScreensTest` lo comprueba; verificado en dispositivo por el equipo el 2026-11-22.
- T23.7 sigue abierta solo por la publicación del aviso en la landing.

- Verificación en dispositivo 2026-11-22: el equipo probó los cambios del cierre local y funcionan.
