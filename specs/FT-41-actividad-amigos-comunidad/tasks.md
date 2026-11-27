# TASKS — FT-41 Actividad de amigos en Comunidad

- [x] T41.1 Crear `FriendsActivityRepository`, DTOs y ruta `GET /api/v1/community/friends-activity` — verif: `py scripts/check_endpoints.py`
- [x] T41.2 Implementar `FriendsActivityViewModel` (MVI) y `FriendsActivityContent` — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T41.3 Reemplazar color ámbar de estrella por `SensusPurple` en `FriendsActivityContent` (Constitución §9.2) — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T41.4 Añadir pruebas unitarias en `FriendsActivityViewModelTest` y repositorio para alcanzar >= 90 % global y alinear lectura paginada (`PagedResult.items`) de `GET /api/v1/community/friends-activity` — verif: `.\gradlew.bat testDebugUnitTest koverVerifyDebug` y `python scripts/check_coverage.py --min 90`
