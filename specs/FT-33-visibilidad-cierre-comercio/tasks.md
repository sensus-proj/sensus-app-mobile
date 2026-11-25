# Tareas — FT-33 Visibilidad al cerrar un comercio

- [ ] T33.1 Cubrir lista vacía autoritativa y errores de red, y corregir la selección de datos en Inicio. Verificación con `testDebugUnitTest` e integración del repositorio de ofertas.
- [ ] T33.2 Aplicar invalidación por ID, protección contra respuestas antiguas y reconciliación del estado. Verificación con `testDebugUnitTest` y recorridos cierre, reapertura y error.
- [ ] T33.3 Verificar OpenAPI y eliminar la lectura previa si el contrato permite actualizar solo el estado. Hasta que exista esa operación, compartir la lectura de perfil que ya esté en curso y conservar la actualización completa validada por el servidor. Verificación con pruebas unitarias de concurrencia y contraste del contrato con el backend de pruebas.
- [ ] T33.4 Medir propagación con dos clientes y resolver dependencias de backend. Verificación con Protocolo FT-32 y checklist.

Evidencia parcial de T33.1 y T33.3 en el checklist. Falta validar con cuentas de prueba la invalidación por comercio y la propagación entre teléfonos.
