# Plan — FT-35 Rendimiento de imágenes

Aplicar después de los cambios comprobables de FT-34 y conservar los pendientes de medición física. Usar la referencia FT-32 para comparar.

Priorizar imágenes visibles en el primer viewport y solicitudes repetidas. Definir tamaños de referencia según el espacio actual y densidad, conservar una ruta compatible para fuentes externas y validar caché, permisos y rotación de versiones antes de aplicar a todas las pantallas.

La primera intervención se limita a imágenes públicas de ofertas en la vista previa del mapa y el catálogo. Usar una variante de Cloudinary solo para URLs HTTPS versionadas, sin transformaciones ni parámetros previos, y conservar la URL original como respaldo. La pantalla de detalle conserva su recurso de resolución completa.

## Verificación

Ejecutar los gates SDD y de DevSecOps del repositorio. Para cambios Android, verificar compilación, pruebas unitarias y de integración, E2E, análisis estático y cobertura mínima de 80 %. Registrar evidencia y bloqueos en el checklist y en `memoria.md`.
