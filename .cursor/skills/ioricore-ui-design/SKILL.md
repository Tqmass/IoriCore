---
name: ioricore-ui-design
description: Diseño visual de IoriCore/RoyalCTW (menús, scoreboard, TAB, bossbar, MOTD, ban screens, chat format) con doble sistema MODERN 1.16+ / LEGACY 1.8–1.12 y referencias visuales. Usar al diseñar o revisar cualquier salida visual al jugador.
---

# ioricore-ui-design

## Flujo
1. Buscar la feature en `.cursor/docs/IMAGE-INDEX.md` (⚠ ficheros `01`–`14` tienen nombres desplazados: usar la tabla "contenido real").
2. Abrir **solo** 1–3 imágenes relevantes. Si hay que analizar muchas, delegar en el agente `ui-reference-analyst`.
3. Brief y paleta: `.cursor/docs/memory/DESIGN_BRIEF.md`. Separadores/estilo de mensajes: `.cursor/design/` + skills Cursor `yaml-messages-modern`, `yaml-messages-legacy-hcf`.
4. Diseñar identidad **propia** RoyalCTW: tomar estructura/ideas, nunca copiar layout completo de HonlyCraft, Helium, pDiscord, DUELOS, etc.

## Dual
| | MODERN (1.16+) | LEGACY (1.8–1.12) |
|---|---|---|
| Color | hex `<#RRGGBB>`, gradients, MiniMessage | solo `&` |
| Recursos | bossbar, hover rico, small-caps, iconos unicode | sin bossbar (o off), hover simple, MOTD PictureLogin (cara ASCII `PlayerHeadAscii`) |
| Selección | protocolo del **receptor** (ViaVersion si está) vs `SETTINGS.LEGACY-MAX-VERSION` (default 1.12.2 / p340) | |

Nunca asumir que todos los clientes tienen la misma versión: render por jugador.

## Convenciones existentes
- Separador chat `&7&m------------------------------------------&r` · help `&8• &f/cmd &7- &edesc` · bullets `×` · títulos cian.
- Sanciones modern rojo/blanco/negro; alerts staff cian/rosa/amarillo.
- Menús: layout `menus/{modern,legacy}/<menu>.yml` (filas, slots, fillers, sonidos) + textos en messages. Personalización máxima estilo IoriCTW (fillers/decoradores).
- Branding solo RoyalCTW; URLs (tienda/discord) por config.
