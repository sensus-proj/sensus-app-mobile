# Checklist — FT-34 Rendimiento del mapa y ofertas cercanas

- [x] Prueba unitaria de una consulta en curso por búsqueda y descarte de la respuesta del radio anterior.
- [x] Lecturas de caché del mapa y fallback del repositorio fuera del hilo principal.
- [ ] Aceptación de RF-34-01 demostrada con evidencia (`T34.1` y `T34.2` PASS en pruebas unitarias/Room; falta (acción del usuario: capturar trazas en dispositivo físico; ver plan cierre móvil §5.Z-3)).
- [ ] Medición reproducible y resultados de dispositivos documentados (acción del usuario: ejecutar protocolo FT-32 en dispositivo físico; ver plan cierre móvil §5.Z-3).
- [x] Funcionalidad existente y recorridos afectados verificados (`BuyerViewModelsTest`, `OffersRepositoryOfflineTest`, `MapPreferencesRepositoryTest` y `EncryptedDatabaseIntegrationTest`).
- [x] Gates, cobertura aplicable y revisión validados (`549/549`, Kover `80.81 %`, `detekt`/`ktlintCheck`/`lint` y `5/5` Python PASS).
- [x] Evidencia y estado final registrados en `memoria.md` (`Bloque D/E` 2026-11-26).
