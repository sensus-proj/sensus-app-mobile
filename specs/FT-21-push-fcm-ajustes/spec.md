# SPEC — FT-21 Preferencias y activación de notificaciones

> Rector: `.specify/memory/constitution.md`. Slice local autorizada 2026-11-14; backend main `01dad6e`.

## 1. Contexto y objetivo

Sustituir las muestras de NotificationsScreen por preferencias reales y activación explícita del dispositivo.
Conservar la firma pública y los callbacks de navegación. No presentar una bandeja inexistente como remota.

## 2. Requisitos funcionales

- RF-21-01: GET `/api/v1/users/settings` carga pushEnabled, offerAlerts, socialAlerts y radiusKm.
- RF-21-02: PUT de la misma ruta envía los cuatro valores y refleja la respuesta autoritativa.
- RF-21-03: Validar radio entero entre 1 y 50 km. Error de guardado conserva borrador sin afirmar persistencia.
- RF-21-04: Activar solicita POST_NOTIFICATIONS en API 33+ antes de obtener credenciales SDK.
- RF-21-05: FirebaseApp.initializeApp(context) usa recursos existentes; null significa configuración no disponible.
- RF-21-06: Token de Firebase Messaging y UUID por instalación alimentan PUT `/api/v1/users/device-token`, de acuerdo con FT-39.
- RF-21-07: Si Android bloquea las notificaciones o falta configuración de Firebase, no se registra el dispositivo. La activación, la renovación y los reintentos comprueban el permiso antes del SDK y antes del PUT.

## 3. Requisitos no funcionales

Clean + MVI, Hilt, dominio puro, JUnit4. Sin persistencia en texto plano ni logs de token/ID/payload.
Los cambios son locales; main integra y ejecuta gates sin Gradle concurrente. Sin APK/AAB, commit ni push.

## 4. Arquitectura y contratos internos

NotificationsApiService y DTO serializables en data; NotificationsRepository y NotificationsPushDeviceProvider en dominio.
Casos de uso puros provistos por NotificationsModule. ViewModel con estado, intenciones y efectos explícitos.
GET settings devuelve userId, pushEnabled, offerAlerts, socialAlerts, radiusKm, createdAtUtc, updatedAtUtc nullable.
PUT settings acepta esos cuatro campos editables nullable; la UI envía la instantánea completa.
PUT device-token requiere deviceId (1..100 caracteres), platform exactamente Android/iOS/Web, token de al menos 20 caracteres.
Respuesta: deviceId, platform, token, isActive, registeredAtUtc, message. Solo isActive=true confirma registro.
No hay GET de bandeja ni marcado de leídas implementado. GET device-token no se consume (405 Allow PUT).

## 5. Criterios de aceptación

- Carga real o error explícito sin valores predeterminados presentados como remotos.
- Guardado confirmado exclusivamente mediante respuesta válida; reintento conserva borrador.
- Activación usa permiso y SDK real; recursos ausentes muestran requisito de configuración.
- Sin lista de muestras ni control inoperante de marcado de leídas.

## 6. Plan de pruebas

NotificationPreferencesUseCasesTest, NotificationPreferencesApiTest, NotificationPreferencesViewModelTest y
NotificationPreferencesComposeTest cubren validación, contrato HTTP, estados y controles Compose.
Ejecución y cobertura >=80% pendientes por main; no se declara cierre ni pruebas verdes.

## 7. Riesgos, límites y deuda

No se encontraron recursos google-services/Firebase configurados en el árbol inspeccionado: se necesita aprovisionar
el proyecto Firebase y recursos Android oficiales fuera de esta slice. No se fabrica configuración.
SDK Firebase Messaging 26.0.2 agregado por main. POST_NOTIFICATIONS faltaba en manifest durante inspección:
main debe declararlo; sin declaración no puede concederse el permiso en API33+.
Registro no asegura entrega push: servicio receptor, canales, renovación de token, logout y bandeja quedan pendientes.
Preferencias se mantienen en memoria; no se añade almacenamiento ni sincronización offline en esta slice.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Slice autorizada, integración pendiente | Usuario | 2026-11-14 | Contratos backend 01dad6e; ejecución de gates pendiente |
