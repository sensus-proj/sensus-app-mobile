# Plan — FT-36 Rendimiento de sesión y arranque

Aplicar después de los cambios comprobables de FT-35. Usar la referencia FT-32 para comparar.

Tomar las mediciones de referencia e identificar el tramo dominante. Revisar `LoginViewModel`, repositorio de sesión y navegación sin mezclar cambios del sistema visual de login. Aplazar inicializaciones únicamente si sus puntos de entrada siguen disponibles, incluidos enlaces y notificaciones. Revisar `docs/errores-conocidos.md` antes de tocar persistencia o sesión.

La primera corrección evita solicitudes simultáneas en el punto de entrada existente. El ViewModel admite un solo intento activo de autenticación o modo invitado y libera el estado tras la respuesta. El tiempo del servidor se mide por separado.

Para RF-36-02, diferir la resolución de DAOs de sesión hasta la purga y la del DAO de preferencias hasta su lectura en IO. Resolver el repositorio de amigos bajo demanda para enlaces de invitación. Mantener la apertura validada de SQLCipher, el vídeo de inicio y las rutas de login y notificaciones.

## Verificación

Ejecutar los gates SDD y de DevSecOps del repositorio. Para cambios Android, verificar compilación, pruebas unitarias y de integración, E2E, análisis estático y cobertura mínima de 80 %. Registrar evidencia y bloqueos en el checklist y en `memoria.md`.
