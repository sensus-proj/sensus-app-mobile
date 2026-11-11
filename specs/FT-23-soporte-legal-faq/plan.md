# PLAN — FT-23 Centro de Ayuda, FAQ, Soporte y Términos Legales

> Derivado de `spec.md`. Historias/Alcance: SE-042, SE-043, SE-044 (Pantalla P-35; Componentes C-29, C-49).
> Rector: `.specify/memory/constitution.md` §2, §4, §5, §6, §10 y §11.

## 1. Diseño

1. **Centro de ayuda (`P-35`).** `HelpCenterScreen` presenta preguntas frecuentes en acordeones expandibles (`C-49`) para Compradores y Comerciantes sin requerir red.
2. **Canal de soporte seguro.** Acción `Contactar soporte` lanza `Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:soporte@sensus.mx"))` con `try/catch (ActivityNotFoundException)`.
3. **Cumplimiento LFPDPPP.** Enlace directo a `https://rodnix.com.mx/sensus/privacidad.html` y términos de uso desde `HelpCenterScreen`, `BuyerProfileScreen` y `TermsScreen`.

## 2. UI y navegación

- `HelpCenterScreen` (`P-35`) en `presentation/support/` con buscador local de preguntas, acordeones `C-49` y tarjeta de contacto.
- Previews: `FaqCollapsed`, `FaqExpanded`, `SupportContactCard`.

## 3. DI

| Binding | Implementación |
|---|---|
| — | Sin nuevos bindings requeridos |

## 4. Gates

- `./gradlew.bat compileDebugKotlin` y `compileDebugAndroidTestSources` verdes.
- `./gradlew.bat detekt ktlintCheck lint` verdes (0 violaciones).
- `./gradlew.bat testDebugUnitTest koverVerifyDebug` verdes (cobertura `>= 80%`).
- `python scripts/check_sdd.py` PASS.
- `python scripts/check_layers.py` PASS.
- `python scripts/check_endpoints.py` PASS.
- `python scripts/check_secrets.py` PASS.
- `python scripts/check_coverage.py --min 80` PASS.

## 5. Orden de implementación

1. Crear modelo `FaqItem` y catálogo estático de preguntas frecuentes en `presentation/support/`.
2. Implementar `HelpCenterScreen` (`P-35`) con acordeones `C-49` y enlaces legales.
3. Registrar `Route.HelpCenter` en `NavGraph.kt` y conectar el botón `Centro de ayuda y soporte` en `BuyerProfileScreen.kt` (línea 329).
4. Añadir pruebas Compose UI para expansión de acordeones y navegación desde perfil.

## 6. Prohibiciones

- Prohibido dejar `onClick = {}` vacío en `Centro de ayuda y soporte` (`BuyerProfileScreen.kt`).
- Prohibido lanzar `Intent.ACTION_SENDTO` sin `try/catch (ActivityNotFoundException)`.

## Tareas

Ver `tasks.md`.
