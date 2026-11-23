# CHECKLIST — FT-26 Reseñas de comercio estilo Play Store y calificación real

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + tabla `## Aprobación`
- [x] Endpoints alineados con `056-resenas-comercios` y `docs/endpoints-internos.md`
- [x] `plan.md` y `tasks.md` actualizados

## Funcionalidad y reglas de negocio

- [x] Click en tarjeta del catálogo abre `StoreReviewsScreen`
- [x] Calificación 1..5 estrellas, like y respuesta en hilo implementados con filtro de groserías
- [x] Calificación real en `MerchantProfileScreen` sin promedios falsos

## Arquitectura y seguridad

- [x] `domain/` puro y `presentation/` sin importar `data/` (`check_layers.py` en PASS)
- [x] `StoreReviewsViewModel` hereda de `MviViewModel`
- [x] Sin secretos ni PII en logs (`check_secrets.py` en PASS)

## UI y accesibilidad

- [x] Íconos de estrella usando `SensusPurple` / `SensusTextMuted` sin tonos amarillos ni ámbar (§9.2)
- [x] Etiquetas accesibles en estrellas, botones de like y respuestas

## Gates automatizados

- [x] `.\gradlew.bat compileDebugKotlin detekt ktlintCheck lint testDebugUnitTest koverVerifyDebug`
- [x] `py scripts/check_sdd.py`, `check_layers.py`, `check_endpoints.py`, `check_secrets.py`, `check_coverage.py --min 80`
