# CHECKLIST — FT-40 Me gusta en publicaciones de producto

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + `## Aprobación`
- [x] Contrato `POST /api/v1/offers/{id}/like` documentado
- [x] Endpoints registrados en `docs/endpoints-internos.md`

## Arquitectura y calidad

- [x] `domain/` puro y `presentation/` sin importar `data/` (`check_layers.py` PASS)
- [x] Sin cambios a `OfferCategory` ni creación/edición de ofertas
- [x] Kover >= 80 % y `detekt` / `ktlintCheck` / `lint` en verde
