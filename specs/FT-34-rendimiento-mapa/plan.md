# Plan — FT-34 Rendimiento del mapa y ofertas cercanas

Aplicar después de los cambios comprobables de FT-33. Usar la referencia FT-32 para comparar y mantener abierta la verificación con comercio y dos clientes.

Inventariar cada disparador de `BuyerMapViewModel` y `BuyerMapScreen` antes de unificarlo. Reusar repositorios inyectados por Hilt. Definir claves, vigencia, tope de filas y migración de Room tras medir cardinalidad y patrones de búsqueda. Comparar resultados de ubicación y overlays de la misma zona antes y después.

## Verificación

Ejecutar los gates SDD y de DevSecOps del repositorio. Para cambios Android, verificar compilación, pruebas unitarias y de integración, E2E, análisis estático y cobertura mínima de 80 %. Registrar evidencia y bloqueos en el checklist y en `memoria.md`.
