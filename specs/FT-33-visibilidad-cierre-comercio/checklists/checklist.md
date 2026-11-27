# Checklist — FT-33 Visibilidad al cerrar un comercio

- [x] Regresión unitaria de catálogo vacío autoritativo y fallback sin red.
- [x] Lectura de perfil compartida entre pantalla y cierre concurrentes, con rollback probado.
- [x] Contrato local del backend revisado. El PUT de perfil todavía requiere dirección y código postal. El código de la consulta de ofertas filtra comercios activos.
- [ ] Aceptación de RF-33-01 demostrada con evidencia (código y tests unitarios `T33.1..T33.3` PASS; falta (acción del usuario: prueba con dos dispositivos según protocolo FT-32; ver plan cierre móvil §5.Z-3)).
- [ ] Medición reproducible y resultados de dispositivos documentados (acción del usuario: capturar mediciones de propagación en teléfono físico; ver plan cierre móvil §5.Z-3).
- [x] Funcionalidad existente y recorridos afectados verificados (`BuyerViewModelsTest`, `MerchantProfileViewModelTest` y `MerchantOpenStatusAndMetricsTest` PASS).
- [x] Gates, cobertura aplicable y revisión validados (`549/549`, Kover `80.81 %`, `detekt`/`ktlintCheck`/`lint` y `5/5` Python PASS).
- [x] Evidencia y estado final registrados en `memoria.md` (`Bloque D` 2026-11-26).
