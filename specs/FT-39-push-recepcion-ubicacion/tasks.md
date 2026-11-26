# TASKS — FT-39 Recepción de notificaciones push, ciclo de sesión y ubicación para ofertas cercanas

> Orden: Contratos/SDD -> Dominio -> Data/DI -> Presentación -> Pruebas -> Gates/Docs -> Dispositivo.
> Implementación pedida explícitamente por el usuario el 2026-11-19. Las tareas en dispositivo quedan pendientes.

- [x] T39.1 Registrar contratos y documentos SDD (`spec.md`, `plan.md`, `tasks.md`, `checklists/checklist.md`, `specs/README.md`, `docs/endpoints-internos.md`, `docs/rate-matrix.md`) — verif: `py scripts/check_sdd.py` y `py scripts/check_endpoints.py` PASS
- [x] T39.2 Dominio: `PushModels.kt`, `NotificationsLocalStore`, `NotificationPermissionChecker`, `NotificationsLocationUseCase`, `NotificationsDeviceSync`, `LogoutUseCase(deviceId)` — verif: `py scripts/check_layers.py` PASS
- [x] T39.3 Data y DI: `NotificationsInstallationStore`, `AndroidNotificationPermissionChecker`, proveedor Firebase con UUID, `notification-location` PUT/DELETE, `logout(@Query deviceId)`, 503 → reintentable, `SensusContainer`, `NotificationsModule` — verif: `./gradlew.bat compileDebugKotlin --no-daemon` PASS
- [x] T39.4 Presentación: `SensusMessagingService`, canal y `PushNotificationPresenter`, `PushLaunchParser`/`PushOpenRequests`, `PushViewModel`, `MainPushHost`, interruptor de ubicación en `NotificationsScreen`, `MainActivity`, `SensusApp`, manifiesto — verif: `compileDebugKotlin` PASS
- [x] T39.5 Pruebas unitarias nuevas y adaptadas (`PushPayloadTest`, `NotificationsLocationUseCaseTest`, `NotificationsDeviceSyncTest`, `LogoutUseCaseTest`, `NotificationsPushApiTest`, `NotificationsInstallationStoreTest`, `PushViewModelTest`, `NotificationsLocationToggleTest`, `MainPushNavigationTest`, `PushLaunchParserTest`) — verif: `./gradlew.bat testDebugUnitTest --no-daemon` (413 pruebas, 0 fallos)
- [x] T39.6 Gates estáticos y cobertura: `compileDebugAndroidTestSources lint detekt ktlintCheck`, `koverVerify`, `py scripts/check_coverage.py --min 80` — PASS, Kover 80.46 %
- [ ] T39.7 Teléfono físico: permiso con explicación previa y `PUT /users/device-token` 200 (verificar en logs del backend, no en el teléfono)
- [ ] T39.8 Teléfono físico: tocar una notificación con la app en primer plano, en segundo plano y cerrada; cada una abre su destino
- [ ] T39.9 Backend desplegado: crear una oferta cerca (llega «Nuevo {producto} cerca de ti»), apartar una oferta (llegan avisos al comprador y al comercio) y cerrar sesión (dejan de llegar)
- [ ] T39.10 `CHECKLIST_PR.md` verde y PR de la slice

- [ ] Verificar que el aviso se conserve al cerrar sesión y que la ubicación quede desactivada.
