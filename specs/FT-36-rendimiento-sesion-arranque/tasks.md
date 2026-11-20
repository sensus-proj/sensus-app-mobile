# Tareas — FT-36 Rendimiento de sesión y arranque

- [ ] T36.1 Medir API, persistencia y primera pantalla útil de forma separada. Verificación con Protocolo FT-32 y trazas Android.
- [ ] T36.2 Optimizar el tramo medido y verificar dependencias entre las cargas de arranque. Verificación con `compileDebugKotlin`, `testDebugUnitTest` y pruebas de navegación.
- [ ] T36.3 Comparar arranque y login y verificar flujos de sesión en dispositivo. Verificación con E2E y revisión de `docs/errores-conocidos.md`.
- [ ] T36.4 Diferir la apertura validada de Room fuera de la construcción de sesión y navegación. Verificar acceso diferido, purga y carga de preferencias, y repetir las 20 muestras del V2341 con la compilación optimizada.

T36.2 evita dobles envíos desde la presentación. T36.4 tiene cambios y regresiones unitarias verificados. Quedan pendientes las mediciones del login y de la nueva compilación en el V2341.
