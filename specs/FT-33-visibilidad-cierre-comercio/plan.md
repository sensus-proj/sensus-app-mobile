# Plan — FT-33 Visibilidad al cerrar un comercio

Comienza tras capturar la referencia disponible de FT-32. La comparación física de cierre requiere una cuenta de comercio de prueba y un segundo cliente. El resultado vacío y la protección frente a consultas obsoletas se verifican primero con pruebas controladas.

Comenzar por el resultado remoto vacío en `BuyerHomeViewModel` y revisar su interacción con el fallback de `OffersRepositoryImpl`. Mantener la composición del inventario del comerciante. Aplicar la invalidación al catálogo público y garantizar que las respuestas concurrentes no recuperen un comercio cerrado. Ajustar la solicitud de cierre solo tras verificar el contrato y acordar el mecanismo de actualización entre dispositivos.

## Verificación

Ejecutar los gates SDD y de DevSecOps del repositorio. Para cambios Android, verificar compilación, pruebas unitarias y de integración, E2E, análisis estático y cobertura mínima de 80 %. Registrar evidencia y bloqueos en el checklist y en `memoria.md`.
