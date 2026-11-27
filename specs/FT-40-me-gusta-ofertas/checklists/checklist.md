# CHECKLIST — FT-40 Me gusta en publicaciones de producto

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + `## Aprobación`
- [x] Diferencia frente a `FT-19` (`POST /api/v1/feed/posts/{id}/like`) documentada
- [x] Endpoints registrados en `docs/endpoints-internos.md`

## Arquitectura y calidad

- [x] `domain/` puro y `presentation/` sin importar `data/` (`check_layers.py` PASS)
- [x] Sin cambios a `OfferCategory` ni creación/edición de ofertas
- [x] Kover >= 80 % y `detekt` / `ktlintCheck` / `lint` en verde
