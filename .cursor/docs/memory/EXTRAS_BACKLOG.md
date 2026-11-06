# Extras backlog (post-H0 lock)

Activables / por fase; no bloquean H1 skeleton.

## Core — chat & formats
- `formats.yml` parts (rank/player/tag/message) con Hover/Click; modern vs legacy por **protocolo del receptor** (ViaVersion).
- Click nick → `SUGGEST_COMMAND /msg {player} `
- Hover rango + hover tag (exclusivo evento/fiesta/torneo)
- Soft-hook **Geyser** (`geyserApi`) para marcar Bedrock en format
- Chat lock/mute + `*.bypass`
- `/settings` menú: idioma, género (módulo OFF default), sonidos
- Donor join broadcast + sound (solo backends Core)
- ChatGames (Core-only, MODULES.CHAT-GAMES, off default)

## Core — essentials-lite
- `/gm` + aliases gmc/gms/gma/gmsp + tab + números 0–3
- Permisos por modo + `*.others`

## Core — logs (filesystem) — PRIORIDAD: por jugador (estilo Helium)
```
plugins/IoriCore/logs/
  players/<name|uuid>/chat.log
  players/<name|uuid>/commands.log
  chat/  chat/donors/  chat/staff/
  blocked-commands/
  staff-actions/
```
- Header: Player / Total / Generated + bloques `----- date -----`
- Línea cmd: `[HH:mm:ss] (server) [rank] name: /cmd [✓ ALLOWED|! BLOCKED]`
- Cap ~100k; purge >2 meses; async append

## Core — ranks (Helium UX / IoriCTW look)
- Menú lista + tooltip completo (prefix/suffix/priority/parents/staff…)
- Soft-hook LP; editor menús (anvil después)
- Fillers/menus.yml personalización IoriCTW

## Core — easter eggs
- Eggs/regalos configurables; progreso por jugador; title + claim; MODULE off default

## Proxy — staff auth
- Auth obligatorio en proxy **solo** si cambio IP/geo vs última sesión
- Discord DM embed (país, IP parcial, botón Approve) vía JDA
- Blind/immobile en Core si llega sin sesión auth

## Punishments & ranks
- Sistema propio + soft-hook LiteBans
- Ranks estilo Helium (ideas; LP soft-hook)

## Storage
- MariaDB/MySQL preferido; **H2** si no hay DB configurada · **H1 ✓** (`CoreSqlStorage`)

## Stubbed in H1 (código base, features incompletas)
- `formats.yml` skeleton Core
- `ModuleRegistry` + `GenderModule` (OFF)
- `SoftGeyserHook` (reflect GeyserApi)
- Abstraction/`VersionWrapper` (ex NmsBridge) anvil/title defaults
