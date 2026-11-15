# CHECKLIST — FT-21 Slice preferencias y activación

## Implementación local (sin cierre)

- [x] DTOs y rutas alineados con backend main 01dad6e.
- [x] Dominio puro y DI dedicada sin bindings preexistentes encontrados.
- [x] Preferencias MVI sustituyen lista fake y marcado de leídas inoperante.
- [x] Proveedor real Firebase y activación explícita con permiso Android13+.
- [x] Textos en strings_notification_preferences.xml y previews por estado disponible.
- [x] Fuentes nuevas JUnit4 de API/dominio/VM y Compose.
- [x] Main integra permiso POST_NOTIFICATIONS y adapta BuyerViewModelsTest.
- [ ] Recursos oficiales de proyecto Firebase aprovisionados por equipo de despliegue.

## Validación pendiente por main

- [x] compileDebugKotlin y compileDebugAndroidTestSources verdes.
- [ ] Suite unitaria y Compose ejecutadas con éxito.
- [x] Cobertura Kover >=80% medida.
- [x] lint, detekt y ktlintCheck verdes.
- [x] Gates SDD, capas, endpoints, secretos y cobertura verdes.
- [ ] Revisión de seguridad e integración completa.
- [x] Documentación compartida y memoria conciliadas por main.

## Deuda fuera de esta slice

- [ ] Recepción FCM, canales, renovación de token y ciclo login/logout.
- [ ] Bandeja cifrada y lectura solo con contratos publicados.
- [ ] Aprobación final y CHECKLIST_PR completo; sin cierre total de FT-21.

Evidencia local: 257 tests sin fallos/errores/omisiones; Kover 82.82%. Tests Compose compilados, no ejecutados; Firebase real y validación E2E/configuración pendientes. No se declara suite remota DevSecOps completa aprobada.
