# Checklist — FT-36 Rendimiento de sesión y arranque

- [x] Regresiones unitarias de intents repetidos y concurrentes de login y acceso como invitado.
- [x] Verificada la resolución diferida de los DAOs y del repositorio de amigos, la purga de sesión y la navegación por enlace.
- [ ] Confirmar RF-36-02 con 20 arranques fríos de la nueva compilación en el V2341 y medir pantalla usable (acción del usuario: ejecutar las 20 muestras de arranque frío en V2341; ver plan cierre móvil §5.Z-3).
- [ ] Aceptación de RF-36-01 demostrada con evidencia (`T36.2`/`T36.4` cubiertos en unitarias; falta (acción del usuario: trazas de arranque en V2341; ver plan cierre móvil §5.Z-3)).
- [ ] Medición reproducible y resultados de dispositivos documentados (acción del usuario: documentar mediciones de arranque en dispositivo físico; ver plan cierre móvil §5.Z-3).
- [x] Funcionalidad existente y recorridos afectados verificados (`AppViewModelTest`, `MainScreenViewModelTest` y `OffersRepositoryOfflineTest` PASS).
- [x] Gates, cobertura aplicable y revisión validados (`549/549`, Kover `80.81 %`, `detekt`/`ktlintCheck`/`lint` y `5/5` Python PASS).
- [x] Evidencia y estado final registrados en `memoria.md` (`Bloque D/E` 2026-11-26).
