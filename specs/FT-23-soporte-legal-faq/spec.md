# SPEC — FT-23 Centro de Ayuda (FAQs), Legal Offline y Banner de Invitado

> Rector: `.specify/memory/constitution.md` §2, §9 y §11. Plantilla: `.specify/templates/spec-template.md`.
> Drive: **SE-042 (FAQs), SE-043 (Términos offline), SE-044 (Banner invitado)**; Componentes **C-49, C-50**.

## 1. Contexto y objetivo

En `BuyerProfileScreen.kt` la opción `"Centro de ayuda y soporte"` tiene un `onClick = {}` vacío, `TermsScreen.kt` no contiene enlace completo o lectura garantizada offline de políticas de privacidad/cancelación/términos (LFPDPPP, constitución §11), y en modo invitado falta un banner persistente inferior (`C-49`) que invite al usuario a registrarse en 30 segundos para ampliar el radio a 5 km y participar en la red social.

**Objetivo:** implementar la pantalla de Soporte Técnico y Preguntas Frecuentes (`HelpFaqScreen`) con acordeones expandibles accesibles offline, completar la vista de Términos y Aviso de Privacidad legible sin conexión con enlaces a la landing page oficial (`https://rodnix.com.mx/sensus/privacidad.html`), y añadir la barra inferior persistente `'Modo invitado'` (`C-49`) con navegación a registro/login.

**Alcance:** `HelpFaqScreen` (acordeones offline en `strings.xml` / recursos), enlace desde `BuyerProfileScreen` (`"Centro de ayuda y soporte"`), lectura offline en `TermsScreen`, y componente `GuestModeBanner` (`C-49`) visible para sesiones invitadas.
**Fuera de alcance:** chat de soporte en vivo con agente humano o tickets en backend.

## 2. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-23-01 | Conectar `"Centro de ayuda y soporte"` en `BuyerProfileScreen` a `HelpFaqScreen` con acordeones expandibles sobre radio 1–5 km, activación comercial sin INE, reservas QR y privacidad/ARCO. |
| RF-23-02 | Garantizar que el texto completo de Términos, Políticas de Cancelación y Aviso de Privacidad en `TermsScreen` sea legible 100 % sin conexión (recursos embebidos) e incluya enlace externo a `https://rodnix.com.mx/sensus/privacidad.html`. |
| RF-23-03 | En modo invitado, mostrar la barra/banner no intrusivo (`C-49`) destacando beneficios (ampliar radio hasta 5 km, usar buscador ML y comunidad) con CTA directo hacia Registro / Inicio de sesión. |

## 3. Requisitos no funcionales

- **Offline:** tanto FAQs como Términos y Aviso de Privacidad deben abrir y leerse con modo avión activado.
- **Accesibilidad:** los encabezados expandibles del acordeón deben anunciar estado expandido/colapsado (`semantics`) y tener `contentDescription` en iconos vectoriales.

## 4. Arquitectura y contratos internos

```text
presentation/buyer/
  HelpFaqScreen.kt            # Acordeones expandibles offline
  TermsScreen.kt              # Texto legal offline completo + URL rodnix.com.mx
presentation/common/
  GuestModeBanner.kt          # C-49 barra/banner flotante para invitado
presentation/buyer/
  BuyerProfileScreen.kt       # Conecta "Centro de ayuda y soporte"
```

## 5. Criterios de aceptacion

- **Given** el usuario está sin internet y pulsa `"Centro de ayuda y soporte"` **When** abre `HelpFaqScreen` **Then** visualiza los acordeones de preguntas frecuentes y puede expandir/colapsar cada tema.
- **Given** un usuario en modo avión abre `TermsScreen` **When** hace scroll **Then** lee íntegramente el Aviso de Privacidad y el tratamiento de ubicación sin requerir webview obligatorio.
- **Given** una sesión en modo invitado **When** navega por Mapa o Lista **Then** ve el banner de invitado (`C-49`) con acción directa a Registro.

## 6. Plan de pruebas

- **Unitarias:** verificación de carga de ítems FAQ desde proveedor puro / estado de acordeón.
- **UI (Compose Test Rule):** expansión/colapso de acordeón en `HelpFaqScreen`, presencia de texto legal offline en `TermsScreen` y visibilidad/acción de `GuestModeBanner` en modo invitado vs oculto en usuario registrado.

## 7. Riesgos, limites y deuda

- La URL `https://rodnix.com.mx/sensus/privacidad.html` debe mantenerse activa y con TLS válido en la infraestructura Neubox de la landing page.

## Aprobación

| Estado | Revisor | Fecha | Evidencia |
|---|---|---|---|
| Borrador | — | — | Pendiente de ejecución |
