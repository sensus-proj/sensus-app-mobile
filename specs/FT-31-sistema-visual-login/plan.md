# PLAN — FT-31 Sistema visual y login

1. Centralizar el esquema de Material 3, sus colores de estado y la escala Inter local.
2. Sustituir colores de presentación aislados por tokens semánticos y ajustar el login con ancho máximo, desplazamiento y transiciones de estado.
3. Compilar y pasar análisis estático, pruebas unitarias y cobertura. Compilar y ejecutar el escenario instrumentado en un dispositivo.

## Presentación del acceso
Compartir cabecera y selector en Compose. Aplicar los tokens del tema en ambos formularios y sus acciones. Reutilizar los callbacks actuales.

Compartir el contenedor de acceso y aplicar una entrada breve con movimiento moderado.

Trasladar la cabecera y las cápsulas a componentes compartidos de Compose y conservar las rutas y validaciones actuales.

Compartir el marco de formulario y centrar el contenedor desplazable en el espacio disponible.

Dibujar la ola en el contenedor y superponer la tarjeta rectangular opaca.

Reutilizar el acceso de invitado existente desde Registro y animar el fondo y la entrada del contenedor compartido.
