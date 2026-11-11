# PLAN — FT-16 Inventario de Ofertas del Comerciante, Ciclo de Vida y Métricas

> Derivado de `spec.md`. Historias/Alcance: SE-023, SE-024, SE-025, SE-026, SE-045 (Pantallas P-30, P-31; Componentes C-41..C-44).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Dashboard y métricas (`SE-045` / `P-30`).** `GetMerchantStatsUseCase` consume `GET /api/v1/merchants/me/stats` y alimenta la grilla KPI (`C-42`) y gráfica semanal (`C-43`).
2. **Inventario paginado (`SE-023` / `P-31`).** `GetMerchantOffersUseCase` consume `GET /api/v1/merchants/me/offers` filtrando por `ACTIVE`, `PAUSED`, `SOLD_OUT` y `EXPIRED`.
3. **Transiciones de ciclo de vida (`SE-024..SE-026`).** `ChangeOfferLifecycleUseCase` ejecuta `PATCH /api/v1/offers/{id}/pause`, `/activate`, `/sold-out` y `DELETE /api/v1/offers/{id}` con `rowVersion` para control de concurrencia (`409 Conflict`).

## 2. UI y navegación

- `MerchantDashboardScreen` (`P-30`) con `MerchantStatusBanner` (`C-41`), `KpiGrid2x2` (`C-42`) y `WeeklyBarChart` (`C-43`).
- `MerchantOffersScreen` (`P-31`) con tabs por estado y `MerchantOfferManageCard` (`C-44`).
- Previews: `DashboardLoaded`, `InventoryActiveTab`, `InventorySoldOutTab`, `DeleteConfirmDialog`.

## 3. DI

| Binding | Implementación |
|---|---|
| `GetMerchantStatsUseCase` | Inyección por constructor con `MerchantRepository` |
| `GetMerchantOffersUseCase` | Inyección por constructor con `OffersRepository` |
| `ChangeOfferLifecycleUseCase` | Inyección por constructor con `OffersRepository` |

## 4. Gates

- `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes.
- `./gradlew.bat detekt ktlintCheck lint` verdes (0 violaciones).
- `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (cobertura `>= 80%`).
- `python scripts/check_sdd.py` PASS.
- `python scripts/check_layers.py` PASS.
- `python scripts/check_endpoints.py` PASS.
- `python scripts/check_secrets.py` PASS.
- `python scripts/check_coverage.py --min 80` PASS.

## 5. Orden de implementación

1. Definir `MerchantDashboardStats`, `OfferLifecycleAction` y casos de uso en `domain/`.
2. Añadir endpoints de inventario, ciclo de vida y stats en `MerchantApiService` y `OffersApiService`.
3. Refactorizar `MerchantDashboardViewModel` eliminando `MockDataProvider`.
4. Crear `MerchantOffersViewModel` y conectar `MerchantOffersScreen` (`P-31`).
5. Probar transiciones y conflicto `409` con `MockWebServer`.

## 6. Prohibiciones

- Prohibido leer métricas o inventario desde `MockDataProvider` en producción.
- Prohibido eliminar una oferta sin diálogo de confirmación.

## Tareas

Ver `tasks.md`.
