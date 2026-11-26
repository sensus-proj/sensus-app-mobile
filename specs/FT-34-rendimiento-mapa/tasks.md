# Tareas — FT-34 Rendimiento del mapa y ofertas cercanas

- [ ] T34.1 Unificar disparadores de búsqueda y cancelar o descartar resultados obsoletos. Verificación con `testDebugUnitTest` con cambios rápidos de radio y sesión.
- [ ] T34.2 Agregar lectura compatible en Room, vigencia e invalidación con E/S fuera del hilo principal. Verificación con Pruebas Room + SQLCipher y escenarios offline.
- [ ] T34.3 Separar ubicación y refresco del render y reducir trabajo de overlays con base en las trazas. Verificación con E2E mapa y protocolo FT-32.

Evidencia parcial de T34.1 y T34.2 en el checklist. La caché persistente por búsqueda y la reducción de trabajo de overlays siguen pendientes.
