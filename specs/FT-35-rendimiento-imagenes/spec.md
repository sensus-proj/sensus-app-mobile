# SPEC — FT-35 Rendimiento de imágenes

> Rector: `.specify/memory/constitution.md`. Dependencias: FT-34, FT-12, FT-15.

## 1. Contexto y objetivo

Reducir bytes transferidos y tiempos de carga en listados, mapa, perfiles y detalle, preservando las imágenes, sus controles y su política de acceso.

## 2. Requisitos funcionales

RF-35-01. Solicitar una variante al tamaño de visualización en listas, avatares y tarjetas. Usar la variante de detalle apropiada al abrirla. Reutilizar la carga de Coil con claves estables por versión y tamaño.

## 3. Requisitos no funcionales

Medir tiempo a imagen visible y bytes con caché fría y caliente. La caché persistente de Coil aplica a medios públicos aptos para caché. Los medios privados siguen la política de cifrado y aislamiento de sesión.

## 4. Arquitectura y contratos internos

Reusar Coil y verificar qué URLs admiten variantes en el proveedor/CDN. Obtener variantes autorizadas por contrato o en la capa de datos. El componente de imagen conserva las acciones, el layout y accesibilidad.

## 5. Criterios de aceptación

Una revisita reutiliza imágenes compatibles. Reemplazar una foto invalida su versión anterior. La tarjeta conserva texto y acciones mientras carga. Se conservan el detalle, la vista previa local, la carga y la moderación de imágenes.

## 6. Plan de pruebas

Unitarias de selección de variante y versión. Integración de caché, URLs firmadas e imágenes no transformables. Compose para carga, error y navegación. Comparación de bytes en recursos de prueba.

## 7. Riesgos, límites y deuda

Las transformaciones requieren compatibilidad confirmada del CDN. Se conserva el URL original cuando el proveedor o una firma requieren ese recurso.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Especificada | Pendiente | — | [Plan](plan.md) · [Tareas](tasks.md) |
