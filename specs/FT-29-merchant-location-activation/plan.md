# PLAN — FT-29 Activación de Comercio con Ubicación Interactiva y Persistencia en Perfil

> Derivado de `spec.md`. Rector: `.specify/memory/constitution.md` §2, §3, §4, §5, §6 y §9.

## 1. Diseño

1. **Contratos de red (`POST /api/v1/merchants/activate`, `GET /api/v1/merchants/me`, `PUT /api/v1/merchants/profile`).**
   - Añadir `ActivateMerchantRequest` y `RegisterComercioResponseDto` en `MerchantDtos.kt`; incorporar `latitud: Double?` y `longitud: Double?` opcionales también en `UpdateMerchantProfileRequest`.
   - Exponer `@POST("api/v1/merchants/activate")` en `MerchantApiService.kt`.
2. **Geocodificación inversa desacoplada (Clean Architecture).**
   - Definir `ResolvedAddress` y `ReverseGeocodingRepository` en `domain/`.
   - Implementar `AndroidReverseGeocodingRepository` en `data/location/` usando `android.location.Geocoder` (API 33+ con `suspendCancellableCoroutine` y fallback `< 33` en `Dispatchers.IO`), capturando únicamente excepciones específicas (`IOException`, `IllegalArgumentException`) y mapeando dirección + código postal limpios.
3. **Casos de uso y persistencia de sesión de comercio.**
   - Crear `ActivateMerchantUseCase`, `GetMyMerchantUseCase` y `ResolveAddressFromCoordinatesUseCase`.
   - Extender `SecurePreferences` y `SessionRepositoryImpl` (`SessionState`) con `isMerchantRegistered` y `merchantStoreName`, sincronizados automáticamente en login/registro por rol `Comerciante`, en `activateMerchant` (guardando además el nuevo JWT retornado) y en `getMyMerchant()`.
4. **Presentación MVI y Mapa Interactivo.**
   - Actualizar `MerchantRegisterContract`, `MerchantRegisterViewModel`, `MerchantRegisterScreen` y crear `MerchantLocationPickerModal.kt` (excluido de Kover como modal UI puro).
   - Dibujar el badge/botón del gato curioseando sobre el icono de ubicación junto al campo dirección con `Canvas` / vector en blanco y `SensusPurple` (`#4A148C` / `#6E4DDB`), respetando Constitución §9.2 (sin amarillo/ámbar).
   - Actualizar `BuyerProfileContract`, `BuyerProfileViewModel`, `BuyerProfileScreen` y `MainAppContainer` para mantener activa la casilla de comerciante mostrando el nombre del negocio activo y abrir el modo comercio al pulsar la tarjeta o su texto.

## 2. UI y mapeos

- Formulario de comercio conservando exclusivamente los 3 campos (`Nombre comercial`, `Dirección completa del local`, `Código postal`).
- Opción compacta pegada a `Dirección` con el gato blanco/morado asomándose sobre el pin de mapa y texto `"Usar mi ubicación actual"`.
- Mapa modal `osmdroid` con pin central interactivo (reacciona a arrastre o toque en el mapa), botón de recentrado GPS, vista previa en vivo de dirección resuelta y botón `"Confirmar ubicación"`.

## 3. DI

| Binding | Implementación |
|---|---|
| `ReverseGeocodingRepository` | `AndroidReverseGeocodingRepository` en `RepositoryModule` y `SensusContainer` |
| `ActivateMerchantUseCase` / `GetMyMerchantUseCase` / `ResolveAddressFromCoordinatesUseCase` | Proveídos en `UseCaseModule` y `SensusContainer` |
| `RetrofitMerchantRepository` | Recibe `SecurePreferences` y callback `onSessionUpdated` en `SensusContainer` |

## 4. Gates

- `./gradlew.bat compileDebugKotlin lint detekt ktlintCheck testDebugUnitTest koverXmlReportDebug koverVerifyDebug compileDebugAndroidTestSources --no-daemon`
- `python scripts/check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 80`

## 5. Orden de implementación

1. Artefactos SDD (`spec.md`, `plan.md`, `tasks.md`, `checklist.md`, `specs/README.md`, `docs/endpoints-internos.md`, `docs/rate-matrix.md`).
2. Modelos/repositorios/casos de uso en `domain/`.
3. DTOs, `MerchantApiService`, `AndroidReverseGeocodingRepository`, `SecurePreferences`, `SessionRepositoryImpl`, `RetrofitRepositories` y DI.
4. `MerchantRegisterContract`/`ViewModel`/`Screen`/`MerchantLocationPickerModal`.
5. `BuyerProfileContract`/`ViewModel`/`Screen` y `MainAppContainer`.
6. Pruebas unitarias y `MockWebServer` (`>= 80%` Kover), gates y documentación en `Memoria.md`.

## 6. Prohibiciones

- Prohibido usar tonos amarillos o ámbar en UI interna o en el icono del gato (Constitución §9.2).
- Prohibido `git push` hasta autorización explícita del usuario.
- Prohibido importar `android.*`/`data.*`/`retrofit2.*` desde `domain/`.
