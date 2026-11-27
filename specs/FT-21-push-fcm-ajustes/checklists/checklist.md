# CHECKLIST — FT-21 Slice preferencias y activación

## Implementación local (sin cierre)

- [x] DTOs y rutas alineados con backend main 01dad6e.
- [x] Dominio puro y DI dedicada sin bindings preexistentes encontrados.
- [x] Preferencias MVI sustituyen lista fake y marcado de leídas inoperante.
- [x] Proveedor real Firebase y activación explícita con permiso Android13+.
- [x] Textos en strings_notification_preferences.xml y previews por estado disponible.
- [x] Fuentes nuevas JUnit4 de API/dominio/VM y Compose.
- [x] Main integra permiso POST_NOTIFICATIONS y adapta BuyerViewModelsTest.
- [ ] Recursos oficiales de proyecto Firebase aprovisionados por equipo de despliegue (acción del usuario: aportar credenciales/proyecto Firebase oficial para release en CI; ver plan cierre móvil §5.Z-1 / §5.Z-4).

## Validación pendiente por main

- [x] compileDebugKotlin y compileDebugAndroidTestSources verdes.
- [x] Suite unitaria y Compose ejecutadas con éxito (`NotificationPreferences*`, `Notifications*`, `Push*` en verde dentro de las `549` unitarias y Compose compilado; ejecución física en dispositivo pasa a `§5.Z-3`).
- [x] Cobertura Kover >=80% medida.
- [x] lint, detekt y ktlintCheck verdes.
- [x] Gates SDD, capas, endpoints, secretos y cobertura verdes.
- [ ] Revisión de seguridad e integración completa (`check_secrets.py` PASS; falta (acción del usuario: revisar escáneres remotos sobre Firebase/manifest; ver plan cierre móvil §5.Z-4)).
- [x] Documentación compartida y memoria conciliadas por main.

## Deuda fuera de esta slice

- [ ] Recepción FCM, canales, renovación de token y ciclo login/logout (implementado en código en `FT-39`; falta (acción del usuario: validar recepción real en teléfono físico con backend desplegado; ver plan cierre móvil §5.Z-3)).
- [ ] Bandeja cifrada y lectura solo con contratos publicados — fuera de alcance (sin contrato backend; decisión del responsable 2026-11-26).
- [ ] Aprobación final y CHECKLIST_PR completo (acción del usuario: aprobar PR y completar `CHECKLIST_PR.md`; ver plan cierre móvil §5.Z-5).

Evidencia local: 257 tests sin fallos/errores/omisiones; Kover 82.82%. Tests Compose compilados, no ejecutados; Firebase real y validación E2E/configuración pendientes. No se declara suite remota DevSecOps completa aprobada.
