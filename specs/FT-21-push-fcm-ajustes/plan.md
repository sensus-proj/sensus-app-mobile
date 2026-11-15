# PLAN — FT-21 Preferencias y activación

## 1. Contrato

Consumir únicamente GET/PUT api/v1/users/settings y PUT api/v1/users/device-token publicados en main 01dad6e.
No implementar rutas notifications/settings, devices, read-all ni bandeja remota.

## 2. Capas

Dominio puro: NotificationsPreferences, NotificationsDevice, repositorio, proveedor SDK abstracto y tres casos de uso.
Data: DTO exacto, NotificationsApiService, repositorio con errores sanitizados y proveedor Firebase real.
DI: NotificationsModule con @Binds para repositorio/proveedor y @Provides para API/casos de uso.
Presentación: estado MVI, borrador editable, guardado explícito, activación y resultados localizados.

## 3. Integración main

NetworkClient.notificationsApi debe usar publishingHttpClient(TIMEOUT_SECONDS), sin logging de cuerpos.
Main agrega Firebase Messaging 26.0.2 y permiso POST_NOTIFICATIONS en manifest, sin fabricar recursos Firebase.
BuyerViewModelsTest: reemplazar constructor NotificationsViewModel(storeRepo) y test CategorySelected por casos de uso
NotificationsGetSettingsUseCase, NotificationsUpdateSettingsUseCase, NotificationsRegisterDeviceUseCase y estado preferences.
No se modificaron tests compartidos, build, manifiesto, NetworkClient ni contenedor desde esta slice.

## 4. Verificación pendiente (secuencial por main)

- ./gradlew.bat compileDebugKotlin --no-daemon
- ./gradlew.bat testDebugUnitTest --tests '*NotificationPreferences*' --no-daemon
- ./gradlew.bat compileDebugAndroidTestSources --no-daemon
- ./gradlew.bat lint detekt ktlintCheck --no-daemon
- ./gradlew.bat testDebugUnitTest koverXmlReportDebug koverVerifyDebug --no-daemon
- Ejecutar NotificationPreferencesComposeTest en dispositivo/emulador y verificar cobertura >=80%.
- python scripts/check_sdd.py; python scripts/check_layers.py; python scripts/check_endpoints.py
- python scripts/check_secrets.py; python scripts/check_coverage.py --min 80

No se ejecuta Gradle por coordinación explícita con main. No se marca gate verde sin evidencia.
Revisión local realizada: diff --check de archivos propios sin errores, XML de 22 strings parseado,
fuentes propias <=160 líneas y <=160 columnas; dominio sin imports de frameworks.
Se agregó NotificationPreferencesFirebaseProviderTest para inicialización sin recursos, tareas SDK y error sanitizado.
Estas pruebas son fuentes pendientes de ejecución; no se infiere cobertura ni compatibilidad compilada.

## 5. Límites

Sin recursos Firebase oficiales no puede activarse el dispositivo; el proveedor devuelve indisponibilidad explícita.
Permiso denegado no llama al proveedor ni al backend. Sin token manual, persistencia ni logs de credenciales.
Canales, recepción, rotación y baja en logout requieren slices posteriores y contratos publicados.

## Corrección del gate inicial (validación pendiente)

Detekt reportó ReturnCount en mapSettings, previews privadas sin uso y radio literal de preview.
Se refactorizó a un único return, previews internal y PREVIEW_RADIUS_KM; sin suppressions ni cambios de gates.
Intenciones serializadas por viewModelScope en Main y lecturas de guarda desde una instantánea de estado.
NotificationPreferencesConcurrencyTest añade cargas/guardados suspendidos para rechazar operaciones duplicadas y ediciones tardías.
Revisión Firebase: CancellationException se relanza antes de IllegalStateException; IOException se traduce a Network,
FirebaseException/errores de inicialización a Generic sanitizado, y configuración ausente a indisponibilidad explícita.
Se añadieron fuentes de pruebas para estas rutas. Compilación, detekt, ktlint y ejecución siguen pendientes por main.

## Revisión de seguridad de sesión (validación pendiente)

Los tres casos de uso reciben SessionRepository obligatorio desde NotificationsModule, sin sesión predeterminada.
Se exige sesión activa, registrada y token no vacío antes de GET/PUT o adquisición SDK privada.
Se verifica la identidad de sesión (token y cuenta) después del resultado y antes de registrar credenciales SDK.
El ViewModel observa cambios de identidad sin exponer tokens y limpia preferencias/borrador al cambiar la sesión.
Constructor de ViewModel conservado con tres casos de uso. Fuentes de pruebas propias adaptadas y
NotificationPreferencesSessionTest agregado para invitados, logout y cambio de cuenta durante operaciones.
No se ejecutó Gradle; validación de compilación, pruebas y gates pendiente por main.
