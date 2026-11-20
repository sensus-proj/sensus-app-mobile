# TASKS — FT-23 Centro de Ayuda (FAQs), Legal Offline y Banner de Invitado

> 1 PR = 1 slice. Orden: Componentes -> Pantallas -> Integración -> Tests -> Gates.

## Fase A — Pantallas y Componentes

- [x] T23.1 Crear `HelpFaqScreen.kt` con acordeones expandibles offline cubriendo radio, comercios, QR y LFPDPPP — verif: `./gradlew.bat compileDebugKotlin`
- [x] T23.2 Conectar `"Centro de ayuda y soporte"` en `BuyerProfileScreen.kt` a `HelpFaqScreen` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T23.3 Completar textos offline de Términos, Cancelación y Privacidad en `TermsScreen.kt` junto a enlace oficial `rodnix.com.mx` — verif: `./gradlew.bat compileDebugKotlin`
- [x] T23.4 Crear `GuestModeBanner.kt` (`C-49`) y mostrarlo en `MainAppContainer.kt` exclusivamente cuando `isGuest == true` — verif: `./gradlew.bat compileDebugKotlin`

## Fase B — Pruebas y Cierre

- [x] T23.5 Añadir pruebas Compose UI (`Ft30Ft20Ft23ComposeTest.kt`) verificando expansión de acordeones, banner invitado y sección LFPDPPP en `TermsScreen` — verif: `./gradlew.bat compileDebugAndroidTestSources` (compilación verificada; ejecución en emulador pendiente)
- [x] T23.6 Ejecutar suite unitaria, Kover `>= 80%`, análisis estático y actualizar `Memoria.md` §6 — verif: `python scripts/check_coverage.py --min 80` (`80.87%`)
- [ ] T23.7 Añadir al Aviso de Privacidad (recurso embebido y landing) la finalidad de personalización del feed y el registro seudonimizado de interacciones (RF-23-04) — verif: recurso embebido en `TermsScreen.kt` y `FaqCatalog.kt` completado; texto de la **landing** web fuera de este repositorio pendiente
