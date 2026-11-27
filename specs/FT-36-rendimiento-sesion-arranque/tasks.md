# Tareas — FT-36 Rendimiento de sesión y arranque

- [ ] T36.1 Medir API, persistencia y primera pantalla útil de forma separada (acción del usuario: capturar trazas Android de arranque en dispositivo V2341 según FT-32; ver plan cierre móvil §5.Z-3).
- [x] T36.2 Optimizar el tramo medido y verificar dependencias entre las cargas de arranque (`AppViewModel.kt`, `AppViewModelTest.kt`, `MainScreenViewModelTest.kt`).
- [ ] T36.3 Comparar arranque y login y verificar flujos de sesión en dispositivo (acción del usuario: ejecutar E2E en dispositivo físico y validar login/arranque; ver plan cierre móvil §5.Z-3).
- [ ] T36.4 Diferir la apertura validada de Room fuera de la construcción de sesión y navegación (`SessionRepositoryImpl` y `OffersRepositoryOfflineTest` verifican apertura diferida de DAOs; pendiente (acción del usuario: repetir las 20 muestras de arranque frío en el V2341; ver plan cierre móvil §5.Z-3)).

T36.2 evita dobles envíos desde la presentación. T36.4 tiene cambios y regresiones unitarias verificados. Quedan pendientes las mediciones del login y de la nueva compilación en el V2341.
