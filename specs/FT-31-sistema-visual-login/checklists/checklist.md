# CHECKLIST — FT-31 Sistema visual y login

- [x] Paleta semántica compartida e Inter local con escala en sp.
- [x] Navegación por SMS, recuperación, registro e invitado disponible desde el login.
- [x] Compilar la app y los tests instrumentados y pasar detekt y ktlint.
- [x] Pasar Lint, unitarias debug y Kover debug (80.46 %).
- [ ] Revisar el hallazgo preexistente de Semgrep sobre MainActivity exportada (acción del usuario: confirmar/suprimir `exported_activity` necesario para launcher/App Links; ver plan cierre móvil §5.Z-4).
- [ ] Ejecutar la prueba instrumentada de ventana compacta y fuente ampliada en un dispositivo (`AuthFlowE2ETest` compilado en `compileDebugAndroidTestSources`; acción del usuario: ejecutar en dispositivo físico; ver plan cierre móvil §5.Z-3).


- [ ] Verificar el selector de Login y Registro en ambos temas, con texto ampliado y teclado abierto (`AuthAccessMotionTest` + `AuthFlowE2ETest` compilados; acción del usuario: verificar en dispositivo físico; ver plan cierre móvil §5.Z-3).

- [ ] Verificar la ola, la transparencia del gato y el alcance táctil en modo claro y oscuro (`AuthAccessScreenLayout.kt` aplicado; acción del usuario: inspección visual en dispositivo físico; ver plan cierre móvil §5.Z-3).
