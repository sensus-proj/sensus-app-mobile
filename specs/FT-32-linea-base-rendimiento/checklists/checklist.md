# Checklist — FT-32 Línea base de rendimiento

- [x] Resumidor CSV verificado con cinco pruebas unitarias de datos sintéticos.
- [x] Veinte muestras de tiempo al primer fotograma en arranque frío documentadas en el plan. Inicio usable y demás recorridos quedan pendientes.
- [ ] Aceptación de RF-32-01 demostrada con evidencia (acción del usuario: recopilar muestras de protocolo FT-32 en teléfono físico; ver plan cierre móvil §5.Z-3).
- [ ] Medición reproducible y resultados de dispositivos documentados (acción del usuario: documentar mediciones reales en V2341 con `summarize_performance.py`; ver plan cierre móvil §5.Z-3).
- [ ] Funcionalidad existente y recorridos afectados verificados (`549/549` unitarias y Compose compilados PASS; acción del usuario: validar recorridos en dispositivo físico; ver plan cierre móvil §5.Z-3).
- [x] Gates, cobertura aplicable y revisión validados (`549/549`, Kover `80.81 %`, `detekt`/`ktlintCheck`/`lint` y `5/5` Python PASS).
- [x] Evidencia y estado final registrados en `memoria.md` (`Bloque D/E` 2026-11-26).
