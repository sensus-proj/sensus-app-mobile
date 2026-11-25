# SPEC — FT-34 Rendimiento del mapa y ofertas cercanas

> Rector: `.specify/memory/constitution.md`. Dependencias: FT-33, FT-11, FT-28.

## 1. Contexto y objetivo

Hacer usable el mapa mientras se actualizan ubicación, comercios y ofertas, conservando las capas, el radio, los filtros, la selección y la navegación actuales.

## 2. Requisitos funcionales

RF-34-01. Mostrar resultados locales compatibles cuando existan. Reutilizar una ubicación vigente y permitida si está disponible. Compartir solicitudes equivalentes y aplicar únicamente resultados de la búsqueda vigente.

## 3. Requisitos no funcionales

Contenido útil p95 ≤ 2000 ms en las condiciones de FT-32 acordadas. Accesos Room fuera del hilo principal. La caché puede degradar a vacía si falla. La capa de mapa mantiene interacción durante el refresco.

## 4. Arquitectura y contratos internos

El repositorio usa Room + SQLCipher con clave por zona, radio, filtros y alcance de sesión. Conserva separadas las lecturas de catálogo comprador e inventario comerciante. El ViewModel observa datos y vigencia en MVI. Reusar el renderer de OSMDroid y actualizar los overlays que cambien.

## 5. Criterios de aceptación

Una sola actualización por búsqueda equivalente en curso. El último radio prevalece bajo concurrencia. Se conservan el límite de invitado, los permisos, recenter, capas, marcadores, selección, navegación y el estado offline.

## 6. Plan de pruebas

Unitarias para claves, permisos, caché, caducidad y concurrencia. Integración SQLCipher de migración y cambios de sesión. E2E de mapa offline, GPS apagado, filtros y radios.

## 7. Riesgos, límites y deuda

Definir antigüedad y precisión aceptables de ubicación, límites de la caché y política de teselas conforme al proveedor. Medir el costo del render antes de ampliar su alcance.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Especificada | Pendiente | — | [Plan](plan.md) · [Tareas](tasks.md) |
