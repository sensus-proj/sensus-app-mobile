# TASKS — FT-32 Filtro local de groserías y ajustes de UX

- [x] T32.1 Crear `ProfanityFilter.kt` en `domain/moderation/` sin dependencias de Android — verif: `py scripts/check_layers.py`
- [x] T32.2 Integrar `ProfanityFilter` en casos de uso de usuario, comercio, ofertas y reseñas — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T32.3 Actualizar `SplashScreen.kt` (`TextureView`/`MediaPlayer` silencioso) y `statusBarsPadding()` en perfiles — verif: `.\gradlew.bat compileDebugKotlin`
- [x] T32.4 Completar cobertura de `ProfanityFilterTest` y validar `koverVerifyDebug` >= 80 % — verif: `.\gradlew.bat testDebugUnitTest koverVerifyDebug`
