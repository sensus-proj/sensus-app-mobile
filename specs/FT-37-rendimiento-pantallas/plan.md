# Plan — FT-37 Rendimiento de las demás pantallas

Comienza después de la etapa FT-36. Usar la referencia FT-32 para comparar.

Comparar tiempos de navegación, primer contenido, red y render. Seleccionar una mejora medible por flujo y validar sus riesgos de vigencia y acceso. Conservar el orden entre slices y actualizar el índice con su evidencia.

### Inventario inicial para la medición

| Recorrido | Punto de inicio | Pantalla o estado usable |
|---|---|---|
| Catálogo, detalle y guardados | Apertura desde Inicio, mapa o guardados | Productos con precio y acciones disponibles |
| Rutas y apartados | Selección de una oferta o apartado | Ruta mostrada o estado del apartado con sus acciones |
| Perfil, amigos y comunidad | Entrada en la sección o apertura de un enlace | Datos de perfil, lista o primera publicación visible |
| Sensi y notificaciones | Apertura de la pestaña | Conversación inicial o lista de notificaciones consultable |
| Comercio | Entrada al panel, inventario, edición o ventas | Inventario y acciones o ventas consultables |

Registrar las condiciones y fallos con FT-32. Ordenar el inventario cuando existan datos por recorrido.

## Verificación

Ejecutar los gates SDD y de DevSecOps del repositorio. Para cambios Android, verificar compilación, pruebas unitarias y de integración, E2E, análisis estático y cobertura mínima de 80 %. Registrar evidencia y bloqueos en el checklist y en `memoria.md`.
