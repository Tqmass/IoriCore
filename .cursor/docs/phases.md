# IoriProxy — Plan de fases (actualizado)

Proyecto: **IoriProxy** · Paquete: `me.tqmass.dev.proxy` · Build: **Gradle** · Java **17**  
Módulos: `common/` · `velocity/` · `bungee/`  
Permisos: `ioriproxy.*` · SQL: MySQL/MariaDB · MongoDB opcional · Redis opcional

## Referencias

- Helium (proxy MOTD/maintenance ideas)
- CoreDub Proxy-API (mensajería tipada; transporte propio, no copiar sockets)
- IoriCTW Hikari/MySQL + Logger/Tasks
- Darling utils (patrones FileConfig/TimeUtils/Logger)
- HonlyCore (player/utilities/announcer layout)
- kennytv/Maintenance (GPL-3 — estudiar patrones, no copiar código)
- Fallback-Server (lobby/fallback/multilobby ideas)
- UI refs: stream, report tabcomplete, helpop, friends, donation GG wave

## Fase 2 — Estructura, config, logger ✅

- Gradle multi-módulo + shadow jars
- Bootstrap Velocity/Bungee
- Configs separadas + languages es/en
- Logger banner + ModuleManager
- Skills + docs (gitignore)

## Fase 3 — Redis + SQL/Mongo + protocolo eventos ✅

- Jedis pool, TLS/ACL, reconexión, pub/sub versionado
- Hikari MySQL + Mongo sync driver
- Migraciones, `/redistest`
- Storage none|mysql|mongodb
- Permisos EN (`ioriproxy.find`, `ioriproxy.request.*`, `ioriproxy.hub`, `ioriproxy.youtube`…)
- Webhooks temporales en `webhooks/*.yml`
- Labels de comando case-insensitive (`/LOBBY` == `/lobby`)

## Fase 4 — Presencia, lobby, transferencias ✅

- Presencia UUID global (common) + listeners solo en velocity/bungee
- `/send`, `/goto`, `/find` (+ `/whereis`), `/servers`
- `/hub` (+ `/lobby`), MultiLobby, fallback kick

## Fase 5 — Maintenance, whitelist, MOTD, blacklist ✅

- Scoped maintenance + countdown + Redis sync
- MOTD profiles (legacy/modern) + maintenance MOTD
- Network blacklist + `/blacklist` `/unblacklist` [-s]

## Fase 6 — Staff tools + chat + stream + anuncios ✅

- StaffChat / AdminChat (toggle + Redis + intercept chat)
- `/staff list` (`ioriproxy.staff` + grupos en settings)
- `/stream` `/directo` `/youtube` `/video` (legacy protocol aware)
- Announcements timer + MiniMessage
- TabCompleteFilter + command blocker

## Fase 6b — Webhook HTTP sender ✅

- `WebhookClient` POST async (Java 17 HttpClient) + helpers en `WebhookRegistry`

## Fase 7 — Report, HelpOp, Discord, donations ✅

- `/report` reasons + pending/resolved + network notify
- `/helpop` alias `/request` + [MOVE]/[REPLY]
- Discord webhooks por feature + status embed
- Tebex opcional + GG Wave

## Fase 8 — Playtime, analytics, placeholders, ping, queue ✅

- Playtime total/servidor/modo (+ SQL `ip_playtime`, memoria si SQL cae)
- Analytics joins/peak + SQL diario `ip_analytics` v3
- Placeholders `%ioriproxy_*%` internos
- `/ping` local + presencia remota
- `/queue` prioridades por permiso + bypass

## Fase 9 — Friends ✅

- `FriendService` + comandos + SQL/Redis + memoria
- Upsert `ip_players` (IP/jugadores) puede aterrizar desde otro agente en paralelo

## Fase 10 — Docs + empaquetado ✅

- **Tests:** eliminados al cerrar fases (sin JUnit ni `src/test`)
- **Docs locales:** `docs/INSTALL.md`, `docs/JAVA_REPORT.md`
- **Shadow jars:** `IoriProxy-Velocity` / `IoriProxy-Bungee`
- **Sin PacketEvents.** Hooks LP / PAPI / LiteBans solo soft y cuando una feature los necesite.

## Regla

No avanzar a fase dependiente si la anterior no compila.
