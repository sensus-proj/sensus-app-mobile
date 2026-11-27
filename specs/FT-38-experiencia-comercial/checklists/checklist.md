# FT-38 — Checklist

- [x] Tema y animaciones aplicados en las superficies principales.
- [x] Gates locales, compilación instrumentada y Kover debug verificados.
- [ ] Revisar el hallazgo preexistente de Semgrep sobre la actividad lanzadora (acción del usuario: confirmar/suprimir `exported_activity` de `MainActivity` necesario para launcher/App Links; ver plan cierre móvil §5.Z-4).
- [ ] Revisión visual y ejecución instrumentada en dispositivo (acción del usuario: validar visualmente claro/oscuro y ejecutar `androidTest` en dispositivo físico; ver plan cierre móvil §5.Z-3).

## Alcance revisado en el código

- [x] Arranque, permisos de ubicación, login, registro, OTP, recuperación y términos.
- [x] Cliente. Mapa, feed, detalle de oferta, catálogo, ruta, apartado, bolsa, intereses guardados y notificaciones.
- [x] Comunidad. Feed, amigos, QR, ofertas compartidas y Sensi.
- [x] Comercio. Panel, alta, nueva oferta, edición, canje y perfil.
- [x] Presentación compartida. Menú inferior, campos, acciones, hojas, diálogos, estados y selector de apariencia.
- [x] Compilación Kotlin e instrumentada, Lint, Detekt, ktlint, 422 pruebas unitarias y Kover 80.69 %.
- [ ] Recorrer estas superficies en dispositivo con ambos temas y texto ampliado (acción del usuario: validación en dispositivo físico; ver plan cierre móvil §5.Z-3).

- [ ] Verificar que el arranque muestre el video del gato sin una capa amarilla adicional (`SplashScreen.kt` + `SplashScreenTimingTest` PASS; falta (acción del usuario: verificación visual en dispositivo físico; ver plan cierre móvil §5.Z-3)).

- [ ] Verificar que el video cubra visualmente las franjas superiores e inferiores en pantallas altas (`R.color.sensus_video_matte` `#F3B443` configurado; falta (acción del usuario: inspección visual en pantallas altas; ver plan cierre móvil §5.Z-3)).

## Comercio — fidelidad Figma (frames 13 a 17)

- [x] Ventas y canje (16, 14e), panel e inventario (14, 14b), nueva oferta (15), editar oferta (14d), registro y foto (13, 13b) y perfil (17) rediseñados sin tocar ViewModels ni contratos.
- [x] Textos nuevos en `strings_merchant_ui.xml`.
- [x] Compilación de app e instrumentadas, Lint, Detekt, ktlint, pruebas unitarias y gates SDD (`check_sdd`, `check_layers`, `check_endpoints`, `check_secrets`).
- [ ] Recorrer en dispositivo las pantallas del vendedor en ambos temas y ejecutar las pruebas instrumentadas.
- [ ] Pantallas nuevas 14c, 14f, 15b, 17b y 17c: pendientes de contrato o slice propia.
