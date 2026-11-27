# FT-38 — Plan

1. Inspirarse en la jerarquía comercial de [Shopify](https://shopify.dev/docs/apps/design/layout) y las transiciones de [Compose](https://developer.android.com/develop/ui/compose/animation/quick-guide).
2. Aplicar el tema existente y componentes de presentación comunes. Conectar carga, error y reintento a los estados e intents existentes.
3. Ejecutar compilación, análisis, pruebas y gates SDD. Revisar en dispositivo.

4. Ampliar el tema raíz con esquemas claro y oscuro, Inter y formas compartidas. Guardar la apariencia como preferencia exclusiva de presentación.
5. Migrar superficies, estados, etiquetas y controles de acceso, Cliente y Comercio. Conservar los callbacks y el menú inferior existentes.
6. Comprobar temas, contraste, tamaño compacto, compilación, pruebas y gates.

## Splash
Reproducir directamente el video original y retirar la capa amarilla añadida al arranque.

Rellenar los márgenes del reproductor con el amarillo del fotograma y conservar su relación de aspecto.

## Comercio — fidelidad Figma
7. Capturar los frames 13 a 17 del lienzo SENSUS en modo presentación y registrar sus IDs de nodo.
8. Rediseñar cada pantalla en su archivo, reutilizando tokens de `theme/` y componentes compartidos del paquete `merchant`. No tocar ViewModels ni contratos.
9. Compilar app e instrumentadas, ejecutar Detekt, ktlint y pruebas unitarias. Revisar visualmente en dispositivo.
