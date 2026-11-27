# CHECKLIST — FT-41 Actividad de amigos en Comunidad

## Spec y alcance

- [x] `spec.md` con las 7 secciones canónicas + `## Aprobación`
- [x] Subpestañas de `CommunityScreen` limitadas a **Actividad** (`FT-41`) y **Amigos y QR** (`FT-25`)
- [x] Endpoint `GET /api/v1/community/friends-activity` en `docs/endpoints-internos.md`

## Arquitectura y UI

- [x] `domain/` puro y `presentation/` sin importar `data/` (`check_layers.py` PASS)
- [x] Sin tonos amarillos/ámbar en UI interna (§9.2)
- [x] Kover >= 80 % y static analysis en verde
