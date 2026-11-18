# SPEC — FT-29 Activación de Comercio con Ubicación Interactiva y Persistencia en Perfil

> Rector: `.specify/memory/constitution.md` §2, §3, §4, §5, §6 y §9. Plantilla: `specs/_template.md`.
> Alineada con backend `sensus-back` Spec `005` (`POST /api/v1/merchants/activate`), Spec `006` (`GET /api/v1/merchants/me`) y Spec `031` (`PUT /api/v1/merchants/profile`).

## 1. Contexto y objetivo

El formulario actual de activación de comercio (`MerchantRegisterScreen`) solicita los tres campos esenciales del negocio (`nombreComercial`, `direccion`, `codigoPostal`), pero `MerchantRegisterViewModel` simula el alta localmente sin consumir el contrato real `POST /api/v1/merchants/activate`, no permite capturar ni ajustar la ubicación exacta del local en un mapa interactivo desde el registro y tampoco persiste el estado activo del comercio en el perfil del usuario tras reiniciar la app.

**Objetivo:** Conservar exactamente los 3 campos base de registro del comercio (`nombreComercial`, `direccion`, `codigoPostal`) añadiendo una acción opcional junto al campo de dirección —con el logotipo del gato de la marca asomando la cabeza con curiosidad sobre un icono de ubicación, sin fondos amarillos/ámbar según §9.2— que detecta la ubicación actual por GPS, abre un mapa interactivo `osmdroid` para ajustar el pin en caso de imprecisión y autocompleta dirección, código postal y coordenadas (`latitud`/`longitud`). Adicionalmente, persistir el estado activo del comercio y su nombre comercial para que la casilla de comerciante en `BuyerProfileScreen` permanezca activa y permita ingresar directamente al modo comercio al pulsarla.

**Alcance:**
- Contrato real `POST /api/v1/merchants/activate` (y coordenadas opcionales `latitud`/`longitud` en `ActivateMerchantRequest` y `UpdateMerchantProfileRequest`) con actualización del token JWT y roles persistidos en `SecurePreferences`.
- Abstracción limpia `ReverseGeocodingRepository` en `domain/` e implementación `AndroidReverseGeocodingRepository` en `data/location/` usando `android.location.Geocoder`.
- Casos de uso `ActivateMerchantUseCase`, `GetMyMerchantUseCase` y `ResolveAddressFromCoordinatesUseCase`.
- Modal interactivo `MerchantLocationPickerModal` sobre `osmdroid` para mover/tocar el mapa, recentrar en GPS y confirmar coordenadas/dirección resuelta.
- Elemento gráfico opcional junto al campo de dirección en `MerchantRegisterScreen` con el gato curioseando en blanco y morado Sensus (`SensusPurple`) sobre pin de ubicación.
- Persistencia en `SessionState` / `SecurePreferences` y sincronización en `BuyerProfileViewModel` y `MainAppContainer` para mantener activa la casilla de comerciante en `BuyerProfileScreen` y navegar directo al comercio activo.

**Fuera de alcance:** Verificación documental del comercio, sucursales múltiples y alta de ofertas (cubierto en `FT-15`).

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-29-01 | `MerchantRegisterScreen` solicita nombre comercial, dirección, código postal, ubicación opcional interactiva (gato morado sobre pin) y foto obligatoria de la fachada del comercio (cámara con permiso runtime `CAMERA` o galería). |
| RF-29-02 | Al pulsar la opción de ubicación automática, la app solicita permiso de ubicación en tiempo de ejecución si hace falta, obtiene la ubicación actual mediante `GetUserLocationUseCase` y abre `MerchantLocationPickerModal` con un mapa interactivo `osmdroid` donde el usuario puede mover o tocar el mapa para afinar las coordenadas si la detección GPS es imprecisa. |
| RF-29-03 | Al confirmar en el mapa interactivo, `ResolveAddressFromCoordinatesUseCase` resuelve dirección y código postal y rellena automáticamente los campos del formulario conservando `latitud` y `longitud` opcionales seleccionadas (permitiendo edición manual posterior). |
| RF-29-04 | `ActivateMerchantUseCase` valida nombre comercial (`2..150`), dirección (`5..250`), código postal y rango de coordenadas opcionales (`[-90, 90]` / `[-180, 180]`), enviando `POST /api/v1/merchants/activate`; ante respuesta `201` persiste el nuevo JWT, estado de comercio activo y nombre comercial en `SecurePreferences`. |
| RF-29-05 | Si el permiso de ubicación es denegado o falla el geocoder, el usuario conserva la captura manual completa de dirección y código postal sin bloquear el registro ni lanzar fallos no controlados. |
| RF-29-06 | En `BuyerProfileScreen`, cuando el usuario ya cuenta con comercio activo (persistido localmente o sincronizado vía `GET /api/v1/merchants/me`), la casilla de comerciante permanece activa mostrando el nombre del comercio y al pulsarla (o tocar el texto del comercio) navega directamente al flujo activo de comerciante sin reabrir `MerchantRegisterScreen`. |
| RF-29-07 | En `MerchantProfileScreen`, al pulsar el ícono de la tienda se permite tomar o elegir nueva foto de fachada (`fotoFachadaUrl`), subiéndola vía `POST /api/v1/media/upload` y guardándola en `PUT /api/v1/merchants/profile`; la tarjeta `"Información del establecimiento"` omite el ícono de teléfono vacío. |

## 3. Requisitos no funcionales

