# SPEC — FT-33 Visibilidad al cerrar un comercio

> Rector: `.specify/memory/constitution.md`. Dependencias: FT-32, FT-06, FT-11.

## 1. Contexto y objetivo

Reducir la demora entre cerrar un comercio y retirar sus ofertas del catálogo público, preservando el inventario, la reapertura y los apartados existentes.

## 2. Requisitos funcionales

RF-33-01. Reflejar el cierre pendiente en el dispositivo del comerciante, reconciliarlo con el estado del servidor e invalidar resultados del comercio por su ID. Respetar una respuesta remota vacía. Mantener disponibles las acciones de inventario y los apartados previos según el contrato vigente.

## 3. Requisitos no funcionales

Medir feedback, confirmación remota y propagación por separado con el protocolo FT-32. El estado confirmado de visibilidad se impone a la caché y a respuestas anteriores.

## 4. Arquitectura y contratos internos

MVI expresa pendiente, confirmado y error. El repositorio reconcilia e invalida por comercio. Verificar OpenAPI antes de sustituir la lectura y actualización de perfil por una sola operación de estado. La visibilidad pública en otros dispositivos depende del backend.

## 5. Criterios de aceptación

Tras un cierre confirmado, los resultados públicos actualizados excluyen las ofertas del comercio. Una respuesta antigua no las vuelve a mostrar. Reapertura, cierre fallido, respuesta perdida y sincronización de un cliente observador quedan cubiertos.

## 6. Plan de pruebas

Pruebas unitarias de caché vacía y orden de respuestas. Integración de actualización, rollback o reconciliación y reapertura. E2E con dos clientes y apartados previos.

## 7. Riesgos, límites y deuda

La operación específica y la propagación sin refresco podrían requerir un cambio de contrato y despliegue de backend. Medir ambos límites antes de acordar la solución remota.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Especificada | Pendiente | — | [Plan](plan.md) · [Tareas](tasks.md) |
