# TASKS — FT-13-reserva-qr-cliente

- [x] Materializar DTOs exactos y POST sin body con HTTP 200.
- [x] Implementar dominio, repositorio, sesión fail closed y Hilt ClaimsModule.
- [x] Implementar contratos/ViewModels MVI y UI explícita sin éxito simulado.
- [x] Añadir pruebas JUnit4/API/ViewModel y Compose en archivos Claims nuevos.
- [x] Integración principal: claimsApi sanitizada, ZXing core y navegación.
- [x] Ejecutar gates locales Gradle/Python, compilación y cobertura >=80% en integración principal.
- [x] Listado y cancelación integrados en el cliente sobre el contrato propuesto de `docs/endpoints-internos.md` (pendiente de publicación backend).

Contrato auditado: backend main 01dad6e, OfferClaimDtos.cs y OfferEndpoints.cs.
Validación principal ejecutada: 257 pruebas unitarias sin fallos/errores/omisiones, Kover 82.82%, compileDebugKotlin/compileDebugAndroidTestSources/lint/detekt/ktlint PASS. Pruebas Compose compiladas, no ejecutadas.

- [x] Verificar Initialize por offerId sin POST y efecto Confirmed único con callback onClaimConfirmed sin vaciar el carrito antes de confirmación; retirada automática posterior no implementada.

- [x] Revisión: capturar JSON/HTTP inválidos; comparar identidad de autenticación; expirar QR con reloj; limitar payload y probar fallos sin éxito falso.

- [x] Agrupar callbacks/reloj en ClaimsScreenActions sin supresiones; migrar pruebas propias y comunicar firma de navegación a MAIN.

- [x] Simplificar igualdad de identidad privada sin condición compleja ni exposición de token; verificar contrato equals/hashCode.

- [x] Retener confirmaciones solo en memoria por oferta; Initialize(expectExisting) restaura o bloquea POST sin código; probar cambios de oferta y borrado por identidad.


- [ ] Ejecutar pruebas instrumentadas en dispositivo y E2E con cuenta real sin simulación.
- [x] Retirar una unidad de la oferta en la bolsa tras la primera confirmación del servidor y conservar los demás artículos.
- Suite remota de seguridad/CI no ejecutada sin push; paquete SAST p/mobile externo devuelve 404. No se cierran estas condiciones.

## Avance registrado 2026-11-21

- La confirmación del apartado consume una unidad de la oferta en la bolsa. Se conserva cualquier otra unidad o artículo. La operación ocurre antes del efecto de UI y solo ante éxito de creación.
- El backend main 1c27253 expone GET /api/v1/offers/claims/{claimId}, una consulta individual con ID conocido. No se hallaron listado ni cancelación, y el detalle de oferta no devuelve el ID de su reserva activa.
- AWS respondió `401` al consultar por ID sin sesión. La recuperación del QR de un apartado desconocido para la instalación sigue pendiente del identificador o del listado autenticado.
- Validación local: 446 pruebas unitarias sin fallos; Kover Debug 80.60 %. Compilación principal e instrumentada, lint, Detekt, ktlint y cinco gates Python en verde. Pruebas instrumentadas sin ejecutar en dispositivo.

## Recuperación por identificador confirmado en la instalación

- GET por claimId recupera el QR activo al recrear la pantalla. El índice cifrado conserva únicamente identificadores por cuenta y oferta. No se repite la creación ni se vuelve a descontar una unidad de la bolsa.
- En main del backend, revisión f42ca817, no hay rutas de listado ni cancelación y el detalle de oferta no proporciona el identificador del apartado. Las reservas desconocidas para esta instalación siguen pendientes del contrato del servidor.

## Listado, cancelación y recuperación del QR — 2026-11-22

- [x] T13.L1 `ClaimsApiService`: `GET /api/v1/offers/claims` (lista `ClaimDetailDto` con `offerTitle` opcional) y `PATCH /api/v1/offers/claims/{claimId}/cancel`. Contrato propuesto; `sensus-back` (main f42ca81, Back_Rocio 77944e2, Back_Rodrigo fee4bf0) aún no publica estas rutas.
- [x] T13.L2 Dominio: `ClaimSummary`, `ClaimStatus`, `GetMyClaimsUseCase` (activas primero) y `CancelClaimUseCase` (valida ID).
- [x] T13.L3 `ClaimsRepositoryImpl`: listado tolerante a filas inválidas, cancelación que exige `Cancelled` con el mismo ID, sesión fail-closed.
- [x] T13.L4 Recuperación del QR sin ID local: si el índice cifrado no conoce la reserva, se consulta el listado, se toma la reserva activa vigente de la oferta, se pide el detalle si falta el QR y se guarda el ID en el índice. No repite el POST ni descuenta la bolsa.
- [x] T13.L5 UI MVI `MyClaimsScreen` ("Mis reservas" en Perfil): estados de carga, error con reintento, vacío, QR de reservas activas y cancelación con diálogo de confirmación.
- [x] T13.L6 Pruebas: `MyClaimsRepositoryTest`, `MyClaimsViewModelTest` y `MyClaimsScreenTest` (Compose, compilada).
- [ ] T13.L7 E2E contra backend cuando publique listado y cancelación. Mientras tanto la pantalla muestra "No se encontró la oferta o el apartado" (404) y la recuperación cae al mensaje de código no disponible.