- **Arquitectura Clean + MVI:** `domain/` permanece puro sin imports de Android/Retrofit/Serialization; la geocodificación inversa se abstrae con `ReverseGeocodingRepository` y `ResolvedAddress`.
- **UI/UX Constitución §9.2:** Prohibitivo el uso de amarillos/ámbar o emojis en interfaz interna; icono del gato y pin dibujados en blanco y `SensusPurple` (`#4A148C` / `#6E4DDB`) con `contentDescription` explícito.
- **Seguridad:** Token JWT renovado y bandera/nombre del comercio almacenados exclusivamente en `SecurePreferences` (EncryptedSharedPreferences); sin logs de coordenadas o dirección en `Release`.
- **Calidad:** `detekt`, `ktlintCheck`, `lint`, Kover `>= 80%` y los 5 scripts `scripts/check_*.py` en `PASS`.

## 4. Arquitectura y contratos internos

- Endpoints consumidos (registrados en `docs/endpoints-internos.md`):
  - `POST /api/v1/merchants/activate` (JWT usuario registrado -> `201 Created` con `RegisterComercioResponseDto` y JWT actualizado con rol `Comerciante`).
  - `GET /api/v1/merchants/me` (JWT `Comerciante` -> `200 OK` con `ComercioDto` / `404`).
  - `PUT /api/v1/merchants/profile` (JWT `Comerciante`, admite `latitud` y `longitud` opcionales alineados a Spec 031).

```text
domain/
  model/ResolvedAddress.kt
  repository/
    ReverseGeocodingRepository.kt
    MerchantRepository.kt                 # + activateMerchant(...) y coordenadas opcionales en update
    SessionRepository.kt                  # SessionState + isMerchantRegistered, merchantStoreName
  usecase/
    ActivateMerchantUseCase.kt
    GetMyMerchantUseCase.kt
    ResolveAddressFromCoordinatesUseCase.kt
data/
  remote/dto/MerchantDtos.kt              # ActivateMerchantRequest, RegisterComercioResponseDto
  remote/api/MerchantApiService.kt        # @POST("api/v1/merchants/activate")
  location/AndroidReverseGeocodingRepository.kt
  local/SecurePreferences.kt              # persistencia cifrada de estado y nombre de comercio
  repository/RetrofitRepositories.kt, SessionRepositoryImpl.kt
presentation/
  merchant/MerchantRegisterContract.kt, MerchantRegisterViewModel.kt, MerchantRegisterScreen.kt, MerchantLocationPickerModal.kt
  buyer/BuyerProfileContract.kt, BuyerProfileViewModel.kt, BuyerProfileScreen.kt
  main/MainAppContainer.kt
```

## 5. Criterios de aceptacion

- **Given** un usuario registrado abre `MerchantRegisterScreen` **When** pulsa el botón del gato con ubicación junto al campo dirección **Then** se solicita permiso de ubicación (si falta), se centra el mapa `osmdroid` en su posición actual y puede arrastrar o tocar el mapa para afinar el pin antes de confirmar.
- **Given** el usuario confirma coordenadas precisas o edita manualmente dirección y código postal **When** pulsa activar comercio con datos válidos **Then** se ejecuta `POST /api/v1/merchants/activate`, se actualiza la sesión cifrada y entra al panel de comerciante.
- **Given** el usuario ya activó su comercio **When** cierra y reabre la app y entra a Perfil **Then** la casilla de comerciante aparece activa con el nombre de su comercio y pulsar el texto del comercio lo lleva directo a la vista de comerciante sin pedir alta nueva.
- **Given** el comerciante apaga el switch `Tienda Abierta` en `MerchantProfileScreen` **When** regresa al perfil de usuario normal o vuelve a abrir el perfil comercial **Then** el estado `Tienda Cerrada` (`activo = false`) persiste en backend y preferencias cifradas, y el comercio junto con sus ofertas dejan de mostrarse en el mapa hasta volver a encenderlo (`activo = true`).

## 6. Plan de pruebas

- **Unitarias (`JUnit 4` + `MockK` + `Turbine`):**
  - `MerchantActivationUseCasesTest` (`ActivateMerchantUseCase`, `GetMyMerchantUseCase`, `ResolveAddressFromCoordinatesUseCase`, `GeoCoordinatePolicy`).
  - `MerchantViewModelsTest` (`MerchantRegisterViewModel` con apertura/movimiento/confirmación de mapa, reverse geocoding, fallback ante error y alta exitosa/error).
  - `BuyerViewModelsTest` (`BuyerProfileViewModel` sincronizando comercio activo y reflejando estado persistido).
  - `RetrofitRepositoriesTest` (`MockWebServer` para `POST /api/v1/merchants/activate` `201`/`400`/`409` y guardado de token/estado).
- **Calidad y Gates:** `./gradlew.bat lint detekt ktlintCheck testDebugUnitTest koverXmlReportDebug koverVerifyDebug compileDebugAndroidTestSources` y los 5 scripts `python scripts/check_*.py` en `PASS`.

## 7. Riesgos, limites y deuda

- `android.location.Geocoder` depende de conectividad y proveedores de geocodificación del dispositivo; si no responde, se conservan coordenadas seleccionadas y se mantiene captura manual.
- Las pruebas instrumentadas requieren emulador/dispositivo con Play Services / OSMDroid; el entorno CI sin dispositivo valida `compileDebugAndroidTestSources` y pruebas unitarias JVM con `MockWebServer`.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| En revision | Pendiente revisión PR en `movil_Beto` | 2026-11-13 | `158` unit tests PASS (`0` fallos), Kover `90.72%`, `detekt`/`ktlintCheck`/`lint`/`compileDebugAndroidTestSources` y 5 `check_*.py` PASS; alineado a `sensus-back` Specs 005/006/031 |
