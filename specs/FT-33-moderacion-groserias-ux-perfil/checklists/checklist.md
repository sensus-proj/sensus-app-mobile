# CHECKLIST — FT-33 Filtro local de groserías y ajustes de UX

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + `## Aprobación`
- [x] Alineado con `sensus-back/specs/059-moderacion-texto-filtro-groserias`
- [x] Sin endpoints nuevos no registrados en `docs/endpoints-internos.md`

## Arquitectura y calidad

- [x] `ProfanityFilter` en `domain/` sin imports de Android (`check_layers.py` PASS)
- [x] Sin alteraciones a `OfferCategory`
- [x] Kover >= 80 % y `detekt` / `ktlintCheck` / `lint` en verde
