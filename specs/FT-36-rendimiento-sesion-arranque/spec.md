# SPEC — FT-36 Rendimiento de sesión y arranque

> Rector: `.specify/memory/constitution.md`. Dependencias: FT-35, FT-04, FT-31.

## 1. Contexto y objetivo

Reducir el tiempo entre el arranque o el envío de credenciales y la primera pantalla útil, conservando autenticación, términos, permisos y rutas por rol.

## 2. Requisitos funcionales

RF-36-01. Aprovechar la sesión cifrada vigente cuando corresponda y reutilizar el perfil recibido del servidor. Ejecutar en paralelo o diferir solo el trabajo independiente de sesión, términos, rol y navegación.

## 3. Requisitos no funcionales

Medir arranque frío/caliente, feedback y login útil con FT-32. Las credenciales se validan en el servidor. Mantener cifrado, expiración y aislamiento al cambiar de cuenta.

RF-36-02. Alcanzar p95 inferior a 1000 ms hasta el primer fotograma en arranque frío, sobre el V2341 y la condición de referencia de FT-32. Medir también el tiempo hasta la pantalla usable para detectar cualquier demora trasladada a la navegación.

## 4. Arquitectura y contratos internos

Conservar las rutas MVI y los límites dominio/datos. Medir red, persistencia y tiempo al primer contenido por separado. Revisar inicializaciones y E/S del arranque según la traza antes de modificar su orden.

## 5. Criterios de aceptación

El acceso autorizado y la recuperación de sesión llegan a las mismas rutas. Términos, invitado, registro, recuperación, enlaces externos de navegación, push, logout e invalidación por 401 siguen sus recorridos previstos.

## 6. Plan de pruebas

Unitarias JUnit 4 de sesión y MVI e integración de persistencia y 401. E2E de login, arranque con sesión, navegación por rol, cambio de cuenta, reapertura y Sensi.

## 7. Riesgos, límites y deuda

La red y la API condicionan el login con contraseña. Si las trazas ubican la mayor demora en el servidor, coordinar con backend y volver a medir.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Especificada | Pendiente | — | [Plan](plan.md) · [Tareas](tasks.md) |
