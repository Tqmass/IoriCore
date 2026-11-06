# IoriProxy — Catálogo de features

| Feature | Descripción | Persistencia | Redis | Discord |
|---------|-------------|--------------|-------|---------|
| Maintenance | Global/scoped, countdown, whitelist, MOTD | SQL/Mongo | Sync estado | Canal maintenance |
| Lobby / MultiLobby | `/lobby` `/hub`, balanceo, fallback | Config | Opcional | — |
| MOTD | Random + protocol profiles + favicon | Config/DB | Sync | — |
| Friends | Requests, lista interactiva, notify | SQL/Mongo | Presencia | — |
| StaffChat / AdminChat | Canales globales | — | Mensajes | — |
| Staff list | `/staff list` + grupos | Config | Presencia | — |
| Stream | `/stream` `/directo` `/video` | Config | Broadcast | Canal stream |
| Announcements | Por servidor, MiniMessage/hex | Config | Opcional | — |
| Report | Reasons, estados, network | SQL/Mongo | Notify | Webhook reports |
| HelpOp | Tickets, reply/move, history | SQL/Mongo | Notify | Webhook helpop |
| Queue | Prioridades, bypass, target | Redis/mem | Sí | — |
| Ping | Cross-server + placeholders | Mem/Redis | Sí | — |
| Playtime | Total / server / mode | SQL/Mongo | Sesión | — |
| Analytics | Picos, conexiones, medias | SQL | — | — |
| Placeholders | Proxy/player/server/modules | — | — | — |
| Donations + GG Wave | Tebex opcional | Config | Broadcast | Canal donations |
| Blacklist | `/blacklist` `/unblacklist` [-s] | SQL/Mongo | Sync | Canal blacklist |
| TabCompleteFilter | Ocultar sugerencias | Config | — | — |
| Discord status embed | Live players/totals/bans | — | Datos | Canal status |

## Permisos (raíz `ioriproxy`)

Ver `commands.yml`. Ejemplos: `ioriproxy.staff`, `ioriproxy.report.receive`, `ioriproxy.helpop.reply`, `ioriproxy.maintenance.bypass`.

## Plataformas

- Proxy: Velocity 3.x (primario), BungeeCord (adaptador)
- Storage: none / MySQL / MongoDB
- Optional: Redis, Tebex; soft hooks LP/PAPI/LiteBans solo si una feature los necesita
