# SPEC — FT-31 Sistema visual y login

## 1. Contexto y objetivo

El login usa un encabezado alto y concentra enlaces largos en una fila. Actualizar la base visual compartida y hacer que el acceso funcione en ventanas compactas y con texto ampliado.

## 2. Alcance

Paleta y escala Inter del tema Compose, migración de colores de presentación y rediseño del login. El alcance funcional de autenticación sigue definido en FT-04, FT-05 y FT-07.

## 3. Historias de usuario

Como usuario, quiero identificar la acción principal, leer los errores y alcanzar las opciones de acceso aunque use una pantalla pequeña o texto ampliado.

## 4. Requisitos funcionales

El acceso admite credenciales, SMS, recuperación, registro e invitado. La carga y el error tienen transiciones breves y texto visible. Cuando el teclado reduce el espacio disponible, el formulario se desplaza.

## 5. Requisitos no funcionales

Usar la escala Inter en sp, objetivos táctiles de al menos 48 dp, insets de ventana y fuente empacada para uso sin conexión.

## 6. Criterios de aceptación

La prueba instrumentada cubre una ventana de 320 dp con fuente al 150 %, validación local, visibilidad de contraseña y acceso a las rutas secundarias.

## 7. Riesgos, limites y deuda

Algunas pantallas fijan su tamaño tipográfico en el componente y requieren revisión visual individual. El valor histórico de morado de marca sigue documentado en la constitución.

## Aprobación

Alcance solicitado el 2026-11-19. Validación visual en dispositivo pendiente.

## Acceso y registro
Login y Registro comparten el gatito original, el eslogan y un selector animado que usa las rutas existentes. El acceso de invitado se destaca en naranja. Se mantienen formularios, textos informativos y validaciones.

Login y Registro incorporan un fondo cálido animado y mantienen Inter y la paleta oficial en ambos temas.

El prototipo aprobado usa una ola profunda, el PNG original del gato sin placa y botones en cápsula. La cabecera y las superficies usan colores planos en ambos temas.

Los datos de Login y Registro se presentan en una superficie enmarcada y centrada. El acceso sigue siendo desplazable en pantallas bajas y con el teclado abierto.

La ola permanece como fondo detrás de la tarjeta del formulario en ambas pantallas.

Login y Crear cuenta muestran la acción de invitado dentro de la tarjeta. El selector superior sustituye los enlaces de navegación duplicados. La ola tiene movimiento suave, y el formulario entra con una transición breve.
