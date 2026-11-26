# Plan — FT-32 Línea base de rendimiento

Primera etapa del programa. Capturar la referencia real disponible antes de optimizar. Cuando una condición requiera otra cuenta, dispositivo o red, conservar la versión base como referencia y registrar la condición pendiente. Las pruebas reproducibles de código pueden sustentar el desarrollo de una corrección mientras se completa la comparación física.

### Protocolo de captura

- Registrar el build/commit, perfil del dispositivo, tipo de red, caché fría/caliente y fecha de la sesión. Usar cuentas y comercios de prueba. Capturar con un reloj común para el cierre en dos pantallas.
- Preparar caché fría en un entorno de pruebas dedicado o con una zona/imagen aún sin consultar. Para caché caliente, repetir con el mismo recurso ya cargado. Para arranque frío, partir del proceso detenido. Mantener sesiones, permisos, ámbito de búsqueda y datos comparables entre versiones.
- Arranque: desde tocar el icono hasta la primera pantalla operable. Login: desde enviar credenciales válidas hasta operar la pantalla de destino. Mapa: desde abrirlo hasta poder moverlo y ver resultados válidos o un estado vacío confirmado. Imágenes: desde que la tarjeta es visible hasta que se muestra su foto. Cierre: desde tocar el interruptor hasta la confirmación visible y, en un cliente separado, hasta la desaparición de anuncios tras esa confirmación. Si el cliente ya se actualizó al confirmar, registrar 0 ms para ese tramo.
- Medir feedback desde el gesto hasta la primera respuesta visible. Capturar vídeo a 60 fps o más y calcular la diferencia por fotogramas. Conservar solo el CSV sanitizado. Para las imágenes, contrastar también los bytes de transferencia de recursos de prueba cuando se disponga de trazas de red.
- Repetir al menos 20 intentos en cada condición comparable. Si un intento falla o no completa dentro de 30 s, incluirlo como `error` o `timeout`. En una propagación que excede 30 s, verificar y anotar si requirió refresco manual.
- Comparar el comportamiento visible con las cuentas, permisos, filtros y flujos actuales antes de tomar la muestra posterior a cada optimización.

### Captura parcial en Android físico

En Sensus debug 1.4.0, código 9, dispositivo V2341 con datos móviles y sesión cerrada, `adb shell am start -S -W` registró 20 arranques COLD válidos. Tiempo al primer fotograma p50 1239 ms, p95 1377 ms, mínimo 1175 ms y máximo 1558 ms. Esta medición detiene el proceso entre intentos y conserva los datos en disco. Queda pendiente medir la pantalla usable, la autenticación, el mapa, las imágenes y el cierre con cuentas de prueba y dos clientes.

### Formato de entrada

`flow,metric,build,device,network,cache,outcome,duration_ms`

```csv
flow,metric,build,device,network,cache,outcome,duration_ms
map,usable,baseline-a,gama-media-a,wifi,warm,ok,1320
map,usable,baseline-a,gama-media-a,wifi,warm,timeout,
```

Métricas disponibles `feedback`, `usable`, `confirmed` y `propagated`. Redes `wifi`, `mobile` y `offline` y estados de caché `cold` o `warm`. Usar etiquetas de hasta 64 caracteres ASCII con letras, números, punto, guion o guion bajo, comenzando con letra o número. Para `propagated`, `device` identifica el par de dispositivos con una etiqueta de prueba. Duración entera en milisegundos en filas `ok`. Salidas `error` y `timeout` llevan duración vacía. El resumen usa rango más próximo sobre intentos exitosos y cuenta fallos en el cumplimiento del límite. Una condición con menos de 20 intentos queda marcada como muestra insuficiente.

Ejecutar `python scripts/summarize_performance.py <muestras.csv>` para obtener un resumen CSV. `p50_ok_ms` y `p95_ok_ms` describen los intentos exitosos. `within_budget_pct` incluye todos los intentos y `status` compara el resultado con el objetivo de 95 %. Para el cierre se reportan confirmación y propagación por separado con 2000 ms como referencia de cada tramo. Las filas de ejemplo sirven para verificar el formato.

## Verificación

Ejecutar los gates SDD y de DevSecOps del repositorio. Para cambios Android, verificar compilación, pruebas unitarias y de integración, E2E, análisis estático y cobertura mínima de 80 %. Registrar evidencia y bloqueos en el checklist y en `memoria.md`.
