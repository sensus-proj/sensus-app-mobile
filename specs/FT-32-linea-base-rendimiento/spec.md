# SPEC — FT-32 Línea base de rendimiento

> Rector: `.specify/memory/constitution.md`. Dependencias: FT-04, FT-06, FT-11.

## 1. Contexto y objetivo

Medir la versión de referencia en recorridos de arranque, login, mapa, imágenes y cierre del comercio antes de cambiar su comportamiento.

## 2. Requisitos funcionales

RF-32-01. Registrar el tiempo desde el gesto hasta la primera respuesta visual y hasta el resultado usable. Medir por separado el cierre confirmado y la desaparición de ofertas en un segundo dispositivo con el mapa abierto. Identificar también qué carga introduce esperas en los demás recorridos frecuentes.

## 3. Requisitos no funcionales

Publicar p50, p95, número de intentos y fallos por recorrido, métrica, versión, dispositivo, red y estado de caché. La comparación usará al menos 20 intentos por condición. Objetivos propuestos para las mejoras: p95 ≤ 100 ms para feedback y ≤ 2000 ms para contenido útil en condiciones acordadas. Conservar el conteo de fallos y tratar los fallos como incumplimientos del presupuesto.

## 4. Arquitectura y contratos internos

Medición externa sobre la versión de referencia mediante captura de pantalla con tiempo por fotograma y, cuando esté disponible, trazas de Android para diagnosticar el tramo lento. El resumen recibe un CSV de duraciones sin credenciales, URLs, identificadores de usuario ni coordenadas. El código de producto queda fuera del alcance de esta slice.

## 5. Criterios de aceptación

Se puede reproducir el protocolo y comparar los mismos escenarios entre versiones en condiciones equivalentes. Los datos de ejemplo y los resultados de emulador se identifican como tales. El informe de referencia incluye muestras reales de los cinco recorridos y documenta las que falten.

## 6. Plan de pruebas

Probar agrupación, percentiles, fallos y entradas inválidas del resumidor. Repetir las mediciones en Android físico de gama media con Wi‑Fi y datos móviles. Usar un segundo cliente para el cierre.

## 7. Riesgos, límites y deuda

La primera apertura y las revisitas tienen perfiles distintos. Se necesitan un dispositivo Android con la app, un segundo cliente para el cierre, cuentas de prueba y un backend disponible.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Especificada | Pendiente | — | [Plan](plan.md) · [Tareas](tasks.md) |
