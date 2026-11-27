# FT-38 — Experiencia comercial

## 1. Contexto y objetivo
Mejorar la primera impresión y las superficies principales del marketplace aplicando los tokens de FT-31. Actualmente se mezclan escalas locales, acciones competidoras y tarjetas sin imagen.

## 2. Alcance
Arranque, acceso, recuperación, onboarding, legal y todas las interfaces existentes de Cliente y Comercio, incluidas las hojas, los diálogos y el menú inferior. Mantener las rutas, acciones y contratos existentes.

## 3. Historias
Como comprador quiero reconocer productos y precios rápidamente. Como comercio quiero publicar, canjear y gestionar ofertas desde una jerarquía clara.

## 4. Requisitos
Usar Inter, una jerarquía común, violeta para acciones primarias y naranja para descuentos. Permitir elegir el modo claro, oscuro o automático desde ambos perfiles y conservar la elección al reiniciar. Mantener búsqueda, mapa, filtros, guardado, canje y edición.

## 5. Requisitos no funcionales
Usar superficies flexibles y controles de al menos 48 dp en las acciones revisadas. Evitar animaciones ambientales continuas. Mantener el campo blanco de los QR y reutilizar las fotos y recursos de marca existentes.

## 6. Criterios de aceptación
Las pantallas compilan con sus rutas actuales. Comprobar los pares principales de contraste y la selección del tema con pruebas. Recorrer los flujos existentes en ambos modos, incluidos errores, estados vacíos, carga, teclado y texto ampliado en una ventana compacta.

## 7. Riesgos y límites
Validar la fidelidad visual con el equipo. El acceso al lienzo Figma requiere permisos. La divergencia histórica del token violeta está registrada en FT-31.

## Aprobación
Implementación y ampliación del alcance solicitadas por el usuario. Validación visual pendiente.

## Splash
El video original del gato es la única presentación de arranque.

El amarillo de los fotogramas llena los márgenes del video hasta los bordes de la ventana.

## Comercio — fidelidad a la maqueta Figma (frames 13 a 17)
Rediseñar las pantallas existentes del vendedor según los frames del lienzo SENSUS: 13 y 13b (registro del negocio y foto), 14 y 14b (panel sin ofertas e inventario), 14d (editar oferta), 14e (escanear código), 15 (nueva oferta), 16 (ventas y canjes) y 17 (perfil del comercio).

Requisitos:
- Mantener ViewModels, intents, estados, callbacks, rutas y contratos. El cambio es solo de presentación.
- Fondo lila de la maqueta, tarjetas blancas con borde suave, encabezado con flecha en círculo y título, acciones primarias violetas, contenedor naranja para el canje y para avisos.
- Textos de interfaz nuevos en `strings.xml`. Conservar los textos que validan las pruebas instrumentadas de canje y edición.
- Lo que la maqueta muestra sin datos en el contrato actual no se inventa: resumen del día y pedidos recientes (16), métodos de pago, RFC y stock (13), y métricas por oferta (14c).

Fuera de alcance, pendiente de contrato o de una slice propia: 14c (métricas por oferta), 14f (canje validado con cliente, producto y monto), 15b (oferta publicada con vista previa), 17b (verificación del comercio) y 17c (horarios y contacto editables).
