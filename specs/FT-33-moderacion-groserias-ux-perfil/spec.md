# FT-33 — Filtro local de groserías y ajustes de UX (Splash y Perfiles)

| Campo | Valor |
|---|---|
| Feature | `FT-33-moderacion-groserias-ux-perfil` |
| Estado | `En revision` |
| Depende de | `FT-04-auth-roles`, `FT-06-profiles-spatial`, `FT-15-oferta-creacion-comercio`, `FT-26-resenas-comercio` |
| Spec backend | [`059-moderacion-texto-filtro-groserias`](../../../sensus-back/specs/059-moderacion-texto-filtro-groserias/spec.md) |
| Pantalla / Componentes | `ProfanityFilter`, `SplashScreen`, `BuyerProfileScreen`, `MerchantProfileScreen` |

## 1. Contexto y objetivo

Complementar la moderación fail-closed del servidor con un filtro determinístico instantáneo de groserías en cliente (`ProfanityFilter` en `domain/moderation/`) para evitar envíos ofensivos en registro/edición de usuario, registro/edición de comercio, ofertas y reseñas/respuestas; además, asegurar que el splash animado de arranque (`SplashScreen`) se reproduzca silencioso y fluido sin retrasos innecesarios y que las pantallas de perfil (`BuyerProfileScreen` / `MerchantProfileScreen`) respeten los márgenes de la barra de estado (`statusBarsPadding`).

## 2. Usuarios y flujo principal

1. Al iniciar la app, `SplashScreen` reproduce el video introductorio (`R.raw.splash_animation`) en silencio sobre `TextureView`/`MediaPlayer` con escalado `center-crop` y transición inmediata al finalizar o al cumplirse `SPLASH_SAFETY_TIMEOUT_MS`.
2. Al capturar o actualizar texto en registro/perfil de usuario, activación/edición de comercio, creación/edición de oferta o reseñas/respuestas, el caso de uso correspondiente valida mediante `ProfanityFilter.containsProfanity(text)` antes de invocar red.
3. Si detecta lenguaje ofensivo, rechaza inmediatamente con `DEFAULT_REJECTION_MESSAGE`; en cualquier otro caso, el backend aplica su doble barrera fail-closed (`Spec 059`).
4. Las cabeceras de `BuyerProfileScreen` y `MerchantProfileScreen` aplican `statusBarsPadding()` para evitar solapamiento bajo el *notch* o barra superior del sistema.

## 3. Requisitos funcionales

- **`RF-33-01`:** Proveer `ProfanityFilter` puro en `domain/moderation/` con normalización Unicode, decodificación *leetspeak*, compresión de letras repetidas y frases prohibidas en español e inglés.
- **`RF-33-02`:** Aplicar `ProfanityFilter` en casos de uso de registro/perfil de usuario, activación/perfil de comercio, reseñas/respuestas y creación/edición de ofertas.
- **`RF-33-03`:** Reproducir `SplashScreen` mediante `TextureView` + `MediaPlayer` en silencio (`volume = 0f`) con cierre automático al terminar y respaldo `SPLASH_SAFETY_TIMEOUT_MS` sin bloqueos.
- **`RF-33-04`:** Respetar los *insets* del sistema (`statusBarsPadding`) en cabeceras de `BuyerProfileScreen` y `MerchantProfileScreen`.

## 4. Requisitos no funcionales y arquitectura

- `ProfanityFilter` vive en `domain/` sin imports de Android (`check_layers.py` en PASS).
- `SplashScreen` libera correctamente `MediaPlayer` y `Surface` en `DisposableEffect` para evitar fugas de memoria.
- Cobertura unitaria Kover >= 80 %.

## 5. Contratos y navegación

Esta feature refuerza la validación previa del cliente sobre las rutas existentes sin añadir endpoints nuevos (`POST /api/v1/merchants/{merchantId}/reviews`, `POST /api/v1/merchants/reviews/{reviewId}/replies`, `POST /api/v1/offers`, `POST /api/v1/merchants/activate`).

## 6. Criterios de aceptación

- Palabras como `"pito"`, `"puta"`, `"p3nd3j0"` o `"pppputaaa"` son rechazadas en el cliente y en el backend.
- Nombres comerciales y productos legítimos (`"Cafetería Central"`, `"Tamales de pollo"`) pasan el filtro sin falsos positivos.
- El splash se completa limpiamente y las cabeceras de perfil nunca quedan recortadas tras la barra de estado.

## 7. Pruebas requeridas

- `ProfanityFilterTest` (groserías en español/inglés, acentos, *leetspeak*, repeticiones, palabras separadas por símbolos y no falsos positivos).

## Aprobación

| Rol | Responsable | Estado | Fecha |
|---|---|---|---|
| Producto / SDD | Equipo Sensus | Aprobada (alineación `Spec 059`) | 2026-11-19 |
| Android Lead | Equipo Móvil | En revisión de cobertura y gates | 2026-11-19 |
