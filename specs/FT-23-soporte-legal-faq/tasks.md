# TASKS — FT-23 Centro de Ayuda (FAQs), Legal Offline y Banner de Invitado

> 1 PR = 1 slice. Orden: Componentes -> Pantallas -> Integración -> Tests -> Gates.

## Fase A — Pantallas y Componentes

- [ ] T23.1 Crear `HelpFaqScreen.kt` con acordeones expandibles offline cubriendo radio, comercios, QR y LFPDPPP — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T23.2 Conectar `"Centro de ayuda y soporte"` en `BuyerProfileScreen.kt` a `HelpFaqScreen` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T23.3 Completar textos offline de Términos, Cancelación y Privacidad en `TermsScreen.kt` junto a enlace oficial `rodnix.com.mx` — verif: `./gradlew.bat compileDebugKotlin`
- [ ] T23.4 Crear `GuestModeBanner.kt` (`C-49`) y mostrarlo en `MainAppContainer.kt` exclusivamente cuando `isGuest == true` — verif: `./gradlew.bat compileDebugKotlin`

## Fase B — Pruebas y Cierre

- [ ] T23.5 Añadir pruebas Compose UI (`HelpFaqScreenTest`, `TermsScreenTest`, `GuestModeBannerTest`) verificando expansión de acordeones y visibilidad condicionada — verif: `./gradlew.bat compileDebugAndroidTestSources`
- [ ] T23.6 Ejecutar suite unitaria, Kover `>= 80%`, análisis estático y actualizar `Memoria.md` §6 — verif: `python scripts/check_coverage.py --min 80`
