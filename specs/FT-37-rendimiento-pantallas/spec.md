# SPEC — FT-37 Rendimiento de las demás pantallas

> Rector: `.specify/memory/constitution.md`. Dependencias: FT-36.

## 1. Contexto y objetivo

Aplicar los mismos criterios de rendimiento al resto de los flujos de comprador y comercio, en el orden que indiquen sus mediciones.

## 2. Requisitos funcionales

RF-37-01. Inventariar pantallas y recorridos y medir sus esperas. Priorizar por impacto y frecuencia de uso. Para cada cambio conservar acciones, permisos, filtros, navegación y comportamiento offline.

## 3. Requisitos no funcionales

Feedback y contenido útil se comparan con FT-32. Cada cambio debe justificar su costo de memoria, disco y red. Conservar validaciones y confirmaciones remotas de acciones de negocio.

## 4. Arquitectura y contratos internos

Reusar casos de uso, repositorios MVI y Hilt. Adoptar las políticas de las slices previas cuando encajen con el alcance de sesión y vigencia de cada dato. Perfilar render antes de tocar composición o inicialización.

## 5. Criterios de aceptación

El inventario incluye ambas modalidades y el resto de los recorridos existentes. Cada optimización tiene una comparación reproducible y evidencia de regresión funcional.

## 6. Plan de pruebas

Inventariar y ejecutar recorridos de catálogo, detalle, guardados, perfil, comunidad, amigos, Sensi, notificaciones, apartados, rutas y operaciones de comercio. Aplicar pruebas de capas, integración y E2E al flujo concreto.

## 7. Riesgos, límites y deuda

El cuello de botella y el alcance de cada cambio dependen del inventario. Antes de optimizar un flujo, registrar su tarea y actualizar los artefactos de la feature correspondiente si requiere una slice propia.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Especificada | Pendiente | — | [Plan](plan.md) · [Tareas](tasks.md) |
