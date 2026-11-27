# Checklist — FT-35 Rendimiento de imágenes

- [x] Variantes públicas versionadas en vista previa y catálogo, con pruebas de URLs compatibles, externas y firmadas.
- [ ] Aceptación de RF-35-01 demostrada con evidencia (`T35.2` cubierto por `PublicOfferThumbnailTest`; falta (acción del usuario: recolectar muestras de bytes y tiempos en V2341; ver plan cierre móvil §5.Z-3)).
- [ ] Medición reproducible y resultados de dispositivos documentados (acción del usuario: ejecutar protocolo FT-32 de imágenes en dispositivo físico; ver plan cierre móvil §5.Z-3).
- [x] Funcionalidad existente y recorridos afectados verificados (`PublicOfferThumbnailTest`, `ProductOfferCard`, `ProductDetailComponents` PASS).
- [x] Gates, cobertura aplicable y revisión validados (`549/549`, Kover `80.81 %`, `detekt`/`ktlintCheck`/`lint` y `5/5` Python PASS).
- [x] Evidencia y estado final registrados en `memoria.md` (`Bloque D/E` 2026-11-26).
